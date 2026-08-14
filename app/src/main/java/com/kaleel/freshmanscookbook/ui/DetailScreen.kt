package com.kaleel.freshmanscookbook.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kaleel.freshmanscookbook.CookbookViewModel
import com.kaleel.freshmanscookbook.MealLogState
import com.kaleel.freshmanscookbook.data.*
import com.kaleel.freshmanscookbook.formatQuantity
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    recipeId: String,
    viewModel: CookbookViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
    onForecast: () -> Unit
) {
    val recipes by viewModel.recipes.collectAsState()
    val recipe = recipes.firstOrNull { it.id == recipeId }
    val meal by viewModel.meal.collectAsState()
    val nutrition by viewModel.recipeNutrition.collectAsState()
    val pinned by viewModel.pinnedNutrient.collectAsState()
    val contributions by viewModel.pinnedContributions.collectAsState()
    val logState by viewModel.mealLogState.collectAsState()
    var showDelete by remember { mutableStateOf(false) }
    var showQuickAdd by remember { mutableStateOf(false) }
    var nutritionExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(recipe?.id) { recipe?.let(viewModel::beginMeal) }
    BackHandler { viewModel.endMeal(recipeId); onBack() }

    if (showDelete) AlertDialog(
        onDismissRequest = { showDelete = false },
        title = { Text("Delete this recipe?") },
        text = { Text("This removes it from your cookbook. Logged meals stay in your history.") },
        confirmButton = { TextButton(onClick = { viewModel.delete(recipeId); viewModel.endMeal(recipeId); onDeleted() }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Keep recipe") } }
    )
    if (showQuickAdd) QuickAddDialog(viewModel, onDismiss = { showQuickAdd = false })

    if (recipe == null || meal?.recipeId != recipeId) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val currentMeal = meal ?: return
    val contributionById = contributions.associateBy { it.ingredientId }
    val pinnedMeta = pinned?.let(NutrientCatalog.byKey::get)

    Scaffold(
        containerColor = Paper,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = { viewModel.endMeal(recipeId); onBack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                actions = {
                    TextButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Edit") }
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

                Spacer(Modifier.height(24.dp))
                NutritionNote(
                    whole = nutrition,
                    servings = recipe.servings,
                    expanded = nutritionExpanded,
                    onToggle = { nutritionExpanded = !nutritionExpanded }
                )

                if (pinnedMeta != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        color = MintWash,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.PushPin, null, tint = Herb)
                                Spacer(Modifier.width(8.dp))
                                Text("${pinnedMeta.displayName} pinned", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                TextButton(onClick = { viewModel.pinNutrient(null) }) { Text("Clear") }
                            }
                            contributions.firstOrNull()?.let { top ->
                                Text("${top.ingredientName} contributes the most ${pinnedMeta.displayName.lowercase()} in this meal.", color = Muted)
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth().padding(top = 30.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Ingredients", style = MaterialTheme.typography.headlineMedium)
                        Text("Check what actually went in", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = viewModel::checkAllMealIngredients) { Text("Check all") }
                    TextButton(onClick = viewModel::resetMealChecks, enabled = currentMeal.checkedIngredientIds.isNotEmpty()) { Text("Reset") }
                }
                HorizontalDivider(color = Line)
                currentMeal.ingredients.sortedBy { it.order }.forEach { ingredient ->
                    MealIngredientRow(
                        ingredient = ingredient,
                        pinned = pinnedMeta,
                        contribution = contributionById[ingredient.id]?.amount,
                        onToggle = { viewModel.toggleMealIngredient(ingredient.id) },
                        onAmountChange = { quantity, unit -> viewModel.updateMealAmount(ingredient.id, quantity, unit) },
                        onRemove = if (ingredient.sourceIngredientId == null) ({ viewModel.removeQuickAddedIngredient(ingredient.id) }) else null
                    )
                }
                OutlinedButton(onClick = { showQuickAdd = true }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Quick add ingredient")
                }

                Text("Steps", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 32.dp, bottom = 10.dp))
                HorizontalDivider(color = Line)
                recipe.steps.forEachIndexed { index, step ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                        Box(Modifier.size(32.dp).background(MintWash, CircleShape), contentAlignment = Alignment.Center) {
                            Text("${index + 1}", color = Herb, style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.width(13.dp)); Text(step.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    }
                }

                Surface(Modifier.fillMaxWidth().padding(top = 28.dp), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Ready to eat?", style = MaterialTheme.typography.headlineMedium)
                        OutlinedTextField(
                            value = formatQuantity(currentMeal.servingsConsumed),
                            onValueChange = viewModel::setServingsConsumed,
                            label = { Text("Servings consumed") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(onClick = onForecast, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.AutoGraph, null); Spacer(Modifier.width(8.dp)); Text("If I eat this") }
                        Button(
                            onClick = viewModel::logCurrentMeal,
                            enabled = currentMeal.checkedIngredientIds.isNotEmpty() && logState != MealLogState.SAVING && logState != MealLogState.SAVED,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (logState == MealLogState.SAVED) "Meal logged" else if (logState == MealLogState.SAVING) "Logging…" else "Log Meal") }
                        when {
                            currentMeal.checkedIngredientIds.isEmpty() -> Text("Check at least one ingredient before logging.", color = Muted, style = MaterialTheme.typography.bodySmall)
                            logState == MealLogState.SAVED -> Text("Added to today’s nutrition.", color = Herb, style = MaterialTheme.typography.bodySmall)
                            logState == MealLogState.ERROR -> Text("Couldn’t log this meal. Please try again.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(Modifier.height(36.dp))
            }
        }
    }
}

@Composable
private fun MealIngredientRow(
    ingredient: MealIngredient,
    pinned: NutrientMetadata?,
    contribution: Double?,
    onToggle: () -> Unit,
    onAmountChange: (String, IngredientUnit) -> Unit,
    onRemove: (() -> Unit)?
) {
    var editing by remember(ingredient.id) { mutableStateOf(false) }
    var quantity by remember(ingredient.id, ingredient.quantity) { mutableStateOf(formatQuantity(ingredient.quantity)) }
    var unit by remember(ingredient.id, ingredient.unit) { mutableStateOf(ingredient.unit) }
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(ingredient.isChecked, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f).clickable { onToggle() }) {
                Text(naturalIngredient(ingredient), style = MaterialTheme.typography.bodyLarge)
                if (ingredient.foodId != null && ingredient.gramsEquivalent == null) Text("No supported gram conversion", color = Muted, style = MaterialTheme.typography.labelSmall)
                if (pinned != null && contribution != null) Text("+${formatNutrition(contribution)} ${pinned.unit.symbol} ${pinned.displayName.lowercase()}", color = Herb, style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = { editing = !editing }) { Icon(Icons.Rounded.Tune, "Edit amount", tint = Muted) }
            if (onRemove != null) IconButton(onClick = onRemove) { Icon(Icons.Rounded.Close, "Remove quick-added ingredient", tint = Muted) }
        }
        if (editing) Row(Modifier.padding(start = 48.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(quantity, { quantity = it }, label = { Text("Amount") }, singleLine = true, modifier = Modifier.weight(1f))
            FoodUnitMenu(unit, { unit = it }, Modifier.weight(1f))
            FilledIconButton(onClick = { onAmountChange(quantity, unit); editing = false }) { Icon(Icons.Rounded.Check, "Apply") }
        }
    }
}

@Composable
private fun QuickAddDialog(viewModel: CookbookViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf(IngredientUnit.NONE) }
    var selected by remember { mutableStateOf<FoodEntity?>(null) }
    var suggestions by remember { mutableStateOf(emptyList<FoodEntity>()) }
    LaunchedEffect(name, selected) {
        suggestions = emptyList()
        if (selected == null && name.trim().length >= 2) { delay(180); suggestions = viewModel.searchFoods(name) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick add") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it; selected = null }, label = { Text("Ingredient") }, singleLine = true)
                suggestions.take(4).forEach { food ->
                    TextButton(onClick = { selected = food; name = food.name; suggestions = emptyList() }, modifier = Modifier.fillMaxWidth()) { Text(food.name, modifier = Modifier.fillMaxWidth()) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(quantity, { quantity = it }, label = { Text("Amount") }, singleLine = true, modifier = Modifier.weight(1f))
                    FoodUnitMenu(unit, { unit = it }, Modifier.weight(1f))
                }
                Text(if (selected == null) "Custom ingredients are allowed; nutrition stays unknown." else "Matched to USDA Foundation food.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { Button(onClick = { viewModel.quickAddIngredient(name, quantity, unit, selected); onDismiss() }, enabled = name.isNotBlank()) { Text("Add checked") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun naturalIngredient(item: MealIngredient): String {
    val unit = if (item.unit == IngredientUnit.NONE) "" else item.unit.label
    return listOf(formatQuantity(item.quantity), unit, item.name).filter(String::isNotBlank).joinToString(" ")
}
