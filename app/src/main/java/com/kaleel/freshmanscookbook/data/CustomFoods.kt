package com.kaleel.freshmanscookbook.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** User-authored nutrition as entered per serving. Null means not provided. */
@Entity(
    tableName = "custom_foods",
    indices = [Index(value = ["searchName"])]
)
data class CustomFoodEntity(
    @PrimaryKey val foodId: String,
    val name: String,
    val searchName: String,
    val description: String?,
    val servingQuantity: Double,
    val servingUnit: IngredientUnit,
    val servingGrams: Double?,
    val caloriesKcal: Double?,
    val proteinG: Double?,
    val carbohydrateG: Double?,
    val fatG: Double?,
    val fiberG: Double?,
    val totalSugarsG: Double?,
    val calciumMg: Double?,
    val ironMg: Double?,
    val magnesiumMg: Double?,
    val phosphorusMg: Double?,
    val potassiumMg: Double?,
    val sodiumMg: Double?,
    val zincMg: Double?,
    val copperMg: Double?,
    val manganeseMg: Double?,
    val seleniumMcg: Double?,
    val vitaminAMcgRae: Double?,
    val vitaminCMg: Double?,
    val vitaminDMcg: Double?,
    val vitaminEMg: Double?,
    val vitaminKMcg: Double?,
    val thiaminB1Mg: Double?,
    val riboflavinB2Mg: Double?,
    val niacinB3Mg: Double?,
    val pantothenicAcidB5Mg: Double?,
    val vitaminB6Mg: Double?,
    val folateMcg: Double?,
    val folateMcgDfe: Double?,
    val vitaminB12Mcg: Double?,
    val cholineMg: Double?,
    val saturatedFatG: Double?,
    val monounsaturatedFatG: Double?,
    val polyunsaturatedFatG: Double?,
    val cholesterolMg: Double?,
    val createdAt: Long,
    val updatedAt: Long
)

data class CustomFoodInput(
    val foodId: String? = null,
    val name: String,
    val description: String? = null,
    val servingQuantity: Double,
    val servingUnit: IngredientUnit,
    val servingGrams: Double? = null,
    val totalSugarsG: Double? = null,
    val nutrients: Map<NutrientKey, Double?> = emptyMap()
)

data class FoodSearchResult(
    val food: FoodEntity,
    val customFood: CustomFoodEntity? = null
) {
    val source: FoodSource get() = food.foodSource
    val servingLabel: String?
        get() = customFood?.let {
            val quantity = formatCustomNumber(it.servingQuantity)
            val grams = it.servingGrams?.let(::formatCustomNumber)
            "$quantity ${it.servingUnit.label}${grams?.let { value -> " = $value g" }.orEmpty()}"
        }
}

fun resolvedCustomServingGrams(input: CustomFoodInput): Double? {
    if (input.servingQuantity <= 0.0) return null
    val exactMass = when (input.servingUnit) {
        IngredientUnit.G -> input.servingQuantity
        IngredientUnit.KG -> input.servingQuantity * 1_000.0
        IngredientUnit.OZ -> input.servingQuantity * 28.349523125
        IngredientUnit.LB -> input.servingQuantity * 453.59237
        else -> null
    }
    return exactMass ?: input.servingGrams?.takeIf { it > 0.0 }
}

