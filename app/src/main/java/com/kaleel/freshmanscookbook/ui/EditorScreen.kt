package com.kaleel.freshmanscookbook.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kaleel.freshmanscookbook.*
import com.kaleel.freshmanscookbook.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

private val editorLabels = listOf("Basics", "Ingredients", "Steps", "Review")

private data class CustomFoodRequest(
    val ingredientId: String,
    val suggestedName: String,
    val initial: CustomFoodEntity? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(viewModel: CookbookViewModel, onClose: () -> Unit, onSaved: (String) -> Unit) {
    val draft by viewModel.draft.collectAsState()
    var page by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var validationError by remember { mutableStateOf<String?>(null) }
    var customFoodRequest by remember { mutableStateOf<CustomFoodRequest?>(null) }

    customFoodRequest?.let { request ->
        CustomFoodEditorScreen(
            initial = request.initial,
            suggestedName = request.suggestedName,
            onBack = { customFoodRequest = null },
            onSave = { input ->
                scope.launch {
                    val saved = viewModel.saveCustomFood(input)
                    viewModel.updateDraft { current ->
                        current.copy(ingredients = current.ingredients.map { ingredient ->
                            if (ingredient.id != request.ingredientId) ingredient
                            else ingredient.copy(
                                name = saved.displayName,
                                foodId = saved.food.foodId,
                                foodSource = FoodSource.CUSTOM,
                                quantityText = ingredient.quantityText.ifBlank { formatQuantity(saved.customFood?.servingQuantity) },
                                unit = ingredient.unit.takeIf { it != IngredientUnit.NONE } ?: saved.customFood?.servingUnit ?: IngredientUnit.NONE,
                                gramsEquivalent = null
                            )
                        })
                    }
                    customFoodRequest = null
                }
            }
        )
        return
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
                            validationError = null
                            if (page < 3) page++
                            else validateRecipeDraft(draft)?.let { issue ->
                                validationError = issue.second
                                page = issue.first
                            } ?: scope.launch { onSaved(viewModel.save()) }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (page == 3) "Save Recipe" else "Continue") }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            EditorProgress(page, onStep = { page = it; validationError = null })
            validationError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            }
            when (page) {
                0 -> BasicsStep(draft, viewModel::updateDraft)
                1 -> IngredientsStep(
                    draft,
                    viewModel,
                    viewModel::updateDraft,
                    onCreateCustomFood = { ingredientId, name -> customFoodRequest = CustomFoodRequest(ingredientId, name) },
                    onEditCustomFood = { ingredientId, foodId, name ->
                        scope.launch {
                            customFoodRequest = CustomFoodRequest(ingredientId, name, viewModel.getCustomFood(foodId))
                        }
                    }
                )
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
                Modifier.width(64.dp).clickable { onStep(index) },
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
    update: ((RecipeDraft) -> RecipeDraft) -> Unit,
    onCreateCustomFood: (String, String) -> Unit,
    onEditCustomFood: (String, String, String) -> Unit
) {
    var expandedId by rememberSaveable(draft.id) { mutableStateOf<String?>(null) }
    var focusId by remember { mutableStateOf<String?>(null) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dragSourceIndex by remember { mutableIntStateOf(-1) }
    var dragTargetIndex by remember { mutableIntStateOf(-1) }
    var dragStartPointerY by remember { mutableFloatStateOf(0f) }
    var dragBoundsSnapshot by remember { mutableStateOf<Map<String, Pair<Float, Float>>>(emptyMap()) }
    val rowBounds = remember(draft.id) { mutableStateMapOf<String, Pair<Float, Float>>() }
    val handleCenters = remember(draft.id) { mutableStateMapOf<String, Float>() }

    LaunchedEffect(draggingId) {
        val id = draggingId ?: return@LaunchedEffect
        // Allow the expanded row to collapse and every measured bound to settle
        // before freezing the geometry used for this gesture.
        withFrameNanos { }
        withFrameNanos { }
        dragBoundsSnapshot = rowBounds.toMap()
        val pointerY = dragStartPointerY + dragOffset
        dragTargetIndex = insertionIndexForPointer(draft.ingredients.map(IngredientDraft::id), id, pointerY, dragBoundsSnapshot)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp)
    ) {
        item(key = "ingredients-header") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { SectionIntro("Ingredients", "Build a compact list, then open one item to edit.") }
                TextButton(onClick = {
                    val item = IngredientDraft()
                    update { it.copy(ingredients = it.ingredients + item) }
                    expandedId = item.id
                    focusId = item.id
                }) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(5.dp)); Text("Add") }
            }
            Spacer(Modifier.height(12.dp))
        }
        itemsIndexed(draft.ingredients, key = { _, ingredient -> ingredient.id }) { index, ingredient ->
            val previewOffset = when {
                draggingId == null || ingredient.id == draggingId || dragSourceIndex < 0 || dragTargetIndex < 0 -> 0f
                dragSourceIndex < dragTargetIndex && index in (dragSourceIndex + 1)..dragTargetIndex -> -(dragBoundsSnapshot[draggingId]?.let { it.second - it.first } ?: 0f)
                dragSourceIndex > dragTargetIndex && index in dragTargetIndex until dragSourceIndex -> dragBoundsSnapshot[draggingId]?.let { it.second - it.first } ?: 0f
                else -> 0f
            }
                CompactIngredientEditor(
                    item = ingredient,
                    expanded = expandedId == ingredient.id && draggingId == null,
                    dragging = draggingId == ingredient.id,
                    dragOffset = if (draggingId == ingredient.id) dragOffset else 0f,
                    previewOffset = previewOffset,
                    requestFocus = focusId == ingredient.id,
                    viewModel = viewModel,
                    onBoundsChanged = { top, bottom -> rowBounds[ingredient.id] = top to bottom },
                    onHandleCenterChanged = { center -> handleCenters[ingredient.id] = center },
                    onToggle = {
                        expandedId = if (expandedId == ingredient.id) null else ingredient.id
                        focusId = null
                    },
                    onChange = { changed ->
                        update { current -> current.copy(ingredients = current.ingredients.map { if (it.id == changed.id) changed else it }) }
                    },
                    onDelete = {
                        update { current -> current.copy(ingredients = current.ingredients.filterNot { it.id == ingredient.id }) }
                        if (expandedId == ingredient.id) expandedId = null
                    },
                    onDragStart = {
                        expandedId = null
                        focusId = null
                        draggingId = ingredient.id
                        dragOffset = 0f
                        dragSourceIndex = draft.ingredients.indexOfFirst { it.id == ingredient.id }
                        dragTargetIndex = dragSourceIndex
                        dragStartPointerY = handleCenters[ingredient.id] ?: 0f
                        dragBoundsSnapshot = emptyMap()
                    },
                    onDrag = { delta ->
                        dragOffset += delta
                        if (dragBoundsSnapshot.isNotEmpty()) {
                            dragTargetIndex = insertionIndexForPointer(
                                draft.ingredients.map(IngredientDraft::id), ingredient.id, dragStartPointerY + dragOffset, dragBoundsSnapshot
                            )
                        }
                    },
                    onDragEnd = {
                        val movedId = draggingId
                        val destination = dragTargetIndex
                        if (movedId != null && destination >= 0) {
                            update { current -> current.copy(ingredients = current.ingredients.movedByStableId(movedId, destination, IngredientDraft::id)) }
                        }
                        draggingId = null
                        dragOffset = 0f
                        dragSourceIndex = -1
                        dragTargetIndex = -1
                        dragStartPointerY = 0f
                        dragBoundsSnapshot = emptyMap()
                    },
                    onDragCancel = {
                        draggingId = null
                        dragOffset = 0f
                        dragSourceIndex = -1
                        dragTargetIndex = -1
                        dragStartPointerY = 0f
                        dragBoundsSnapshot = emptyMap()
                    },
                    onCreateCustomFood = { onCreateCustomFood(ingredient.id, ingredient.name) },
                    onEditCustomFood = { foodId -> onEditCustomFood(ingredient.id, foodId, ingredient.name) }
                )
        }
        item(key = "ingredients-tip") {
            Surface(color = MintWash, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Lightbulb, null, tint = Herb, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Use the handle to reorder. Ingredients collapse automatically while dragging.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun CompactIngredientEditor(
    item: IngredientDraft,
    expanded: Boolean,
    dragging: Boolean,
    dragOffset: Float,
    previewOffset: Float,
    requestFocus: Boolean,
    viewModel: CookbookViewModel,
    onBoundsChanged: (Float, Float) -> Unit,
    onHandleCenterChanged: (Float) -> Unit,
    onToggle: () -> Unit,
    onChange: (IngredientDraft) -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onCreateCustomFood: () -> Unit,
    onEditCustomFood: (String) -> Unit
) {
    val focusRequester = remember(item.id) { FocusRequester() }
    var resolvedGrams by remember(item.id) { mutableStateOf<Double?>(null) }
    LaunchedEffect(requestFocus, expanded) { if (requestFocus && expanded) focusRequester.requestFocus() }
    LaunchedEffect(item.foodId, item.quantityText, item.unit) {
        resolvedGrams = viewModel.resolveIngredientGrams(item.foodId, item.quantityText, item.unit)
    }
    Surface(
        shape = RoundedCornerShape(if (expanded) 16.dp else 0.dp),
        color = if (expanded) MaterialTheme.colorScheme.surfaceVariant else Paper,
        border = if (expanded || dragging) BorderStroke(1.dp, if (dragging) Herb else Line) else null,
        shadowElevation = if (dragging) 7.dp else 0.dp,
        modifier = Modifier.fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val top = coordinates.positionInWindow().y
                onBoundsChanged(top, top + coordinates.size.height)
            }
            .zIndex(if (dragging) 2f else 0f)
            .graphicsLayer { translationY = dragOffset + previewOffset }
    ) {
        Column {
            Row(Modifier.fillMaxWidth().heightIn(min = 54.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.DragHandle,
                    "Drag to reorder",
                    tint = Muted,
                    modifier = Modifier.padding(horizontal = 10.dp).size(22.dp)
                        .onGloballyPositioned { coordinates ->
                            onHandleCenterChanged(coordinates.positionInWindow().y + coordinates.size.height / 2f)
                        }
                        .pointerInput(item.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart() },
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
                            onDrag = { change, amount -> change.consume(); onDrag(amount.y) }
                        )
                    }
                )
                Row(Modifier.weight(1f).clickable(onClick = onToggle).padding(vertical = 13.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name.ifBlank { "New ingredient" }, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    if (item.foodSource == FoodSource.CUSTOM) CustomBadge()
                    Spacer(Modifier.width(8.dp))
                    Text(naturalIngredientAmount(item), color = Muted, style = MaterialTheme.typography.labelMedium)
                    Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ChevronRight, null, tint = Muted, modifier = Modifier.size(20.dp))
                }
            }
            if (expanded) Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FoodAutocompleteField(
                    item = item,
                    viewModel = viewModel,
                    onChange = onChange,
                    focusRequester = focusRequester,
                    onCreateCustomFood = onCreateCustomFood
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuantityAmountField(
                        item.quantityText,
                        { raw -> onChange(item.copy(quantityText = raw)) },
                        modifier = Modifier.weight(1f)
                    )
                    FoodUnitMenu(item.unit, onSelect = { onChange(item.copy(unit = it, gramsEquivalent = null)) }, modifier = Modifier.weight(1f))
                }
                Text("Quick amounts", color = Muted, style = MaterialTheme.typography.labelMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("1/4", "1/3", "1/2", "2/3", "3/4", "1").forEach { fraction ->
                        SuggestionChip(onClick = { onChange(item.copy(quantityText = fraction)) }, label = { Text(fraction) })
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (resolvedGrams != null) Icons.Rounded.CheckCircle else Icons.Rounded.Info, null, tint = if (resolvedGrams != null) Herb else Muted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        resolvedGrams?.let { "≈ ${formatQuantity(it)} g · ${if (item.foodSource == FoodSource.CUSTOM) "Custom serving" else "USDA conversion"}" }
                            ?: if (item.foodId == null) "Select a food to calculate nutrition" else "No supported gram conversion",
                        color = if (resolvedGrams != null) Herb else Muted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    if (item.foodSource == FoodSource.CUSTOM && item.foodId != null) TextButton(onClick = { onEditCustomFood(item.foodId) }) { Text("Edit") }
                }
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Rounded.DeleteOutline, null); Spacer(Modifier.width(6.dp)); Text("Remove ingredient")
                }
            }
        }
    }
}

