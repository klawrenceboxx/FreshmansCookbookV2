package com.kaleel.freshmanscookbook.ui

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.kaleel.freshmanscookbook.CookbookViewModel

@Composable
fun CookbookApp(viewModel: CookbookViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onAdd = { nav.navigate("editor/new") },
                onOpen = { nav.navigate("detail/$it") },
                onProfile = { nav.navigate("profile") }
            )
        }
        composable(
            "editor/{recipeId}",
            arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
        ) { entry ->
            val recipeId = entry.arguments?.getString("recipeId") ?: "new"
            var prepared by rememberSaveable(recipeId) { mutableStateOf(false) }
            LaunchedEffect(recipeId) {
                if (!prepared) {
                    if (recipeId == "new") viewModel.startNew() else viewModel.startEdit(recipeId)
                    prepared = true
                }
            }
            if (prepared) {
                EditorScreen(
                    viewModel = viewModel,
                    onClose = { nav.popBackStack() },
                    onSaved = { id ->
                        nav.navigate("detail/$id") {
                            popUpTo("home")
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
        composable(
            "detail/{recipeId}",
            arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("recipeId") ?: return@composable
            DetailScreen(
                recipeId = id,
                viewModel = viewModel,
                onBack = { nav.popBackStack() },
                onEdit = { viewModel.endMeal(id); nav.navigate("editor/$id") },
                onDeleted = { nav.popBackStack("home", inclusive = false) },
                onForecast = { nav.navigate("forecast") }
            )
        }
        composable("profile") {
            ProfileScreen(
                viewModel = viewModel,
                onBack = { nav.popBackStack() },
                onAddFood = { nav.navigate("add-food") }
            )
        }
        composable("add-food") {
            AddFoodScreen(
                viewModel = viewModel,
                onBack = { nav.popBackStack() },
                onLogged = { nav.popBackStack() }
            )
        }
        composable("forecast") {
            ForecastScreen(
                viewModel = viewModel,
                onBack = { nav.popBackStack() },
                onPinned = { nav.popBackStack() }
            )
        }
    }
}