fun CustomFoodInput.toEntities(
    existingCreatedAt: Long? = null,
    now: Long = System.currentTimeMillis()
): Pair<CustomFoodEntity, FoodEntity> {
    val id = foodId ?: "custom:${UUID.randomUUID()}"
    val grams = resolvedCustomServingGrams(this)
    fun per100(key: NutrientKey): Double? = nutrients[key]?.let { value ->
        grams?.takeIf { it > 0.0 }?.let { value * 100.0 / it }
    }
    val custom = CustomFoodEntity(
        foodId = id,
        name = name.trim(),
        searchName = normalizeFoodSearchName(name),
        description = description?.trim()?.takeIf(String::isNotBlank),
        servingQuantity = servingQuantity,
        servingUnit = servingUnit,
        servingGrams = grams,
        caloriesKcal = nutrients[NutrientKey.CALORIES],
        proteinG = nutrients[NutrientKey.PROTEIN],
        carbohydrateG = nutrients[NutrientKey.CARBOHYDRATE],
        fatG = nutrients[NutrientKey.FAT],
        fiberG = nutrients[NutrientKey.FIBER],
        totalSugarsG = totalSugarsG,
        calciumMg = nutrients[NutrientKey.CALCIUM], ironMg = nutrients[NutrientKey.IRON],
        magnesiumMg = nutrients[NutrientKey.MAGNESIUM], phosphorusMg = nutrients[NutrientKey.PHOSPHORUS],
        potassiumMg = nutrients[NutrientKey.POTASSIUM], sodiumMg = nutrients[NutrientKey.SODIUM],
        zincMg = nutrients[NutrientKey.ZINC], copperMg = nutrients[NutrientKey.COPPER],
        manganeseMg = nutrients[NutrientKey.MANGANESE], seleniumMcg = nutrients[NutrientKey.SELENIUM],
        vitaminAMcgRae = nutrients[NutrientKey.VITAMIN_A], vitaminCMg = nutrients[NutrientKey.VITAMIN_C],
        vitaminDMcg = nutrients[NutrientKey.VITAMIN_D], vitaminEMg = nutrients[NutrientKey.VITAMIN_E],
        vitaminKMcg = nutrients[NutrientKey.VITAMIN_K], thiaminB1Mg = nutrients[NutrientKey.THIAMIN_B1],
        riboflavinB2Mg = nutrients[NutrientKey.RIBOFLAVIN_B2], niacinB3Mg = nutrients[NutrientKey.NIACIN_B3],
        pantothenicAcidB5Mg = nutrients[NutrientKey.PANTOTHENIC_ACID_B5], vitaminB6Mg = nutrients[NutrientKey.VITAMIN_B6],
        folateMcg = nutrients[NutrientKey.FOLATE], folateMcgDfe = null,
        vitaminB12Mcg = nutrients[NutrientKey.VITAMIN_B12], cholineMg = nutrients[NutrientKey.CHOLINE],
        saturatedFatG = nutrients[NutrientKey.SATURATED_FAT], monounsaturatedFatG = nutrients[NutrientKey.MONOUNSATURATED_FAT],
        polyunsaturatedFatG = nutrients[NutrientKey.POLYUNSATURATED_FAT], cholesterolMg = nutrients[NutrientKey.CHOLESTEROL],
        createdAt = existingCreatedAt ?: now,
        updatedAt = now
    )
    val food = FoodEntity(
        foodId = id, name = custom.name, searchName = custom.searchName, category = FoodCategory.OTHER,
        caloriesKcal = per100(NutrientKey.CALORIES), proteinG = per100(NutrientKey.PROTEIN),
        carbohydrateG = per100(NutrientKey.CARBOHYDRATE), fatG = per100(NutrientKey.FAT),
        fiberG = per100(NutrientKey.FIBER), totalSugarsG = totalSugarsG?.let { value -> grams?.let { value * 100.0 / it } },
        calciumMg = per100(NutrientKey.CALCIUM), ironMg = per100(NutrientKey.IRON),
        magnesiumMg = per100(NutrientKey.MAGNESIUM), phosphorusMg = per100(NutrientKey.PHOSPHORUS),
        potassiumMg = per100(NutrientKey.POTASSIUM), sodiumMg = per100(NutrientKey.SODIUM),
        zincMg = per100(NutrientKey.ZINC), copperMg = per100(NutrientKey.COPPER),
        manganeseMg = per100(NutrientKey.MANGANESE), seleniumMcg = per100(NutrientKey.SELENIUM),
        vitaminAMcgRae = per100(NutrientKey.VITAMIN_A), vitaminCMg = per100(NutrientKey.VITAMIN_C),
        vitaminDMcg = per100(NutrientKey.VITAMIN_D), vitaminEMg = per100(NutrientKey.VITAMIN_E),
        vitaminKMcg = per100(NutrientKey.VITAMIN_K), thiaminB1Mg = per100(NutrientKey.THIAMIN_B1),
        riboflavinB2Mg = per100(NutrientKey.RIBOFLAVIN_B2), niacinB3Mg = per100(NutrientKey.NIACIN_B3),
        pantothenicAcidB5Mg = per100(NutrientKey.PANTOTHENIC_ACID_B5), vitaminB6Mg = per100(NutrientKey.VITAMIN_B6),
        folateMcg = per100(NutrientKey.FOLATE), folateMcgDfe = null,
        vitaminB12Mcg = per100(NutrientKey.VITAMIN_B12), cholineMg = per100(NutrientKey.CHOLINE),
        saturatedFatG = per100(NutrientKey.SATURATED_FAT), monounsaturatedFatG = per100(NutrientKey.MONOUNSATURATED_FAT),
        polyunsaturatedFatG = per100(NutrientKey.POLYUNSATURATED_FAT), cholesterolMg = per100(NutrientKey.CHOLESTEROL),
        source = "User custom food", sourceFoodId = id, foodSource = FoodSource.CUSTOM
    )
    return custom to food
}

