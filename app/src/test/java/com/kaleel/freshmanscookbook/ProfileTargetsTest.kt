package com.kaleel.freshmanscookbook

import com.kaleel.freshmanscookbook.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileTargetsTest {
    private fun profile(
        weightKg: Double = 80.0,
        trainingGoal: TrainingGoal = TrainingGoal.GENERAL_HEALTH,
        overrides: NutritionTargetOverrides = NutritionTargetOverrides(),
        sex: BiologicalSex = BiologicalSex.MALE
    ) = NutritionProfile(
        ageYears = 25,
        sex = sex,
        heightCm = 180.0,
        weightKg = weightKg,
        activityLevel = ActivityLevel.MODERATELY_ACTIVE,
        overrides = overrides,
        trainingGoal = trainingGoal
    )

    @Test
    fun poundsConvertAccuratelyToCanonicalKilograms() {
        assertEquals(72.5747792, BodyWeightConversion.poundsToKilograms(160.0), 0.000001)
        assertEquals(160.0, BodyWeightConversion.kilogramsToPounds(72.5747792), 0.000001)
    }

    @Test
    fun buildMuscleUsesDocumentedWeightBasedFactor() {
        val user = profile(
            weightKg = BodyWeightConversion.poundsToKilograms(160.0),
            trainingGoal = TrainingGoal.BUILD_MUSCLE
        )
        val target = NutritionTargets.calculate(user)[NutrientKey.PROTEIN]!!

        assertEquals(1.8, NutritionTargets.proteinFactor(user), 0.0)
        assertEquals(130.63460256, target.amount, 0.000001)
        assertEquals("Build muscle · 1.8 g/kg", target.recommendationLabel)
    }

    @Test
    fun generalHealthRetainsAdultRdaFactor() {
        assertEquals(64.0, NutritionTargets.calculate(profile())[NutrientKey.PROTEIN]!!.amount, 0.0)
    }

    @Test
    fun weightChangeRecalculatesRecommendationWithoutOverride() {
        val before = NutritionTargets.calculate(profile(weightKg = 70.0, trainingGoal = TrainingGoal.BUILD_MUSCLE))[NutrientKey.PROTEIN]!!
        val after = NutritionTargets.calculate(profile(weightKg = 80.0, trainingGoal = TrainingGoal.BUILD_MUSCLE))[NutrientKey.PROTEIN]!!
        assertEquals(126.0, before.amount, 0.0)
        assertEquals(144.0, after.amount, 0.0)
    }

    @Test
    fun manualProteinOverrideSurvivesWeightChangeAndCanReset() {
        val overridden = profile(
            weightKg = 70.0,
            trainingGoal = TrainingGoal.BUILD_MUSCLE,
            overrides = NutritionTargetOverrides(proteinG = 150.0)
        )
        val changedWeight = overridden.copy(weightKg = 80.0)
        val target = NutritionTargets.calculate(changedWeight)[NutrientKey.PROTEIN]!!
        assertEquals(150.0, target.amount, 0.0)
        assertEquals(144.0, target.recommendedAmount, 0.0)
        assertTrue(target.isOverridden)

        val reset = changedWeight.copy(overrides = changedWeight.overrides.copy(proteinG = null))
        val resetTarget = NutritionTargets.calculate(reset)[NutrientKey.PROTEIN]!!
        assertEquals(144.0, resetTarget.amount, 0.0)
        assertFalse(resetTarget.isOverridden)
    }

    @Test
    fun hydrationUsesBeverageWaterRecommendationAndCanonicalOverride() {
        assertEquals(3000.0, NutritionTargets.hydrationRecommendationMl(profile(sex = BiologicalSex.MALE)), 0.0)
        assertEquals(2200.0, NutritionTargets.hydrationRecommendationMl(profile(sex = BiologicalSex.FEMALE)), 0.0)

        val overridden = profile(overrides = NutritionTargetOverrides(hydrationMl = 2750.0))
        val target = NutritionTargets.hydrationTarget(overridden)
        assertEquals(2750.0, target.amountMl, 0.0)
        assertEquals(3000.0, target.recommendedMl, 0.0)
        assertTrue(target.isOverridden)
        assertEquals(11.0, WaterConversion.fromMilliliters(target.amountMl, WaterDisplayUnit.CUPS), 0.0)

        val reset = NutritionTargets.hydrationTarget(overridden.copy(overrides = overridden.overrides.copy(hydrationMl = null)))
        assertEquals(3000.0, reset.amountMl, 0.0)
        assertFalse(reset.isOverridden)
    }
}
