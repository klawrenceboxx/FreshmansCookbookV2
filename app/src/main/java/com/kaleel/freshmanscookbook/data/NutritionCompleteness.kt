package com.kaleel.freshmanscookbook.data

/** How much the app can truthfully claim about one nutrient amount. */
enum class NutrientCompleteness {
    /** Every included source reports this nutrient. A numeric zero is meaningful. */
    COMPLETE,

    /** At least one source reports the nutrient and at least one does not. */
    PARTIAL,

    /** No included source reports a useful value for this nutrient. */
    UNKNOWN
}

data class NutrientValue(
    val amount: Double,
    val completeness: NutrientCompleteness
) {
    val hasKnownValue: Boolean
        get() = completeness != NutrientCompleteness.UNKNOWN

    val isPartial: Boolean
        get() = completeness == NutrientCompleteness.PARTIAL
}

data class NutritionCompletenessReport(
    val totals: NutritionTotals,
    val completeness: Map<NutrientKey, NutrientCompleteness>
) {
    fun value(nutrient: NutrientKey): NutrientValue = NutrientValue(
        amount = NutritionCalculator.valueFor(totals, nutrient),
        completeness = completeness[nutrient] ?: NutrientCompleteness.UNKNOWN
    )

    val knownMask: Long
        get() = NutrientKnowledgeMask.maskOf(
            completeness.filterValues { it != NutrientCompleteness.UNKNOWN }.keys
        )

    val completeMask: Long
        get() = NutrientKnowledgeMask.maskOf(
            completeness.filterValues { it == NutrientCompleteness.COMPLETE }.keys
        )
}

/**
 * Compact persistence representation for the stable [NutrientKey] enum.
 * `knownMask` means at least one source supplied a value; `completeMask` means
 * every included source supplied one. With 32 nutrients both fit in a Long.
 */
object NutrientKnowledgeMask {
    val all: Long = NutrientKey.entries.fold(0L) { mask, key -> mask or bit(key) }

    fun bit(nutrient: NutrientKey): Long = 1L shl nutrient.ordinal

    fun maskOf(nutrients: Iterable<NutrientKey>): Long =
        nutrients.fold(0L) { mask, nutrient -> mask or bit(nutrient) }

    fun completeness(
        nutrient: NutrientKey,
        knownMask: Long,
        completeMask: Long
    ): NutrientCompleteness = when {
        completeMask and bit(nutrient) != 0L -> NutrientCompleteness.COMPLETE
        knownMask and bit(nutrient) != 0L -> NutrientCompleteness.PARTIAL
        else -> NutrientCompleteness.UNKNOWN
    }
}

object NutritionCompletenessCalculator {
    fun forMeal(
        meal: MealInstance,
        foodsById: Map<String, FoodEntity>,
        checkedOnly: Boolean = false,
        applyConsumedServingScale: Boolean = false
    ): NutritionCompletenessReport {
        val ingredients = meal.ingredients.filter { !checkedOnly || it.isChecked }
        val baseTotals = if (checkedOnly) {
            MealNutritionCalculator.checkedTotals(meal, foodsById)
        } else {
            MealNutritionCalculator.plannedTotals(meal, foodsById)
        }
        val totals = if (applyConsumedServingScale) {
            NutritionCalculator.multiply(baseTotals, meal.consumedServingScale())
        } else {
            baseTotals
        }

        return NutritionCompletenessReport(
            totals = totals,
            completeness = NutrientKey.entries.associateWith { nutrient ->
                completenessForIngredients(ingredients, foodsById, nutrient)
            }
        )
    }

    fun forFood(food: FoodEntity, grams: Double): NutritionCompletenessReport =
        NutritionCompletenessReport(
            totals = NutritionCalculator.forFood(food, grams),
            completeness = NutrientKey.entries.associateWith { nutrient ->
                if (grams > 0.0 && food.nutrientPer100g(nutrient) != null) {
                    NutrientCompleteness.COMPLETE
                } else {
                    NutrientCompleteness.UNKNOWN
                }
            }
        )

    fun fromPersisted(
        totals: NutritionTotals,
        knownMask: Long,
        completeMask: Long
    ): NutritionCompletenessReport = NutritionCompletenessReport(
        totals = totals,
        completeness = NutrientKey.entries.associateWith { nutrient ->
            NutrientKnowledgeMask.completeness(nutrient, knownMask, completeMask)
        }
    )

    /** Empty daily history is a confirmed zero; otherwise all logs participate. */
    fun aggregate(reports: List<NutritionCompletenessReport>): NutritionCompletenessReport {
        if (reports.isEmpty()) {
            return NutritionCompletenessReport(
                totals = NutritionTotals(),
                completeness = NutrientKey.entries.associateWith { NutrientCompleteness.COMPLETE }
            )
        }

        return NutritionCompletenessReport(
            totals = NutritionCalculator.sum(reports.map { it.totals }),
            completeness = NutrientKey.entries.associateWith { nutrient ->
                val states = reports.map { it.completeness.getValue(nutrient) }
                when {
                    states.all { it == NutrientCompleteness.COMPLETE } -> NutrientCompleteness.COMPLETE
                    states.any { it != NutrientCompleteness.UNKNOWN } -> NutrientCompleteness.PARTIAL
                    else -> NutrientCompleteness.UNKNOWN
                }
            }
        )
    }

    private fun completenessForIngredients(
        ingredients: List<MealIngredient>,
        foodsById: Map<String, FoodEntity>,
        nutrient: NutrientKey
    ): NutrientCompleteness {
        if (ingredients.isEmpty()) return NutrientCompleteness.UNKNOWN

        var known = 0
        ingredients.forEach { ingredient ->
            val food = ingredient.foodId?.let(foodsById::get)
            if (
                ingredient.gramsEquivalent?.takeIf { it > 0.0 } != null &&
                food?.nutrientPer100g(nutrient) != null
            ) {
                known++
            }
        }

        return when (known) {
            0 -> NutrientCompleteness.UNKNOWN
            ingredients.size -> NutrientCompleteness.COMPLETE
            else -> NutrientCompleteness.PARTIAL
        }
    }
}
