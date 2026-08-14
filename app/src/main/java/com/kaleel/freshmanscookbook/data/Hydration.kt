package com.kaleel.freshmanscookbook.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Index
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

enum class WaterUnit(val label: String) {
    MILLILITERS("mL"),
    LITERS("L"),
    CUPS("cups")
}

enum class WaterDisplayUnit { LITERS, CUPS }

/** Canadian metric cup. Recipe ingredient cups remain food-specific USDA weights. */
object WaterConversion {
    const val MILLILITERS_PER_LITER = 1000.0
    const val MILLILITERS_PER_CUP = 250.0

    fun toMilliliters(amount: Double, unit: WaterUnit): Double = when (unit) {
        WaterUnit.MILLILITERS -> amount
        WaterUnit.LITERS -> amount * MILLILITERS_PER_LITER
        WaterUnit.CUPS -> amount * MILLILITERS_PER_CUP
    }

    fun fromMilliliters(amountMl: Double, unit: WaterDisplayUnit): Double = when (unit) {
        WaterDisplayUnit.LITERS -> amountMl / MILLILITERS_PER_LITER
        WaterDisplayUnit.CUPS -> amountMl / MILLILITERS_PER_CUP
    }
}

@Entity(tableName = "water_logs", indices = [Index("loggedAt")])
data class WaterLogEntity(
    @PrimaryKey val id: String,
    val amountMl: Double,
    val enteredAmount: Double,
    val enteredUnit: WaterUnit,
    val label: String?,
    val loggedAt: Long
)

@Entity(tableName = "hydration_preferences")
data class HydrationPreferencesEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val displayUnit: WaterDisplayUnit = WaterDisplayUnit.LITERS,
    val bottleMl: Double? = null
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

data class HydrationDaySnapshot(
    val startInclusive: Long,
    val endExclusive: Long,
    val logs: List<WaterLogEntity>,
    val totalMl: Double,
    val preferences: HydrationPreferencesEntity
)

object WaterLogSummary {
    fun totalMl(logs: Iterable<WaterLogEntity>): Double = logs.sumOf { it.amountMl }
}

@Dao
interface HydrationDao {
    @Query(
        """SELECT * FROM water_logs
           WHERE loggedAt >= :startInclusive AND loggedAt < :endExclusive
           ORDER BY loggedAt DESC"""
    )
    fun observeBetween(startInclusive: Long, endExclusive: Long): Flow<List<WaterLogEntity>>

    @Query(
        """SELECT * FROM water_logs
           WHERE loggedAt >= :startInclusive AND loggedAt < :endExclusive
           ORDER BY loggedAt DESC"""
    )
    suspend fun getBetween(startInclusive: Long, endExclusive: Long): List<WaterLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WaterLogEntity)

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteLog(id: String)

    @Query("SELECT * FROM hydration_preferences WHERE id = 1")
    fun observePreferences(): Flow<HydrationPreferencesEntity?>

    @Query("SELECT * FROM hydration_preferences WHERE id = 1")
    suspend fun getPreferences(): HydrationPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreferences(preferences: HydrationPreferencesEntity)
}

class HydrationRepository(private val database: CookbookDatabase) {
    private val dao = database.hydrationDao()

    fun observeBetween(startInclusive: Long, endExclusive: Long): Flow<HydrationDaySnapshot> =
        combine(
            dao.observeBetween(startInclusive, endExclusive),
            dao.observePreferences()
        ) { logs, preferences ->
            HydrationDaySnapshot(
                startInclusive = startInclusive,
                endExclusive = endExclusive,
                logs = logs,
                totalMl = WaterLogSummary.totalMl(logs),
                preferences = preferences ?: HydrationPreferencesEntity()
            )
        }

    suspend fun log(
        amount: Double,
        unit: WaterUnit,
        label: String? = null,
        loggedAt: Long = System.currentTimeMillis(),
        id: String = UUID.randomUUID().toString()
    ): WaterLogEntity {
        val amountMl = WaterConversion.toMilliliters(amount, unit)
        require(amountMl > 0.0) { "Water amount must be greater than zero." }
        val log = WaterLogEntity(id, amountMl, amount, unit, label, loggedAt)
        dao.insertLog(log)
        return log
    }

    suspend fun logBottle(loggedAt: Long = System.currentTimeMillis()): WaterLogEntity {
        val bottleMl = dao.getPreferences()?.bottleMl
            ?.takeIf { it > 0.0 }
            ?: error("Configure My bottle before logging it.")
        return log(bottleMl, WaterUnit.MILLILITERS, label = "My bottle", loggedAt = loggedAt)
    }

    suspend fun delete(id: String) = dao.deleteLog(id)

    suspend fun setDisplayUnit(unit: WaterDisplayUnit) {
        val current = dao.getPreferences() ?: HydrationPreferencesEntity()
        dao.savePreferences(current.copy(displayUnit = unit))
    }

    suspend fun setBottle(amount: Double, unit: WaterUnit) {
        val amountMl = WaterConversion.toMilliliters(amount, unit)
        require(amountMl > 0.0) { "Bottle amount must be greater than zero." }
        val current = dao.getPreferences() ?: HydrationPreferencesEntity()
        dao.savePreferences(current.copy(bottleMl = amountMl))
    }
}
