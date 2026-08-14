package com.kaleel.freshmanscookbook.data

/**
 * Pure meal-session editing operations.
 *
 * These helpers intentionally return new MealInstance values instead of
 * mutating a saved Recipe or depending on Compose state. A UI can therefore
 * hold:
 *
 *     var meal by remember { mutableStateOf(MealInstance.fromRecipe(recipe)) }
 *
 * and replace meal with the result of these functions.
 *
 * No Room, Android, or Compose dependencies belong here.
 */
object MealSession {

    fun toggleChecked(
        meal: MealInstance,
        ingredientId: String
    ): MealInstance {
        val ingredient = meal.ingredients.firstOrNull { it.id == ingredientId }
            ?: return meal

        return meal.updateIngredient(
            ingredient.copy(isChecked = !ingredient.isChecked)
        )
    }

    fun setChecked(
        meal: MealInstance,
        ingredientId: String,
        checked: Boolean
    ): MealInstance {
        val ingredient = meal.ingredients.firstOrNull { it.id == ingredientId }
            ?: return meal

        if (ingredient.isChecked == checked) return meal

        return meal.updateIngredient(
            ingredient.copy(isChecked = checked)
        )
    }

    fun resetChecked(meal: MealInstance): MealInstance =
        meal.copy(
            ingredients = meal.ingredients.map {
                if (it.isChecked) it.copy(isChecked = false) else it
            }
        )

    fun checkAll(meal: MealInstance): MealInstance =
        meal.copy(
            ingredients = meal.ingredients.map {
                if (!it.isChecked) it.copy(isChecked = true) else it
            }
        )

    /**
     * Changes the amount of a temporary meal ingredient without touching the
     * saved Recipe.
     *
     * gramsEquivalent should be the newly resolved gram amount for the edited
     * quantity/unit. Pass null when the new amount cannot yet be converted
     * defensibly.
     */
    fun updateAmount(
        meal: MealInstance,
        ingredientId: String,
        quantity: Double?,
        unit: IngredientUnit,
        gramsEquivalent: Double?
    ): MealInstance {
        val ingredient = meal.ingredients.firstOrNull { it.id == ingredientId }
            ?: return meal

        return meal.updateIngredient(
            ingredient.copy(
                quantity = quantity,
                unit = unit,
                gramsEquivalent = gramsEquivalent
            )
        )
    }

    /**
     * Relinks a temporary ingredient to a USDA food.
     *
     * This is useful when a custom/unlinked recipe ingredient is resolved while
     * cooking. The canonical Recipe remains unchanged.
     */
    fun linkFood(
        meal: MealInstance,
        ingredientId: String,
        foodId: String?,
        gramsEquivalent: Double?
    ): MealInstance {
        val ingredient = meal.ingredients.firstOrNull { it.id == ingredientId }
            ?: return meal

        return meal.updateIngredient(
            ingredient.copy(
                foodId = foodId,
                gramsEquivalent = gramsEquivalent
            )
        )
    }

    fun renameIngredient(
        meal: MealInstance,
        ingredientId: String,
        name: String
    ): MealInstance {
        val ingredient = meal.ingredients.firstOrNull { it.id == ingredientId }
            ?: return meal

        return meal.updateIngredient(
            ingredient.copy(name = name.trim())
        )
    }

    /**
     * Adds an ingredient only to this meal session.
     *
     * Quick-added ingredients default to checked because the user is explicitly
     * adding them while cooking. The caller may override that behavior.
     */
    fun quickAdd(
        meal: MealInstance,
        name: String,
        quantity: Double?,
        unit: IngredientUnit,
        foodId: String? = null,
        gramsEquivalent: Double? = null,
        checked: Boolean = true
    ): MealInstance {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return meal

        return meal.addIngredient(
            MealIngredient.quickAdd(
                name = trimmedName,
                quantity = quantity,
                unit = unit,
                order = meal.ingredients.size,
                foodId = foodId,
                gramsEquivalent = gramsEquivalent,
                isChecked = checked
            )
        )
    }

    fun removeIngredient(
        meal: MealInstance,
        ingredientId: String
    ): MealInstance =
        meal.removeIngredient(ingredientId)

    fun setServingsConsumed(
        meal: MealInstance,
        servingsConsumed: Double
    ): MealInstance =
        meal.copy(servingsConsumed = servingsConsumed.coerceAtLeast(0.0))

    /**
     * Ingredients to use for an "If I eat this" forecast.
     *
     * Forecast intentionally includes the whole planned meal regardless of
     * checkbox state because cooking checkboxes describe what was actually used,
     * not what is planned before eating.
     */
    fun forecastIngredients(meal: MealInstance): List<Ingredient> =
        meal.ingredients.map { it.toIngredient() }

    /**
     * Ingredients to use when logging what was actually consumed.
     *
     * Anything left unchecked is excluded.
     */
    fun consumedIngredients(meal: MealInstance): List<Ingredient> =
        meal.ingredients
            .filter { it.isChecked }
            .map { it.toIngredient() }

    fun checkedCount(meal: MealInstance): Int =
        meal.ingredients.count { it.isChecked }

    fun allIngredientsChecked(meal: MealInstance): Boolean =
        meal.ingredients.isNotEmpty() && meal.ingredients.all { it.isChecked }
}
