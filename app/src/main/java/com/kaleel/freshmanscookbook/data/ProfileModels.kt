package com.kaleel.freshmanscookbook.data

/**
 * V1 profile data used to personalize nutrition guidance.
 *
 * This file deliberately contains DATA MODELS ONLY.
 * The evidence-based target calculations belong in NutritionTargets.kt so
 * profile storage, UI, and nutrition science remain separate concerns.
 */
data class NutritionProfile(
    val ageYears: Int,
    val sex: BiologicalSex,
    val heightCm: Double,
    val weightKg: Double,
    val activityLevel: ActivityLevel,
    val goal: NutritionGoal = NutritionGoal.MAINTAIN,

    /**
     * Optional manual overrides let a user replace calculated guidance without
     * changing the underlying profile.
     */
    val overrides: NutritionTargetOverrides = NutritionTargetOverrides()
)

enum class BiologicalSex {
    MALE,
    FEMALE
}

/**
 * V1 uses broad activity categories to keep onboarding short.
 *
 * These labels are intentionally descriptive rather than embedding numeric
 * calorie multipliers in the model. The calculation file owns those details.
 */
enum class ActivityLevel {
    SEDENTARY,
    LIGHTLY_ACTIVE,
    MODERATELY_ACTIVE,
    VERY_ACTIVE,
    EXTRA_ACTIVE
}

enum class NutritionGoal {
    LOSE_WEIGHT,
    MAINTAIN,
    GAIN_WEIGHT
}

/**
 * Stable nutrient keys used throughout the app.
 *
 * This follows the useful pattern from the earlier nutrition-tracker project:
 * screens should refer to a nutrient by a stable key instead of repeatedly
 * hard-coding display labels and units.
 */
enum class NutrientKey {
    CALORIES,
    PROTEIN,
    CARBOHYDRATE,
    FAT,
    FIBER,

    CALCIUM,
    IRON,
    MAGNESIUM,
    PHOSPHORUS,
    POTASSIUM,
    SODIUM,
    ZINC,
    COPPER,
    MANGANESE,
    SELENIUM,

    VITAMIN_A,
    VITAMIN_C,
    VITAMIN_D,
    VITAMIN_E,
    VITAMIN_K,
    THIAMIN_B1,
    RIBOFLAVIN_B2,
    NIACIN_B3,
    PANTOTHENIC_ACID_B5,
    VITAMIN_B6,
    FOLATE,
    VITAMIN_B12,
    CHOLINE,

    SATURATED_FAT,
    MONOUNSATURATED_FAT,
    POLYUNSATURATED_FAT,
    CHOLESTEROL
}

enum class NutrientCategory {
    ENERGY,
    MACRO,
    VITAMIN,
    MINERAL,
    FAT_COMPONENT
}

enum class NutrientUnit(val symbol: String) {
    KCAL("kcal"),
    GRAM("g"),
    MILLIGRAM("mg"),
    MICROGRAM("mcg"),
    MICROGRAM_RAE("mcg RAE"),
    MICROGRAM_DFE("mcg DFE")
}

/**
 * Central display metadata.
 *
 * This prevents ProfileScreen, NutritionDashboard, forecasting, pinned
 * nutrients, and future recommendation screens from each inventing their own
 * names/units.
 */
data class NutrientMetadata(
    val key: NutrientKey,
    val displayName: String,
    val unit: NutrientUnit,
    val category: NutrientCategory
)

