package com.kaleel.freshmanscookbook.data

/**
 * Pure nutrition math for foods, recipe ingredients, meal instances,
 * forecasts, pinned-nutrient views, and recommendation logic.
 *
 * All USDA FoodEntity nutrient values are stored per 100 g. This calculator
 * scales them to an actual gram amount, then sums them.
 *
 * This file intentionally has no Room, Android, or Compose dependencies so it
 * is easy to unit test.
 */
object NutritionCalculator {

    /**
     * Scale one food's per-100-g nutrition to an actual gram amount.
     *
     * USDA nulls mean "not available", not necessarily zero. NutritionTotals
     * currently stores numeric totals only, so unavailable values contribute
     * 0 to the aggregate. If we later want completeness indicators, they should
     * be tracked separately rather than changing this arithmetic.
     */
    fun forFood(food: FoodEntity, grams: Double): NutritionTotals {
        if (grams <= 0.0) return NutritionTotals()

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

    /**
     * Sum any collection of nutrition totals.
     */
    fun sum(items: Iterable<NutritionTotals>): NutritionTotals =
        items.fold(NutritionTotals(), ::add)

    /**
     * Add two nutrient totals.
     */
    fun add(a: NutritionTotals, b: NutritionTotals): NutritionTotals =
        NutritionTotals(
            caloriesKcal = a.caloriesKcal + b.caloriesKcal,
            proteinG = a.proteinG + b.proteinG,
            carbohydrateG = a.carbohydrateG + b.carbohydrateG,
            fatG = a.fatG + b.fatG,
            fiberG = a.fiberG + b.fiberG,
            totalSugarsG = a.totalSugarsG + b.totalSugarsG,

            calciumMg = a.calciumMg + b.calciumMg,
            ironMg = a.ironMg + b.ironMg,
            magnesiumMg = a.magnesiumMg + b.magnesiumMg,
            phosphorusMg = a.phosphorusMg + b.phosphorusMg,
            potassiumMg = a.potassiumMg + b.potassiumMg,
            sodiumMg = a.sodiumMg + b.sodiumMg,
            zincMg = a.zincMg + b.zincMg,
            copperMg = a.copperMg + b.copperMg,
            manganeseMg = a.manganeseMg + b.manganeseMg,
            seleniumMcg = a.seleniumMcg + b.seleniumMcg,

            vitaminAMcgRae = a.vitaminAMcgRae + b.vitaminAMcgRae,
            vitaminCMg = a.vitaminCMg + b.vitaminCMg,
            vitaminDMcg = a.vitaminDMcg + b.vitaminDMcg,
            vitaminEMg = a.vitaminEMg + b.vitaminEMg,
            vitaminKMcg = a.vitaminKMcg + b.vitaminKMcg,
            thiaminB1Mg = a.thiaminB1Mg + b.thiaminB1Mg,
            riboflavinB2Mg = a.riboflavinB2Mg + b.riboflavinB2Mg,
            niacinB3Mg = a.niacinB3Mg + b.niacinB3Mg,
            pantothenicAcidB5Mg = a.pantothenicAcidB5Mg + b.pantothenicAcidB5Mg,
            vitaminB6Mg = a.vitaminB6Mg + b.vitaminB6Mg,
            folateMcg = a.folateMcg + b.folateMcg,
            folateMcgDfe = a.folateMcgDfe + b.folateMcgDfe,
            vitaminB12Mcg = a.vitaminB12Mcg + b.vitaminB12Mcg,
            cholineMg = a.cholineMg + b.cholineMg,

            saturatedFatG = a.saturatedFatG + b.saturatedFatG,
            monounsaturatedFatG = a.monounsaturatedFatG + b.monounsaturatedFatG,
            polyunsaturatedFatG = a.polyunsaturatedFatG + b.polyunsaturatedFatG,
            cholesterolMg = a.cholesterolMg + b.cholesterolMg
        )

    /**
     * Multiply an already-calculated nutrition total.
     *
     * Useful for meal servings, forecasts, and "what if I eat 1.5 servings?"
     */
    fun multiply(total: NutritionTotals, multiplier: Double): NutritionTotals {
        if (multiplier <= 0.0) return NutritionTotals()

        return NutritionTotals(
            caloriesKcal = total.caloriesKcal * multiplier,
            proteinG = total.proteinG * multiplier,
            carbohydrateG = total.carbohydrateG * multiplier,
            fatG = total.fatG * multiplier,
            fiberG = total.fiberG * multiplier,
            totalSugarsG = total.totalSugarsG * multiplier,

            calciumMg = total.calciumMg * multiplier,
            ironMg = total.ironMg * multiplier,
            magnesiumMg = total.magnesiumMg * multiplier,
            phosphorusMg = total.phosphorusMg * multiplier,
            potassiumMg = total.potassiumMg * multiplier,
            sodiumMg = total.sodiumMg * multiplier,
            zincMg = total.zincMg * multiplier,
            copperMg = total.copperMg * multiplier,
            manganeseMg = total.manganeseMg * multiplier,
            seleniumMcg = total.seleniumMcg * multiplier,

            vitaminAMcgRae = total.vitaminAMcgRae * multiplier,
            vitaminCMg = total.vitaminCMg * multiplier,
            vitaminDMcg = total.vitaminDMcg * multiplier,
            vitaminEMg = total.vitaminEMg * multiplier,
            vitaminKMcg = total.vitaminKMcg * multiplier,
            thiaminB1Mg = total.thiaminB1Mg * multiplier,
            riboflavinB2Mg = total.riboflavinB2Mg * multiplier,
            niacinB3Mg = total.niacinB3Mg * multiplier,
            pantothenicAcidB5Mg = total.pantothenicAcidB5Mg * multiplier,
            vitaminB6Mg = total.vitaminB6Mg * multiplier,
            folateMcg = total.folateMcg * multiplier,
            folateMcgDfe = total.folateMcgDfe * multiplier,
            vitaminB12Mcg = total.vitaminB12Mcg * multiplier,
            cholineMg = total.cholineMg * multiplier,

            saturatedFatG = total.saturatedFatG * multiplier,
            monounsaturatedFatG = total.monounsaturatedFatG * multiplier,
            polyunsaturatedFatG = total.polyunsaturatedFatG * multiplier,
            cholesterolMg = total.cholesterolMg * multiplier
        )
    }

    /**
     * Divide a full recipe into equal servings.
     */
    fun perServing(totalRecipe: NutritionTotals, servings: Int): NutritionTotals {
        if (servings <= 0) return totalRecipe
        return multiply(totalRecipe, 1.0 / servings.toDouble())
    }

    /**
     * Calculate a recipe using the linked FoodEntity records.
     *
     * - Ingredients without a foodId are ignored nutritionally.
     * - Ingredients without a valid gramsEquivalent are ignored.
     * - This is intentional: we do not guess nutrition or weights.
     */
    fun forRecipe(
        recipe: Recipe,
        foodsById: Map<String, FoodEntity>
    ): NutritionTotals {
        return sum(
            recipe.ingredients.mapNotNull { ingredient ->
                nutritionForIngredient(ingredient, foodsById)
            }
        )
    }

    /**
     * Same calculation but only for selected ingredient IDs.
     *
     * This is the foundation for #4: checked ingredients count; unchecked
     * ingredients do not.
     */
    fun forRecipe(
        recipe: Recipe,
        foodsById: Map<String, FoodEntity>,
        includedIngredientIds: Set<String>
    ): NutritionTotals {
        return sum(
            recipe.ingredients
                .filter { it.id in includedIngredientIds }
                .mapNotNull { nutritionForIngredient(it, foodsById) }
        )
    }

    /**
     * Nutrition contribution for a single recipe ingredient.
     *
     * This directly supports #12/#13, where a pinned nutrient can show each
     * ingredient's contribution.
     */
    fun nutritionForIngredient(
        ingredient: Ingredient,
        foodsById: Map<String, FoodEntity>
    ): NutritionTotals? {
        val foodId = ingredient.foodId ?: return null
        val grams = ingredient.gramsEquivalent ?: return null
        if (grams <= 0.0) return null

        val food = foodsById[foodId] ?: return null
        return forFood(food, grams)
    }

    /**
     * Return one nutrient from NutritionTotals using the stable NutrientKey
     * shared by Profile, Dashboard, Forecast, and suggestion features.
     */
    fun valueFor(total: NutritionTotals, nutrient: NutrientKey): Double =
        when (nutrient) {
            NutrientKey.CALORIES -> total.caloriesKcal
            NutrientKey.PROTEIN -> total.proteinG
            NutrientKey.CARBOHYDRATE -> total.carbohydrateG
            NutrientKey.FAT -> total.fatG
            NutrientKey.FIBER -> total.fiberG

            NutrientKey.CALCIUM -> total.calciumMg
            NutrientKey.IRON -> total.ironMg
            NutrientKey.MAGNESIUM -> total.magnesiumMg
            NutrientKey.PHOSPHORUS -> total.phosphorusMg
            NutrientKey.POTASSIUM -> total.potassiumMg
            NutrientKey.SODIUM -> total.sodiumMg
            NutrientKey.ZINC -> total.zincMg
            NutrientKey.COPPER -> total.copperMg
            NutrientKey.MANGANESE -> total.manganeseMg
            NutrientKey.SELENIUM -> total.seleniumMcg

            NutrientKey.VITAMIN_A -> total.vitaminAMcgRae
            NutrientKey.VITAMIN_C -> total.vitaminCMg
            NutrientKey.VITAMIN_D -> total.vitaminDMcg
            NutrientKey.VITAMIN_E -> total.vitaminEMg
            NutrientKey.VITAMIN_K -> total.vitaminKMcg
            NutrientKey.THIAMIN_B1 -> total.thiaminB1Mg
            NutrientKey.RIBOFLAVIN_B2 -> total.riboflavinB2Mg
            NutrientKey.NIACIN_B3 -> total.niacinB3Mg
            NutrientKey.PANTOTHENIC_ACID_B5 -> total.pantothenicAcidB5Mg
            NutrientKey.VITAMIN_B6 -> total.vitaminB6Mg
            NutrientKey.FOLATE ->
                if (total.folateMcgDfe > 0.0) total.folateMcgDfe else total.folateMcg
            NutrientKey.VITAMIN_B12 -> total.vitaminB12Mcg
            NutrientKey.CHOLINE -> total.cholineMg

            NutrientKey.SATURATED_FAT -> total.saturatedFatG
            NutrientKey.MONOUNSATURATED_FAT -> total.monounsaturatedFatG
            NutrientKey.POLYUNSATURATED_FAT -> total.polyunsaturatedFatG
            NutrientKey.CHOLESTEROL -> total.cholesterolMg
        }

    /**
     * Rank linked recipe ingredients by their contribution to one nutrient.
     *
     * Example:
     * rankIngredientContributions(recipe, foods, NutrientKey.PROTEIN)
     * returns chicken ahead of broccoli when chicken contributes more protein.
     *
     * This will power the future "Chicken contributes most of this meal's
     * protein" suggestion without requiring an LLM.
     */
    fun rankIngredientContributions(
        recipe: Recipe,
        foodsById: Map<String, FoodEntity>,
        nutrient: NutrientKey
    ): List<IngredientNutrientContribution> =
        recipe.ingredients
            .mapNotNull { ingredient ->
                val nutrition = nutritionForIngredient(ingredient, foodsById)
                    ?: return@mapNotNull null

                IngredientNutrientContribution(
                    ingredientId = ingredient.id,
                    ingredientName = ingredient.name,
                    nutrient = nutrient,
                    amount = valueFor(nutrition, nutrient)
                )
            }
            .filter { it.amount > 0.0 }
            .sortedByDescending { it.amount }
}

/**
 * Explainable nutrient contribution from one ingredient.
 *
 * Kept outside the calculator object so UI/recommendation code can use it
 * directly later.
 */
data class IngredientNutrientContribution(
    val ingredientId: String,
    val ingredientName: String,
    val nutrient: NutrientKey,
    val amount: Double
)
