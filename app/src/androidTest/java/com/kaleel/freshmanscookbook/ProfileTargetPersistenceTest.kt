package com.kaleel.freshmanscookbook

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaleel.freshmanscookbook.data.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileTargetPersistenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "profile-target-test.db"

    @Before fun before() { context.deleteDatabase(databaseName) }
    @After fun after() { context.deleteDatabase(databaseName) }

    @Test
    fun trainingGoalAndOverridesSurviveRestart() = runBlocking {
        val profile = NutritionProfile(
            ageYears = 25,
            sex = BiologicalSex.MALE,
            heightCm = 180.0,
            weightKg = 72.0,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            overrides = NutritionTargetOverrides(proteinG = 150.0, hydrationMl = 2750.0),
            trainingGoal = TrainingGoal.BUILD_MUSCLE
        )
        var db = openDatabase()
        db.profileDao().saveProfile(NutritionProfileEntity.fromDomain(profile))
        db.close()

        db = openDatabase()
        val restored = db.profileDao().getProfile()!!.toDomain()
        assertEquals(TrainingGoal.BUILD_MUSCLE, restored.trainingGoal)
        assertEquals(150.0, restored.overrides.proteinG ?: 0.0, 0.0)
        assertEquals(2750.0, restored.overrides.hydrationMl ?: 0.0, 0.0)
        db.close()
    }

    private fun openDatabase(): CookbookDatabase =
        Room.databaseBuilder(context, CookbookDatabase::class.java, databaseName).build()
}
