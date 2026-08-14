package com.kaleel.freshmanscookbook.data

/**
 * Result of asking: "If I eat this meal, where will my day land?"
 */
data class NutritionForecastResult(
    val currentReport: NutritionCompletenessReport,
    val mealReport: NutritionCompletenessReport,
    val projectedReport: NutritionCompletenessReport,
    val progress: List<NutrientProgress>
) {
    val current: NutritionTotals get() = currentReport.totals
    val meal: NutritionTotals get() = mealReport.totals
    val projected: NutritionTotals get() = projectedReport.totals
}

/**
 * Pure deterministic forecast logic.
 *
 * Forecasting uses every planned meal ingredient, regardless of cooking
 * checkbox state, then scales the planned recipe by servingsConsumed.
 * Checkboxes are for what was actually used when logging the finished meal.
 */
object NutritionForecast {

    fun calculate(
        current: NutritionTotals,
        meal: MealInstance,
        foodsById: Map<String, FoodEntity>,
        targets: DailyNutritionTargets?
    ): NutritionForecastResult = calculate(
        current = NutritionCompletenessReport(
            totals = current,
            completeness = NutrientKey.entries.associateWith { NutrientCompleteness.COMPLETE }
        ),
        meal = meal,
        foodsById = foodsById,
        targets = targets
    )

    fun calculate(
        current: NutritionCompletenessReport,
        meal: MealInstance,
        foodsById: Map<String, FoodEntity>,
        targets: DailyNutritionTargets?
    ): NutritionForecastResult {
        val proposedMeal = NutritionCompletenessCalculator.forMeal(
            meal = meal,
            foodsById = foodsById,
            applyConsumedServingScale = true
        )
        val projected = NutritionCompletenessCalculator.aggregate(listOf(current, proposedMeal))

        val progress = NutrientCatalog.all.map { metadata ->
            NutrientProgress(
                nutrient = metadata.key,
                consumed = MealNutritionCalculator.nutrientValue(
                    current.totals,
                    metadata.key
                ),
                target = targets?.get(metadata.key)?.amount,
                projected = MealNutritionCalculator.nutrientValue(
                    projected.totals,
                    metadata.key
                )
            )
        }

        return NutritionForecastResult(
            currentReport = current,
            mealReport = proposedMeal,
            projectedReport = projected,
            progress = progress
        )
    }

    private fun add(
        first: NutritionTotals,
        second: NutritionTotals
    ): NutritionTotals =
        NutritionTotals(
            caloriesKcal = first.caloriesKcal + second.caloriesKcal,
            proteinG = first.proteinG + second.proteinG,
            carbohydrateG = first.carbohydrateG + second.carbohydrateG,
            fatG = first.fatG + second.fatG,
            fiberG = first.fiberG + second.fiberG,
            totalSugarsG = first.totalSugarsG + second.totalSugarsG,

            calciumMg = first.calciumMg + second.calciumMg,
            ironMg = first.ironMg + second.ironMg,
            magnesiumMg = first.magnesiumMg + second.magnesiumMg,
            phosphorusMg = first.phosphorusMg + second.phosphorusMg,
            potassiumMg = first.potassiumMg + second.potassiumMg,
            sodiumMg = first.sodiumMg + second.sodiumMg,
            zincMg = first.zincMg + second.zincMg,
            copperMg = first.copperMg + second.copperMg,
            manganeseMg = first.manganeseMg + second.manganeseMg,
            seleniumMcg = first.seleniumMcg + second.seleniumMcg,

            vitaminAMcgRae = first.vitaminAMcgRae + second.vitaminAMcgRae,
            vitaminCMg = first.vitaminCMg + second.vitaminCMg,
            vitaminDMcg = first.vitaminDMcg + second.vitaminDMcg,
            vitaminEMg = first.vitaminEMg + second.vitaminEMg,
            vitaminKMcg = first.vitaminKMcg + second.vitaminKMcg,
            thiaminB1Mg = first.thiaminB1Mg + second.thiaminB1Mg,
            riboflavinB2Mg = first.riboflavinB2Mg + second.riboflavinB2Mg,
            niacinB3Mg = first.niacinB3Mg + second.niacinB3Mg,
            pantothenicAcidB5Mg =
                first.pantothenicAcidB5Mg + second.pantothenicAcidB5Mg,
            vitaminB6Mg = first.vitaminB6Mg + second.vitaminB6Mg,
            folateMcg = first.folateMcg + second.folateMcg,
            folateMcgDfe = first.folateMcgDfe + second.folateMcgDfe,
            vitaminB12Mcg = first.vitaminB12Mcg + second.vitaminB12Mcg,
            cholineMg = first.cholineMg + second.cholineMg,

            saturatedFatG = first.saturatedFatG + second.saturatedFatG,
            monounsaturatedFatG =
                first.monounsaturatedFatG + second.monounsaturatedFatG,
            polyunsaturatedFatG =
                first.polyunsaturatedFatG + second.polyunsaturatedFatG,
            cholesterolMg = first.cholesterolMg + second.cholesterolMg
        )

    private fun multiply(
        totals: NutritionTotals,
        factor: Double
    ): NutritionTotals {
        val safeFactor = factor.coerceAtLeast(0.0)

        return NutritionTotals(
            caloriesKcal = totals.caloriesKcal * safeFactor,
            proteinG = totals.proteinG * safeFactor,
            carbohydrateG = totals.carbohydrateG * safeFactor,
            fatG = totals.fatG * safeFactor,
            fiberG = totals.fiberG * safeFactor,
            totalSugarsG = totals.totalSugarsG * safeFactor,

            calciumMg = totals.calciumMg * safeFactor,
            ironMg = totals.ironMg * safeFactor,
            magnesiumMg = totals.magnesiumMg * safeFactor,
            phosphorusMg = totals.phosphorusMg * safeFactor,
            potassiumMg = totals.potassiumMg * safeFactor,
            sodiumMg = totals.sodiumMg * safeFactor,
            zincMg = totals.zincMg * safeFactor,
            copperMg = totals.copperMg * safeFactor,
            manganeseMg = totals.manganeseMg * safeFactor,
            seleniumMcg = totals.seleniumMcg * safeFactor,

            vitaminAMcgRae = totals.vitaminAMcgRae * safeFactor,
            vitaminCMg = totals.vitaminCMg * safeFactor,
            vitaminDMcg = totals.vitaminDMcg * safeFactor,
            vitaminEMg = totals.vitaminEMg * safeFactor,
            vitaminKMcg = totals.vitaminKMcg * safeFactor,
            thiaminB1Mg = totals.thiaminB1Mg * safeFactor,
            riboflavinB2Mg = totals.riboflavinB2Mg * safeFactor,
            niacinB3Mg = totals.niacinB3Mg * safeFactor,
            pantothenicAcidB5Mg = totals.pantothenicAcidB5Mg * safeFactor,
            vitaminB6Mg = totals.vitaminB6Mg * safeFactor,
            folateMcg = totals.folateMcg * safeFactor,
            folateMcgDfe = totals.folateMcgDfe * safeFactor,
            vitaminB12Mcg = totals.vitaminB12Mcg * safeFactor,
            cholineMg = totals.cholineMg * safeFactor,

            saturatedFatG = totals.saturatedFatG * safeFactor,
            monounsaturatedFatG = totals.monounsaturatedFatG * safeFactor,
            polyunsaturatedFatG = totals.polyunsaturatedFatG * safeFactor,
            cholesterolMg = totals.cholesterolMg * safeFactor
        )
    }
}
