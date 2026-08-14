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

    @Query("DELETE FROM food_aliases")
    suspend fun deleteAllAliases()

    @Query("DELETE FROM food_portions WHERE foodId IN (SELECT foodId FROM foods WHERE foodSource = 'USDA')")
    suspend fun deleteAllUsdaPortions()

    @Query("DELETE FROM food_portions WHERE foodId = :foodId")
    suspend fun deletePortionsForFood(foodId: String)

    @Query("SELECT * FROM foods ORDER BY name")
    suspend fun getAllFoods(): List<FoodEntity>

    @Query("SELECT * FROM foods WHERE foodSource = 'USDA' ORDER BY name")
    suspend fun getAllUsdaFoods(): List<FoodEntity>

    @Query("SELECT * FROM foods WHERE foodId IN (:foodIds)")
    suspend fun getByIds(foodIds: List<String>): List<FoodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDisplayNameOverride(override: FoodDisplayNameOverrideEntity)

    @Query("DELETE FROM food_display_name_overrides WHERE foodId = :foodId")
    suspend fun deleteDisplayNameOverride(foodId: String)

    @Query("SELECT * FROM food_display_name_overrides WHERE foodId IN (:foodIds)")
    suspend fun getDisplayNameOverrides(foodIds: List<String>): List<FoodDisplayNameOverrideEntity>

    @Query(
        """
        SELECT DISTINCT foods.* FROM foods
        LEFT JOIN food_aliases ON food_aliases.foodId = foods.foodId
        LEFT JOIN food_display_name_overrides ON food_display_name_overrides.foodId = foods.foodId
        WHERE foods.searchName LIKE '%' || :query || '%'
           OR food_aliases.alias LIKE '%' || :query || '%'
           OR food_display_name_overrides.searchName LIKE '%' || :query || '%'
        ORDER BY CASE
            WHEN foods.searchName = :query THEN 0
            WHEN food_display_name_overrides.searchName = :query THEN 0
            WHEN foods.searchName LIKE :query || '%' THEN 1
            WHEN food_display_name_overrides.searchName LIKE :query || '%' THEN 1
            ELSE 2
        END, COALESCE(food_display_name_overrides.displayName, foods.displayName, foods.name)
        LIMIT :limit
        """
    )
    suspend fun search(query: String, limit: Int = 8): List<FoodEntity>

    @Query("SELECT * FROM food_portions WHERE foodId = :foodId AND unit = :unit ORDER BY description")
    suspend fun portions(foodId: String, unit: IngredientUnit): List<FoodPortionEntity>
}

@Dao
interface CustomFoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(food: CustomFoodEntity)

    @Query("SELECT * FROM custom_foods WHERE foodId = :foodId")
    suspend fun get(foodId: String): CustomFoodEntity?

    @Query("SELECT * FROM custom_foods WHERE foodId IN (:foodIds)")
    suspend fun getByIds(foodIds: List<String>): List<CustomFoodEntity>
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

    @TypeConverter fun foodSourceToString(value: FoodSource) = value.name
    @TypeConverter fun stringToFoodSource(value: String) = FoodSource.valueOf(value)

    @TypeConverter fun biologicalSexToString(value: BiologicalSex) = value.name
    @TypeConverter fun stringToBiologicalSex(value: String) = BiologicalSex.valueOf(value)

    @TypeConverter fun activityLevelToString(value: ActivityLevel) = value.name
    @TypeConverter fun stringToActivityLevel(value: String) = ActivityLevel.valueOf(value)

    @TypeConverter fun nutritionGoalToString(value: NutritionGoal) = value.name
    @TypeConverter fun stringToNutritionGoal(value: String) = NutritionGoal.valueOf(value)

    @TypeConverter fun waterUnitToString(value: WaterUnit) = value.name
    @TypeConverter fun stringToWaterUnit(value: String) = WaterUnit.valueOf(value)

    @TypeConverter fun waterDisplayUnitToString(value: WaterDisplayUnit) = value.name
    @TypeConverter fun stringToWaterDisplayUnit(value: String) = WaterDisplayUnit.valueOf(value)
}

