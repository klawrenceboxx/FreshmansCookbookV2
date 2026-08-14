package com.kaleel.freshmanscookbook.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Aggregated nutrition for a caller-supplied time window.
 *
 * Daily UI should pass the local day's [startInclusive, endExclusive) epoch
 * boundaries. Keeping timezone/date boundary calculation outside this data
 * class avoids hiding timezone assumptions in persistence code.
 */
data class DailyNutritionSnapshot(
    val startInclusive: Long,
    val endExclusive: Long,
    val meals: List<MealLogWithIngredients>,
    val totals: NutritionTotals
) {
    /**
     * Converts the aggregate into the same progress model used by future
     * dashboard and forecast screens.
     */
    fun progress(
        targets: DailyNutritionTargets?
    ): List<NutrientProgress> =
        NutrientCatalog.all.map { metadata ->
            val target = targets?.get(metadata.key)?.amount

            NutrientProgress(
                nutrient = metadata.key,
                consumed = MealNutritionCalculator.nutrientValue(
                    totals,
                    metadata.key
                ),
                target = target
            )
        }
}

class DailyNutritionRepository(
    database: CookbookDatabase
) {
    private val mealLogDao = database.mealLogDao()

    fun observeBetween(
        startInclusive: Long,
        endExclusive: Long
    ): Flow<DailyNutritionSnapshot> =
        mealLogDao
            .observeBetween(startInclusive, endExclusive)
            .map { meals ->
                snapshot(
                    startInclusive = startInclusive,
                    endExclusive = endExclusive,
                    meals = meals
                )
            }

    suspend fun getBetween(
        startInclusive: Long,
        endExclusive: Long
    ): DailyNutritionSnapshot =
        snapshot(
            startInclusive = startInclusive,
            endExclusive = endExclusive,
            meals = mealLogDao.getBetween(
                startInclusive,
                endExclusive
            )
        )

    private fun snapshot(
        startInclusive: Long,
        endExclusive: Long,
        meals: List<MealLogWithIngredients>
    ): DailyNutritionSnapshot =
        DailyNutritionSnapshot(
            startInclusive = startInclusive,
            endExclusive = endExclusive,
            meals = meals,
            totals = meals.fold(NutritionTotals()) { running, record ->
                add(running, record.meal.nutritionTotals())
            }
        )

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
}
