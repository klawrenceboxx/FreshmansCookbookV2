package com.kaleel.freshmanscookbook.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kaleel.freshmanscookbook.data.*

private val customCoreNutrients = listOf(
    NutrientKey.CALORIES, NutrientKey.PROTEIN, NutrientKey.CARBOHYDRATE,
    NutrientKey.FAT, NutrientKey.FIBER
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomFoodEditorScreen(
    initial: CustomFoodEntity?,
    suggestedName: String,
    onBack: () -> Unit,
    onSave: (CustomFoodInput) -> Unit
) {
    val initialInput = remember(initial?.foodId) { initial?.toInput() }
    var name by remember(initial?.foodId) { mutableStateOf(initialInput?.name ?: suggestedName) }
    var description by remember(initial?.foodId) { mutableStateOf(initialInput?.description.orEmpty()) }
    var servingQuantity by remember(initial?.foodId) { mutableStateOf(initialInput?.servingQuantity?.let(::editableNumber) ?: "1") }
    var servingUnit by remember(initial?.foodId) { mutableStateOf(initialInput?.servingUnit ?: IngredientUnit.SCOOP) }
    var servingGrams by remember(initial?.foodId) { mutableStateOf(initialInput?.servingGrams?.let(::editableNumber).orEmpty()) }
    var sugar by remember(initial?.foodId) { mutableStateOf(initialInput?.totalSugarsG?.let(::editableNumber).orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    val nutrientValues = remember(initial?.foodId) {
        mutableStateMapOf<NutrientKey, String>().apply {
            initialInput?.nutrients?.forEach { (key, value) -> if (value != null) put(key, editableNumber(value)) }
            customCoreNutrients.forEach { putIfAbsent(it, "") }
        }
    }
    val optionalKeys = remember(initial?.foodId) {
        mutableStateListOf<NutrientKey>().apply {
            addAll(initialInput?.nutrients.orEmpty().filter { (key, value) -> key !in customCoreNutrients && value != null }.keys)
        }
    }
    val quantityValue = servingQuantity.toDoubleOrNull()
    val massServing = servingUnit in setOf(IngredientUnit.G, IngredientUnit.KG, IngredientUnit.OZ, IngredientUnit.LB)

    Scaffold(
        containerColor = Paper,
        topBar = {
            TopAppBar(
                title = { Text(if (initial == null) "Create custom food" else "Edit custom food") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("My food", style = MaterialTheme.typography.headlineMedium)
            Text("Save label nutrition once, then reuse it in any recipe.", color = Muted)
            OutlinedTextField(name, { name = it; error = null }, label = { Text("Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it }, label = { Text("Description (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())

            Text("Default serving", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuantityAmountField(
                    value = servingQuantity,
                    onValueChange = { servingQuantity = numericText(it); error = null },
                    modifier = Modifier.weight(.8f),
                    allowFractions = false
                )
                FoodUnitMenu(servingUnit, { servingUnit = it; error = null }, Modifier.weight(1.2f), includeNone = false)
            }
            MatchingTextField(
                value = servingGrams,
                onValueChange = { servingGrams = numericText(it); error = null },
                label = if (massServing) "Gram weight (calculated from unit)" else "Gram weight for this serving",
                enabled = !massServing,
                suffix = "g",
                modifier = Modifier.fillMaxWidth()
            )
            if (!massServing) Text("Leave gram weight blank if it is unknown. Nutrition will remain unresolved instead of being guessed.", color = Muted, style = MaterialTheme.typography.bodySmall)

            Text("Nutrition per serving", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
            customCoreNutrients.forEach { key ->
                val metadata = NutrientCatalog.byKey.getValue(key)
                NutrientEntryRow(metadata, nutrientValues[key].orEmpty(), onValueChange = { nutrientValues[key] = numericText(it) })
            }
            NutrientEntryRow(
                NutrientMetadata(NutrientKey.FIBER, "Total sugars", NutrientUnit.GRAM, NutrientCategory.MACRO),
                sugar,
                onValueChange = { sugar = numericText(it) }
            )
            optionalKeys.forEach { key ->
                val metadata = NutrientCatalog.byKey.getValue(key)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NutrientEntryRow(metadata, nutrientValues[key].orEmpty(), { nutrientValues[key] = numericText(it) }, Modifier.weight(1f))
                    IconButton(onClick = { optionalKeys.remove(key); nutrientValues.remove(key) }) { Icon(Icons.Rounded.Close, "Remove ${metadata.displayName}") }
                }
            }
            OptionalNutrientMenu(
                available = NutrientCatalog.all.map { it.key }.filter { it !in customCoreNutrients && it !in optionalKeys },
                onSelect = { optionalKeys += it; nutrientValues[it] = "" }
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Button(
                onClick = {
                    val quantity = quantityValue
                    val grams = servingGrams.toDoubleOrNull()
                    when {
                        name.isBlank() -> error = "Enter a food name."
                        quantity == null || quantity <= 0 -> error = "Enter a positive serving amount."
                        servingUnit == IngredientUnit.NONE -> error = "Choose a serving unit."
                        !massServing && servingGrams.isNotBlank() && (grams == null || grams <= 0) -> error = "Gram weight must be positive."
                        else -> onSave(
                            CustomFoodInput(
                                foodId = initial?.foodId,
                                name = name,
                                description = description,
                                servingQuantity = quantity,
                                servingUnit = servingUnit,
                                servingGrams = grams,
                                totalSugarsG = sugar.toDoubleOrNull(),
                                nutrients = nutrientValues.mapValues { (_, value) -> value.toDoubleOrNull() }
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 28.dp)
            ) { Text(if (initial == null) "Save custom food" else "Save changes") }
        }
    }
}

@Composable
private fun MatchingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    suffix: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        enabled = enabled,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.height(58.dp)
    )
}

@Composable
private fun NutrientEntryRow(
    metadata: NutrientMetadata,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(metadata.displayName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            suffix = { Text(metadata.unit.symbol) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(150.dp)
        )
    }
}

@Composable
private fun OptionalNutrientMenu(available: List<NutrientKey>, onSelect: (NutrientKey) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }, enabled = available.isNotEmpty()) {
            Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text("Add nutrient")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            available.forEach { key ->
                DropdownMenuItem(
                    text = { Text(NutrientCatalog.byKey.getValue(key).displayName) },
                    onClick = { onSelect(key); open = false }
                )
            }
        }
    }
}

private fun numericText(value: String): String = value.filter { it.isDigit() || it == '.' }
private fun editableNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
