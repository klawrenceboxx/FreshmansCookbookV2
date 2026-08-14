package com.kaleel.freshmanscookbook.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kaleel.freshmanscookbook.CookbookViewModel
import com.kaleel.freshmanscookbook.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(viewModel: CookbookViewModel, onBack: () -> Unit) {
    val forecast by viewModel.forecast.collectAsState()
    val meal by viewModel.meal.collectAsState()
    val foods by viewModel.mealFoods.collectAsState()
    val pinned by viewModel.pinnedNutrient.collectAsState()
    var selected by remember { mutableStateOf<NutrientKey?>(null) }
    Scaffold(
        containerColor = Paper,
        topBar = {
            TopAppBar(
                title = { Text("If I eat this") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper)
            )
        }
    ) { padding ->
        val result = forecast
        val currentMeal = meal
        if (result == null || currentMeal == null) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text(currentMeal.recipeName, style = MaterialTheme.typography.headlineMedium)
            Text(
                "${formatNutrition(currentMeal.servingsConsumed)} serving${if (currentMeal.servingsConsumed == 1.0) "" else "s"} planned · recipe yields ${currentMeal.recipeServings}",
                color = Muted,
                style = MaterialTheme.typography.bodySmall
            )
            Text("Current → projected / target", color = Herb, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 17.dp, bottom = 7.dp))

            val ordered = buildList {
                addAll(primaryNutrientKeys())
                pinned?.takeIf { it !in this }?.let(::add)
                addAll(NutrientCatalog.all.map { it.key }.filter { it !in this })
            }
            Surface(color = Paper, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Line), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    ordered.forEach { key ->
                        val metadata = NutrientCatalog.byKey.getValue(key)
                        val current = MealNutritionCalculator.nutrientValue(result.current, key)
                        val proposed = MealNutritionCalculator.nutrientValue(result.meal, key)
                        val projected = MealNutritionCalculator.nutrientValue(result.projected, key)
                        val target = result.progress.firstOrNull { it.nutrient == key }?.target
                        val known = NutritionPresentation.isKnown(currentMeal, foods, key)
                        CompactForecastRow(
                            metadata = metadata,
                            current = current,
                            mealAmount = proposed,
                            projected = projected,
                            target = target,
                            known = known,
                            selected = selected == key,
                            pinned = pinned == key,
                            onClick = { selected = if (selected == key) null else key },
                            onPin = { viewModel.pinNutrient(if (pinned == key) null else key) }
                        )
                        if (selected == key) {
                            NutrientContributionDetail(
                                metadata = metadata,
                                amount = proposed,
                                known = known,
                                target = target,
                                contributions = MealNutritionCalculator.rankedContributions(currentMeal, foods, key, applyConsumedServingScale = true),
                                pinned = pinned == key,
                                onPin = { viewModel.pinNutrient(if (pinned == key) null else key) }
                            )
                        }
                    }
                }
            }
            Text("Meal values update with the serving amount selected on the recipe.", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 14.dp))
        }
    }
}

@Composable
private fun CompactForecastRow(
    metadata: NutrientMetadata,
    current: Double,
    mealAmount: Double,
    projected: Double,
    target: Double?,
    known: Boolean,
    selected: Boolean,
    pinned: Boolean,
    onClick: () -> Unit,
    onPin: () -> Unit
) {
    val percent = if (known) NutritionPresentation.percent(projected, target) else null
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(metadata.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (percent != null) Text("$percent%", style = MaterialTheme.typography.titleMedium)
            Icon(
                Icons.Rounded.PushPin,
                if (pinned) "Unpin ${metadata.displayName}" else "Pin ${metadata.displayName}",
                tint = if (pinned) Herb else Muted,
                modifier = Modifier.padding(start = 9.dp).size(18.dp).clickable(onClick = onPin)
            )
        }
        Text(
            if (!known) "${formatNutrition(current)} → unknown projected amount"
            else if (target == null) "${formatNutrition(current)} → ${formatNutrition(projected)} ${metadata.unit.symbol}"
            else "${formatNutrition(current)} → ${formatNutrition(projected)} / ${formatNutrition(target)} ${metadata.unit.symbol}",
            color = if (known) MaterialTheme.colorScheme.onSurface else Muted,
            style = MaterialTheme.typography.bodySmall
        )
        Text(if (known) "+${formatNutrition(mealAmount)} ${metadata.unit.symbol} from meal" else "Meal contribution is incomplete", color = Muted, style = MaterialTheme.typography.labelSmall)
        NutritionPresentation.progressFraction(projected, if (known) target else null)?.let { progress ->
            LinearProgressIndicator(progress = { progress }, color = Herb, trackColor = Line, modifier = Modifier.fillMaxWidth().height(5.dp))
        }
        if (selected || pinned) HorizontalDivider(color = if (selected) Herb else Line, thickness = if (selected) 2.dp else 1.dp)
        else HorizontalDivider(color = Line)
    }
}
