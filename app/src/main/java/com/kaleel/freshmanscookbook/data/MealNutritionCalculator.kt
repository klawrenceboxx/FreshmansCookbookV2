package com.kaleel.freshmanscookbook.data

/**
 * Nutrition calculations for a temporary MealInstance.
 *
 * This file intentionally depends only on stable domain models. It does not
 * depend on Room, Android, Compose, or the exact public method names of the
 * separately-created NutritionCalculator.kt.
 *
 * Once NutritionCalculator.kt is committed and its API is stable, duplicated
 * low-level arithmetic here can be consolidated if desired.
 */
object MealNutritionCalculator {

    data class IngredientContribution(
        val ingredientId: String,
        val ingredientName: String,
        val nutrient: NutrientKey,
        val amount: Double
    )

    /**
     * Nutrition for the complete planned meal, regardless of checkbox state.
     *
     * This is the appropriate basis for "If I eat this" forecasting.
     */
    fun plannedTotals(
        meal: MealInstance,
        foodsById: Map<String, FoodEntity>
    ): NutritionTotals =
        totalsForIngredients(meal.ingredients, foodsById)

    /**
     * Nutrition for only the ingredients confirmed with cooking checkboxes.
     *
     * This represents the final prepared meal before applying the fraction of
     * recipe servings that the user actually ate.
     */
    fun checkedTotals(
        meal: MealInstance,
        foodsById: Map<String, FoodEntity>
    ): NutritionTotals =
        totalsForIngredients(
            ingredients = meal.ingredients.filter { it.isChecked },
            foodsById = foodsById
        )

    /**
     * Nutrition actually consumed after both checkbox filtering and serving
     * scaling are applied.
     */
    fun consumedTotals(
        meal: MealInstance,
        foodsById: Map<String, FoodEntity>
    ): NutritionTotals =
        multiply(
            totals = checkedTotals(meal, foodsById),
            factor = meal.consumedServingScale()
        )

    /**
     * Nutrition for a single temporary ingredient at its current gram amount.
     *
     * Unlinked ingredients or ingredients without a defensible gram conversion
     * contribute zero until they are linked/resolved.
     */
    fun ingredientTotals(
        ingredient: MealIngredient,
        foodsById: Map<String, FoodEntity>
    ): NutritionTotals {
        val foodId = ingredient.foodId ?: return NutritionTotals()
        val grams = ingredient.gramsEquivalent
            ?.takeIf { it > 0.0 }
            ?: return NutritionTotals()
        val food = foodsById[foodId] ?: return NutritionTotals()

        return scaleFood(food, grams)
    }

    /**
     * Ranks meal ingredients by their contribution to a pinned nutrient.
     *
     * By default this describes the planned meal. Set checkedOnly=true when
     * describing what actually made it into the prepared meal.
     *
     * applyConsumedServingScale is useful when the UI should show contribution
     * to the amount the user will personally consume rather than the whole
     * prepared recipe.
     */
    fun rankedContributions(
        meal: MealInstance,
        foodsById: Map<String, FoodEntity>,
        nutrient: NutrientKey,
        checkedOnly: Boolean = false,
        applyConsumedServingScale: Boolean = false
    ): List<IngredientContribution> {
        val servingScale =
            if (applyConsumedServingScale) meal.consumedServingScale() else 1.0

        return meal.ingredients
            .asSequence()
            .filter { !checkedOnly || it.isChecked }
            .map { ingredient ->
                val amount = nutrientValue(
                    ingredientTotals(ingredient, foodsById),
                    nutrient
                ) * servingScale

                IngredientContribution(
                    ingredientId = ingredient.id,
                    ingredientName = ingredient.name,
                    nutrient = nutrient,
                    amount = amount
                )
            }
            .filter { it.amount > 0.0 }
            .sortedByDescending { it.amount }
            .toList()
    }