@Database(
    entities = [
        RecipeEntity::class,
        IngredientEntity::class,
        StepEntity::class,
        FoodEntity::class,
        FoodAliasEntity::class,
        FoodPortionEntity::class,
        FoodDisplayNameOverrideEntity::class,
        CustomFoodEntity::class,
        NutritionProfileEntity::class,
        MealLogEntity::class,
        MealLogIngredientEntity::class,
        FoodLogEntity::class,
        WaterLogEntity::class,
        HydrationPreferencesEntity::class
    ],
    version = 8,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class CookbookDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun foodDao(): FoodDao
    abstract fun customFoodDao(): CustomFoodDao
    abstract fun profileDao(): ProfileDao
    abstract fun mealLogDao(): MealLogDao
    abstract fun foodLogDao(): FoodLogDao
    abstract fun hydrationDao(): HydrationDao

    companion object {
        @Volatile
        private var instance: CookbookDatabase? = null

        fun get(context: Context): CookbookDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CookbookDatabase::class.java,
                    "cookbook.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8
                    )
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
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `alias` TEXT NOT NULL,
                        `foodId` TEXT NOT NULL)"""
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_food_aliases_alias` ON `food_aliases` (`alias`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_aliases_foodId` ON `food_aliases` (`foodId`)")

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `food_portions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `foodId` TEXT NOT NULL,
                        `unit` TEXT NOT NULL,
                        `description` TEXT,
                        `gramsPerUnit` REAL NOT NULL)"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_portions_foodId` ON `food_portions` (`foodId`)")
            }
        }

        /**
         * Adds the single persisted nutrition profile.
         *
         * Kept as its own migration because some local installs may already
         * have run the previously-prepared v2 -> v3 profile migration.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `nutrition_profile` (
                        `id` INTEGER NOT NULL,
                        `ageYears` INTEGER NOT NULL,
                        `sex` TEXT NOT NULL,
                        `heightCm` REAL NOT NULL,
                        `weightKg` REAL NOT NULL,
                        `activityLevel` TEXT NOT NULL,
                        `goal` TEXT NOT NULL,
                        `caloriesOverrideKcal` REAL,
                        `proteinOverrideG` REAL,
                        `carbohydrateOverrideG` REAL,
                        `fatOverrideG` REAL,
                        `fiberOverrideG` REAL,
                        PRIMARY KEY(`id`)
                    )"""
                )
            }
        }

        /**
         * Adds immutable historical meal snapshots.
         *
         * meal_logs intentionally has no foreign key to recipes. Deleting or
         * editing a recipe must never erase or rewrite nutrition history.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `meal_logs` (
                        `id` TEXT NOT NULL,
                        `recipeId` TEXT,
                        `recipeName` TEXT NOT NULL,
                        `recipeServings` INTEGER NOT NULL,
                        `servingsConsumed` REAL NOT NULL,
                        `startedAt` INTEGER NOT NULL,
                        `loggedAt` INTEGER NOT NULL,

                        `caloriesKcal` REAL NOT NULL,
                        `proteinG` REAL NOT NULL,
                        `carbohydrateG` REAL NOT NULL,
                        `fatG` REAL NOT NULL,
                        `fiberG` REAL NOT NULL,
                        `totalSugarsG` REAL NOT NULL,

                        `calciumMg` REAL NOT NULL,
                        `ironMg` REAL NOT NULL,
                        `magnesiumMg` REAL NOT NULL,
                        `phosphorusMg` REAL NOT NULL,
                        `potassiumMg` REAL NOT NULL,
                        `sodiumMg` REAL NOT NULL,
                        `zincMg` REAL NOT NULL,
                        `copperMg` REAL NOT NULL,
                        `manganeseMg` REAL NOT NULL,
                        `seleniumMcg` REAL NOT NULL,

                        `vitaminAMcgRae` REAL NOT NULL,
                        `vitaminCMg` REAL NOT NULL,
                        `vitaminDMcg` REAL NOT NULL,
                        `vitaminEMg` REAL NOT NULL,
                        `vitaminKMcg` REAL NOT NULL,
                        `thiaminB1Mg` REAL NOT NULL,
                        `riboflavinB2Mg` REAL NOT NULL,
                        `niacinB3Mg` REAL NOT NULL,
                        `pantothenicAcidB5Mg` REAL NOT NULL,
                        `vitaminB6Mg` REAL NOT NULL,
                        `folateMcg` REAL NOT NULL,
                        `folateMcgDfe` REAL NOT NULL,
                        `vitaminB12Mcg` REAL NOT NULL,
                        `cholineMg` REAL NOT NULL,

                        `saturatedFatG` REAL NOT NULL,
                        `monounsaturatedFatG` REAL NOT NULL,
                        `polyunsaturatedFatG` REAL NOT NULL,
                        `cholesterolMg` REAL NOT NULL,

                        PRIMARY KEY(`id`)
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_logs_loggedAt` ON `meal_logs` (`loggedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_logs_recipeId` ON `meal_logs` (`recipeId`)")

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `meal_log_ingredients` (
                        `id` TEXT NOT NULL,
                        `mealLogId` TEXT NOT NULL,
                        `sourceIngredientId` TEXT,
                        `foodId` TEXT,
                        `name` TEXT NOT NULL,
                        `quantity` REAL,
                        `unit` TEXT NOT NULL,
                        `gramsEquivalent` REAL,
                        `wasChecked` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,

                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`mealLogId`) REFERENCES `meal_logs`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_log_ingredients_mealLogId` ON `meal_log_ingredients` (`mealLogId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_log_ingredients_foodId` ON `meal_log_ingredients` (`foodId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_log_ingredients_sourceIngredientId` ON `meal_log_ingredients` (`sourceIngredientId`)")
            }
        }

        /**
         * Adds standalone food/snack logs.
         *
         * Nutrition is snapshotted directly on each row so historical totals
         * remain stable if the USDA seed database changes later.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `food_logs` (
                        `id` TEXT NOT NULL,
                        `foodId` TEXT NOT NULL,
                        `foodName` TEXT NOT NULL,
                        `quantity` REAL,
                        `unit` TEXT NOT NULL,
                        `gramsEquivalent` REAL NOT NULL,
                        `loggedAt` INTEGER NOT NULL,

                        `caloriesKcal` REAL NOT NULL,
                        `proteinG` REAL NOT NULL,
                        `carbohydrateG` REAL NOT NULL,
                        `fatG` REAL NOT NULL,
                        `fiberG` REAL NOT NULL,
                        `totalSugarsG` REAL NOT NULL,

                        `calciumMg` REAL NOT NULL,
                        `ironMg` REAL NOT NULL,
                        `magnesiumMg` REAL NOT NULL,
                        `phosphorusMg` REAL NOT NULL,
                        `potassiumMg` REAL NOT NULL,
                        `sodiumMg` REAL NOT NULL,
                        `zincMg` REAL NOT NULL,
                        `copperMg` REAL NOT NULL,
                        `manganeseMg` REAL NOT NULL,
                        `seleniumMcg` REAL NOT NULL,

                        `vitaminAMcgRae` REAL NOT NULL,
                        `vitaminCMg` REAL NOT NULL,
                        `vitaminDMcg` REAL NOT NULL,
                        `vitaminEMg` REAL NOT NULL,
                        `vitaminKMcg` REAL NOT NULL,
                        `thiaminB1Mg` REAL NOT NULL,
                        `riboflavinB2Mg` REAL NOT NULL,
                        `niacinB3Mg` REAL NOT NULL,
                        `pantothenicAcidB5Mg` REAL NOT NULL,
                        `vitaminB6Mg` REAL NOT NULL,
                        `folateMcg` REAL NOT NULL,
                        `folateMcgDfe` REAL NOT NULL,
                        `vitaminB12Mcg` REAL NOT NULL,
                        `cholineMg` REAL NOT NULL,

                        `saturatedFatG` REAL NOT NULL,
                        `monounsaturatedFatG` REAL NOT NULL,
                        `polyunsaturatedFatG` REAL NOT NULL,
                        `cholesterolMg` REAL NOT NULL,

                        PRIMARY KEY(`id`)
                    )"""
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_food_logs_loggedAt` " +
                            "ON `food_logs` (`loggedAt`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_food_logs_foodId` " +
                            "ON `food_logs` (`foodId`)"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `foods` ADD COLUMN `foodSource` TEXT NOT NULL DEFAULT 'USDA'")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `custom_foods` (
                        `foodId` TEXT NOT NULL, `name` TEXT NOT NULL, `searchName` TEXT NOT NULL,
                        `description` TEXT, `servingQuantity` REAL NOT NULL, `servingUnit` TEXT NOT NULL,
                        `servingGrams` REAL, `caloriesKcal` REAL, `proteinG` REAL,
                        `carbohydrateG` REAL, `fatG` REAL, `fiberG` REAL, `totalSugarsG` REAL,
                        `calciumMg` REAL, `ironMg` REAL, `magnesiumMg` REAL, `phosphorusMg` REAL,
                        `potassiumMg` REAL, `sodiumMg` REAL, `zincMg` REAL, `copperMg` REAL,
                        `manganeseMg` REAL, `seleniumMcg` REAL, `vitaminAMcgRae` REAL,
                        `vitaminCMg` REAL, `vitaminDMcg` REAL, `vitaminEMg` REAL, `vitaminKMcg` REAL,
                        `thiaminB1Mg` REAL, `riboflavinB2Mg` REAL, `niacinB3Mg` REAL,
                        `pantothenicAcidB5Mg` REAL, `vitaminB6Mg` REAL, `folateMcg` REAL,
                        `folateMcgDfe` REAL, `vitaminB12Mcg` REAL, `cholineMg` REAL,
                        `saturatedFatG` REAL, `monounsaturatedFatG` REAL, `polyunsaturatedFatG` REAL,
                        `cholesterolMg` REAL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`foodId`)
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_foods_searchName` ON `custom_foods` (`searchName`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `foods` ADD COLUMN `displayName` TEXT")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `food_display_name_overrides` (
                        `foodId` TEXT NOT NULL, `displayName` TEXT NOT NULL, `searchName` TEXT NOT NULL,
                        PRIMARY KEY(`foodId`)
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_display_name_overrides_searchName` ON `food_display_name_overrides` (`searchName`)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val allNutrientsMask = NutrientKnowledgeMask.all
                db.execSQL(
                    "ALTER TABLE `meal_logs` ADD COLUMN `knownNutrientsMask` INTEGER NOT NULL DEFAULT $allNutrientsMask"
                )
                db.execSQL(
                    "ALTER TABLE `meal_logs` ADD COLUMN `completeNutrientsMask` INTEGER NOT NULL DEFAULT $allNutrientsMask"
                )
                db.execSQL(
                    "ALTER TABLE `food_logs` ADD COLUMN `knownNutrientsMask` INTEGER NOT NULL DEFAULT $allNutrientsMask"
                )
                db.execSQL(
                    "ALTER TABLE `food_logs` ADD COLUMN `completeNutrientsMask` INTEGER NOT NULL DEFAULT $allNutrientsMask"
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `water_logs` (
                        `id` TEXT NOT NULL,
                        `amountMl` REAL NOT NULL,
                        `enteredAmount` REAL NOT NULL,
                        `enteredUnit` TEXT NOT NULL,
                        `label` TEXT,
                        `loggedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )"""
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_water_logs_loggedAt` ON `water_logs` (`loggedAt`)"
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `hydration_preferences` (
                        `id` INTEGER NOT NULL,
                        `displayUnit` TEXT NOT NULL,
                        `bottleMl` REAL,
                        PRIMARY KEY(`id`)
                    )"""
                )
            }
        }
    }
}

class RecipeRepository(private val db: CookbookDatabase) {
    private val dao = db.recipeDao()

    val recipes: Flow<List<Recipe>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun get(id: String) =
        dao.getById(id)?.toModel()

    suspend fun save(recipe: Recipe) = db.withTransaction {
        dao.insertRecipe(
            RecipeEntity(
                recipe.id,
                recipe.name,
                recipe.category,
                recipe.servings,
                recipe.imagePath,
                recipe.createdAt,
                recipe.updatedAt
            )
        )

        dao.deleteIngredients(recipe.id)
        dao.deleteSteps(recipe.id)

        dao.insertIngredients(
            recipe.ingredients.mapIndexed { index, item ->
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
            }
        )

        dao.insertSteps(
            recipe.steps.mapIndexed { index, item ->
                StepEntity(item.id, recipe.id, item.text, index)
            }
        )
    }

    suspend fun delete(id: String) =
        dao.deleteRecipe(id)
}
