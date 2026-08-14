package com.kaleel.freshmanscookbook.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kaleel.freshmanscookbook.CookbookViewModel
import com.kaleel.freshmanscookbook.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(viewModel: CookbookViewModel, onBack: () -> Unit, onLogged: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<FoodEntity?>(null) }
    var suggestions by remember { mutableStateOf(emptyList<FoodEntity>()) }
    var quantity by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf(IngredientUnit.G) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(query, selected) {
        suggestions = emptyList()
        if (selected == null && query.trim().length >= 2) { delay(180); suggestions = viewModel.searchFoods(query) }
    }

    Scaffold(
        containerColor = Paper,
        topBar = { TopAppBar(
            title = { Text("Add food") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper)
        ) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Log a snack or ingredient", style = MaterialTheme.typography.displaySmall)
            Text("Choose a USDA food or My Food with a supported gram conversion.", color = Muted)
            OutlinedTextField(
                query,
                { query = it; selected = null; error = null },
                label = { Text("Search foods") },
                trailingIcon = { if (selected != null) Icon(Icons.Rounded.CheckCircle, "Selected", tint = Herb) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (suggestions.isNotEmpty()) Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 3.dp) {
                Column {
                    suggestions.take(8).forEach { food ->
                        Row(
                            Modifier.fillMaxWidth().clickable { selected = food; query = food.name; suggestions = emptyList(); error = null }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(food.name)
                                Text(food.category.name.replace('_', ' ').lowercase(), color = Muted, style = MaterialTheme.typography.labelSmall)
                            }
                            Text(if (food.foodSource == FoodSource.CUSTOM) "CUSTOM" else "USDA", color = Herb, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    quantity,
                    { quantity = it; error = null },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                FoodUnitMenu(unit, { unit = it; error = null }, Modifier.weight(1f))
            }
            Text("Mass units always work. Household units require a saved food-specific gram conversion.", color = Muted, style = MaterialTheme.typography.bodySmall)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    val food = selected ?: return@Button
                    saving = true
                    scope.launch {
                        if (viewModel.logFood(food, quantity, unit)) onLogged()
                        else { error = "That amount can’t be converted to grams. Try grams or another supported unit."; saving = false }
                    }
                },
                enabled = selected != null && quantity.isNotBlank() && !saving,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (saving) "Adding…" else "Add to today") }
        }
    }
}
