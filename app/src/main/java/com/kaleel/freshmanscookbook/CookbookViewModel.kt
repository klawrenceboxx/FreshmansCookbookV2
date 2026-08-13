package com.kaleel.freshmanscookbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kaleel.freshmanscookbook.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class IngredientDraft(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val quantityText: String = "",
    val unit: IngredientUnit = IngredientUnit.NONE
)

data class StepDraft(val id: String = UUID.randomUUID().toString(), val text: String = "")

data class RecipeDraft(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val category: RecipeCategory? = null,
    val servingsText: String = "2",
    val imagePath: String? = null,
    val ingredients: List<IngredientDraft> = listOf(IngredientDraft()),
    val steps: List<StepDraft> = listOf(StepDraft()),
    val createdAt: Long = System.currentTimeMillis()
)

class CookbookViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CookbookApplication).repository
    val recipes = repository.recipes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _draft = MutableStateFlow(RecipeDraft())
    val draft = _draft.asStateFlow()

    fun startNew() { _draft.value = RecipeDraft() }

    suspend fun startEdit(id: String) {
        val recipe = repository.get(id) ?: return
        _draft.value = RecipeDraft(
            id = recipe.id,
            name = recipe.name,
            category = recipe.category,
            servingsText = recipe.servings.toString(),
            imagePath = recipe.imagePath,
            ingredients = recipe.ingredients.map { IngredientDraft(it.id, it.name, formatQuantity(it.quantity), it.unit) },
            steps = recipe.steps.map { StepDraft(it.id, it.text) },
            createdAt = recipe.createdAt
        )
    }

    fun updateDraft(transform: (RecipeDraft) -> RecipeDraft) { _draft.update(transform) }

    suspend fun save(): String {
        val d = draft.value
        val now = System.currentTimeMillis()
        repository.save(Recipe(
            id = d.id,
            name = d.name.trim(),
            category = requireNotNull(d.category),
            servings = d.servingsText.toIntOrNull()?.coerceAtLeast(1) ?: 2,
            imagePath = d.imagePath,
            ingredients = d.ingredients.filter { it.name.isNotBlank() }.mapIndexed { i, it ->
                Ingredient(it.id, it.name.trim(), parseQuantity(it.quantityText), it.unit, i)
            },
            steps = d.steps.filter { it.text.isNotBlank() }.mapIndexed { i, it -> RecipeStep(it.id, it.text.trim(), i) },
            createdAt = d.createdAt,
            updatedAt = now
        ))
        return d.id
    }

    fun delete(id: String) { viewModelScope.launch { repository.delete(id) } }
}

fun parseQuantity(raw: String): Double? {
    val text = raw.trim()
    if (text.isEmpty()) return null
    text.toDoubleOrNull()?.let { return it }
    val parts = text.split("/")
    if (parts.size == 2) {
        val top = parts[0].toDoubleOrNull()
        val bottom = parts[1].toDoubleOrNull()
        if (top != null && bottom != null && bottom != 0.0) return top / bottom
    }
    return null
}

fun formatQuantity(value: Double?): String = when {
    value == null -> ""
    kotlin.math.abs(value - .25) < .001 -> "1/4"
    kotlin.math.abs(value - (1.0 / 3)) < .001 -> "1/3"
    kotlin.math.abs(value - .5) < .001 -> "1/2"
    kotlin.math.abs(value - (2.0 / 3)) < .001 -> "2/3"
    kotlin.math.abs(value - .75) < .001 -> "3/4"
    value % 1.0 == 0.0 -> value.toInt().toString()
    else -> value.toString().trimEnd('0').trimEnd('.')
}
