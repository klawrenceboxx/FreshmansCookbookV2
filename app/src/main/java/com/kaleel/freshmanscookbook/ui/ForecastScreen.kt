package com.kaleel.freshmanscookbook.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kaleel.freshmanscookbook.CookbookViewModel
import com.kaleel.freshmanscookbook.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(viewModel: CookbookViewModel, onBack: () -> Unit, onPinned: () -> Unit) {
    val forecast by viewModel.forecast.collectAsState()
    val meal by viewModel.meal.collectAsState()
    Scaffold(
        containerColor = Paper,
        topBar = { TopAppBar(
            title = { Text("If I eat this") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper)
        ) }
    ) { padding ->
        val result = forecast
        if (result == null) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(22.dp)) {
            Text(meal?.recipeName.orEmpty(), style = MaterialTheme.typography.displaySmall)
            Text("Planned ingredients · ${formatNutrition(meal?.servingsConsumed ?: 0.0)} serving${if (meal?.servingsConsumed == 1.0) "" else "s"}", color = Muted)
            Text("Tap a nutrient to pin its ingredient contributions.", color = Herb, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 20.dp))
            NutrientCatalog.all.forEach { metadata ->
                val current = MealNutritionCalculator.nutrientValue(result.current, metadata.key)
                val proposed = MealNutritionCalculator.nutrientValue(result.meal, metadata.key)
                val projected = MealNutritionCalculator.nutrientValue(result.projected, metadata.key)
                val target = result.progress.firstOrNull { it.nutrient == metadata.key }?.target
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { viewModel.pinNutrient(metadata.key); onPinned() },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(metadata.displayName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.PushPin, "Pin ${metadata.displayName}", tint = Herb, modifier = Modifier.size(18.dp))
                        }
                        Text("Current ${formatNutrition(current)}  ·  Meal +${formatNutrition(proposed)} ${metadata.unit.symbol}", color = Muted, style = MaterialTheme.typography.bodySmall)
                        Text(if (target == null) "Projected ${formatNutrition(projected)} ${metadata.unit.symbol}" else "Projected ${formatNutrition(projected)} / ${formatNutrition(target)} ${metadata.unit.symbol}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
