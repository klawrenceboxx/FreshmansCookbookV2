package com.kaleel.freshmanscookbook.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RecipeCategory(val label: String) {
    BREAKFAST("Breakfast"), LUNCH("Lunch"), DINNER("Dinner"), SNACKS("Snacks"), DESSERT("Dessert")
}

enum class IngredientUnit(val label: String) {
    NONE("none"), G("g"), KG("kg"), OZ("oz"), LB("lb"), ML("ml"), L("L"), TSP("tsp"), TBSP("tbsp"), CUP("cup"), PIECE("piece")
}

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: RecipeCategory,
    val servings: Int,
    val imagePath: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "ingredients",
    foreignKeys = [ForeignKey(
        entity = RecipeEntity::class,
        parentColumns = ["id"],
        childColumns = ["recipeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("recipeId")]
)
data class IngredientEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val name: String,
    val quantity: Double?,
    val unit: IngredientUnit,
    val sortOrder: Int,
    // Reserved for future nutrition support without changing the UI model.
    val foodId: String? = null,
    val gramsEquivalent: Double? = null,
    val nutritionPer100gJson: String? = null
)

@Entity(
    tableName = "steps",
    foreignKeys = [ForeignKey(
        entity = RecipeEntity::class,
        parentColumns = ["id"],
        childColumns = ["recipeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("recipeId")]
)
data class StepEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val text: String,
    val sortOrder: Int
)

data class Recipe(
    val id: String,
    val name: String,
    val category: RecipeCategory,
    val servings: Int,
    val imagePath: String?,
    val ingredients: List<Ingredient>,
    val steps: List<RecipeStep>,
    val createdAt: Long,
    val updatedAt: Long
)

data class Ingredient(val id: String, val name: String, val quantity: Double?, val unit: IngredientUnit, val order: Int)
data class RecipeStep(val id: String, val text: String, val order: Int)
