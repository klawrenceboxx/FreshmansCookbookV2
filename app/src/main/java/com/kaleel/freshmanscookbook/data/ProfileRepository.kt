package com.kaleel.freshmanscookbook.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Domain-facing profile state used by onboarding and the nutrition dashboard.
 */
data class ProfileNutritionState(
    val profile: NutritionProfile,
    val targets: DailyNutritionTargets
)

class ProfileRepository(
    database: CookbookDatabase
) {
    private val dao = database.profileDao()

    /**
     * Emits null until first-time profile setup is complete.
     */
    val profile: Flow<NutritionProfile?> =
        dao.observeProfile().map { entity -> entity?.toDomain() }

    /**
     * Profile and calculated targets stay synchronized automatically.
     */
    val nutritionState: Flow<ProfileNutritionState?> =
        profile.map { current ->
            current?.let {
                ProfileNutritionState(
                    profile = it,
                    targets = NutritionTargets.calculate(it)
                )
            }
        }

    suspend fun getProfile(): NutritionProfile? =
        dao.getProfile()?.toDomain()

    suspend fun getNutritionState(): ProfileNutritionState? =
        getProfile()?.let {
            ProfileNutritionState(
                profile = it,
                targets = NutritionTargets.calculate(it)
            )
        }

    suspend fun saveProfile(profile: NutritionProfile) =
        dao.saveProfile(
            NutritionProfileEntity.fromDomain(profile)
        )

    suspend fun clearProfile() =
        dao.clearProfile()

    suspend fun hasProfile(): Boolean =
        dao.hasProfile()
}
