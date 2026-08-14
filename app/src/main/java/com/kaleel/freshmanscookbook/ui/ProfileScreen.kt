package com.kaleel.freshmanscookbook.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kaleel.freshmanscookbook.CookbookViewModel
import com.kaleel.freshmanscookbook.formatQuantity
import com.kaleel.freshmanscookbook.data.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: CookbookViewModel, onBack: () -> Unit, onAddFood: () -> Unit) {
    val profile by viewModel.profile.collectAsState()
    val profileState by viewModel.profileNutrition.collectAsState()
    val day by viewModel.dailyNutrition.collectAsState()
    val hydration by viewModel.dailyHydration.collectAsState()
    var editing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refreshDay() }

    Scaffold(
        containerColor = Paper,
        topBar = {
            TopAppBar(
                title = { Text(if (profile == null || editing) "Nutrition profile" else "My day") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                actions = {
                    if (profile != null && !editing) IconButton(onClick = { editing = true }) { Icon(Icons.Rounded.Edit, "Edit profile") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper)
            )
        }
    ) { padding ->
        if (profile == null || editing) {
            ProfileForm(
                initial = profile,
                modifier = Modifier.padding(padding),
                onCancel = if (profile != null) ({ editing = false }) else null,
                onSave = { viewModel.saveProfile(it); editing = false }
            )
        } else {
            NutritionDashboard(
                day = day,
                hydration = hydration,
                targets = profileState?.targets,
                onAddFood = onAddFood,
                onDeleteMeal = viewModel::deleteMealLog,
                onDeleteFood = viewModel::deleteFoodLog,
                onLogWater = viewModel::logWater,
                onLogBottle = viewModel::logBottle,
                onDeleteWater = viewModel::deleteWaterLog,
                onSetWaterDisplayUnit = viewModel::setWaterDisplayUnit,
                onSetBottle = viewModel::setBottleAmount,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun NutritionDashboard(
    day: DailyNutritionSnapshot,
    hydration: HydrationDaySnapshot,
    targets: DailyNutritionTargets?,
    onAddFood: () -> Unit,
    onDeleteMeal: (String) -> Unit,
    onDeleteFood: (String) -> Unit,
    onLogWater: (Double, WaterUnit, String?) -> Unit,
    onLogBottle: () -> Unit,
    onDeleteWater: (String) -> Unit,
    onSetWaterDisplayUnit: (WaterDisplayUnit) -> Unit,
    onSetBottle: (Double, WaterUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    var showWaterDialog by remember { mutableStateOf(false) }
    var showBottleDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<HistoryDelete?>(null) }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
        Text("Today’s nutrition", style = MaterialTheme.typography.displaySmall)
        Text("${day.meals.size} logged meal${if (day.meals.size == 1) "" else "s"} · ${day.foods.size} added food${if (day.foods.size == 1) "" else "s"}", color = Muted)
        Button(onClick = onAddFood, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
            Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Add Food")
        }
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(17.dp)) {
                Text("Daily plate", style = MaterialTheme.typography.headlineMedium)
                primaryNutrientKeys().forEach { key ->
                    val metadata = NutrientCatalog.byKey.getValue(key)
                    NutrientProgressRow(metadata, day.nutrient(key), targets?.get(key)?.amount)
                }
                if (primaryNutrientKeys().any { day.nutrient(it).isPartial }) {
                    Text("+ Known subtotal · some logged nutrition is unresolved", color = Muted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        LoggedMealsSection(
            day = day,
            onDeleteMeal = { pendingDelete = HistoryDelete.Meal(it) },
            onDeleteFood = { pendingDelete = HistoryDelete.Food(it) }
        )

        HydrationSection(
            hydration = hydration,
            onAdd = { showWaterDialog = true },
            onLogBottle = onLogBottle,
            onConfigureBottle = { showBottleDialog = true },
            onDisplayUnit = onSetWaterDisplayUnit,
            onDelete = { pendingDelete = HistoryDelete.Water(it) }
        )

        Text("Vitamins & minerals", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 28.dp, bottom = 8.dp))
        NutrientCatalog.all.filter { it.key !in primaryNutrientKeys() }.forEach { metadata ->
            NutrientProgressRow(metadata, day.nutrient(metadata.key), targets?.get(metadata.key)?.amount)
        }
        Text("Targets are general dietary reference estimates, not medical advice.", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 24.dp, bottom = 32.dp))
    }

    if (showWaterDialog) {
        AddWaterDialog(
            bottleMl = hydration.preferences.bottleMl,
            onDismiss = { showWaterDialog = false },
            onLog = { amount, unit, label -> onLogWater(amount, unit, label); showWaterDialog = false },
            onLogBottle = { onLogBottle(); showWaterDialog = false },
            onConfigureBottle = { showWaterDialog = false; showBottleDialog = true }
        )
    }
    if (showBottleDialog) {
        ConfigureBottleDialog(
            currentMl = hydration.preferences.bottleMl,
            onDismiss = { showBottleDialog = false },
            onSave = { amount, unit -> onSetBottle(amount, unit); showBottleDialog = false }
        )
    }
    pendingDelete?.let { deletion ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove this log?") },
            text = { Text("Today’s totals will recalculate from the remaining logs. The original recipe or food will not be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    when (deletion) {
                        is HistoryDelete.Meal -> onDeleteMeal(deletion.id)
                        is HistoryDelete.Food -> onDeleteFood(deletion.id)
                        is HistoryDelete.Water -> onDeleteWater(deletion.id)
                    }
                    pendingDelete = null
                }) { Text("Remove log", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Keep") } }
        )
    }
}

private sealed interface HistoryDelete {
    data class Meal(val id: String) : HistoryDelete
    data class Food(val id: String) : HistoryDelete
    data class Water(val id: String) : HistoryDelete
}

@Composable
private fun LoggedMealsSection(
    day: DailyNutritionSnapshot,
    onDeleteMeal: (String) -> Unit,
    onDeleteFood: (String) -> Unit
) {
    Text("Logged meals", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 28.dp, bottom = 8.dp))
    if (day.meals.isEmpty() && day.foods.isEmpty()) {
        Text("Meals and individually added foods will appear here.", color = Muted, style = MaterialTheme.typography.bodySmall)
        return
    }
    val rows = buildList {
        day.meals.forEach { add(LoggedRow(it.meal.id, it.meal.recipeName, "${formatQuantity(it.meal.servingsConsumed)} serving${if (it.meal.servingsConsumed == 1.0) "" else "s"} · Recipe", it.meal.loggedAt, true)) }
        day.foods.forEach { food ->
            val amount = food.quantity?.let { "${formatQuantity(it)} ${if (food.unit == IngredientUnit.NONE) "serving" else food.unit.label}" } ?: "Added food"
            add(LoggedRow(food.id, food.foodName, "$amount · Added food", food.loggedAt, false))
        }
    }.sortedByDescending { it.loggedAt }
    Surface(color = Paper, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
        Column(Modifier.padding(horizontal = 14.dp)) {
            rows.forEachIndexed { index, row ->
                HistoryRow(row.name, row.detail, row.loggedAt) { if (row.isMeal) onDeleteMeal(row.id) else onDeleteFood(row.id) }
                if (index != rows.lastIndex) HorizontalDivider(color = Line)
            }
        }
    }
}

private data class LoggedRow(val id: String, val name: String, val detail: String, val loggedAt: Long, val isMeal: Boolean)

@Composable
private fun HistoryRow(name: String, detail: String, loggedAt: Long, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text("$detail · ${formatLogTime(loggedAt)}", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Box {
            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, "Log actions") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Remove log") },
                    leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                    onClick = { menuOpen = false; onDelete() }
                )
            }
        }
    }
}