    fun nutrientValue(
        totals: NutritionTotals,
        nutrient: NutrientKey
    ): Double = when (nutrient) {
        NutrientKey.CALORIES -> totals.caloriesKcal
        NutrientKey.PROTEIN -> totals.proteinG
        NutrientKey.CARBOHYDRATE -> totals.carbohydrateG
        NutrientKey.FAT -> totals.fatG
        NutrientKey.FIBER -> totals.fiberG

        NutrientKey.CALCIUM -> totals.calciumMg
        NutrientKey.IRON -> totals.ironMg
        NutrientKey.MAGNESIUM -> totals.magnesiumMg
        NutrientKey.PHOSPHORUS -> totals.phosphorusMg
        NutrientKey.POTASSIUM -> totals.potassiumMg
        NutrientKey.SODIUM -> totals.sodiumMg
        NutrientKey.ZINC -> totals.zincMg
        NutrientKey.COPPER -> totals.copperMg
        NutrientKey.MANGANESE -> totals.manganeseMg
        NutrientKey.SELENIUM -> totals.seleniumMcg

        NutrientKey.VITAMIN_A -> totals.vitaminAMcgRae
        NutrientKey.VITAMIN_C -> totals.vitaminCMg
        NutrientKey.VITAMIN_D -> totals.vitaminDMcg
        NutrientKey.VITAMIN_E -> totals.vitaminEMg
        NutrientKey.VITAMIN_K -> totals.vitaminKMcg
        NutrientKey.THIAMIN_B1 -> totals.thiaminB1Mg
        NutrientKey.RIBOFLAVIN_B2 -> totals.riboflavinB2Mg
        NutrientKey.NIACIN_B3 -> totals.niacinB3Mg
        NutrientKey.PANTOTHENIC_ACID_B5 -> totals.pantothenicAcidB5Mg
        NutrientKey.VITAMIN_B6 -> totals.vitaminB6Mg

        // The app's NutrientCatalog presents folate as DFE. USDA rows that do
        // not expose DFE may still have folateMcg, so fall back to that rather
        // than silently showing zero.
        NutrientKey.FOLATE ->
            if (totals.folateMcgDfe > 0.0) totals.folateMcgDfe else totals.folateMcg

        NutrientKey.VITAMIN_B12 -> totals.vitaminB12Mcg
        NutrientKey.CHOLINE -> totals.cholineMg

        NutrientKey.SATURATED_FAT -> totals.saturatedFatG
        NutrientKey.MONOUNSATURATED_FAT -> totals.monounsaturatedFatG
        NutrientKey.POLYUNSATURATED_FAT -> totals.polyunsaturatedFatG
        NutrientKey.CHOLESTEROL -> totals.cholesterolMg
    }

    private fun totalsForIngredients(
        ingredients: List<MealIngredient>,
        foodsById: Map<String, FoodEntity>
    ): NutritionTotals =
        ingredients.fold(NutritionTotals()) { total, ingredient ->
            add(total, ingredientTotals(ingredient, foodsById))
        }

    private fun scaleFood(
        food: FoodEntity,
        grams: Double
    ): NutritionTotals {
        val factor = grams / 100.0

        fun scaled(value: Double?): Double = (value ?: 0.0) * factor

        return NutritionTotals(
            caloriesKcal = scaled(food.caloriesKcal),
            proteinG = scaled(food.proteinG),
            carbohydrateG = scaled(food.carbohydrateG),
            fatG = scaled(food.fatG),
            fiberG = scaled(food.fiberG),
            totalSugarsG = scaled(food.totalSugarsG),

            calciumMg = scaled(food.calciumMg),
            ironMg = scaled(food.ironMg),
            magnesiumMg = scaled(food.magnesiumMg),
            phosphorusMg = scaled(food.phosphorusMg),
            potassiumMg = scaled(food.potassiumMg),
            sodiumMg = scaled(food.sodiumMg),
            zincMg = scaled(food.zincMg),
            copperMg = scaled(food.copperMg),
            manganeseMg = scaled(food.manganeseMg),
            seleniumMcg = scaled(food.seleniumMcg),

            vitaminAMcgRae = scaled(food.vitaminAMcgRae),
            vitaminCMg = scaled(food.vitaminCMg),
            vitaminDMcg = scaled(food.vitaminDMcg),
            vitaminEMg = scaled(food.vitaminEMg),
            vitaminKMcg = scaled(food.vitaminKMcg),
            thiaminB1Mg = scaled(food.thiaminB1Mg),
            riboflavinB2Mg = scaled(food.riboflavinB2Mg),
            niacinB3Mg = scaled(food.niacinB3Mg),
            pantothenicAcidB5Mg = scaled(food.pantothenicAcidB5Mg),
            vitaminB6Mg = scaled(food.vitaminB6Mg),
            folateMcg = scaled(food.folateMcg),
            folateMcgDfe = scaled(food.folateMcgDfe),
            vitaminB12Mcg = scaled(food.vitaminB12Mcg),
            cholineMg = scaled(food.cholineMg),

            saturatedFatG = scaled(food.saturatedFatG),
            monounsaturatedFatG = scaled(food.monounsaturatedFatG),
            polyunsaturatedFatG = scaled(food.polyunsaturatedFatG),
            cholesterolMg = scaled(food.cholesterolMg)
        )
    }

    private fun add(
        first: NutritionTotals,
        second: NutritionTotals
    ): NutritionTotals = NutritionTotals(
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
        pantothenicAcidB5Mg = first.pantothenicAcidB5Mg + second.pantothenicAcidB5Mg,
        vitaminB6Mg = first.vitaminB6Mg + second.vitaminB6Mg,
        folateMcg = first.folateMcg + second.folateMcg,
        folateMcgDfe = first.folateMcgDfe + second.folateMcgDfe,
        vitaminB12Mcg = first.vitaminB12Mcg + second.vitaminB12Mcg,
        cholineMg = first.cholineMg + second.cholineMg,

        saturatedFatG = first.saturatedFatG + second.saturatedFatG,
        monounsaturatedFatG = first.monounsaturatedFatG + second.monounsaturatedFatG,
        polyunsaturatedFatG = first.polyunsaturatedFatG + second.polyunsaturatedFatG,
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
