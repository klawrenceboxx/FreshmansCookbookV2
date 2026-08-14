package com.kaleel.freshmanscookbook

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kaleel.freshmanscookbook.data.CookbookDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration8To9Test {
    private val databaseName = "migration-8-9-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        CookbookDatabase::class.java
    )

    @Test
    fun migrationDefaultsExistingProfilesAndPreservesHydrationData() {
        helper.createDatabase(databaseName, 8).apply {
            execSQL(
                """INSERT INTO nutrition_profile(
                    id,ageYears,sex,heightCm,weightKg,activityLevel,goal,
                    caloriesOverrideKcal,proteinOverrideG,carbohydrateOverrideG,fatOverrideG,fiberOverrideG
                ) VALUES (1,25,'MALE',180.0,72.0,'MODERATELY_ACTIVE','MAINTAIN',NULL,150.0,NULL,NULL,NULL)"""
            )
            execSQL("INSERT INTO hydration_preferences(id,displayUnit,bottleMl) VALUES (1,'CUPS',1000.0)")
            execSQL("INSERT INTO water_logs(id,amountMl,enteredAmount,enteredUnit,label,loggedAt) VALUES ('water-1',500.0,500.0,'MILLILITERS',NULL,10)")
            close()
        }

        helper.runMigrationsAndValidate(databaseName, 9, true, CookbookDatabase.MIGRATION_8_9).use { db ->
            db.query("SELECT trainingGoal,proteinOverrideG,hydrationOverrideMl FROM nutrition_profile WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("GENERAL_HEALTH", cursor.getString(0))
                assertEquals(150.0, cursor.getDouble(1), 0.0)
                assertTrue(cursor.isNull(2))
            }
            db.query("SELECT displayUnit,bottleMl FROM hydration_preferences WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("CUPS", cursor.getString(0))
                assertEquals(1000.0, cursor.getDouble(1), 0.0)
            }
            db.query("SELECT amountMl FROM water_logs WHERE id = 'water-1'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(500.0, cursor.getDouble(0), 0.0)
            }
        }
    }
}
