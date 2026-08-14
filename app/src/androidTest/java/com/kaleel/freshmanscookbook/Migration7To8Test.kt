package com.kaleel.freshmanscookbook

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kaleel.freshmanscookbook.data.CookbookDatabase
import com.kaleel.freshmanscookbook.data.NutrientKnowledgeMask
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration7To8Test {
    private val databaseName = "migration-7-8-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        CookbookDatabase::class.java
    )

    @Test
    fun migrationPreservesDataAndAddsCompletenessAndHydrationTables() {
        helper.createDatabase(databaseName, 7).apply {
            execSQL(
                """INSERT INTO recipes(id,name,category,servings,imagePath,createdAt,updatedAt)
                   VALUES ('recipe-1','Nutty Pudding','BREAKFAST',2,NULL,1,1)"""
            )
            close()
        }

        helper.runMigrationsAndValidate(databaseName, 8, true, CookbookDatabase.MIGRATION_7_8).use { db ->
            db.query("SELECT name FROM recipes WHERE id = 'recipe-1'").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("Nutty Pudding", cursor.getString(0))
            }
            db.query("PRAGMA table_info(meal_logs)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                val defaultIndex = cursor.getColumnIndexOrThrow("dflt_value")
                val defaults = mutableMapOf<String, String?>()
                while (cursor.moveToNext()) defaults[cursor.getString(nameIndex)] = cursor.getString(defaultIndex)
                assertEquals(NutrientKnowledgeMask.all.toString(), defaults["knownNutrientsMask"])
                assertEquals(NutrientKnowledgeMask.all.toString(), defaults["completeNutrientsMask"])
            }
            db.execSQL("INSERT INTO hydration_preferences(id,displayUnit,bottleMl) VALUES (1,'CUPS',1000.0)")
            db.execSQL("INSERT INTO water_logs(id,amountMl,enteredAmount,enteredUnit,label,loggedAt) VALUES ('water-1',250.0,1.0,'CUPS',NULL,10)")
            db.query("SELECT amountMl FROM water_logs WHERE id = 'water-1'").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals(250.0, cursor.getDouble(0), 0.0)
            }
        }
    }
}