fun CustomFoodEntity.toInput(): CustomFoodInput = CustomFoodInput(
    foodId = foodId,
    name = name,
    description = description,
    servingQuantity = servingQuantity,
    servingUnit = servingUnit,
    servingGrams = servingGrams,
    totalSugarsG = totalSugarsG,
    nutrients = mapOf(
        NutrientKey.CALORIES to caloriesKcal, NutrientKey.PROTEIN to proteinG,
        NutrientKey.CARBOHYDRATE to carbohydrateG, NutrientKey.FAT to fatG,
        NutrientKey.FIBER to fiberG,
        NutrientKey.CALCIUM to calciumMg, NutrientKey.IRON to ironMg, NutrientKey.MAGNESIUM to magnesiumMg,
        NutrientKey.PHOSPHORUS to phosphorusMg, NutrientKey.POTASSIUM to potassiumMg, NutrientKey.SODIUM to sodiumMg,
        NutrientKey.ZINC to zincMg, NutrientKey.COPPER to copperMg, NutrientKey.MANGANESE to manganeseMg,
        NutrientKey.SELENIUM to seleniumMcg, NutrientKey.VITAMIN_A to vitaminAMcgRae, NutrientKey.VITAMIN_C to vitaminCMg,
        NutrientKey.VITAMIN_D to vitaminDMcg, NutrientKey.VITAMIN_E to vitaminEMg, NutrientKey.VITAMIN_K to vitaminKMcg,
        NutrientKey.THIAMIN_B1 to thiaminB1Mg, NutrientKey.RIBOFLAVIN_B2 to riboflavinB2Mg,
        NutrientKey.NIACIN_B3 to niacinB3Mg, NutrientKey.PANTOTHENIC_ACID_B5 to pantothenicAcidB5Mg,
        NutrientKey.VITAMIN_B6 to vitaminB6Mg, NutrientKey.FOLATE to folateMcg,
        NutrientKey.VITAMIN_B12 to vitaminB12Mcg, NutrientKey.CHOLINE to cholineMg,
        NutrientKey.SATURATED_FAT to saturatedFatG, NutrientKey.MONOUNSATURATED_FAT to monounsaturatedFatG,
        NutrientKey.POLYUNSATURATED_FAT to polyunsaturatedFatG, NutrientKey.CHOLESTEROL to cholesterolMg
    )
)

private fun formatCustomNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString().trimEnd('0').trimEnd('.')