@Composable
private fun FoodAutocompleteField(
    item: IngredientDraft,
    viewModel: CookbookViewModel,
    onChange: (IngredientDraft) -> Unit,
    focusRequester: FocusRequester,
    onCreateCustomFood: () -> Unit
) {
    var suggestions by remember(item.id) { mutableStateOf(emptyList<FoodSearchResult>()) }
    var linkedFood by remember(item.id) { mutableStateOf<FoodEntity?>(null) }
    var showRename by remember(item.id) { mutableStateOf(false) }
    var renameValue by remember(item.id) { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    LaunchedEffect(item.foodId) {
        linkedFood = item.foodId?.let { viewModel.getFood(it) }
    }
    LaunchedEffect(item.name, item.foodId) {
        suggestions = emptyList()
        if (item.foodId == null && item.name.trim().length >= 2) {
            delay(180)
            suggestions = viewModel.searchFoodOptions(item.name)
        }
    }

    if (showRename && item.foodId != null) AlertDialog(
        onDismissRequest = { showRename = false },
        title = { Text("Rename this food") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(renameValue, { renameValue = it }, label = { Text("Display name") }, singleLine = true)
                linkedFood?.let { Text("USDA description: ${it.name}", color = Muted, style = MaterialTheme.typography.bodySmall) }
                Text("Leave blank to use the generated name. Nutrition and the USDA link stay unchanged.", color = Muted, style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                val foodId = item.foodId ?: return@Button
                scope.launch {
                    viewModel.setFoodDisplayName(foodId, renameValue)?.let { food ->
                        linkedFood = food
                        onChange(item.copy(name = food.userFacingName))
                    }
                    showRename = false
                }
            }) { Text("Save name") }
        },
        dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } }
    )

    Column {
        OutlinedTextField(
            value = item.name,
            onValueChange = { onChange(item.copy(name = it, foodId = null, gramsEquivalent = null, foodSource = null)) },
            label = { Text("Food") },
            singleLine = true,
            trailingIcon = {
                if (item.foodId != null) Icon(Icons.Rounded.CheckCircle, "Matched food", tint = Herb)
            },
            supportingText = if (item.foodId != null) ({
                Text(
                    if (item.foodSource == FoodSource.CUSTOM) "My Food" else linkedFood?.name?.let { "USDA · $it" } ?: "USDA food",
                    color = Herb,
                    maxLines = 2
                )
            }) else null,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
        )
        if (suggestions.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Column {
                    suggestions.take(8).forEach { result ->
                        val food = result.food
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                linkedFood = food
                                onChange(item.copy(name = result.displayName, foodId = food.foodId, gramsEquivalent = null, foodSource = food.foodSource))
                                suggestions = emptyList()
                            }.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(result.displayName, style = MaterialTheme.typography.bodyMedium)
                                if (result.displayName != food.name) Text(food.name, style = MaterialTheme.typography.labelSmall, color = Muted, maxLines = 2)
                                result.servingLabel?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = Muted, maxLines = 1) }
                            }
                            if (food.foodSource == FoodSource.CUSTOM) CustomBadge() else Text("USDA", style = MaterialTheme.typography.labelMedium, color = Herb)
                        }
                        HorizontalDivider(color = Line)
                    }
                }
            }
        }
        if (item.foodId == null && item.name.isNotBlank()) {
            TextButton(onClick = onCreateCustomFood, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(5.dp)); Text("Create custom food")
            }
        } else if (item.foodSource == FoodSource.USDA && item.foodId != null) {
            TextButton(
                onClick = { renameValue = item.name; showRename = true },
                modifier = Modifier.align(Alignment.End)
            ) { Icon(Icons.Rounded.Edit, null, Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("Rename display") }
        }
    }
}

