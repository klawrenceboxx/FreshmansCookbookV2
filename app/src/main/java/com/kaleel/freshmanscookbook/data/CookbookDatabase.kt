package com.kaleel.freshmanscookbook.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
interface RecipeDao {
    @Transaction
    @Query("SELECT * FROM recipes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<RecipeWithItems>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getById(id: String): RecipeWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(items: List<IngredientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(items: List<StepEntity>)

    @Query("DELETE FROM ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredients(recipeId: String)

    @Query("DELETE FROM steps WHERE recipeId = :recipeId")
    suspend fun deleteSteps(recipeId: String)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipe(id: String)
}

data class RecipeWithItems(
    @Embedded val recipe: RecipeEntity,
    @Relation(parentColumn = "id", entityColumn = "recipeId") val ingredients: List<IngredientEntity>,
    @Relation(parentColumn = "id", entityColumn = "recipeId") val steps: List<StepEntity>
) {
    fun toModel() = Recipe(
        id = recipe.id,
        name = recipe.name,
        category = recipe.category,
        servings = recipe.servings,
        imagePath = recipe.imagePath,
        ingredients = ingredients.sortedBy { it.sortOrder }.map { Ingredient(it.id, it.name, it.quantity, it.unit, it.sortOrder) },
        steps = steps.sortedBy { it.sortOrder }.map { RecipeStep(it.id, it.text, it.sortOrder) },
        createdAt = recipe.createdAt,
        updatedAt = recipe.updatedAt
    )
}

class Converters {
    @TypeConverter fun categoryToString(value: RecipeCategory) = value.name
    @TypeConverter fun stringToCategory(value: String) = RecipeCategory.valueOf(value)
    @TypeConverter fun unitToString(value: IngredientUnit) = value.name
    @TypeConverter fun stringToUnit(value: String) = IngredientUnit.valueOf(value)
}

@Database(entities = [RecipeEntity::class, IngredientEntity::class, StepEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class CookbookDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile private var instance: CookbookDatabase? = null
        fun get(context: Context): CookbookDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context, CookbookDatabase::class.java, "cookbook.db").build().also { instance = it }
        }
    }
}

class RecipeRepository(private val db: CookbookDatabase) {
    private val dao = db.recipeDao()
    val recipes: Flow<List<Recipe>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun get(id: String) = dao.getById(id)?.toModel()

    suspend fun save(recipe: Recipe) = db.withTransaction {
        dao.insertRecipe(RecipeEntity(recipe.id, recipe.name, recipe.category, recipe.servings, recipe.imagePath, recipe.createdAt, recipe.updatedAt))
        dao.deleteIngredients(recipe.id)
        dao.deleteSteps(recipe.id)
        dao.insertIngredients(recipe.ingredients.mapIndexed { index, item ->
            IngredientEntity(item.id, recipe.id, item.name, item.quantity, item.unit, index)
        })
        dao.insertSteps(recipe.steps.mapIndexed { index, item -> StepEntity(item.id, recipe.id, item.text, index) })
    }

    suspend fun delete(id: String) = dao.deleteRecipe(id)
}
