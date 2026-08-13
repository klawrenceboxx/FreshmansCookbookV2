package com.kaleel.freshmanscookbook.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kaleel.freshmanscookbook.*
import com.kaleel.freshmanscookbook.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

private val editorLabels = listOf("Basics", "Ingredients", "Steps", "Review")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(viewModel: CookbookViewModel, onClose: () -> Unit, onSaved: (String) -> Unit) {
    val draft by viewModel.draft.collectAsState()
    var page by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val canContinue = when (page) {
        0 -> draft.name.isNotBlank() && draft.category != null && (draft.servingsText.toIntOrNull() ?: 0) > 0
        1 -> draft.ingredients.any { it.name.isNotBlank() }
        2 -> draft.steps.any { it.text.isNotBlank() }
        else -> true
    }

    Scaffold(
        containerColor = Paper,
        topBar = {
            TopAppBar(
                title = { Text(if (draft.createdAt == 0L) "Add recipe" else if (page == 3) "Review recipe" else "Recipe editor") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Close") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Paper) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (page > 0) OutlinedButton(onClick = { page-- }, modifier = Modifier.weight(1f)) { Text("Back") }
                    Button(
                        onClick = {
                            if (page < 3) page++ else scope.launch { onSaved(viewModel.save()) }
                        },
                        enabled = canContinue,
                        modifier = Modifier.weight(1f)
                    ) { Text(if (page == 3) "Save Recipe" else "Continue") }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            EditorProgress(page, onStep = { if (it < page) page = it })
            when (page) {
                0 -> BasicsStep(draft, viewModel::updateDraft)
                1 -> IngredientsStep(draft, viewModel, viewModel::updateDraft)
                2 -> StepsStep(draft, viewModel::updateDraft)
                else -> ReviewStep(draft, onEdit = { page = it })
            }
        }
    }
}

@Composable
private fun EditorProgress(current: Int, onStep: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.Top) {
        editorLabels.forEachIndexed { index, label ->
            Column(
                Modifier.width(64.dp).clickable(enabled = index < current) { onStep(index) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(if (index <= current) Herb else Line),
                    contentAlignment = Alignment.Center
                ) {
                    if (index < current) Icon(Icons.Rounded.Check, null, tint = Paper, modifier = Modifier.size(17.dp))
                    else Text("${index + 1}", color = if (index == current) Paper else Muted, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = if (index == current) Ink else Muted)
            }
            if (index < 3) HorizontalDivider(Modifier.weight(1f).padding(top = 15.dp), color = if (index < current) Herb else Line)
        }
    }
}

@Composable
private fun BasicsStep(draft: RecipeDraft, update: ((RecipeDraft) -> RecipeDraft) -> Unit) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { imported -> copyImageToApp(context, imported)?.let { path -> update { it.copy(imagePath = path) } } }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SectionIntro("The essentials", "Just enough detail to find and cook this again.")
        RecipeImage(draft.imagePath, Modifier.fillMaxWidth().aspectRatio(1f), 24)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { picker.launch("image/*") }) { Text(if (draft.imagePath == null) "Choose image" else "Replace") }
            if (draft.imagePath != null) TextButton(onClick = { update { it.copy(imagePath = null) } }) { Text("Remove") }
        }
        OutlinedTextField(
            value = draft.name,
            onValueChange = { value -> update { it.copy(name = value) } },
            label = { Text("Recipe name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text("Category", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RecipeCategory.entries.forEach { category ->
                FilterChip(
                    selected = draft.category == category,
                    onClick = { update { it.copy(category = category) } },
                    label = { Text(category.label) }
                )
            }
        }
        OutlinedTextField(
            value = draft.servingsText,
            onValueChange = { value -> if (value.all(Char::isDigit)) update { it.copy(servingsText = value) } },
            label = { Text("Servings") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun IngredientsStep(
    draft: RecipeDraft,
    viewModel: CookbookViewModel,
    update: ((RecipeDraft) -> RecipeDraft) -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionIntro("Build your ingredient list", "Keep each item separate so it’s easy to scan at the stove.")
        draft.ingredients.forEachIndexed { index, ingredient ->
            IngredientEditor(
                number = index + 1,
                item = ingredient,
                viewModel = viewModel,
                canMoveUp = index > 0,
                canMoveDown = index < draft.ingredients.lastIndex,
                onChange = { changed -> update { it.copy(ingredients = it.ingredients.toMutableList().apply { set(index, changed) }) } },
                onDelete = { update { it.copy(ingredients = it.ingredients.filterIndexed { i, _ -> i != index }) } },
                onMove = { direction ->
                    moveItem(draft.ingredients.size, index, direction)?.let { target -> update { it.copy(ingredients = it.ingredients.moved(index, target)) } }
                }
            )
        }
        OutlinedButton(
            onClick = { update { it.copy(ingredients = it.ingredients + IngredientDraft()) } },
            modifier = Modifier.fillMaxWidth()
        ) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Add Ingredient") }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun IngredientEditor(
    number: Int,
    item: IngredientDraft,
    viewModel: CookbookViewModel,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onChange: (IngredientDraft) -> Unit,
    onDelete: () -> Unit,
    onMove: (Int) -> Unit
) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("INGREDIENT ${number.toString().padStart(2, '0')}", style = MaterialTheme.typography.labelMedium, color = Herb, modifier = Modifier.weight(1f))
                IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) { Icon(Icons.Rounded.KeyboardArrowUp, "Move up") }
                IconButton(onClick = { onMove(1) }, enabled = canMoveDown) { Icon(Icons.Rounded.KeyboardArrowDown, "Move down") }
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, "Delete", tint = Muted) }
            }
            FoodAutocompleteField(item = item, viewModel = viewModel, onChange = onChange)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    item.quantityText,
                    { raw -> if (raw.all { it.isDigit() || it == '.' || it == '/' }) onChange(item.copy(quantityText = raw)) },
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                UnitMenu(item.unit, onSelect = { onChange(item.copy(unit = it)) }, modifier = Modifier.weight(1f))
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("1/4", "1/3", "1/2", "2/3", "3/4").forEach { fraction ->
                    SuggestionChip(onClick = { onChange(item.copy(quantityText = fraction)) }, label = { Text(fraction) })
                }
            }
        }
    }
}

