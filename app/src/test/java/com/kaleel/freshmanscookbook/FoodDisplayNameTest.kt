package com.kaleel.freshmanscookbook

import com.kaleel.freshmanscookbook.data.*
import org.junit.Assert.assertEquals
import org.junit.Test

class FoodDisplayNameTest {
    @Test
    fun userFacingNameDoesNotReplaceAuthoritativeDescription() {
        val (_, generated) = CustomFoodInput(
            name = "Temporary",
            servingQuantity = 100.0,
            servingUnit = IngredientUnit.G
        ).toEntities(now = 1L)
        val food = generated.copy(
            name = "Nuts, walnuts, English, halves, raw",
            displayName = "Walnuts",
            foodSource = FoodSource.USDA
        )

        assertEquals("Walnuts", food.userFacingName)
        assertEquals("Nuts, walnuts, English, halves, raw", food.name)
    }

    @Test
    fun customFoodNameRemainsItsDisplayName() {
        val (_, food) = CustomFoodInput(
            name = "My protein blend",
            servingQuantity = 1.0,
            servingUnit = IngredientUnit.SCOOP,
            servingGrams = 30.0
        ).toEntities(now = 1L)

        assertEquals("My protein blend", food.userFacingName)
    }
}
