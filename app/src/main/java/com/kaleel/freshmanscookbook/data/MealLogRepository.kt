package com.kaleel.freshmanscookbook.data

import kotlinx.coroutines.flow.Flow

/**
 * Persistence boundary for finalized meals.
 *
 * The repository resolves the FoodEntity rows referenced by the temporary meal,
 * calculates checked + consumed-serving nutrition, snapshots the result, and
 * then writes the entire log transactionally through MealLogDao.
 */
class MealLogRepository(
    private val database: CookbookDatabase
) {
    private val mealLogDao = database.mealLogDao()
    private val foodDao = database.foodDao()

    fun observeBetween(
        startInclusive: Long,
        endExclusive: Long
    ): Flow<List<MealLogWithIngredients>> =
        mealLogDao.observeBetween(startInclusive, endExclusive)

    suspend fun getBetween(
        startInclusive: Long,
        endExclusive: Long
    ): List<MealLogWithIngredients> =
        mealLogDao.getBetween(startInclusive, endExclusive)

    suspend fun getById(id: String): MealLogWithIngredients? =
        mealLogDao.getById(id)

    /**
     * Finalizes and saves one MealInstance.
     *
     * Unchecked ingredients remain in the historical ingredient snapshot but
     * contribute no nutrition to the saved MealLogEntity.
     */
    suspend fun logMeal(
        meal: MealInstance,
        loggedAt: Long = System.currentTimeMillis()
    ): MealLogRecord {
        require(meal.ingredients.none {
            it.isChecked && it.quantity != null && it.quantity > 0 && it.unit != IngredientUnit.NONE && it.gramsEquivalent == null
        }) {
            "Checked measured ingredients must have an authoritative gram conversion before logging"
        }
        val foodsById = resolveFoods(meal)
        val consumedNutrition =
            MealNutritionCalculator.consumedTotals(meal, foodsById)

        val record = MealLogRecord.fromMeal(
            mealInstance = meal,
            consumedNutrition = consumedNutrition,
            loggedAt = loggedAt
        )

        mealLogDao.save(record)
        return record
    }

    suspend fun delete(id: String) =
        mealLogDao.deleteById(id)

    private suspend fun resolveFoods(
        meal: MealInstance
    ): Map<String, FoodEntity> {
        val foodIds = meal.ingredients
            .mapNotNull { it.foodId }
            .distinct()

        if (foodIds.isEmpty()) return emptyMap()

        return foodDao
            .getByIds(foodIds)
            .associateBy { it.foodId }
    }
}
