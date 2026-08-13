package com.kaleel.freshmanscookbook.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Room access for the single local nutrition profile used in V1.
 */
@Dao
interface ProfileDao {

    /**
     * Observe profile changes so Profile/Dashboard UI can update automatically.
     * Emits null until onboarding has created a profile.
     */
    @Query(
        "SELECT * FROM nutrition_profile " +
            "WHERE id = ${NutritionProfileEntity.SINGLE_PROFILE_ID} LIMIT 1"
    )
    fun observeProfile(): Flow<NutritionProfileEntity?>

    /**
     * One-shot read for operations that do not need continuous observation.
     */
    @Query(
        "SELECT * FROM nutrition_profile " +
            "WHERE id = ${NutritionProfileEntity.SINGLE_PROFILE_ID} LIMIT 1"
    )
    suspend fun getProfile(): NutritionProfileEntity?

    /**
     * Creates the profile or replaces the existing single-profile row.
     */
    @Upsert
    suspend fun saveProfile(profile: NutritionProfileEntity)

    /**
     * Useful for reset/onboarding-again functionality.
     */
    @Query(
        "DELETE FROM nutrition_profile " +
            "WHERE id = ${NutritionProfileEntity.SINGLE_PROFILE_ID}"
    )
    suspend fun clearProfile()

    /**
     * Lets navigation/onboarding cheaply determine whether setup is complete.
     */
    @Query(
        "SELECT EXISTS(" +
            "SELECT 1 FROM nutrition_profile " +
            "WHERE id = ${NutritionProfileEntity.SINGLE_PROFILE_ID}" +
        ")"
    )
    suspend fun hasProfile(): Boolean
}
