package com.kaleel.freshmanscookbook.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.kaleel.freshmanscookbook.CookbookViewModel
import com.kaleel.freshmanscookbook.data.Ingredient
import com.kaleel.freshmanscookbook.data.IngredientUnit
import com.kaleel.freshmanscookbook.formatQuantity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    recipeId: String,
    viewModel: CookbookViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit
) {
    val recipes by viewModel.recipes.collectAsState()
    val recipe = recipes.firstOrNull { it.id == recipeId }
    var checked by remember(recipeId) { mutableStateOf(setOf<String>()) }
    var showDelete by remember { mutableStateOf(false) }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete this recipe?") },
            text = { Text("This removes it from your cookbook. This can’t be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(recipeId); showDelete = false; onDeleted() }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Keep recipe") } }
        )
    }

    if (recipe == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Scaffold(
        containerColor = Paper,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                actions = {
                    TextButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Edit") }
                    IconButton(onClick = { showDelete = true }) { Icon(Icons.Rounded.DeleteOutline, "Delete") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            RecipeImage(recipe.imagePath, Modifier.fillMaxWidth().aspectRatio(1.15f), 0)
            Column(Modifier.padding(horizontal = 22.dp, vertical = 24.dp)) {
                Text(recipe.name, style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(7.dp))
                Text("${recipe.category.label}  •  ${recipe.servings} servings", color = Muted, style = MaterialTheme.typography.bodyLarge)

                Row(Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Ingredients", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { checked = emptySet() }, enabled = checked.isNotEmpty()) { Text("Reset") }
                }
                HorizontalDivider(color = Line)
                recipe.ingredients.forEach { ingredient ->
                    IngredientCheckRow(
                        ingredient = ingredient,
                        checked = ingredient.id in checked,
                        onToggle = { checked = if (ingredient.id in checked) checked - ingredient.id else checked + ingredient.id }
                    )
                }

                Text("Steps", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 32.dp, bottom = 10.dp))
                HorizontalDivider(color = Line)
                recipe.steps.forEachIndexed { index, step ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                        Box(Modifier.size(32.dp).background(MintWash, CircleShape), contentAlignment = Alignment.Center) {
                            Text("${index + 1}", color = Herb, style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.width(13.dp))
                        Text(step.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(36.dp))
            }
        }
    }
}

@Composable
private fun IngredientCheckRow(ingredient: Ingredient, checked: Boolean, onToggle: () -> Unit) {
    val rowColor by animateColorAsState(if (checked) MaterialTheme.colorScheme.surfaceVariant else Paper, label = "ingredient color")
    Row(
        Modifier.fillMaxWidth().background(rowColor).clickable(onClick = onToggle).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Spacer(Modifier.width(4.dp))
        Text(
            naturalIngredient(ingredient),
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f).alpha(if (checked) .55f else 1f)
        )
    }
}

private fun naturalIngredient(item: Ingredient): String {
    val quantity = formatQuantity(item.quantity)
    val unit = if (item.unit == IngredientUnit.NONE) "" else item.unit.label
    return listOf(quantity, unit, item.name).filter { it.isNotBlank() }.joinToString(" ")
}