@Composable
private fun HydrationSection(
    hydration: HydrationDaySnapshot,
    onAdd: () -> Unit,
    onLogBottle: () -> Unit,
    onConfigureBottle: () -> Unit,
    onDisplayUnit: (WaterDisplayUnit) -> Unit,
    onDelete: (String) -> Unit
) {
    Text("Water", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 28.dp, bottom = 8.dp))
    Surface(color = MintWash, shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WaterDrop, null, tint = Herb)
                Spacer(Modifier.width(9.dp))
                Text(formatWaterTotal(hydration.totalMl, hydration.preferences.displayUnit), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                FilterChip(
                    selected = hydration.preferences.displayUnit == WaterDisplayUnit.LITERS,
                    onClick = { onDisplayUnit(WaterDisplayUnit.LITERS) },
                    label = { Text("L") }
                )
                Spacer(Modifier.width(5.dp))
                FilterChip(
                    selected = hydration.preferences.displayUnit == WaterDisplayUnit.CUPS,
                    onClick = { onDisplayUnit(WaterDisplayUnit.CUPS) },
                    label = { Text("cups") }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAdd, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(5.dp)); Text("Add water") }
                if (hydration.preferences.bottleMl != null) OutlinedButton(onClick = onLogBottle, modifier = Modifier.weight(1f)) { Text("My bottle") }
                else OutlinedButton(onClick = onConfigureBottle, modifier = Modifier.weight(1f)) { Text("Set bottle") }
            }
            if (hydration.logs.isNotEmpty()) {
                HorizontalDivider(color = Line)
                Text("Water today", style = MaterialTheme.typography.titleMedium)
                hydration.logs.forEach { log ->
                    val label = log.label ?: formatEnteredWater(log.enteredAmount, log.enteredUnit)
                    HistoryRow(label, formatEnteredWater(log.enteredAmount, log.enteredUnit), log.loggedAt) { onDelete(log.id) }
                }
            }
        }
    }
}

