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
class Migration5To6Test {
    private val databaseName = "migration-5-6-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        CookbookDatabase::class.java
    )

    @Test
    fun migrationPreservesExistingFoodsAndAddsCustomFoodStorage() {
        helper.createDatabase(databaseName, 5).apply {
            execSQL(
                """INSERT INTO foods (
                    foodId,name,searchName,category,source,sourceFoodId
                ) VALUES ('usda-1','Walnuts','walnuts','NUTS_SEEDS','USDA','usda-1')"""
            )
            close()
        }

        helper.runMigrationsAndValidate(databaseName, 6, true, CookbookDatabase.MIGRATION_5_6).use { db ->
            db.query("SELECT foodSource FROM foods WHERE foodId = 'usda-1'").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("USDA", cursor.getString(0))
            }
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='custom_foods'").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
            }
        }
    }
}
