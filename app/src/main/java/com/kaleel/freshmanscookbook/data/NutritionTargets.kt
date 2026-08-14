package com.kaleel.freshmanscookbook.data

/**
 * V1 nutrition-target engine for generally healthy ADULTS (19+).
 *
 * Sources:
 * - Health Canada / National Academies Dietary Reference Intakes (DRIs)
 * - Current DRI Estimated Energy Requirement (EER) equations
 *
 * Design choices for V1:
 * - Calories: DRI EER using age, sex, height, weight and activity category.
 * - Protein: adult RDA = 0.8 g/kg/day for General Health; Build Muscle uses
 *   1.8 g/kg/day. Morton et al. (2018, BJSM, PMID 28698222) found the pooled
 *   resistance-training benefit plateau near 1.6 g/kg/day. 1.8 provides a
 *   practical buffer within the commonly used 1.6-2.2 g/kg range without
 *   presenting 1 g/lb as a universal requirement.
 * - Carbohydrate: adult RDA = 130 g/day.
 * - Fibre: DRI age/sex AI values.
 * - Total fat: midpoint of the adult AMDR (20-35% energy) as a simple
 *   dashboard target, not an RDA.
 * - Vitamins/minerals: RDA where available; AI where no RDA exists.
 *
 * This is general nutrition guidance, not medical nutrition therapy.
 *
 * IMPORTANT:
 * NutritionProfile currently has only MALE/FEMALE and no pregnancy/lactation
 * state, so this V1 calculator intentionally does not attempt pregnancy or
 * lactation targets.
 */
object NutritionTargets {

    fun calculate(profile: NutritionProfile): DailyNutritionTargets {
        require(profile.ageYears >= 19) {
            "V1 nutrition targets currently support adults age 19 and older."
        }
        require(profile.heightCm > 0.0) { "Height must be greater than 0 cm." }
        require(profile.weightKg > 0.0) { "Weight must be greater than 0 kg." }

        val calories = profile.overrides.caloriesKcal ?: estimatedEnergyRequirement(profile)
        val recommendedProtein = proteinRecommendation(profile)
        val protein = profile.overrides.proteinG ?: recommendedProtein
        val carbs = profile.overrides.carbohydrateG ?: 130.0

        // Adult AMDR for total fat is 20-35% of energy.
        // V1 uses the midpoint (27.5%) as a neutral single dashboard target.
        val fat = profile.overrides.fatG ?: ((calories * 0.275) / 9.0)

        val fiber = profile.overrides.fiberG ?: fiberAi(profile)

        val targets = linkedMapOf<NutrientKey, NutrientTarget>()

        fun add(
            key: NutrientKey,
            amount: Double,
            type: NutritionReferenceType,
            source: String = DRI_SOURCE
        ) {
            targets[key] = NutrientTarget(
                nutrient = key,
                amount = amount,
                referenceType = type,
                sourceLabel = source
            )
        }

        add(
            NutrientKey.CALORIES,
            calories,
            if (profile.overrides.caloriesKcal != null)
                NutritionReferenceType.USER_OVERRIDE
            else
                NutritionReferenceType.CALCULATED_ESTIMATE,
            if (profile.overrides.caloriesKcal != null) USER_SOURCE else EER_SOURCE
        )

        val proteinLabel = when (profile.trainingGoal) {
            TrainingGoal.GENERAL_HEALTH -> "General health · $GENERAL_HEALTH_PROTEIN_G_PER_KG g/kg"
            TrainingGoal.BUILD_MUSCLE -> "Build muscle · $BUILD_MUSCLE_PROTEIN_G_PER_KG g/kg"
        }
        targets[NutrientKey.PROTEIN] = NutrientTarget(
            nutrient = NutrientKey.PROTEIN,
            amount = protein,
            referenceType = if (profile.overrides.proteinG != null) NutritionReferenceType.USER_OVERRIDE
            else if (profile.trainingGoal == TrainingGoal.BUILD_MUSCLE) NutritionReferenceType.GUIDELINE
            else NutritionReferenceType.RDA,
            sourceLabel = if (profile.overrides.proteinG != null) USER_SOURCE else proteinLabel,
            recommendedAmount = recommendedProtein,
            recommendationLabel = proteinLabel
        )
        addWithOverride(
            targets,
            NutrientKey.CARBOHYDRATE,
            carbs,
            profile.overrides.carbohydrateG,
            NutritionReferenceType.RDA
        )
        addWithOverride(
            targets,
            NutrientKey.FAT,
            fat,
            profile.overrides.fatG,
            NutritionReferenceType.GUIDELINE,
            "DRI adult AMDR midpoint (20-35% energy)"
        )
        addWithOverride(
            targets,
            NutrientKey.FIBER,
            fiber,
            profile.overrides.fiberG,
            NutritionReferenceType.AI
        )

        // Minerals
        add(NutrientKey.CALCIUM, calcium(profile), NutritionReferenceType.RDA)
        add(NutrientKey.IRON, iron(profile), NutritionReferenceType.RDA)
        add(NutrientKey.MAGNESIUM, magnesium(profile), NutritionReferenceType.RDA)
        add(NutrientKey.PHOSPHORUS, phosphorus(profile), NutritionReferenceType.RDA)
        add(NutrientKey.POTASSIUM, potassium(profile), NutritionReferenceType.AI)
        add(NutrientKey.SODIUM, sodium(profile), NutritionReferenceType.AI)
        add(NutrientKey.ZINC, zinc(profile), NutritionReferenceType.RDA)
        add(NutrientKey.COPPER, 0.9, NutritionReferenceType.RDA)
        add(NutrientKey.MANGANESE, manganese(profile), NutritionReferenceType.AI)
        add(NutrientKey.SELENIUM, 55.0, NutritionReferenceType.RDA)

        // Vitamins
        add(NutrientKey.VITAMIN_A, if (profile.sex == BiologicalSex.MALE) 900.0 else 700.0, NutritionReferenceType.RDA)
        add(NutrientKey.VITAMIN_C, if (profile.sex == BiologicalSex.MALE) 90.0 else 75.0, NutritionReferenceType.RDA)
        add(NutrientKey.VITAMIN_D, if (profile.ageYears > 70) 20.0 else 15.0, NutritionReferenceType.RDA)
        add(NutrientKey.VITAMIN_E, 15.0, NutritionReferenceType.RDA)
        add(NutrientKey.VITAMIN_K, if (profile.sex == BiologicalSex.MALE) 120.0 else 90.0, NutritionReferenceType.AI)
        add(NutrientKey.THIAMIN_B1, if (profile.sex == BiologicalSex.MALE) 1.2 else 1.1, NutritionReferenceType.RDA)
        add(NutrientKey.RIBOFLAVIN_B2, if (profile.sex == BiologicalSex.MALE) 1.3 else 1.1, NutritionReferenceType.RDA)
        add(NutrientKey.NIACIN_B3, if (profile.sex == BiologicalSex.MALE) 16.0 else 14.0, NutritionReferenceType.RDA)
        add(NutrientKey.PANTOTHENIC_ACID_B5, 5.0, NutritionReferenceType.AI)
        add(NutrientKey.VITAMIN_B6, vitaminB6(profile), NutritionReferenceType.RDA)
        add(NutrientKey.FOLATE, 400.0, NutritionReferenceType.RDA)
        add(NutrientKey.VITAMIN_B12, 2.4, NutritionReferenceType.RDA)
        add(NutrientKey.CHOLINE, if (profile.sex == BiologicalSex.MALE) 550.0 else 425.0, NutritionReferenceType.AI)

        return DailyNutritionTargets(targets)
    }

