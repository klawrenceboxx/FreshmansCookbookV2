package com.kaleel.freshmanscookbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaleel.freshmanscookbook.ui.CookbookApp
import com.kaleel.freshmanscookbook.ui.CookbookTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { CookbookTheme { CookbookApp(viewModel()) } }
    }
}
