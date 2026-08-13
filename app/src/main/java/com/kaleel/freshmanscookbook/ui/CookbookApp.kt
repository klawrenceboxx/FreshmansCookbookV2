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
                onOpen = { nav.navigate("detail/$it") }
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
                onEdit = { nav.navigate("editor/$id") },
                onDeleted = { nav.popBackStack("home", inclusive = false) }
            )
        }
    }
}
