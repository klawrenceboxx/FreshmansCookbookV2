package com.kaleel.freshmanscookbook.data

import java.util.UUID

/**
 * A temporary, editable copy of a saved recipe for one cooking/eating session.
 *
 * MealInstance is deliberately not a Room entity yet. It represents working
 * state that can be changed without modifying the canonical Recipe.
 *
 * Later stages can persist/log the final state separately.
 */
data class MealInstance(
    val id: String,
    val recipeId: String?,
    val recipeName: String,
    val recipeServings: Int,
    val servingsConsumed: Double,
    val ingredients: List<MealIngredient>,
    val startedAt: Long
) {
    /**
     * All ingredients currently present in the temporary meal.
     *
     * Useful for pre-meal nutrition forecasting, where the intended meal should
     * count even before cooking checkboxes have been completed.
     */
    val forecastIngredientIds: Set<String>
        get() = ingredients.mapTo(linkedSetOf()) { it.id }

    /**
     * Only ingredients confirmed via the cooking checkbox.
     *
     * This is the set that should eventually be used for actual logged
     * nutrition so skipped ingredients do not count.
     */
    val checkedIngredientIds: Set<String>
        get() = ingredients
            .asSequence()
            .filter { it.isChecked }
            .mapTo(linkedSetOf()) { it.id }

    /**
     * Scale a full-recipe nutrition total to the amount actually consumed.
     *
     * Example:
     * recipeServings = 4, servingsConsumed = 1.5 -> scale = 0.375
     */
    fun consumedServingScale(): Double {
        if (recipeServings <= 0 || servingsConsumed <= 0.0) return 0.0
        return servingsConsumed / recipeServings.toDouble()
    }

    fun updateIngredient(updated: MealIngredient): MealInstance =
        copy(
            ingredients = ingredients.map { current ->
                if (current.id == updated.id) updated else current
            }
        )

    fun addIngredient(ingredient: MealIngredient): MealInstance =
        copy(
            ingredients = ingredients + ingredient.copy(
                order = ingredients.size
            )
        )

    fun removeIngredient(ingredientId: String): MealInstance =
        copy(
            ingredients = ingredients
                .filterNot { it.id == ingredientId }
                .mapIndexed { index, ingredient -> ingredient.copy(order = index) }
        )

    companion object {
        /**
         * Creates an independent working meal from a saved recipe.
         *
         * Each MealIngredient receives a new ID so temporary meal edits are
         * isolated from the Recipe. sourceIngredientId preserves the link back
         * to the original recipe ingredient.
         */
        fun fromRecipe(
            recipe: Recipe,
            startedAt: Long = System.currentTimeMillis()
        ): MealInstance = MealInstance(
            id = UUID.randomUUID().toString(),
            recipeId = recipe.id,
            recipeName = recipe.name,
            recipeServings = recipe.servings.coerceAtLeast(1),
            servingsConsumed = 1.0,
            ingredients = recipe.ingredients.mapIndexed { index, ingredient ->
                MealIngredient.fromRecipeIngredient(
                    ingredient = ingredient,
                    order = index
                )
            },
            startedAt = startedAt
        )
    }
}

/**
 * One ingredient inside a temporary MealInstance.
 *
 * sourceIngredientId is null for quick-added foods that were not part of the
 * saved recipe.
 *
 * gramsEquivalent represents the current temporary amount in grams. When a
 * quantity/unit edit is made later, the UI/domain layer should recalculate this
 * value from the linked USDA food/portion rather than changing the Recipe.
 */
data class MealIngredient(
    val id: String,
    val sourceIngredientId: String?,
    val name: String,
    val quantity: Double?,
    val unit: IngredientUnit,
    val order: Int,
    val foodId: String?,
    val gramsEquivalent: Double?,
    val isChecked: Boolean
) {
    /**
     * Converts temporary meal state into the existing Ingredient calculation
     * shape. This lets NutritionCalculator work with meal ingredients without
     * introducing Android, Room, or Compose dependencies into the calculator.
     */
    fun toIngredient(): Ingredient = Ingredient(
        id = id,
        name = name,
        quantity = quantity,
        unit = unit,
        order = order,
        foodId = foodId,
        gramsEquivalent = gramsEquivalent
    )

    companion object {
        fun fromRecipeIngredient(
            ingredient: Ingredient,
            order: Int = ingredient.order
        ): MealIngredient = MealIngredient(
            id = UUID.randomUUID().toString(),
            sourceIngredientId = ingredient.id,
            name = ingredient.name,
            quantity = ingredient.quantity,
            unit = ingredient.unit,
            order = order,
            foodId = ingredient.foodId,
            gramsEquivalent = ingredient.gramsEquivalent,
            isChecked = false
        )

        /**
         * Factory for a temporary ingredient added while cooking.
         *
         * foodId and gramsEquivalent may remain null for an intentionally
         * unlinked/custom ingredient. Such an ingredient can still appear in
         * the meal but will contribute no USDA nutrition until linked.
         */
        fun quickAdd(
            name: String,
            quantity: Double?,
            unit: IngredientUnit,
            order: Int,
            foodId: String? = null,
            gramsEquivalent: Double? = null,
            isChecked: Boolean = true
        ): MealIngredient = MealIngredient(
            id = UUID.randomUUID().toString(),
            sourceIngredientId = null,
            name = name,
            quantity = quantity,
            unit = unit,
            order = order,
            foodId = foodId,
            gramsEquivalent = gramsEquivalent,
            isChecked = isChecked
        )
    }
}
