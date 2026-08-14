package com.kaleel.freshmanscookbook.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * One individually logged food/snack.
 *
 * This is intentionally independent from recipes and meal logs. The nutrition
 * is snapshotted when the food is logged so later USDA/database changes do not
 * rewrite historical daily totals.
 */
@Entity(
    tableName = "food_logs",
    indices = [
        Index("loggedAt"),
        Index("foodId")
    ]
)
data class FoodLogEntity(
    @PrimaryKey val id: String,
    val foodId: String,
    val foodName: String,
    val quantity: Double?,
    val unit: IngredientUnit,
    val gramsEquivalent: Double,
    val loggedAt: Long,
    @ColumnInfo(defaultValue = "4294967295") val knownNutrientsMask: Long,
    @ColumnInfo(defaultValue = "4294967295") val completeNutrientsMask: Long,
    @Embedded val nutrition: NutritionTotals
) {
    fun nutritionReport(): NutritionCompletenessReport =
        NutritionCompletenessCalculator.fromPersisted(
            totals = nutrition,
            knownMask = knownNutrientsMask,
            completeMask = completeNutrientsMask
        )
}

/**
 * Input after the UI has selected a USDA food and resolved its entered amount
 * to grams through FoodRepository.gramsFor(...).
 */
data class FoodLogDraft(
    val foodId: String,
    val foodName: String,
    val quantity: Double?,
    val unit: IngredientUnit,
    val gramsEquivalent: Double,
    val loggedAt: Long = System.currentTimeMillis()
)

@Dao
interface FoodLogDao {

    @Query(
        """
        SELECT * FROM food_logs
        WHERE loggedAt >= :startInclusive
          AND loggedAt < :endExclusive
        ORDER BY loggedAt DESC
        """
    )
    fun observeBetween(
        startInclusive: Long,
        endExclusive: Long
    ): Flow<List<FoodLogEntity>>

    @Query(
        """
        SELECT * FROM food_logs
        WHERE loggedAt >= :startInclusive
          AND loggedAt < :endExclusive
        ORDER BY loggedAt DESC
        """
    )
    suspend fun getBetween(
        startInclusive: Long,
        endExclusive: Long
    ): List<FoodLogEntity>

    @Query("SELECT * FROM food_logs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FoodLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: FoodLogEntity)

    @Query("DELETE FROM food_logs WHERE id = :id")
    suspend fun deleteById(id: String)
}

/**
 * Logs standalone foods/snacks using the same USDA nutrition math as meal
 * ingredients.
 */
class FoodLogRepository(
    private val database: CookbookDatabase
) {
    private val foodDao = database.foodDao()
    private val foodLogDao = database.foodLogDao()

    fun observeBetween(
        startInclusive: Long,
        endExclusive: Long
    ): Flow<List<FoodLogEntity>> =
        foodLogDao.observeBetween(startInclusive, endExclusive)

    suspend fun getBetween(
        startInclusive: Long,
        endExclusive: Long
    ): List<FoodLogEntity> =
        foodLogDao.getBetween(startInclusive, endExclusive)

    suspend fun getById(id: String): FoodLogEntity? =
        foodLogDao.getById(id)

    suspend fun logFood(
        draft: FoodLogDraft,
        id: String = UUID.randomUUID().toString()
    ): FoodLogEntity {
        require(draft.foodId.isNotBlank()) { "foodId cannot be blank." }
        require(draft.gramsEquivalent > 0.0) {
            "A standalone food must have a defensible gram amount before logging."
        }

        val food = foodDao
            .getByIds(listOf(draft.foodId))
            .firstOrNull()
            ?: error("Food ${draft.foodId} was not found in the local nutrition database.")

        val nutrition = NutritionCompletenessCalculator.forFood(food, draft.gramsEquivalent)

        val entity = FoodLogEntity(
            id = id,
            foodId = food.foodId,
            foodName = draft.foodName.ifBlank { food.userFacingName },
            quantity = draft.quantity,
            unit = draft.unit,
            gramsEquivalent = draft.gramsEquivalent,
            loggedAt = draft.loggedAt,
            knownNutrientsMask = nutrition.knownMask,
            completeNutrientsMask = nutrition.completeMask,
            nutrition = nutrition.totals
        )

        foodLogDao.insert(entity)
        return entity
    }

    suspend fun delete(id: String) =
        foodLogDao.deleteById(id)
}
