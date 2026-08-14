package com.kaleel.freshmanscookbook

import com.kaleel.freshmanscookbook.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FoodConversionTest {
    private fun portion(unit: IngredientUnit, grams: Double, description: String) =
        FoodPortionEntity(foodId = "food", unit = unit, description = description, gramsPerUnit = grams)

    @Test
    fun nativeUsdaPortionConvertsTablespoons() {
        assertEquals(14.0, resolveGrams(2.0, IngredientUnit.TBSP, listOf(portion(IngredientUnit.TBSP, 7.0, "1 tablespoon")))!!, 0.0001)
    }

    @Test
    fun enrichedUsdaPortionsConvertTeaspoonsAndCups() {
        val portions = listOf(
            portion(IngredientUnit.TSP, 3.5, "1 teaspoon"),
            portion(IngredientUnit.CUP, 168.0, "1 cup")
        )
        assertEquals(7.0, resolveGrams(2.0, IngredientUnit.TSP, portions)!!, 0.0001)
        assertEquals(42.0, resolveGrams(0.25, IngredientUnit.CUP, portions)!!, 0.0001)
    }

    @Test
    fun savedIngredientWithNullGramsIsBackfilledWhenAuthoritativePortionExists() = runBlocking {
        val meal = mealWith(grams = null)
        val resolved = resolveMissingGramEquivalents(meal) { _, quantity, _ -> quantity!! * 7.0 }
        assertEquals(14.0, resolved.ingredients.single().gramsEquivalent!!, 0.0001)
        assertTrue(resolved.unresolvedNutritionIngredientIds().isEmpty())
    }

    @Test
    fun missingAuthoritativeConversionRemainsExplicitlyUnresolved() = runBlocking {
        val unresolved = resolveMissingGramEquivalents(mealWith(grams = null)) { _, _, _ -> null }
        assertNull(unresolved.ingredients.single().gramsEquivalent)
        assertEquals(setOf("ingredient"), unresolved.unresolvedNutritionIngredientIds())
    }

    @Test
    fun ambiguousPortionsDoNotSilentlyChooseAWeight() {
        val portions = listOf(
            portion(IngredientUnit.CUP, 100.0, "1 cup, chopped"),
            portion(IngredientUnit.CUP, 140.0, "1 cup, whole")
        )
        assertNull(resolveGrams(1.0, IngredientUnit.CUP, portions))
    }

    private fun mealWith(grams: Double?) = MealInstance(
        id = "meal", recipeId = "recipe", recipeName = "Recipe", recipeServings = 1,
        servingsConsumed = 1.0,
        ingredients = listOf(
            MealIngredient(
                id = "ingredient", sourceIngredientId = "saved", name = "Food", quantity = 2.0,
                unit = IngredientUnit.TBSP, order = 0, foodId = "food", gramsEquivalent = grams,
                isChecked = false
            )
        ),
        startedAt = 1L
    )
}