object NutrientCatalog {
    val all: List<NutrientMetadata> = listOf(
        NutrientMetadata(NutrientKey.CALORIES, "Calories", NutrientUnit.KCAL, NutrientCategory.ENERGY),
        NutrientMetadata(NutrientKey.PROTEIN, "Protein", NutrientUnit.GRAM, NutrientCategory.MACRO),
        NutrientMetadata(NutrientKey.CARBOHYDRATE, "Carbohydrates", NutrientUnit.GRAM, NutrientCategory.MACRO),
        NutrientMetadata(NutrientKey.FAT, "Fat", NutrientUnit.GRAM, NutrientCategory.MACRO),
        NutrientMetadata(NutrientKey.FIBER, "Fiber", NutrientUnit.GRAM, NutrientCategory.MACRO),

        NutrientMetadata(NutrientKey.CALCIUM, "Calcium", NutrientUnit.MILLIGRAM, NutrientCategory.MINERAL),
        NutrientMetadata(NutrientKey.IRON, "Iron", NutrientUnit.MILLIGRAM, NutrientCategory.MINERAL),
        NutrientMetadata(NutrientKey.MAGNESIUM, "Magnesium", NutrientUnit.MILLIGRAM, NutrientCategory.MINERAL),
        NutrientMetadata(NutrientKey.PHOSPHORUS, "Phosphorus", NutrientUnit.MILLIGRAM, NutrientCategory.MINERAL),
        NutrientMetadata(NutrientKey.POTASSIUM, "Potassium", NutrientUnit.MILLIGRAM, NutrientCategory.MINERAL),
        NutrientMetadata(NutrientKey.SODIUM, "Sodium", NutrientUnit.MILLIGRAM, NutrientCategory.MINERAL),
        NutrientMetadata(NutrientKey.ZINC, "Zinc", NutrientUnit.MILLIGRAM, NutrientCategory.MINERAL),
        NutrientMetadata(NutrientKey.COPPER, "Copper", NutrientUnit.MILLIGRAM, NutrientCategory.MINERAL),
        NutrientMetadata(NutrientKey.MANGANESE, "Manganese", NutrientUnit.MILLIGRAM, NutrientCategory.MINERAL),
        NutrientMetadata(NutrientKey.SELENIUM, "Selenium", NutrientUnit.MICROGRAM, NutrientCategory.MINERAL),

        NutrientMetadata(NutrientKey.VITAMIN_A, "Vitamin A", NutrientUnit.MICROGRAM_RAE, NutrientCategory.VITAMIN),
        NutrientMetadata(NutrientKey.VITAMIN_C, "Vitamin C", NutrientUnit.MILLIGRAM, NutrientCategory.VITAMIN),
        NutrientMetadata(NutrientKey.VITAMIN_D, "Vitamin D", NutrientUnit.MICROGRAM, NutrientCategory.VITAMIN),
        NutrientMetadata(NutrientKey.VITAMIN_E, "Vitamin E", NutrientUnit.MILLIGRAM, NutrientCategory.VITAMIN),
        NutrientMetadata(NutrientKey.VITAMIN_K, "Vitamin K", NutrientUnit.MICROGRAM, NutrientCategory.VITAMIN),
        NutrientMetadata(NutrientKey.THIAMIN_B1, "Thiamin (B1)", NutrientUnit.MILLIGRAM, NutrientCategory.VITAMIN),
        NutrientMetadata(NutrientKey.RIBOFLAVIN_B2, "Riboflavin (B2)", NutrientUnit.MILLIGRAM, NutrientCategory.VITAMIN),
        NutrientMetadata(NutrientKey.NIACIN_B3, "Niacin (B3)", NutrientUnit.MILLIGRAM, NutrientCategory.VITAMIN),
        NutrientMetadata(NutrientKey.PANTOTHENIC_ACID_B5, "Pantothenic Acid (B5)", NutrientUnit.MILLIGRAM, NutrientCategory.VITAMIN),
        NutrientMetadata(NutrientKey.VITAMIN_B6, "Vitamin B6", NutrientUnit.MILLIGRAM, NutrientCategory.VITAMIN),
        NutrientMetadata(NutrientKey.FOLATE, "Folate", NutrientUnit.MICROGRAM_DFE, NutrientCategory.VITAMIN),
        NutrientMetadata(NutrientKey.VITAMIN_B12, "Vitamin B12", NutrientUnit.MICROGRAM, NutrientCategory.VITAMIN),
        NutrientMetadata(NutrientKey.CHOLINE, "Choline", NutrientUnit.MILLIGRAM, NutrientCategory.VITAMIN),

        NutrientMetadata(NutrientKey.SATURATED_FAT, "Saturated Fat", NutrientUnit.GRAM, NutrientCategory.FAT_COMPONENT),
        NutrientMetadata(NutrientKey.MONOUNSATURATED_FAT, "Monounsaturated Fat", NutrientUnit.GRAM, NutrientCategory.FAT_COMPONENT),
        NutrientMetadata(NutrientKey.POLYUNSATURATED_FAT, "Polyunsaturated Fat", NutrientUnit.GRAM, NutrientCategory.FAT_COMPONENT),
        NutrientMetadata(NutrientKey.CHOLESTEROL, "Cholesterol", NutrientUnit.MILLIGRAM, NutrientCategory.FAT_COMPONENT)
    )

    val byKey: Map<NutrientKey, NutrientMetadata> = all.associateBy { it.key }
}

/**
 * A target can represent an RDA, AI, calculated goal, or another reference
 * value. Keeping the source/type attached will let the UI explain where a
 * number came from instead of presenting every value as equally exact.
 */
data class NutrientTarget(
    val nutrient: NutrientKey,
    val amount: Double,
    val referenceType: NutritionReferenceType,
    val sourceLabel: String
)

enum class NutritionReferenceType {
    CALCULATED_ESTIMATE,
    RDA,
    AI,
    GUIDELINE,
    USER_OVERRIDE
}

data class DailyNutritionTargets(
    val targets: Map<NutrientKey, NutrientTarget>
) {
    operator fun get(key: NutrientKey): NutrientTarget? = targets[key]
}

/**
 * Null means "use the app-calculated/reference target."
 *
 * We start with the high-level targets users are most likely to customize.
 * More overrides can be added later without changing NutritionProfile.
 */
data class NutritionTargetOverrides(
    val caloriesKcal: Double? = null,
    val proteinG: Double? = null,
    val carbohydrateG: Double? = null,
    val fatG: Double? = null,
    val fiberG: Double? = null
)

/**
 * Reusable progress model for the daily dashboard, recipe forecast, pinned
 * nutrient mode, and recipe recommendation features.
 */
data class NutrientProgress(
    val nutrient: NutrientKey,
    val consumed: Double,
    val target: Double?,
    val projected: Double? = null
) {
    val remaining: Double?
        get() = target?.let { (it - consumed).coerceAtLeast(0.0) }

    val progressFraction: Double?
        get() = target
            ?.takeIf { it > 0.0 }
            ?.let { (consumed / it).coerceAtLeast(0.0) }

    val projectedProgressFraction: Double?
        get() = if (projected == null) {
            null
        } else {
            target
                ?.takeIf { it > 0.0 }
                ?.let { (projected / it).coerceAtLeast(0.0) }
        }
}
