package com.kaleel.freshmanscookbook.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Aggregated nutrition for a caller-supplied time window.
 *
 * A daily snapshot contains both recipe-based meal logs and individually
 * logged foods/snacks.
 */
data class DailyNutritionSnapshot(
    val startInclusive: Long,
    val endExclusive: Long,
    val meals: List<MealLogWithIngredients>,
    val foods: List<FoodLogEntity>,
    val totals: NutritionTotals
) {
    fun progress(
        targets: DailyNutritionTargets?
    ): List<NutrientProgress> =
        NutrientCatalog.all.map { metadata ->
            NutrientProgress(
                nutrient = metadata.key,
                consumed = MealNutritionCalculator.nutrientValue(
                    totals,
                    metadata.key
                ),
                target = targets?.get(metadata.key)?.amount
            )
        }
}

class DailyNutritionRepository(
    database: CookbookDatabase
) {
    private val mealLogDao = database.mealLogDao()
    private val foodLogDao = database.foodLogDao()

    fun observeBetween(
        startInclusive: Long,
        endExclusive: Long
    ): Flow<DailyNutritionSnapshot> =
        combine(
            mealLogDao.observeBetween(startInclusive, endExclusive),
            foodLogDao.observeBetween(startInclusive, endExclusive)
        ) { meals, foods ->
            snapshot(
                startInclusive = startInclusive,
                endExclusive = endExclusive,
                meals = meals,
                foods = foods
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
            ),
            foods = foodLogDao.getBetween(
                startInclusive,
                endExclusive
            )
        )

    private fun snapshot(
        startInclusive: Long,
        endExclusive: Long,
        meals: List<MealLogWithIngredients>,
        foods: List<FoodLogEntity>
    ): DailyNutritionSnapshot {
        var totals = NutritionTotals()

        meals.forEach { record ->
            totals = add(totals, record.meal.nutritionTotals())
        }

        foods.forEach { food ->
            totals = add(totals, food.nutrition)
        }

        return DailyNutritionSnapshot(
            startInclusive = startInclusive,
            endExclusive = endExclusive,
            meals = meals,
            foods = foods,
            totals = totals
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
}