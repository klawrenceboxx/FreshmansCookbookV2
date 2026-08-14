package com.kaleel.freshmanscookbook.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.UUID

/**
 * Historical snapshot of a finalized meal.
 *
 * Nutrient totals are stored directly on the log instead of being recalculated
 * from the current recipe/USDA database later. This means editing a recipe or
 * updating food data cannot silently rewrite nutrition history.
 */
@Entity(
    tableName = "meal_logs",
    indices = [
        Index("loggedAt"),
        Index("recipeId")
    ]
)
data class MealLogEntity(
    @PrimaryKey val id: String,
    val recipeId: String?,
    val recipeName: String,
    val recipeServings: Int,
    val servingsConsumed: Double,
    val startedAt: Long,
    val loggedAt: Long,

    val caloriesKcal: Double,
    val proteinG: Double,
    val carbohydrateG: Double,
    val fatG: Double,
    val fiberG: Double,
    val totalSugarsG: Double,

    val calciumMg: Double,
    val ironMg: Double,
    val magnesiumMg: Double,
    val phosphorusMg: Double,
    val potassiumMg: Double,
    val sodiumMg: Double,
    val zincMg: Double,
    val copperMg: Double,
    val manganeseMg: Double,
    val seleniumMcg: Double,

    val vitaminAMcgRae: Double,
    val vitaminCMg: Double,
    val vitaminDMcg: Double,
    val vitaminEMg: Double,
    val vitaminKMcg: Double,
    val thiaminB1Mg: Double,
    val riboflavinB2Mg: Double,
    val niacinB3Mg: Double,
    val pantothenicAcidB5Mg: Double,
    val vitaminB6Mg: Double,
    val folateMcg: Double,
    val folateMcgDfe: Double,
    val vitaminB12Mcg: Double,
    val cholineMg: Double,

    val saturatedFatG: Double,
    val monounsaturatedFatG: Double,
    val polyunsaturatedFatG: Double,
    val cholesterolMg: Double
) {
    fun nutritionTotals(): NutritionTotals = NutritionTotals(
        caloriesKcal = caloriesKcal,
        proteinG = proteinG,
        carbohydrateG = carbohydrateG,
        fatG = fatG,
        fiberG = fiberG,
        totalSugarsG = totalSugarsG,

        calciumMg = calciumMg,
        ironMg = ironMg,
        magnesiumMg = magnesiumMg,
        phosphorusMg = phosphorusMg,
        potassiumMg = potassiumMg,
        sodiumMg = sodiumMg,
        zincMg = zincMg,
        copperMg = copperMg,
        manganeseMg = manganeseMg,
        seleniumMcg = seleniumMcg,

        vitaminAMcgRae = vitaminAMcgRae,
        vitaminCMg = vitaminCMg,
        vitaminDMcg = vitaminDMcg,
        vitaminEMg = vitaminEMg,
        vitaminKMcg = vitaminKMcg,
        thiaminB1Mg = thiaminB1Mg,
        riboflavinB2Mg = riboflavinB2Mg,
        niacinB3Mg = niacinB3Mg,
        pantothenicAcidB5Mg = pantothenicAcidB5Mg,
        vitaminB6Mg = vitaminB6Mg,
        folateMcg = folateMcg,
        folateMcgDfe = folateMcgDfe,
        vitaminB12Mcg = vitaminB12Mcg,
        cholineMg = cholineMg,

        saturatedFatG = saturatedFatG,
        monounsaturatedFatG = monounsaturatedFatG,
        polyunsaturatedFatG = polyunsaturatedFatG,
        cholesterolMg = cholesterolMg
    )

    companion object {
        fun fromMeal(
            meal: MealInstance,
            consumedNutrition: NutritionTotals,
            loggedAt: Long = System.currentTimeMillis(),
            id: String = UUID.randomUUID().toString()
        ): MealLogEntity = MealLogEntity(
            id = id,
            recipeId = meal.recipeId,
            recipeName = meal.recipeName,
            recipeServings = meal.recipeServings,
            servingsConsumed = meal.servingsConsumed,
            startedAt = meal.startedAt,
            loggedAt = loggedAt,

            caloriesKcal = consumedNutrition.caloriesKcal,
            proteinG = consumedNutrition.proteinG,
            carbohydrateG = consumedNutrition.carbohydrateG,
            fatG = consumedNutrition.fatG,
            fiberG = consumedNutrition.fiberG,
            totalSugarsG = consumedNutrition.totalSugarsG,

            calciumMg = consumedNutrition.calciumMg,
            ironMg = consumedNutrition.ironMg,
            magnesiumMg = consumedNutrition.magnesiumMg,
            phosphorusMg = consumedNutrition.phosphorusMg,
            potassiumMg = consumedNutrition.potassiumMg,
            sodiumMg = consumedNutrition.sodiumMg,
            zincMg = consumedNutrition.zincMg,
            copperMg = consumedNutrition.copperMg,
            manganeseMg = consumedNutrition.manganeseMg,
            seleniumMcg = consumedNutrition.seleniumMcg,

            vitaminAMcgRae = consumedNutrition.vitaminAMcgRae,
            vitaminCMg = consumedNutrition.vitaminCMg,
            vitaminDMcg = consumedNutrition.vitaminDMcg,
            vitaminEMg = consumedNutrition.vitaminEMg,
            vitaminKMcg = consumedNutrition.vitaminKMcg,
            thiaminB1Mg = consumedNutrition.thiaminB1Mg,
            riboflavinB2Mg = consumedNutrition.riboflavinB2Mg,
            niacinB3Mg = consumedNutrition.niacinB3Mg,
            pantothenicAcidB5Mg = consumedNutrition.pantothenicAcidB5Mg,
            vitaminB6Mg = consumedNutrition.vitaminB6Mg,
            folateMcg = consumedNutrition.folateMcg,
            folateMcgDfe = consumedNutrition.folateMcgDfe,
            vitaminB12Mcg = consumedNutrition.vitaminB12Mcg,
            cholineMg = consumedNutrition.cholineMg,

            saturatedFatG = consumedNutrition.saturatedFatG,
            monounsaturatedFatG = consumedNutrition.monounsaturatedFatG,
            polyunsaturatedFatG = consumedNutrition.polyunsaturatedFatG,
            cholesterolMg = consumedNutrition.cholesterolMg
        )
    }
}

