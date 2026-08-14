package com.kaleel.freshmanscookbook.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Aggregated nutrition for a caller-supplied time window.
 *
 * A daily snapshot contains both recipe-based meal logs and individually
 * logged foods/snacks.
 */
data class DailyNutritionSnapshot(
    val startInclusive: Long,
    val endExclusive: Long,
    val meals: List<MealLogWithIngredients>,
    val foods: List<FoodLogEntity>,
    val nutrition: NutritionCompletenessReport
) {
    val totals: NutritionTotals
        get() = nutrition.totals

    fun nutrient(nutrient: NutrientKey): NutrientValue = nutrition.value(nutrient)

    fun progress(
        targets: DailyNutritionTargets?
    ): List<NutrientProgress> =
        NutrientCatalog.all.map { metadata ->
            NutrientProgress(
                nutrient = metadata.key,
                consumed = MealNutritionCalculator.nutrientValue(
                    totals,
                    metadata.key
                ),
                target = targets?.get(metadata.key)?.amount
            )
        }
}

class DailyNutritionRepository(
    database: CookbookDatabase
) {
    private val mealLogDao = database.mealLogDao()
    private val foodLogDao = database.foodLogDao()

    fun observeBetween(
        startInclusive: Long,
        endExclusive: Long
    ): Flow<DailyNutritionSnapshot> =
        combine(
            mealLogDao.observeBetween(startInclusive, endExclusive),
            foodLogDao.observeBetween(startInclusive, endExclusive)
        ) { meals, foods ->
            snapshot(
                startInclusive = startInclusive,
                endExclusive = endExclusive,
                meals = meals,
                foods = foods
            )
        }

    suspend fun getBetween(
        startInclusive: Long,
        endExclusive: Long
    ): DailyNutritionSnapshot =
        snapshot(
            startInclusive = startInclusive,
            endExclusive = endExclusive,
            meals = mealLogDao.getBetween(
                startInclusive,
                endExclusive
            ),
            foods = foodLogDao.getBetween(
                startInclusive,
                endExclusive
            )
        )

    private fun snapshot(
        startInclusive: Long,
        endExclusive: Long,
        meals: List<MealLogWithIngredients>,
        foods: List<FoodLogEntity>
    ): DailyNutritionSnapshot {
        val nutrition = NutritionCompletenessCalculator.aggregate(
            meals.map { it.meal.nutritionReport() } + foods.map { it.nutritionReport() }
        )

        return DailyNutritionSnapshot(
            startInclusive = startInclusive,
            endExclusive = endExclusive,
            meals = meals,
            foods = foods,
            nutrition = nutrition
        )
    }
}