    fun proteinFactor(profile: NutritionProfile): Double = when (profile.trainingGoal) {
        TrainingGoal.GENERAL_HEALTH -> GENERAL_HEALTH_PROTEIN_G_PER_KG
        TrainingGoal.BUILD_MUSCLE -> BUILD_MUSCLE_PROTEIN_G_PER_KG
    }

    fun proteinRecommendation(profile: NutritionProfile): Double =
        profile.weightKg * proteinFactor(profile)

    /**
     * Beverage-water target, not total-water intake and not a prescription.
     * National Academies/Health Canada DRI guidance gives total-water AIs of
     * 3.7 L for men and 2.7 L for women, with about 81% observed from water and
     * beverages. Rounded beverage equivalents are therefore 3.0 L and 2.2 L.
     * Food moisture remains outside this app's water logger.
     */
    fun hydrationRecommendationMl(profile: NutritionProfile): Double = when (profile.sex) {
        BiologicalSex.MALE -> 3_000.0
        BiologicalSex.FEMALE -> 2_200.0
    }

    fun hydrationTarget(profile: NutritionProfile): HydrationTarget {
        val recommended = hydrationRecommendationMl(profile)
        val override = profile.overrides.hydrationMl?.takeIf { it > 0.0 }
        return HydrationTarget(
            amountMl = override ?: recommended,
            recommendedMl = recommended,
            isOverridden = override != null,
            recommendationLabel = "Beverage-water guide · food moisture not included"
        )
    }

