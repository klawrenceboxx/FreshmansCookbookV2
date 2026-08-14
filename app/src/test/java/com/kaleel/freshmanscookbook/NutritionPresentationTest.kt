package com.kaleel.freshmanscookbook

import com.kaleel.freshmanscookbook.data.*
import org.junit.Assert.*
import org.junit.Test

class NutritionPresentationTest {
    @Test
    fun consumedServingNutritionKeepsRecipeYieldSeparate() {
        val whole = NutritionTotals(caloriesKcal = 800.0, proteinG = 40.0)

        val oneAndHalfServings = NutritionPresentation.forConsumedServings(whole, recipeYield = 4, servingsConsumed = 1.5)

        assertEquals(300.0, oneAndHalfServings.caloriesKcal, 0.001)
        assertEquals(15.0, oneAndHalfServings.proteinG, 0.001)
    }

    @Test
    fun projectedPercentageCanCommunicateMoreThanOneHundredPercent() {
        assertEquals(140, NutritionPresentation.percent(projected = 140.0, target = 100.0))
    }

    @Test
    fun progressRailCapsAtOneHundredPercent() {
        assertEquals(1.0f, NutritionPresentation.progressFraction(projected = 140.0, target = 100.0))
        assertEquals(0.25f, NutritionPresentation.progressFraction(projected = 25.0, target = 100.0))
    }

    @Test
    fun missingOrInvalidTargetDoesNotCreateMisleadingProgress() {
        assertNull(NutritionPresentation.percent(projected = 20.0, target = null))
        assertNull(NutritionPresentation.progressFraction(projected = 20.0, target = 0.0))
    }

    @Test
    fun customFoodMissingMicronutrientRemainsUnknown() {
        val (_, food) = CustomFoodInput(
            name = "Protein powder",
            servingQuantity = 1.0,
            servingUnit = IngredientUnit.SCOOP,
            servingGrams = 30.0,
            nutrients = mapOf(NutrientKey.PROTEIN to 24.0)
        ).toEntities(now = 1L)
        val ingredient = MealIngredient.quickAdd(
            name = food.name,
            quantity = 1.0,
            unit = IngredientUnit.SCOOP,
            order = 0,
            foodId = food.foodId,
            gramsEquivalent = 30.0
        )
        val meal = MealInstance("meal", null, "Shake", 1, 1.0, listOf(ingredient), 1L)
        val foods = mapOf(food.foodId to food)

        assertTrue(NutritionPresentation.isKnown(meal, foods, NutrientKey.PROTEIN))
        assertFalse(NutritionPresentation.isKnown(meal, foods, NutrientKey.MAGNESIUM))
    }

    @Test
    fun unresolvedIngredientMakesDisplayedNutrientUnknown() {
        val (_, food) = CustomFoodInput(
            name = "Powder",
            servingQuantity = 1.0,
            servingUnit = IngredientUnit.SCOOP,
            servingGrams = 30.0,
            nutrients = mapOf(NutrientKey.PROTEIN to 24.0)
        ).toEntities(now = 1L)
        val unresolved = MealIngredient.quickAdd(
            name = food.name,
            quantity = 1.0,
            unit = IngredientUnit.SCOOP,
            order = 0,
            foodId = food.foodId,
            gramsEquivalent = null
        )
        val meal = MealInstance("meal", null, "Shake", 1, 1.0, listOf(unresolved), 1L)

        assertFalse(NutritionPresentation.isKnown(meal, mapOf(food.foodId to food), NutrientKey.PROTEIN))
    }

    @Test
    fun partialNutrientPreservesKnownSubtotal() {
        val (_, food) = CustomFoodInput(
            name = "Chia seeds",
            servingQuantity = 100.0,
            servingUnit = IngredientUnit.G,
            nutrients = mapOf(NutrientKey.PROTEIN to 20.0)
        ).toEntities(now = 1L)
        val known = MealIngredient.quickAdd("Chia seeds", 10.0, IngredientUnit.G, 0, food.foodId, 10.0)
        val unresolved = MealIngredient.quickAdd("Mystery powder", 1.0, IngredientUnit.SCOOP, 1, null, null)
        val report = NutritionCompletenessCalculator.forMeal(
            MealInstance("meal", null, "Pudding", 1, 1.0, listOf(known, unresolved), 1L),
            mapOf(food.foodId to food)
        )

        assertEquals(2.0, report.value(NutrientKey.PROTEIN).amount, 0.001)
        assertEquals(NutrientCompleteness.PARTIAL, report.value(NutrientKey.PROTEIN).completeness)
    }

    @Test
    fun confirmedZeroIsCompleteAndDistinctFromUnknown() {
        val (_, zeroFood) = CustomFoodInput(
            name = "Zero protein drink",
            servingQuantity = 100.0,
            servingUnit = IngredientUnit.G,
            nutrients = mapOf(NutrientKey.PROTEIN to 0.0)
        ).toEntities(now = 1L)
        val ingredient = MealIngredient.quickAdd(zeroFood.name, 100.0, IngredientUnit.G, 0, zeroFood.foodId, 100.0)
        val report = NutritionCompletenessCalculator.forMeal(
            MealInstance("meal", null, "Drink", 1, 1.0, listOf(ingredient), 1L),
            mapOf(zeroFood.foodId to zeroFood)
        )

        assertEquals(0.0, report.value(NutrientKey.PROTEIN).amount, 0.0)
        assertEquals(NutrientCompleteness.COMPLETE, report.value(NutrientKey.PROTEIN).completeness)
        assertEquals(NutrientCompleteness.UNKNOWN, report.value(NutrientKey.MAGNESIUM).completeness)
    }

    @Test
    fun removingLogRecalculatesAggregateWithoutSubtractingUiState() {
        fun report(calories: Double) = NutritionCompletenessReport(
            NutritionTotals(caloriesKcal = calories),
            NutrientKey.entries.associateWith { NutrientCompleteness.COMPLETE }
        )
        val before = NutritionCompletenessCalculator.aggregate(listOf(report(300.0), report(125.0)))
        val after = NutritionCompletenessCalculator.aggregate(listOf(report(300.0)))

        assertEquals(425.0, before.totals.caloriesKcal, 0.0)
        assertEquals(300.0, after.totals.caloriesKcal, 0.0)
    }
}
