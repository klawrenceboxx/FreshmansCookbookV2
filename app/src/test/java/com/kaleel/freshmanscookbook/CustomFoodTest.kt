package com.kaleel.freshmanscookbook

import com.kaleel.freshmanscookbook.data.*
import com.kaleel.freshmanscookbook.ui.moved
import com.kaleel.freshmanscookbook.ui.validateRecipeDraft
import org.junit.Assert.*
import org.junit.Test

class CustomFoodTest {
    private val whey = CustomFoodInput(
        name = "Whey Protein",
        servingQuantity = 1.0,
        servingUnit = IngredientUnit.SCOOP,
        servingGrams = 32.0,
        nutrients = mapOf(
            NutrientKey.CALORIES to 120.0,
            NutrientKey.PROTEIN to 24.0,
            NutrientKey.CARBOHYDRATE to 3.0,
            NutrientKey.FAT to 2.0
        )
    )

    @Test
    fun customServingResolvesOneAndHalfScoopsTo48Grams() {
        val (custom, _) = whey.toEntities(now = 1L)
        val portions = listOf(
            FoodPortionEntity(
                foodId = custom.foodId,
                unit = IngredientUnit.SCOOP,
                description = "1 scoop",
                gramsPerUnit = custom.servingGrams!! / custom.servingQuantity
            )
        )
        assertEquals(48.0, resolveGrams(1.5, IngredientUnit.SCOOP, portions)!!, 0.0001)
    }

    @Test
    fun customNutritionUsesExistingGramBasedCalculator() {
        val (_, food) = whey.toEntities(now = 1L)
        val ingredient = MealIngredient(
            id = "ingredient", sourceIngredientId = null, name = food.name,
            quantity = 1.5, unit = IngredientUnit.SCOOP, order = 0,
            foodId = food.foodId, gramsEquivalent = 48.0, isChecked = true
        )
        val totals = MealNutritionCalculator.ingredientTotals(ingredient, mapOf(food.foodId to food))
        assertEquals(180.0, totals.caloriesKcal, 0.0001)
        assertEquals(36.0, totals.proteinG, 0.0001)
    }

    @Test
    fun missingCustomNutrientsRemainUnknownInsteadOfZero() {
        val (custom, food) = whey.toEntities(now = 1L)
        assertNull(custom.fiberG)
        assertNull(food.fiberG)
        assertNull(food.calciumMg)
    }

    @Test
    fun customAndUsdaSourcesAreExplicit() {
        val (_, customFood) = whey.toEntities(now = 1L)
        assertEquals(FoodSource.CUSTOM, customFood.foodSource)
        assertTrue(customFood.foodId.startsWith("custom:"))

        assertEquals(FoodSource.USDA, testUsdaFood().foodSource)
    }

    @Test
    fun ingredientReorderingIsReflectedByPersistedSortOrderMapping() {
        val reordered = listOf("walnuts", "chia", "milk").moved(0, 2)
        val relations = RecipeWithItems(
            recipe = RecipeEntity("recipe", "Pudding", RecipeCategory.BREAKFAST, 2, null, 1L, 1L),
            ingredients = reordered.mapIndexed { index, name ->
                IngredientEntity(name, "recipe", name, 1.0, IngredientUnit.TBSP, index)
            }.reversed(),
            steps = emptyList()
        )
        assertEquals(listOf("chia", "milk", "walnuts"), relations.toModel().ingredients.map { it.name })
    }

    @Test
    fun finalValidationDoesNotBlockSectionInspection() {
        val issue = validateRecipeDraft(RecipeDraft())
        assertEquals(0, issue?.first)
        assertTrue(issue?.second?.contains("name") == true)
    }

    private fun testUsdaFood() = FoodEntity(
        foodId = "usda", name = "Walnuts", searchName = "walnuts", category = FoodCategory.NUTS_SEEDS,
        caloriesKcal = null, proteinG = null, carbohydrateG = null, fatG = null, fiberG = null,
        totalSugarsG = null, calciumMg = null, ironMg = null, magnesiumMg = null, phosphorusMg = null,
        potassiumMg = null, sodiumMg = null, zincMg = null, copperMg = null, manganeseMg = null,
        seleniumMcg = null, vitaminAMcgRae = null, vitaminCMg = null, vitaminDMcg = null,
        vitaminEMg = null, vitaminKMcg = null, thiaminB1Mg = null, riboflavinB2Mg = null,
        niacinB3Mg = null, pantothenicAcidB5Mg = null, vitaminB6Mg = null, folateMcg = null,
        folateMcgDfe = null, vitaminB12Mcg = null, cholineMg = null, saturatedFatG = null,
        monounsaturatedFatG = null, polyunsaturatedFatG = null, cholesterolMg = null,
        source = "USDA", sourceFoodId = "usda"
    )
}