    /**
     * DRI Estimated Energy Requirement equations for adults 19+.
     *
     * Height is converted to metres before use.
     *
     * The goal field is intentionally NOT used to create an automatic calorie
     * deficit/surplus in V1. EER estimates maintenance energy. Weight-loss or
     * gain targets should later be a separate, explicit feature rather than
     * silently modifying a scientific reference estimate.
     */
    fun estimatedEnergyRequirement(profile: NutritionProfile): Double {
        val age = profile.ageYears.toDouble()
        val weight = profile.weightKg
        val heightM = profile.heightCm / 100.0

        return when (profile.sex) {
            BiologicalSex.MALE -> {
                val pa = malePa(profile.activityLevel)
                662.0 - (9.53 * age) + pa * ((15.91 * weight) + (539.6 * heightM))
            }

            BiologicalSex.FEMALE -> {
                val pa = femalePa(profile.activityLevel)
                354.0 - (6.91 * age) + pa * ((9.36 * weight) + (726.0 * heightM))
            }
        }.coerceAtLeast(0.0)
    }

    private fun malePa(level: ActivityLevel): Double = when (level) {
        ActivityLevel.SEDENTARY -> 1.00
        ActivityLevel.LIGHTLY_ACTIVE -> 1.11
        ActivityLevel.MODERATELY_ACTIVE -> 1.25
        ActivityLevel.VERY_ACTIVE,
        ActivityLevel.EXTRA_ACTIVE -> 1.48
    }

    private fun femalePa(level: ActivityLevel): Double = when (level) {
        ActivityLevel.SEDENTARY -> 1.00
        ActivityLevel.LIGHTLY_ACTIVE -> 1.12
        ActivityLevel.MODERATELY_ACTIVE -> 1.27
        ActivityLevel.VERY_ACTIVE,
        ActivityLevel.EXTRA_ACTIVE -> 1.45
    }

    private fun fiberAi(profile: NutritionProfile): Double = when (profile.sex) {
        BiologicalSex.MALE -> when {
            profile.ageYears <= 50 -> 38.0
            else -> 30.0
        }

        BiologicalSex.FEMALE -> when {
            profile.ageYears <= 50 -> 25.0
            else -> 21.0
        }
    }

    private fun calcium(profile: NutritionProfile): Double = when {
        profile.ageYears > 70 -> 1200.0
        profile.sex == BiologicalSex.FEMALE && profile.ageYears > 50 -> 1200.0
        else -> 1000.0
    }

    private fun iron(profile: NutritionProfile): Double = when {
        profile.sex == BiologicalSex.MALE -> 8.0
        profile.ageYears <= 50 -> 18.0
        else -> 8.0
    }

    private fun magnesium(profile: NutritionProfile): Double = when (profile.sex) {
        BiologicalSex.MALE -> if (profile.ageYears <= 30) 400.0 else 420.0
        BiologicalSex.FEMALE -> if (profile.ageYears <= 30) 310.0 else 320.0
    }

    private fun phosphorus(profile: NutritionProfile): Double = 700.0

    private fun potassium(profile: NutritionProfile): Double =
        if (profile.sex == BiologicalSex.MALE) 3400.0 else 2600.0

    private fun sodium(profile: NutritionProfile): Double =
        if (profile.ageYears > 70) 1200.0
        else if (profile.ageYears > 50) 1300.0
        else 1500.0

    private fun zinc(profile: NutritionProfile): Double =
        if (profile.sex == BiologicalSex.MALE) 11.0 else 8.0

    private fun manganese(profile: NutritionProfile): Double =
        if (profile.sex == BiologicalSex.MALE) 2.3 else 1.8

    private fun vitaminB6(profile: NutritionProfile): Double = when {
        profile.ageYears <= 50 -> 1.3
        profile.sex == BiologicalSex.MALE -> 1.7
        else -> 1.5
    }

    private fun addWithOverride(
        targets: MutableMap<NutrientKey, NutrientTarget>,
        key: NutrientKey,
        amount: Double,
        overrideValue: Double?,
        defaultType: NutritionReferenceType,
        defaultSource: String = DRI_SOURCE
    ) {
        targets[key] = NutrientTarget(
            nutrient = key,
            amount = amount,
            referenceType = if (overrideValue != null)
                NutritionReferenceType.USER_OVERRIDE
            else
                defaultType,
            sourceLabel = if (overrideValue != null) USER_SOURCE else defaultSource
        )
    }

    private const val DRI_SOURCE =
        "Health Canada / National Academies Dietary Reference Intakes"

    private const val EER_SOURCE =
        "Dietary Reference Intakes Estimated Energy Requirement equation"

    private const val USER_SOURCE = "User-defined target"

    const val GENERAL_HEALTH_PROTEIN_G_PER_KG = 0.8
    const val BUILD_MUSCLE_PROTEIN_G_PER_KG = 1.8
}