@Composable
private fun FoodAutocompleteField(
    item: IngredientDraft,
    viewModel: CookbookViewModel,
    onChange: (IngredientDraft) -> Unit
) {
    var suggestions by remember(item.id) { mutableStateOf(emptyList<FoodEntity>()) }
    LaunchedEffect(item.name, item.foodId) {
        suggestions = emptyList()
        if (item.foodId == null && item.name.trim().length >= 2) {
            delay(180)
            suggestions = viewModel.searchFoods(item.name)
        }
    }

    Column {
        OutlinedTextField(
            value = item.name,
            onValueChange = { onChange(item.copy(name = it, foodId = null, gramsEquivalent = null)) },
            label = { Text("Ingredient name") },
            singleLine = true,
            trailingIcon = {
                if (item.foodId != null) Icon(Icons.Rounded.CheckCircle, "Matched to USDA food", tint = Herb)
            },
            supportingText = if (item.foodId != null) ({ Text("USDA Foundation food", color = Herb) }) else null,
            modifier = Modifier.fillMaxWidth()
        )
        if (suggestions.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Column {
                    suggestions.take(6).forEach { food ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                onChange(item.copy(name = food.name, foodId = food.foodId, gramsEquivalent = null))
                                suggestions = emptyList()
                            }.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(food.name, style = MaterialTheme.typography.bodyMedium)
                                Text(food.category.name.replace('_', ' ').lowercase(), style = MaterialTheme.typography.labelMedium, color = Muted)
                            }
                            Text("USDA", style = MaterialTheme.typography.labelMedium, color = Herb)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitMenu(selected: IngredientUnit, onSelect: (IngredientUnit) -> Unit, modifier: Modifier = Modifier) {
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

@Composable
private fun StepsStep(draft: RecipeDraft, update: ((RecipeDraft) -> RecipeDraft) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionIntro("Write the method", "One clear action per step is easiest to follow while cooking.")
        draft.steps.forEachIndexed { index, step ->
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Herb), contentAlignment = Alignment.Center) { Text("${index + 1}", color = Paper) }
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = step.text,
                        onValueChange = { value -> update { it.copy(steps = it.steps.toMutableList().apply { set(index, step.copy(text = value)) }) } },
                        placeholder = { Text("Describe this step") },
                        minLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                    Column {
                        IconButton(onClick = { moveItem(draft.steps.size, index, -1)?.let { target -> update { it.copy(steps = it.steps.moved(index, target)) } } }, enabled = index > 0) { Icon(Icons.Rounded.KeyboardArrowUp, "Move up") }
                        IconButton(onClick = { moveItem(draft.steps.size, index, 1)?.let { target -> update { it.copy(steps = it.steps.moved(index, target)) } } }, enabled = index < draft.steps.lastIndex) { Icon(Icons.Rounded.KeyboardArrowDown, "Move down") }
                        IconButton(onClick = { update { it.copy(steps = it.steps.filterIndexed { i, _ -> i != index }) } }) { Icon(Icons.Rounded.DeleteOutline, "Delete", tint = Muted) }
                    }
                }
            }
        }
        OutlinedButton(onClick = { update { it.copy(steps = it.steps + StepDraft()) } }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Add Step")
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ReviewStep(draft: RecipeDraft, onEdit: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
        RecipeImage(draft.imagePath, Modifier.fillMaxWidth().aspectRatio(1.35f), 24)
        Spacer(Modifier.height(22.dp))
        Text(draft.name, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(6.dp))
        Text("${draft.category?.label.orEmpty()}  •  ${draft.servingsText} servings", color = Muted, style = MaterialTheme.typography.bodyLarge)
        ReviewHeader("Ingredients", "Edit", onClick = { onEdit(1) })
        draft.ingredients.filter { it.name.isNotBlank() }.forEach { item ->
            Text("${naturalIngredient(item)}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 7.dp))
        }
        ReviewHeader("Steps", "Edit", onClick = { onEdit(2) })
        draft.steps.filter { it.text.isNotBlank() }.forEachIndexed { index, step ->
            Row(Modifier.padding(vertical = 9.dp)) {
                Text("${index + 1}.", color = Herb, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(34.dp))
                Text(step.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            }
        }
        TextButton(onClick = { onEdit(0) }, modifier = Modifier.align(Alignment.End)) { Text("Edit basics") }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ReviewHeader(title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) { Text(action) }
    }
    HorizontalDivider(color = Line)
}

@Composable
private fun SectionIntro(title: String, body: String) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(5.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Muted)
    }
}

fun naturalIngredient(item: IngredientDraft): String {
    val quantity = formatQuantity(parseQuantity(item.quantityText))
    val unit = if (item.unit == IngredientUnit.NONE) "" else item.unit.label
    return listOf(quantity, unit, item.name.trim()).filter { it.isNotBlank() }.joinToString(" ")
}

private fun copyImageToApp(context: Context, uri: Uri): String? = runCatching {
    val dir = File(context.filesDir, "recipe_images").apply { mkdirs() }
    val file = File(dir, "${UUID.randomUUID()}.jpg")
    context.contentResolver.openInputStream(uri)!!.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
    file.absolutePath
}.getOrNull()
