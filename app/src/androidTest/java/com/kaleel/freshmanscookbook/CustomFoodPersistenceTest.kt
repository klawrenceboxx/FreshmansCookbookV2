package com.kaleel.freshmanscookbook

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaleel.freshmanscookbook.data.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomFoodPersistenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "custom-food-test.db"

    @Before fun before() { context.deleteDatabase(databaseName) }
    @After fun after() { context.deleteDatabase(databaseName) }

    @Test
    fun customFoodSurvivesRestartAndAppearsInSearch() = runBlocking {
        val input = CustomFoodInput(
            name = "Whey Protein", servingQuantity = 1.0, servingUnit = IngredientUnit.SCOOP,
            servingGrams = 32.0, nutrients = mapOf(NutrientKey.PROTEIN to 24.0)
        )
        val (custom, food) = input.toEntities(now = 1L)
        var database = openDatabase()
        database.customFoodDao().upsert(custom)
        database.foodDao().insertFoods(listOf(food))
        database.close()

        database = openDatabase()
        assertEquals("Whey Protein", database.customFoodDao().get(custom.foodId)?.name)
        val results = database.foodDao().search("whey")
        assertEquals(listOf(FoodSource.CUSTOM), results.map { it.foodSource })
        database.close()
    }

    @Test
    fun usdaDisplayNameOverrideSurvivesRestartAndRemainsSearchable() = runBlocking {
        val (_, template) = CustomFoodInput(
            name = "Temporary", servingQuantity = 100.0, servingUnit = IngredientUnit.G
        ).toEntities(now = 1L)
        val food = template.copy(
            foodId = "usda-walnut",
            name = "Nuts, walnuts, English, halves, raw",
            displayName = "Walnuts",
            searchName = "nuts walnuts english halves raw",
            source = "USDA",
            sourceFoodId = "usda-walnut",
            foodSource = FoodSource.USDA
        )
        var database = openDatabase()
        database.foodDao().insertFoods(listOf(food))
        database.foodDao().upsertDisplayNameOverride(
            FoodDisplayNameOverrideEntity(food.foodId, "Walnut halves", "walnut halves")
        )
        database.close()

        database = openDatabase()
        assertEquals("Walnut halves", database.foodDao().getDisplayNameOverrides(listOf(food.foodId)).single().displayName)
        assertEquals(listOf(food.foodId), database.foodDao().search("walnut halves").map { it.foodId })
        assertEquals("Nuts, walnuts, English, halves, raw", database.foodDao().getByIds(listOf(food.foodId)).single().name)
        database.close()
    }

    private fun openDatabase(): CookbookDatabase = Room.databaseBuilder(context, CookbookDatabase::class.java, databaseName)
        .addMigrations(
            CookbookDatabase.MIGRATION_1_2, CookbookDatabase.MIGRATION_2_3,
            CookbookDatabase.MIGRATION_3_4, CookbookDatabase.MIGRATION_4_5,
            CookbookDatabase.MIGRATION_5_6, CookbookDatabase.MIGRATION_6_7
        )
        .build()
}
