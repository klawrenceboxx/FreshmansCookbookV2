package com.kaleel.freshmanscookbook

import com.kaleel.freshmanscookbook.data.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class NutritionFlowTest {
    private val food = FoodEntity(
        foodId = "usda-1",
        name = "Test food",
        searchName = "test food",
        category = FoodCategory.OTHER,
        caloriesKcal = 200.0,
        proteinG = 20.0,
        carbohydrateG = 10.0,
        fatG = 5.0,
        fiberG = 2.0,
        totalSugarsG = null,
        calciumMg = null,
        ironMg = null,
        magnesiumMg = null,
        phosphorusMg = null,
        potassiumMg = null,
        sodiumMg = null,
        zincMg = null,
        copperMg = null,
        manganeseMg = null,
        seleniumMcg = null,
        vitaminAMcgRae = null,
        vitaminCMg = null,
        vitaminDMcg = null,
        vitaminEMg = null,
        vitaminKMcg = null,
        thiaminB1Mg = null,
        riboflavinB2Mg = null,
        niacinB3Mg = null,
        pantothenicAcidB5Mg = null,
        vitaminB6Mg = null,
        folateMcg = null,
        folateMcgDfe = null,
        vitaminB12Mcg = null,
        cholineMg = null,
        saturatedFatG = null,
        monounsaturatedFatG = null,
        polyunsaturatedFatG = null,
        cholesterolMg = null,
        source = "USDA",
        sourceFoodId = "1"
    )

    private fun recipe() = Recipe(
        id = "recipe-1",
        name = "Dinner",
        category = RecipeCategory.DINNER,
        servings = 4,
        imagePath = null,
        ingredients = listOf(
            Ingredient("ingredient-1", "Test food", 400.0, IngredientUnit.G, 0, food.foodId, 400.0),
            Ingredient("ingredient-2", "Unknown", 1.0, IngredientUnit.PIECE, 1, null, null)
        ),
        steps = emptyList(),
        createdAt = 1L,
        updatedAt = 1L
    )

    @Test
    fun temporaryMealEditsDoNotMutateRecipe() {
        val recipe = recipe()
        val meal = MealInstance.fromRecipe(recipe)
        val edited = MealSession.updateAmount(meal, meal.ingredients.first().id, 200.0, IngredientUnit.G, 200.0)

        assertEquals(400.0, recipe.ingredients.first().gramsEquivalent!!, 0.0)
        assertEquals(200.0, edited.ingredients.first().gramsEquivalent!!, 0.0)
        assertNotEquals(recipe.ingredients.first().id, meal.ingredients.first().id)
    }

    @Test
    fun checkedIngredientsAndConsumedServingsControlLoggedTotals() {
        var meal = MealInstance.fromRecipe(recipe())
        meal = MealSession.setChecked(meal, meal.ingredients.first().id, true)
        meal = MealSession.setServingsConsumed(meal, 1.0)

        val totals = MealNutritionCalculator.consumedTotals(meal, mapOf(food.foodId to food))

        // 400 g prepared = 80 g protein; one of four servings = 20 g.
        assertEquals(20.0, totals.proteinG, 0.0001)
        assertEquals(200.0, totals.caloriesKcal, 0.0001)
    }

    @Test
    fun forecastIncludesPlannedUncheckedIngredient() {
        val meal = MealSession.setServingsConsumed(MealInstance.fromRecipe(recipe()), 1.0)
        assertTrue(meal.checkedIngredientIds.isEmpty())

        val result = NutritionForecast.calculate(NutritionTotals(proteinG = 10.0), meal, mapOf(food.foodId to food), null)

        assertEquals(20.0, result.meal.proteinG, 0.0001)
        assertEquals(30.0, result.projected.proteinG, 0.0001)
    }

    @Test
    fun localDayBoundsFollowZoneCalendarIncludingDst() {
        val zone = ZoneId.of("America/Toronto")
        val bounds = CookbookViewModel.localDayBounds(LocalDate.of(2026, 3, 8), zone)

        assertEquals(23L * 60L * 60L * 1000L, bounds.end - bounds.start)
    }

    @Test
    fun nutritionGoalDoesNotChangeMaintenanceCalories() {
        val base = NutritionProfile(25, BiologicalSex.MALE, 180.0, 80.0, ActivityLevel.MODERATELY_ACTIVE)
        val lose = NutritionTargets.calculate(base.copy(goal = NutritionGoal.LOSE_WEIGHT))[NutrientKey.CALORIES]!!.amount
        val gain = NutritionTargets.calculate(base.copy(goal = NutritionGoal.GAIN_WEIGHT))[NutrientKey.CALORIES]!!.amount

        assertEquals(lose, gain, 0.0)
    }
}
