package com.kaleel.freshmanscookbook.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kaleel.freshmanscookbook.data.*
import java.text.DecimalFormat

private val primaryNutrients = listOf(
    NutrientKey.CALORIES,
    NutrientKey.PROTEIN,
    NutrientKey.CARBOHYDRATE,
    NutrientKey.FAT,
    NutrientKey.FIBER
)

@Composable
fun InteractiveNutritionLabel(
    totals: NutritionTotals,
    wholeRecipeCalories: Double,
    servingLabel: String,
    meal: MealInstance,
    foodsById: Map<String, FoodEntity>,
    targets: DailyNutritionTargets?,
    expanded: Boolean,
    selected: NutrientKey?,
    pinned: NutrientKey?,
    onToggleExpanded: () -> Unit,
    onSelect: (NutrientKey) -> Unit,
    onPin: (NutrientKey?) -> Unit
) {
    val secondary = NutrientCatalog.all.filter { it.key !in primaryNutrients }
    val selectedMeta = selected?.let(NutrientCatalog.byKey::get)
    val completeness = NutritionCompletenessCalculator.forMeal(meal, foodsById)
    Surface(color = Paper, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Line), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Nutrition", style = MaterialTheme.typography.headlineMedium)
                    Text(servingLabel, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onToggleExpanded) {
                    Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, if (expanded) "Show less nutrition" else "Show all nutrition")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface, thickness = 2.dp)
            primaryNutrients.forEachIndexed { index, key ->
                NutritionLabelRow(
                    metadata = NutrientCatalog.byKey.getValue(key),
                    value = NutrientValue(
                        MealNutritionCalculator.nutrientValue(totals, key),
                        completeness.value(key).completeness
                    ),
                    selected = selected == key,
                    pinned = pinned == key,
                    prominent = index == 0,
                    onSelect = { onSelect(key) },
                    onPin = { onPin(if (pinned == key) null else key) }
                )
            }
            val calories = completeness.value(NutrientKey.CALORIES)
            Text(
                when (calories.completeness) {
                    NutrientCompleteness.COMPLETE -> "Whole recipe: ${formatNutrition(wholeRecipeCalories)} kcal"
                    NutrientCompleteness.PARTIAL -> "Known whole-recipe subtotal: ${formatNutrition(wholeRecipeCalories)}+ kcal"
                    NutrientCompleteness.UNKNOWN -> "Whole-recipe calories: Unknown"
                },
                color = Muted,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 7.dp, bottom = 2.dp)
            )
            if (primaryNutrients.any { completeness.value(it).isPartial }) {
                Text(
                    "Known subtotal · some ingredient data unresolved",
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
            if (expanded) {
                HorizontalDivider(Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurface, thickness = 2.dp)
                secondary.forEach { metadata ->
                    NutritionLabelRow(
                        metadata = metadata,
                        value = NutrientValue(
                            MealNutritionCalculator.nutrientValue(totals, metadata.key),
                            completeness.value(metadata.key).completeness
                        ),
                        selected = selected == metadata.key,
                        pinned = pinned == metadata.key,
                        onSelect = { onSelect(metadata.key) },
                        onPin = { onPin(if (pinned == metadata.key) null else metadata.key) }
                    )
                }
            }
            TextButton(onClick = onToggleExpanded, modifier = Modifier.fillMaxWidth()) {
                Text(if (expanded) "Show less" else "View vitamins, minerals, and fats")
                Spacer(Modifier.width(4.dp))
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, Modifier.size(18.dp))
            }
            if (selectedMeta != null) {
                NutrientContributionDetail(
                    metadata = selectedMeta,
                    value = NutrientValue(
                        MealNutritionCalculator.nutrientValue(totals, selectedMeta.key),
                        completeness.value(selectedMeta.key).completeness
                    ),
                    target = targets?.get(selectedMeta.key)?.amount,
                    contributions = MealNutritionCalculator.rankedContributions(meal, foodsById, selectedMeta.key, applyConsumedServingScale = true),
                    pinned = pinned == selectedMeta.key,
                    onPin = { onPin(if (pinned == selectedMeta.key) null else selectedMeta.key) }
                )
            }
        }
    }
}

@Composable
private fun NutritionLabelRow(
    metadata: NutrientMetadata,
    value: NutrientValue,
    selected: Boolean,
    pinned: Boolean,
    prominent: Boolean = false,
    onSelect: () -> Unit,
    onPin: () -> Unit
) {
    Surface(color = if (selected || pinned) MintWash else Paper, shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onSelect).padding(horizontal = 7.dp, vertical = if (prominent) 10.dp else 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                metadata.displayName,
                style = if (prominent) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
                fontWeight = if (prominent || metadata.key in primaryNutrients) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            Text(
                formatNutrientValue(value, metadata, includeCaloriesUnit = false),
                color = if (value.hasKnownValue) MaterialTheme.colorScheme.onSurface else Muted,
                style = if (prominent) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.bodyMedium,
                fontWeight = if (prominent) FontWeight.Bold else FontWeight.Medium
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                if (pinned) Icons.Rounded.PushPin else Icons.Rounded.ChevronRight,
                if (pinned) "Unpin ${metadata.displayName}" else "View ${metadata.displayName} details",
                tint = if (pinned) Herb else Muted,
                modifier = Modifier.size(19.dp).clickable(onClick = if (pinned) onPin else onSelect)
            )
        }
    }
    HorizontalDivider(color = Line)
}