@Composable
private fun CustomBadge() {
    Surface(color = Color(0xFFEDE8FF), shape = RoundedCornerShape(6.dp)) {
        Text("CUSTOM", color = Color(0xFF58439A), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
    }
}

private fun naturalIngredientAmount(item: IngredientDraft): String {
    val quantity = formatQuantity(parseQuantity(item.quantityText))
    val unit = if (item.unit == IngredientUnit.NONE) "" else item.unit.label
    return listOf(quantity, unit).filter(String::isNotBlank).joinToString(" ")
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

/** Returns the editor page and message for the first genuinely required field. */
fun validateRecipeDraft(draft: RecipeDraft): Pair<Int, String>? = when {
    draft.name.isBlank() -> 0 to "Add a recipe name before saving."
    draft.category == null -> 0 to "Choose a recipe category before saving."
    (draft.servingsText.toIntOrNull() ?: 0) <= 0 -> 0 to "Enter at least one serving before saving."
    draft.ingredients.none { it.name.isNotBlank() } -> 1 to "Add at least one ingredient before saving."
    draft.steps.none { it.text.isNotBlank() } -> 2 to "Add at least one cooking step before saving."
    else -> null
}

private fun copyImageToApp(context: Context, uri: Uri): String? = runCatching {
    val dir = File(context.filesDir, "recipe_images").apply { mkdirs() }
    val file = File(dir, "${UUID.randomUUID()}.jpg")
    context.contentResolver.openInputStream(uri)!!.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
    file.absolutePath
}.getOrNull()
