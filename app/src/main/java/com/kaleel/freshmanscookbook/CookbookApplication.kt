package com.kaleel.freshmanscookbook

import android.app.Application
import com.kaleel.freshmanscookbook.data.CookbookDatabase
import com.kaleel.freshmanscookbook.data.FoodRepository
import com.kaleel.freshmanscookbook.data.FoodLogRepository
import com.kaleel.freshmanscookbook.data.DailyNutritionRepository
import com.kaleel.freshmanscookbook.data.MealLogRepository
import com.kaleel.freshmanscookbook.data.ProfileRepository
import com.kaleel.freshmanscookbook.data.RecipeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CookbookApplication : Application() {
    private val database by lazy { CookbookDatabase.get(this) }
    val repository by lazy { RecipeRepository(database) }
    val foodRepository by lazy { FoodRepository(this, database) }
    val mealLogRepository by lazy { MealLogRepository(database) }
    val foodLogRepository by lazy { FoodLogRepository(database) }
    val dailyNutritionRepository by lazy { DailyNutritionRepository(database) }
    val profileRepository by lazy { ProfileRepository(database) }

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            foodRepository.ensureSeeded()
        }
    }
}
