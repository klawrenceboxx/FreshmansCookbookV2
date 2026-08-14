package com.kaleel.freshmanscookbook.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Persisted version of the user's nutrition profile.
 *
 * There is intentionally only one row in V1 (id = 1). Keeping this separate
 * from NutritionProfile means Room persistence can evolve without coupling
 * database annotations to the calculation/domain model.
 */
@Entity(tableName = "nutrition_profile")
data class NutritionProfileEntity(
    @PrimaryKey val id: Int = SINGLE_PROFILE_ID,
    val ageYears: Int,
    val sex: BiologicalSex,
    val heightCm: Double,
    val weightKg: Double,
    val activityLevel: ActivityLevel,
    val goal: NutritionGoal,
    @ColumnInfo(defaultValue = "'GENERAL_HEALTH'") val trainingGoal: TrainingGoal,

    // Optional user-defined targets.
    val caloriesOverrideKcal: Double? = null,
    val proteinOverrideG: Double? = null,
    val carbohydrateOverrideG: Double? = null,
    val fatOverrideG: Double? = null,
    val fiberOverrideG: Double? = null,
    val hydrationOverrideMl: Double? = null
) {
    fun toDomain(): NutritionProfile = NutritionProfile(
        ageYears = ageYears,
        sex = sex,
        heightCm = heightCm,
        weightKg = weightKg,
        activityLevel = activityLevel,
        goal = goal,
        overrides = NutritionTargetOverrides(
            caloriesKcal = caloriesOverrideKcal,
            proteinG = proteinOverrideG,
            carbohydrateG = carbohydrateOverrideG,
            fatG = fatOverrideG,
            fiberG = fiberOverrideG,
            hydrationMl = hydrationOverrideMl
        ),
        trainingGoal = trainingGoal
    )

    companion object {
        const val SINGLE_PROFILE_ID = 1

        fun fromDomain(profile: NutritionProfile): NutritionProfileEntity =
            NutritionProfileEntity(
                id = SINGLE_PROFILE_ID,
                ageYears = profile.ageYears,
                sex = profile.sex,
                heightCm = profile.heightCm,
                weightKg = profile.weightKg,
                activityLevel = profile.activityLevel,
                goal = profile.goal,
                trainingGoal = profile.trainingGoal,
                caloriesOverrideKcal = profile.overrides.caloriesKcal,
                proteinOverrideG = profile.overrides.proteinG,
                carbohydrateOverrideG = profile.overrides.carbohydrateG,
                fatOverrideG = profile.overrides.fatG,
                fiberOverrideG = profile.overrides.fiberG,
                hydrationOverrideMl = profile.overrides.hydrationMl
            )
    }
}