@Composable
fun NutrientContributionDetail(
    metadata: NutrientMetadata,
    value: NutrientValue,
    target: Double?,
    contributions: List<MealNutritionCalculator.IngredientContribution>,
    pinned: Boolean,
    onPin: () -> Unit
) {
    val percent = if (value.completeness == NutrientCompleteness.COMPLETE) NutritionPresentation.percent(value.amount, target) else null
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp), modifier = Modifier.padding(top = 10.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(metadata.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            value.completeness == NutrientCompleteness.UNKNOWN -> "No useful value is known · ingredient nutrition unresolved"
                            value.completeness == NutrientCompleteness.PARTIAL -> "Known subtotal ${formatNutrition(value.amount)}+ ${metadata.unit.symbol} · some ingredient nutrition is unresolved"
                            percent != null -> "${formatNutrition(value.amount)} ${metadata.unit.symbol} · $percent% of daily target"
                            else -> "${formatNutrition(value.amount)} ${metadata.unit.symbol} · no target configured"
                        },
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = onPin) {
                    Icon(Icons.Rounded.PushPin, null, Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text(if (pinned) "Pinned" else "Pin")
                }
            }
            if (contributions.isEmpty()) Text("No resolved ingredient contributions are available.", color = Muted, style = MaterialTheme.typography.bodySmall)
            else {
                Text(if (value.completeness == NutrientCompleteness.COMPLETE) "Top contributors" else "Known contributors", style = MaterialTheme.typography.labelLarge)
                val largest = contributions.first().amount
                contributions.take(5).forEach { contribution ->
                    Column {
                        Row {
                            Text(contribution.ingredientName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            Text("${formatNutrition(contribution.amount)} ${metadata.unit.symbol}", style = MaterialTheme.typography.bodySmall)
                        }
                        LinearProgressIndicator(
                            progress = { (contribution.amount / largest).coerceIn(0.0, 1.0).toFloat() },
                            color = Herb,
                            trackColor = Line,
                            modifier = Modifier.fillMaxWidth().height(4.dp)
                        )
                    }
                }
                Text("${contributions.first().ingredientName} contributes the most ${metadata.displayName.lowercase()} in this meal.", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

@Composable
fun NutritionValueRow(metadata: NutrientMetadata, amount: Double, target: Double? = null, mealAmount: Double? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(metadata.displayName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        if (mealAmount != null) Text("+${formatNutrition(mealAmount)}  ", color = Herb, style = MaterialTheme.typography.labelMedium)
        Text(if (target == null) "${formatNutrition(amount)} ${metadata.unit.symbol}" else "${formatNutrition(amount)} / ${formatNutrition(target)} ${metadata.unit.symbol}", color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun NutrientProgressRow(metadata: NutrientMetadata, value: NutrientValue, target: Double?) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(metadata.displayName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(
                if (target == null || value.completeness != NutrientCompleteness.COMPLETE) {
                    formatNutrientValue(value, metadata)
                } else {
                    "${formatNutrition(value.amount)} / ${formatNutrition(target)} ${metadata.unit.symbol}"
                },
                color = if (value.hasKnownValue) Muted else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        NutritionPresentation.progressFraction(
            value.amount,
            if (value.completeness == NutrientCompleteness.COMPLETE) target else null
        )?.let { progress ->
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(5.dp), color = Herb, trackColor = MintWash)
        }
    }
}

fun formatNutrientValue(
    value: NutrientValue,
    metadata: NutrientMetadata,
    includeCaloriesUnit: Boolean = true
): String {
    if (!value.hasKnownValue) return "Unknown"
    val suffix = if (value.isPartial) "+" else ""
    val unit = if (metadata.key == NutrientKey.CALORIES && !includeCaloriesUnit) "" else " ${metadata.unit.symbol}"
    return "${formatNutrition(value.amount)}$suffix$unit"
}

@Composable
fun FoodUnitMenu(selected: IngredientUnit, onSelect: (IngredientUnit) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(if (selected == IngredientUnit.NONE) "No unit" else selected.label, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ArrowDropDown, null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            IngredientUnit.entries.forEach { unit ->
                DropdownMenuItem(text = { Text(if (unit == IngredientUnit.NONE) "none" else unit.label) }, onClick = { onSelect(unit); open = false })
            }
        }
    }
}

fun formatNutrition(value: Double): String = DecimalFormat(if (value >= 100) "0" else "0.#").format(value)

fun primaryNutrientKeys(): List<NutrientKey> = primaryNutrients
