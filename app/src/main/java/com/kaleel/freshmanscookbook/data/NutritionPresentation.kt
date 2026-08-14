package com.kaleel.freshmanscookbook.data

import kotlin.math.roundToInt

/** Pure display helpers shared by the recipe label and forecast screens. */
object NutritionPresentation {
    fun forConsumedServings(
        wholeRecipe: NutritionTotals,
        recipeYield: Int,
        servingsConsumed: Double
    ): NutritionTotals = NutritionCalculator.multiply(
        wholeRecipe,
        if (recipeYield > 0) servingsConsumed.coerceAtLeast(0.0) / recipeYield else 0.0
    )

    /** The text percentage may exceed 100%; only the finite visual rail is capped. */
    fun percent(projected: Double, target: Double?): Int? = target
        ?.takeIf { it > 0.0 }
        ?.let { (projected.coerceAtLeast(0.0) / it * 100.0).roundToInt() }

    fun progressFraction(projected: Double, target: Double?): Float? = target
        ?.takeIf { it > 0.0 }
        ?.let { (projected.coerceAtLeast(0.0) / it).coerceIn(0.0, 1.0).toFloat() }

    /**
     * A total is only presented as known when every planned ingredient has a
     * gram amount and its source row reports this nutrient. USDA/custom nulls
     * remain unknown instead of becoming a confident zero in the UI.
     */
    fun isKnown(
        meal: MealInstance,
        foodsById: Map<String, FoodEntity>,
        nutrient: NutrientKey
    ): Boolean = meal.ingredients.isNotEmpty() && meal.ingredients.all { ingredient ->
        val grams = ingredient.gramsEquivalent
        val food = ingredient.foodId?.let(foodsById::get)
        grams != null && grams >= 0.0 && food?.nutrientPer100g(nutrient) != null
    }
}

fun FoodEntity.nutrientPer100g(nutrient: NutrientKey): Double? = when (nutrient) {
    NutrientKey.CALORIES -> caloriesKcal
    NutrientKey.PROTEIN -> proteinG
    NutrientKey.CARBOHYDRATE -> carbohydrateG
    NutrientKey.FAT -> fatG
    NutrientKey.FIBER -> fiberG
    NutrientKey.CALCIUM -> calciumMg
    NutrientKey.IRON -> ironMg
    NutrientKey.MAGNESIUM -> magnesiumMg
    NutrientKey.PHOSPHORUS -> phosphorusMg
    NutrientKey.POTASSIUM -> potassiumMg
    NutrientKey.SODIUM -> sodiumMg
    NutrientKey.ZINC -> zincMg
    NutrientKey.COPPER -> copperMg
    NutrientKey.MANGANESE -> manganeseMg
    NutrientKey.SELENIUM -> seleniumMcg
    NutrientKey.VITAMIN_A -> vitaminAMcgRae
    NutrientKey.VITAMIN_C -> vitaminCMg
    NutrientKey.VITAMIN_D -> vitaminDMcg
    NutrientKey.VITAMIN_E -> vitaminEMg
    NutrientKey.VITAMIN_K -> vitaminKMcg
    NutrientKey.THIAMIN_B1 -> thiaminB1Mg
    NutrientKey.RIBOFLAVIN_B2 -> riboflavinB2Mg
    NutrientKey.NIACIN_B3 -> niacinB3Mg
    NutrientKey.PANTOTHENIC_ACID_B5 -> pantothenicAcidB5Mg
    NutrientKey.VITAMIN_B6 -> vitaminB6Mg
    NutrientKey.FOLATE -> folateMcgDfe ?: folateMcg
    NutrientKey.VITAMIN_B12 -> vitaminB12Mcg
    NutrientKey.CHOLINE -> cholineMg
    NutrientKey.SATURATED_FAT -> saturatedFatG
    NutrientKey.MONOUNSATURATED_FAT -> monounsaturatedFatG
    NutrientKey.POLYUNSATURATED_FAT -> polyunsaturatedFatG
    NutrientKey.CHOLESTEROL -> cholesterolMg
}
