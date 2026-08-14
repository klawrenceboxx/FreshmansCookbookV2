package com.kaleel.freshmanscookbook.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kaleel.freshmanscookbook.CookbookViewModel
import com.kaleel.freshmanscookbook.data.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: CookbookViewModel, onBack: () -> Unit, onAddFood: () -> Unit) {
    val profile by viewModel.profile.collectAsState()
    val profileState by viewModel.profileNutrition.collectAsState()
    val day by viewModel.dailyNutrition.collectAsState()
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
                totals = day.totals,
                targets = profileState?.targets,
                mealCount = day.meals.size,
                foodCount = day.foods.size,
                onAddFood = onAddFood,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun NutritionDashboard(
    totals: NutritionTotals,
    targets: DailyNutritionTargets?,
    mealCount: Int,
    foodCount: Int,
    onAddFood: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
        Text("Today’s nutrition", style = MaterialTheme.typography.displaySmall)
        Text("$mealCount logged meal${if (mealCount == 1) "" else "s"} · $foodCount added food${if (foodCount == 1) "" else "s"}", color = Muted)
        Button(onClick = onAddFood, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
            Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Add Food")
        }
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(17.dp)) {
                Text("Daily plate", style = MaterialTheme.typography.headlineMedium)
                primaryNutrientKeys().forEach { key ->
                    val metadata = NutrientCatalog.byKey.getValue(key)
                    NutrientProgressRow(metadata, NutritionCalculator.valueFor(totals, key), targets?.get(key)?.amount)
                }
            }
        }
        Text("Vitamins & minerals", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 28.dp, bottom = 8.dp))
        NutrientCatalog.all.filter { it.key !in primaryNutrientKeys() }.forEach { metadata ->
            NutrientProgressRow(metadata, NutritionCalculator.valueFor(totals, metadata.key), targets?.get(metadata.key)?.amount)
        }
        Text("Targets are general dietary reference estimates, not medical advice.", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 24.dp, bottom = 32.dp))
    }
}

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
