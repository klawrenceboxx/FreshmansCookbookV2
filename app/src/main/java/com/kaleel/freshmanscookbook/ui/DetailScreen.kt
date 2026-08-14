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
    val mealFoods by viewModel.mealFoods.collectAsState()
    val nutrition by viewModel.recipeNutrition.collectAsState()
    val profileNutrition by viewModel.profileNutrition.collectAsState()
    val unresolvedNutritionIds by viewModel.unresolvedNutritionIngredientIds.collectAsState()
    val pinned by viewModel.pinnedNutrient.collectAsState()
    val contributions by viewModel.pinnedContributions.collectAsState()
    val logState by viewModel.mealLogState.collectAsState()
    var showDelete by remember { mutableStateOf(false) }
    var showQuickAdd by remember { mutableStateOf(false) }
    var nutritionExpanded by remember { mutableStateOf(false) }
    var selectedNutrient by remember { mutableStateOf<NutrientKey?>(null) }

    LaunchedEffect(recipe?.id) { recipe?.let(viewModel::beginMeal) }
    LaunchedEffect(pinned) {
        pinned?.let { nutrient ->
            selectedNutrient = nutrient
            if (nutrient !in primaryNutrientKeys()) nutritionExpanded = true
        }
    }
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
                        Button(
                            onClick = viewModel::logCurrentMeal,
                            enabled = currentMeal.checkedIngredientIds.isNotEmpty() && currentMeal.checkedIngredientIds.none(unresolvedNutritionIds::contains) && logState != MealLogState.SAVING && logState != MealLogState.SAVED,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (logState == MealLogState.SAVED) "Meal logged" else if (logState == MealLogState.SAVING) "Logging…" else "Log Meal") }
                        when {
                            currentMeal.checkedIngredientIds.isEmpty() -> Text("Check at least one ingredient before logging.", color = Muted, style = MaterialTheme.typography.bodySmall)
                            currentMeal.checkedIngredientIds.any(unresolvedNutritionIds::contains) -> Text("A checked ingredient has no authoritative gram conversion and can’t be logged yet.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            logState == MealLogState.SAVED -> Text("Added to today’s nutrition.", color = Herb, style = MaterialTheme.typography.bodySmall)
                            logState == MealLogState.ERROR -> Text("Couldn’t log this meal. Please try again.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable(onClick = onForecast),
                    shape = RoundedCornerShape(16.dp),
                    color = Paper,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Line)
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoGraph, null, tint = Herb)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("If I eat this", style = MaterialTheme.typography.titleMedium)
                            Text("See today’s projected nutrition", color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = Muted)
                    }
                }

                Text("Nutrition", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 30.dp, bottom = 10.dp))
                if (unresolvedNutritionIds.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Known subtotal only · one or more ingredient amounts are unresolved.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
                InteractiveNutritionLabel(
                    totals = NutritionPresentation.forConsumedServings(nutrition, recipe.servings, currentMeal.servingsConsumed),
                    wholeRecipeCalories = nutrition.caloriesKcal,
                    servingLabel = "For ${formatNutrition(currentMeal.servingsConsumed)} serving${if (currentMeal.servingsConsumed == 1.0) "" else "s"} · recipe yields ${recipe.servings}",
                    meal = currentMeal,
                    foodsById = mealFoods,
                    targets = profileNutrition?.targets,
                    expanded = nutritionExpanded,
                    selected = selectedNutrient,
                    pinned = pinned,
                    onToggleExpanded = { nutritionExpanded = !nutritionExpanded },
                    onSelect = { nutrient ->
                        selectedNutrient = if (selectedNutrient == nutrient) null else nutrient
                        if (nutrient !in primaryNutrientKeys()) nutritionExpanded = true
                    },
                    onPin = { nutrient ->
                        viewModel.pinNutrient(nutrient)
                        selectedNutrient = nutrient ?: selectedNutrient
                    }
                )
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
                if (ingredient.quantity != null && ingredient.quantity > 0 && ingredient.unit != IngredientUnit.NONE && ingredient.gramsEquivalent == null) Text("No supported gram conversion", color = Muted, style = MaterialTheme.typography.labelSmall)
                if (pinned != null && contribution != null) Text("+${formatNutrition(contribution)} ${pinned.unit.symbol} ${pinned.displayName.lowercase()}", color = Herb, style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = { editing = !editing }) { Icon(Icons.Rounded.Tune, "Edit amount", tint = Muted) }
            if (onRemove != null) IconButton(onClick = onRemove) { Icon(Icons.Rounded.Close, "Remove quick-added ingredient", tint = Muted) }
        }
        if (editing) Row(Modifier.padding(start = 48.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuantityAmountField(quantity, { quantity = it }, Modifier.weight(1f))
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
                    TextButton(onClick = { selected = food; name = food.userFacingName; suggestions = emptyList() }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(food.userFacingName)
                            if (food.userFacingName != food.name) Text(food.name, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuantityAmountField(quantity, { quantity = it }, Modifier.weight(1f))
                    FoodUnitMenu(unit, { unit = it }, Modifier.weight(1f))
                }
                Text(
                    if (selected == null) "Unlinked ingredients keep nutrition unresolved."
                    else if (selected?.foodSource == FoodSource.CUSTOM) "Matched to My Foods."
                    else "Matched to USDA food.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )
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
