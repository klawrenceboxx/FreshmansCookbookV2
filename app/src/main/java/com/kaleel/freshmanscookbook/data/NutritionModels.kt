package com.kaleel.freshmanscookbook.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Canonical nutrition record for a food.
 *
 * Nutrient values are stored per 100 g so recipe calculations have one
 * consistent base unit. Recipe ingredients should reference this table by
 * foodId rather than copying nutrient values into every recipe.
 *
 * Data rows will be added separately from authoritative food-composition data.
 */
@Entity(
    tableName = "foods",
    indices = [
        Index(value = ["name"]),
        Index(value = ["searchName"])
    ]
)
data class FoodEntity(
    @PrimaryKey val foodId: String,
    val name: String,
    val searchName: String,
    val category: FoodCategory,

    // Energy + macros, per 100 g
    val caloriesKcal: Double?,
    val proteinG: Double?,
    val carbohydrateG: Double?,
    val fatG: Double?,
    val fiberG: Double?,
    val totalSugarsG: Double?,

    // Minerals, per 100 g
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

    // Vitamins, per 100 g
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

    // Useful nutrition fields that do not fit the macro/mineral/vitamin groups.
    val saturatedFatG: Double?,
    val monounsaturatedFatG: Double?,
    val polyunsaturatedFatG: Double?,
    val cholesterolMg: Double?,

    // Provenance lets us trace a value back to the source dataset.
    val source: String,
    val sourceFoodId: String?,
    val foodSource: FoodSource = FoodSource.USDA,
    /** Generated common name; [name] remains the authoritative USDA description. */
    val displayName: String? = null
)

val FoodEntity.userFacingName: String
    get() = displayName?.trim()?.takeIf(String::isNotBlank) ?: name

@Entity(
    tableName = "food_display_name_overrides",
    indices = [Index(value = ["searchName"])]
)
data class FoodDisplayNameOverrideEntity(
    @PrimaryKey val foodId: String,
    val displayName: String,
    val searchName: String
)

enum class FoodSource { USDA, CUSTOM }

enum class FoodCategory {
    MEAT,
    POULTRY,
    SEAFOOD,
    EGGS,
    DAIRY,
    GRAINS,
    LEGUMES,
    VEGETABLES,
    FRUIT,
    NUTS_SEEDS,
    OILS_FATS,
    HERBS_SPICES,
    SAUCES_CONDIMENTS,
    BAKING,
    BEVERAGES,
    OTHER
}

/**
 * A human-friendly alias for food search.
 *
 * Example:
 * alias = "chicken breast"
 * foodId = the canonical FoodEntity for cooked chicken breast.
 *
 * Keeping aliases separate means the canonical food name can remain precise
 * while autocomplete can use the words people actually type.
 */
@Entity(
    tableName = "food_aliases",
    indices = [
        Index(value = ["alias"], unique = true),
        Index(value = ["foodId"])
    ]
)
data class FoodAliasEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alias: String,
    val foodId: String
)

/**
 * Optional household-unit conversion for a specific food.
 *
 * All nutrition ultimately resolves to grams:
 * amount * gramsPerUnit -> grams -> nutrient values from FoodEntity.
 *
 * Example:
 * olive oil, TBSP, gramsPerUnit = 13.5
 */
@Entity(
    tableName = "food_portions",
    indices = [Index(value = ["foodId"])]
)
data class FoodPortionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodId: String,
    val unit: IngredientUnit,
    val description: String?,
    val gramsPerUnit: Double
)

/**
 * Nutrition values after scaling a FoodEntity to an actual ingredient amount.
 * This is a calculation model, not a Room table.
 */
data class NutritionTotals(
    val caloriesKcal: Double = 0.0,
    val proteinG: Double = 0.0,
    val carbohydrateG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val totalSugarsG: Double = 0.0,
    val calciumMg: Double = 0.0,
    val ironMg: Double = 0.0,
    val magnesiumMg: Double = 0.0,
    val phosphorusMg: Double = 0.0,
    val potassiumMg: Double = 0.0,
    val sodiumMg: Double = 0.0,
    val zincMg: Double = 0.0,
    val copperMg: Double = 0.0,
    val manganeseMg: Double = 0.0,
    val seleniumMcg: Double = 0.0,
    val vitaminAMcgRae: Double = 0.0,
    val vitaminCMg: Double = 0.0,
    val vitaminDMcg: Double = 0.0,
    val vitaminEMg: Double = 0.0,
    val vitaminKMcg: Double = 0.0,
    val thiaminB1Mg: Double = 0.0,
    val riboflavinB2Mg: Double = 0.0,
    val niacinB3Mg: Double = 0.0,
    val pantothenicAcidB5Mg: Double = 0.0,
    val vitaminB6Mg: Double = 0.0,
    val folateMcg: Double = 0.0,
    val folateMcgDfe: Double = 0.0,
    val vitaminB12Mcg: Double = 0.0,
    val cholineMg: Double = 0.0,
    val saturatedFatG: Double = 0.0,
    val monounsaturatedFatG: Double = 0.0,
    val polyunsaturatedFatG: Double = 0.0,
    val cholesterolMg: Double = 0.0
)
