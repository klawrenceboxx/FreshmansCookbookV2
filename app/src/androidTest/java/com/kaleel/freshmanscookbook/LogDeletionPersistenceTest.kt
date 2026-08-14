package com.kaleel.freshmanscookbook

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaleel.freshmanscookbook.data.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LogDeletionPersistenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "log-deletion-test.db"

    @Before fun before() { context.deleteDatabase(databaseName) }
    @After fun after() { context.deleteDatabase(databaseName) }

    @Test
    fun deletingFoodLogPersistsAndKeepsUnderlyingFood() = runBlocking {
        val (_, food) = CustomFoodInput(
            name = "Test food",
            servingQuantity = 100.0,
            servingUnit = IngredientUnit.G,
            nutrients = mapOf(NutrientKey.CALORIES to 100.0)
        ).toEntities(1L)
        var db = openDatabase()
        db.foodDao().insertFoods(listOf(food))
        db.foodLogDao().insert(
            FoodLogEntity(
                id = "log-1",
                foodId = food.foodId,
                foodName = food.userFacingName,
                quantity = 100.0,
                unit = IngredientUnit.G,
                gramsEquivalent = 100.0,
                loggedAt = 10L,
                knownNutrientsMask = NutrientKnowledgeMask.bit(NutrientKey.CALORIES),
                completeNutrientsMask = NutrientKnowledgeMask.bit(NutrientKey.CALORIES),
                nutrition = NutritionTotals(caloriesKcal = 100.0)
            )
        )
        db.foodLogDao().deleteById("log-1")
        db.close()

        db = openDatabase()
        assertEquals(null, db.foodLogDao().getById("log-1"))
        assertNotNull(db.foodDao().getByIds(listOf(food.foodId)).singleOrNull())
        db.close()
    }

    @Test
    fun deletingMealLogPersistsAndKeepsUnderlyingRecipe() = runBlocking {
        var db = openDatabase()
        db.recipeDao().insertRecipe(RecipeEntity("recipe-1", "Pudding", RecipeCategory.BREAKFAST, 2, null, 1L, 1L))
        val meal = MealInstance("meal-1", "recipe-1", "Pudding", 2, 1.0, emptyList(), 1L)
        val record = MealLogRecord.fromMeal(
            mealInstance = meal,
            consumedNutrition = NutritionCompletenessCalculator.aggregate(emptyList()),
            loggedAt = 10L
        )
        db.mealLogDao().save(record)
        db.mealLogDao().deleteById(record.meal.id)
        db.close()

        db = openDatabase()
        assertEquals(null, db.mealLogDao().getById(record.meal.id))
        assertNotNull(db.recipeDao().getById("recipe-1"))
        db.close()
    }

    @Test
    fun waterDeletionAndPreferencesPersistAcrossRestart() = runBlocking {
        var db = openDatabase()
        val repository = HydrationRepository(db)
        repository.setDisplayUnit(WaterDisplayUnit.CUPS)
        repository.setBottle(1.0, WaterUnit.LITERS)
        repository.log(2.0, WaterUnit.CUPS, id = "water-1", loggedAt = 10L)
        repository.delete("water-1")
        db.close()

        db = openDatabase()
        assertEquals(emptyList<WaterLogEntity>(), db.hydrationDao().getBetween(0L, 100L))
        assertEquals(WaterDisplayUnit.CUPS, db.hydrationDao().getPreferences()?.displayUnit)
        assertEquals(1000.0, db.hydrationDao().getPreferences()?.bottleMl ?: 0.0, 0.0)
        db.close()
    }

    private fun openDatabase(): CookbookDatabase =
        Room.databaseBuilder(context, CookbookDatabase::class.java, databaseName).build()
}
