package com.kaleel.freshmanscookbook.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kaleel.freshmanscookbook.data.*
import java.text.DecimalFormat

private val primaryNutrients = listOf(
    NutrientKey.CALORIES,
    NutrientKey.PROTEIN,
    NutrientKey.FIBER,
    NutrientKey.CARBOHYDRATE,
    NutrientKey.FAT
)

@Composable
fun NutritionNote(whole: NutritionTotals, servings: Int, expanded: Boolean, onToggle: () -> Unit) {
    val perServing = NutritionCalculator.perServing(whole, servings)
    Surface(color = MintWash, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Nutrition note", style = MaterialTheme.typography.titleLarge)
                    Text("Per serving · whole recipe in smaller type", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onToggle) { Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, if (expanded) "Show less" else "Show all nutrients") }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                primaryNutrients.forEach { key ->
                    val meta = NutrientCatalog.byKey.getValue(key)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(formatNutrition(NutritionCalculator.valueFor(perServing, key)), style = MaterialTheme.typography.titleMedium)
                        Text(if (key == NutrientKey.CALORIES) "kcal" else meta.displayName.take(7), color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Text("Whole recipe: ${formatNutrition(whole.caloriesKcal)} kcal", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 10.dp))
            if (expanded) {
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Line)
                NutrientCatalog.all.filter { it.key !in primaryNutrients }.forEach { meta ->
                    NutritionValueRow(meta, NutritionCalculator.valueFor(perServing, meta.key))
                }
            }
        }
    }
}

@Composable
fun NutritionValueRow(metadata: NutrientMetadata, amount: Double, target: Double? = null, mealAmount: Double? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(metadata.displayName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        if (mealAmount != null) Text("+${formatNutrition(mealAmount)}  ", color = Herb, style = MaterialTheme.typography.labelMedium)
        Text(
            if (target == null) "${formatNutrition(amount)} ${metadata.unit.symbol}"
            else "${formatNutrition(amount)} / ${formatNutrition(target)} ${metadata.unit.symbol}",
            color = Muted,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun NutrientProgressRow(metadata: NutrientMetadata, consumed: Double, target: Double?) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        NutritionValueRow(metadata, consumed, target)
        if (target != null && target > 0) LinearProgressIndicator(
            progress = { (consumed / target).coerceIn(0.0, 1.0).toFloat() },
            modifier = Modifier.fillMaxWidth().height(5.dp),
            color = Herb,
            trackColor = MintWash
        )
    }
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