@Composable
private fun AddWaterDialog(
    bottleMl: Double?,
    onDismiss: () -> Unit,
    onLog: (Double, WaterUnit, String?) -> Unit,
    onLogBottle: () -> Unit,
    onConfigureBottle: () -> Unit
) {
    var customAmount by remember { mutableStateOf("") }
    var customUnit by remember { mutableStateOf(WaterUnit.MILLILITERS) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add water") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    AssistChip(onClick = { onLog(250.0, WaterUnit.MILLILITERS, null) }, label = { Text("250 mL") })
                    AssistChip(onClick = { onLog(500.0, WaterUnit.MILLILITERS, null) }, label = { Text("500 mL") })
                    AssistChip(onClick = { onLog(1.0, WaterUnit.CUPS, null) }, label = { Text("1 cup") })
                }
                AssistChip(onClick = { onLog(1.0, WaterUnit.LITERS, null) }, label = { Text("1 L") })
                if (bottleMl != null) AssistChip(onClick = onLogBottle, label = { Text("My bottle · ${formatNutrition(bottleMl)} mL") })
                else TextButton(onClick = onConfigureBottle) { Text("Configure My bottle") }
                OutlinedTextField(
                    value = customAmount,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) customAmount = it },
                    label = { Text("Custom amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                WaterUnitPicker(customUnit) { customUnit = it }
            }
        },
        confirmButton = {
            TextButton(
                enabled = customAmount.toDoubleOrNull()?.let { it > 0.0 } == true,
                onClick = { onLog(customAmount.toDouble(), customUnit, null) }
            ) { Text("Add custom") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ConfigureBottleDialog(currentMl: Double?, onDismiss: () -> Unit, onSave: (Double, WaterUnit) -> Unit) {
    var amount by remember { mutableStateOf(currentMl?.let { formatNutrition(it) }.orEmpty()) }
    var unit by remember { mutableStateOf(WaterUnit.MILLILITERS) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("My bottle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Set the amount once, then log your usual bottle in one tap.", color = Muted, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
                    label = { Text("Bottle amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                WaterUnitPicker(unit) { unit = it }
            }
        },
        confirmButton = { TextButton(enabled = amount.toDoubleOrNull()?.let { it > 0.0 } == true, onClick = { onSave(amount.toDouble(), unit) }) { Text("Save bottle") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun WaterUnitPicker(selected: WaterUnit, onSelect: (WaterUnit) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        WaterUnit.entries.forEach { unit ->
            FilterChip(selected = selected == unit, onClick = { onSelect(unit) }, label = { Text(unit.label) })
        }
    }
}

private fun formatWaterTotal(amountMl: Double, unit: WaterDisplayUnit): String {
    val converted = WaterConversion.fromMilliliters(amountMl, unit)
    return "${formatNutrition(converted)} ${if (unit == WaterDisplayUnit.LITERS) "L" else "cups"} logged"
}

private fun formatEnteredWater(amount: Double, unit: WaterUnit): String =
    "${formatNutrition(amount)} ${if (unit == WaterUnit.CUPS && amount == 1.0) "cup" else unit.label}"

private fun formatLogTime(timestamp: Long): String = Instant.ofEpochMilli(timestamp)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("h:mm a"))

@Composable
private fun ProfileForm(
    initial: NutritionProfile?,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)?,
    onSave: suspend (NutritionProfile) -> Unit
) {
    var age by remember(initial) { mutableStateOf(initial?.ageYears?.toString().orEmpty()) }
    var height by remember(initial) { mutableStateOf(initial?.heightCm?.toString().orEmpty()) }
    var weight by remember(initial) { mutableStateOf(initial?.weightKg?.toString().orEmpty()) }
    var sex by remember(initial) { mutableStateOf(initial?.sex ?: BiologicalSex.MALE) }
    var activity by remember(initial) { mutableStateOf(initial?.activityLevel ?: ActivityLevel.SEDENTARY) }
    var goal by remember(initial) { mutableStateOf(initial?.goal ?: NutritionGoal.MAINTAIN) }
    val scope = rememberCoroutineScope()
    val parsedAge = age.toIntOrNull()
    val parsedHeight = height.toDoubleOrNull()
    val parsedWeight = weight.toDoubleOrNull()
    val valid = parsedAge in 19..120 && parsedHeight != null && parsedHeight in 100.0..250.0 && parsedWeight != null && parsedWeight in 30.0..350.0

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(if (initial == null) "Set up your daily guide" else "Edit your guide", style = MaterialTheme.typography.displaySmall)
        Text("A few basics let the app calculate general daily reference targets. You can keep using recipes without a profile.", color = Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(age, { age = it }, "Age", Modifier.weight(1f))
            NumberField(height, { height = it }, "Height (cm)", Modifier.weight(1f))
        }
        NumberField(weight, { weight = it }, "Weight (kg)", Modifier.fillMaxWidth())
        ChoiceSection("Biological sex") {
            BiologicalSex.entries.forEach { value -> FilterChip(sex == value, { sex = value }, { Text(value.displayLabel()) }) }
        }
        ChoiceSection("Activity level") {
            ActivityLevel.entries.forEach { value -> FilterChip(activity == value, { activity = value }, { Text(value.displayLabel()) }) }
        }
        ChoiceSection("Goal") {
            NutritionGoal.entries.forEach { value -> FilterChip(goal == value, { goal = value }, { Text(value.displayLabel()) }) }
        }
        Text("Your goal is saved for context. It does not automatically change the maintenance calorie estimate.", color = Muted, style = MaterialTheme.typography.bodySmall)
        if (!valid && listOf(age, height, weight).all(String::isNotBlank)) Text("Enter an adult age (19–120), height (100–250 cm), and weight (30–350 kg).", color = MaterialTheme.colorScheme.error)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (onCancel != null) OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = { scope.launch { onSave(NutritionProfile(parsedAge!!, sex, parsedHeight!!, parsedWeight!!, activity, goal, initial?.overrides ?: NutritionTargetOverrides())) } },
                enabled = valid,
                modifier = Modifier.weight(1f)
            ) { Text("Save profile") }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier) {
    OutlinedTextField(value, { if (it.all { char -> char.isDigit() || char == '.' }) onChange(it) }, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = modifier)
}

@Composable
private fun ChoiceSection(title: String, content: @Composable RowScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

private fun BiologicalSex.displayLabel() = if (this == BiologicalSex.MALE) "Male" else "Female"
private fun ActivityLevel.displayLabel() = name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
private fun NutritionGoal.displayLabel() = name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
