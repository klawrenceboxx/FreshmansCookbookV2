package com.kaleel.freshmanscookbook.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MealLogDao {

    /**
     * Observe meal logs inside a half-open time range:
     * startInclusive <= loggedAt < endExclusive.
     *
     * This avoids ambiguity at midnight when building a daily dashboard.
     */
    @Transaction
    @Query(
        """
        SELECT * FROM meal_logs
        WHERE loggedAt >= :startInclusive
          AND loggedAt < :endExclusive
        ORDER BY loggedAt DESC
        """
    )
    fun observeBetween(
        startInclusive: Long,
        endExclusive: Long
    ): Flow<List<MealLogWithIngredients>>

    @Transaction
    @Query(
        """
        SELECT * FROM meal_logs
        WHERE loggedAt >= :startInclusive
          AND loggedAt < :endExclusive
        ORDER BY loggedAt DESC
        """
    )
    suspend fun getBetween(
        startInclusive: Long,
        endExclusive: Long
    ): List<MealLogWithIngredients>

    @Transaction
    @Query("SELECT * FROM meal_logs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MealLogWithIngredients?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<MealLogIngredientEntity>)

    @Query("DELETE FROM meal_log_ingredients WHERE mealLogId = :mealLogId")
    suspend fun deleteIngredientsForMeal(mealLogId: String)

    /**
     * Saves a complete historical snapshot.
     *
     * Re-saving the same meal ID replaces its ingredient snapshot so stale
     * ingredient rows cannot survive an edit.
     */
    @Transaction
    suspend fun save(record: MealLogRecord) {
        insertMeal(record.meal)
        deleteIngredientsForMeal(record.meal.id)

        if (record.ingredients.isNotEmpty()) {
            insertIngredients(record.ingredients)
        }
    }

    @Query("DELETE FROM meal_logs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM meal_logs")
    suspend fun count(): Int
}
