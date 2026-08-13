package com.kaleel.freshmanscookbook

import android.app.Application
import com.kaleel.freshmanscookbook.data.CookbookDatabase
import com.kaleel.freshmanscookbook.data.RecipeRepository

class CookbookApplication : Application() {
    val repository by lazy { RecipeRepository(CookbookDatabase.get(this)) }
}
