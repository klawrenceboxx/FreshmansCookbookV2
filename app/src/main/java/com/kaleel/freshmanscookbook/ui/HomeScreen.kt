package com.kaleel.freshmanscookbook.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kaleel.freshmanscookbook.CookbookViewModel
import com.kaleel.freshmanscookbook.data.Recipe
import com.kaleel.freshmanscookbook.data.RecipeCategory

@Composable
fun HomeScreen(viewModel: CookbookViewModel, onAdd: () -> Unit, onOpen: (String) -> Unit) {
    val recipes by viewModel.recipes.collectAsState()
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val shown = recipes.filter { selected == null || it.category.name == selected }

    Scaffold(
        containerColor = Paper,
        floatingActionButton = {
            if (recipes.isNotEmpty()) FloatingActionButton(onClick = onAdd, containerColor = Herb, contentColor = Paper) {
                Icon(Icons.Rounded.Add, contentDescription = "Add recipe")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, top = 24.dp, end = 20.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Recipes", style = MaterialTheme.typography.displaySmall)
                if (recipes.isEmpty()) IconButton(onClick = onAdd) { Icon(Icons.Rounded.Add, "Add recipe", tint = Herb) }
            }
            CategoryStrip(selected, onSelect = { selected = it })
            if (shown.isEmpty()) {
                EmptyRecipes(filtered = recipes.isNotEmpty(), onAdd = onAdd)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) { items(shown, key = { it.id }) { RecipeCard(it, onOpen) } }
            }
        }
    }
}

@Composable
private fun CategoryStrip(selected: String?, onSelect: (String?) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("All") })
        RecipeCategory.entries.forEach { category ->
            FilterChip(selected = selected == category.name, onClick = { onSelect(category.name) }, label = { Text(category.label) })
        }
    }
}

@Composable
private fun RecipeCard(recipe: Recipe, onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().clickable { onOpen(recipe.id) }) {
        RecipeImage(recipe.imagePath, Modifier.fillMaxWidth().aspectRatio(1f), 18)
        Spacer(Modifier.height(10.dp))
        Text(recipe.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(3.dp))
        Text(recipe.category.label.uppercase(), style = MaterialTheme.typography.labelMedium, color = Herb)
    }
}

@Composable
private fun EmptyContent(filtered: Boolean, onAdd: () -> Unit) {
    Text(if (filtered) "Nothing in this section" else "No recipes yet", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Text(if (filtered) "Choose another category." else "Build a cookbook you’ll actually use.", color = Muted)
    if (!filtered) {
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAdd, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 15.dp)) {
            Icon(Icons.Rounded.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Add Recipe")
        }
    }
}

@Composable
private fun EmptyRecipes(filtered: Boolean, onAdd: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { EmptyContent(filtered, onAdd) }
    }
}
