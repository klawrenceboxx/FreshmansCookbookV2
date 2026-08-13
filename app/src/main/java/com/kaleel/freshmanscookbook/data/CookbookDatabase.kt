package com.kaleel.freshmanscookbook.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

@Dao
interface FoodDao {
    @Query("SELECT COUNT(*) FROM foods")
    suspend fun countFoods(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoods(foods: List<FoodEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAliases(aliases: List<FoodAliasEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPortions(portions: List<FoodPortionEntity>)

    @Query("SELECT * FROM foods ORDER BY name")
    suspend fun getAllFoods(): List<FoodEntity>

    @Query(
        """
        SELECT DISTINCT foods.* FROM foods
        LEFT JOIN food_aliases ON food_aliases.foodId = foods.foodId
        WHERE foods.searchName LIKE '%' || :query || '%'
           OR food_aliases.alias LIKE '%' || :query || '%'
        ORDER BY CASE WHEN foods.searchName LIKE :query || '%' THEN 0 ELSE 1 END, foods.name
        LIMIT :limit
        """
    )
    suspend fun search(query: String, limit: Int = 8): List<FoodEntity>

    @Query("SELECT * FROM food_portions WHERE foodId = :foodId AND unit = :unit ORDER BY description")
    suspend fun portions(foodId: String, unit: IngredientUnit): List<FoodPortionEntity>
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
        ingredients = ingredients.sortedBy { it.sortOrder }.map {
            Ingredient(it.id, it.name, it.quantity, it.unit, it.sortOrder, it.foodId, it.gramsEquivalent)
        },
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
    @TypeConverter fun foodCategoryToString(value: FoodCategory) = value.name
    @TypeConverter fun stringToFoodCategory(value: String) = FoodCategory.valueOf(value)
}

@Database(
    entities = [
        RecipeEntity::class,
        IngredientEntity::class,
        StepEntity::class,
        FoodEntity::class,
        FoodAliasEntity::class,
        FoodPortionEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class CookbookDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun foodDao(): FoodDao

    companion object {
        @Volatile private var instance: CookbookDatabase? = null
        fun get(context: Context): CookbookDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context, CookbookDatabase::class.java, "cookbook.db")
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `foods` (
                        `foodId` TEXT NOT NULL, `name` TEXT NOT NULL, `searchName` TEXT NOT NULL,
                        `category` TEXT NOT NULL, `caloriesKcal` REAL, `proteinG` REAL,
                        `carbohydrateG` REAL, `fatG` REAL, `fiberG` REAL, `totalSugarsG` REAL,
                        `calciumMg` REAL, `ironMg` REAL, `magnesiumMg` REAL, `phosphorusMg` REAL,
                        `potassiumMg` REAL, `sodiumMg` REAL, `zincMg` REAL, `copperMg` REAL,
                        `manganeseMg` REAL, `seleniumMcg` REAL, `vitaminAMcgRae` REAL,
                        `vitaminCMg` REAL, `vitaminDMcg` REAL, `vitaminEMg` REAL, `vitaminKMcg` REAL,
                        `thiaminB1Mg` REAL, `riboflavinB2Mg` REAL, `niacinB3Mg` REAL,
                        `pantothenicAcidB5Mg` REAL, `vitaminB6Mg` REAL, `folateMcg` REAL,
                        `folateMcgDfe` REAL, `vitaminB12Mcg` REAL, `cholineMg` REAL,
                        `saturatedFatG` REAL, `monounsaturatedFatG` REAL, `polyunsaturatedFatG` REAL,
                        `cholesterolMg` REAL, `source` TEXT NOT NULL, `sourceFoodId` TEXT,
                        PRIMARY KEY(`foodId`))"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_foods_name` ON `foods` (`name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_foods_searchName` ON `foods` (`searchName`)")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `food_aliases` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `alias` TEXT NOT NULL, `foodId` TEXT NOT NULL)"""
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_food_aliases_alias` ON `food_aliases` (`alias`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_aliases_foodId` ON `food_aliases` (`foodId`)")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `food_portions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `foodId` TEXT NOT NULL,
                        `unit` TEXT NOT NULL, `description` TEXT, `gramsPerUnit` REAL NOT NULL)"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_portions_foodId` ON `food_portions` (`foodId`)")
            }
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
            IngredientEntity(
                item.id,
                recipe.id,
                item.name,
                item.quantity,
                item.unit,
                index,
                item.foodId,
                item.gramsEquivalent
            )
        })
        dao.insertSteps(recipe.steps.mapIndexed { index, item -> StepEntity(item.id, recipe.id, item.text, index) })
    }

    suspend fun delete(id: String) = dao.deleteRecipe(id)
}