/**
 * Snapshot of one ingredient as it existed when the meal was logged.
 *
 * We retain unchecked ingredients too. wasChecked determines whether the
 * ingredient contributed to MealLogEntity's nutrition snapshot.
 */
@Entity(
    tableName = "meal_log_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = MealLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealLogId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("mealLogId"),
        Index("foodId"),
        Index("sourceIngredientId")
    ]
)
data class MealLogIngredientEntity(
    @PrimaryKey val id: String,
    val mealLogId: String,
    val sourceIngredientId: String?,
    val foodId: String?,
    val name: String,
    val quantity: Double?,
    val unit: IngredientUnit,
    val gramsEquivalent: Double?,
    val wasChecked: Boolean,
    val sortOrder: Int
) {
    companion object {
        fun fromMealIngredient(
            mealLogId: String,
            ingredient: MealIngredient
        ): MealLogIngredientEntity = MealLogIngredientEntity(
            id = UUID.randomUUID().toString(),
            mealLogId = mealLogId,
            sourceIngredientId = ingredient.sourceIngredientId,
            foodId = ingredient.foodId,
            name = ingredient.name,
            quantity = ingredient.quantity,
            unit = ingredient.unit,
            gramsEquivalent = ingredient.gramsEquivalent,
            wasChecked = ingredient.isChecked,
            sortOrder = ingredient.order
        )
    }
}

data class MealLogWithIngredients(
    @Embedded val meal: MealLogEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "mealLogId"
    )
    val ingredients: List<MealLogIngredientEntity>
) {
    val orderedIngredients: List<MealLogIngredientEntity>
        get() = ingredients.sortedBy { it.sortOrder }
}

/**
 * Convenience payload for saving one complete meal log transactionally.
 */
data class MealLogRecord(
    val meal: MealLogEntity,
    val ingredients: List<MealLogIngredientEntity>
) {
    companion object {
        fun fromMeal(
            mealInstance: MealInstance,
            consumedNutrition: NutritionTotals,
            loggedAt: Long = System.currentTimeMillis()
        ): MealLogRecord {
            val mealLog = MealLogEntity.fromMeal(
                meal = mealInstance,
                consumedNutrition = consumedNutrition,
                loggedAt = loggedAt
            )

            return MealLogRecord(
                meal = mealLog,
                ingredients = mealInstance.ingredients.map {
                    MealLogIngredientEntity.fromMealIngredient(
                        mealLogId = mealLog.id,
                        ingredient = it
                    )
                }
            )
        }
    }
}
