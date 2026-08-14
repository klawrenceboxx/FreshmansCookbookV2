package com.kaleel.freshmanscookbook

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kaleel.freshmanscookbook.data.CookbookDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration6To7Test {
    private val databaseName = "migration-6-7-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        CookbookDatabase::class.java
    )

    @Test
    fun migrationPreservesUsdaFoodAndAddsDisplayNameStorage() {
        helper.createDatabase(databaseName, 6).apply {
            execSQL(
                """INSERT INTO foods (
                    foodId,name,searchName,category,source,sourceFoodId,foodSource
                ) VALUES ('usda-1','Nuts, walnuts, English, halves, raw','nuts walnuts english halves raw',
                    'NUTS_SEEDS','USDA','usda-1','USDA')"""
            )
            close()
        }

        helper.runMigrationsAndValidate(databaseName, 7, true, CookbookDatabase.MIGRATION_6_7).use { db ->
            db.query("SELECT name, displayName FROM foods WHERE foodId = 'usda-1'").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("Nuts, walnuts, English, halves, raw", cursor.getString(0))
                assertEquals(true, cursor.isNull(1))
            }
            db.execSQL(
                """INSERT INTO food_display_name_overrides(foodId,displayName,searchName)
                    VALUES ('usda-1','Walnut halves','walnut halves')"""
            )
            db.query("SELECT displayName FROM food_display_name_overrides WHERE foodId = 'usda-1'").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("Walnut halves", cursor.getString(0))
            }
        }
    }
}
