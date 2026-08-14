package com.kaleel.freshmanscookbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kaleel.freshmanscookbook.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class IngredientDraft(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val quantityText: String = "",
    val unit: IngredientUnit = IngredientUnit.NONE,
    val foodId: String? = null,
    val gramsEquivalent: Double? = null,
    val foodSource: FoodSource? = null
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

data class DayBounds(val start: Long, val end: Long)

enum class MealLogState { IDLE, SAVING, SAVED, ERROR }

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CookbookViewModel(application: Application) : AndroidViewModel(application) {
    private val cookbookApplication = application as CookbookApplication
    private val repository = cookbookApplication.repository
    private val foodRepository = cookbookApplication.foodRepository
    private val mealLogRepository = cookbookApplication.mealLogRepository
    private val foodLogRepository = cookbookApplication.foodLogRepository
    private val dailyNutritionRepository = cookbookApplication.dailyNutritionRepository
    private val profileRepository = cookbookApplication.profileRepository
    val recipes = repository.recipes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _draft = MutableStateFlow(RecipeDraft())
    val draft = _draft.asStateFlow()

    val profile = profileRepository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val profileNutrition = profileRepository.nutritionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val dayBounds = MutableStateFlow(localDayBounds())
    val dailyNutrition = dayBounds.flatMapLatest { bounds ->
        dailyNutritionRepository.observeBetween(bounds.start, bounds.end)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DailyNutritionSnapshot(dayBounds.value.start, dayBounds.value.end, emptyList(), emptyList(), NutritionTotals())
    )

    private val _meal = MutableStateFlow<MealInstance?>(null)
    val meal = _meal.asStateFlow()
    private val _mealFoods = MutableStateFlow<Map<String, FoodEntity>>(emptyMap())
    val mealFoods = _mealFoods.asStateFlow()
    private val _pinnedNutrient = MutableStateFlow<NutrientKey?>(null)
    val pinnedNutrient = _pinnedNutrient.asStateFlow()
    private val _mealLogState = MutableStateFlow(MealLogState.IDLE)
    val mealLogState = _mealLogState.asStateFlow()

    val recipeNutrition = combine(_meal, _mealFoods) { currentMeal, foods ->
        currentMeal?.let { MealNutritionCalculator.plannedTotals(it, foods) } ?: NutritionTotals()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NutritionTotals())

    val unresolvedNutritionIngredientIds = _meal.map { currentMeal ->
        currentMeal?.unresolvedNutritionIngredientIds().orEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val forecast = combine(dailyNutrition, _meal, _mealFoods, profileNutrition) { day, currentMeal, foods, profileState ->
        currentMeal?.let { NutritionForecast.calculate(day.totals, it, foods, profileState?.targets) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val pinnedContributions = combine(_meal, _mealFoods, _pinnedNutrient) { currentMeal, foods, nutrient ->
        if (currentMeal == null || nutrient == null) emptyList()
        else MealNutritionCalculator.rankedContributions(currentMeal, foods, nutrient, applyConsumedServingScale = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Keep an open dashboard aligned with the device's calendar day.
        viewModelScope.launch {
            while (isActive) {
                val bounds = localDayBounds()
                dayBounds.value = bounds
                delay((bounds.end - System.currentTimeMillis()).coerceAtLeast(1_000L) + 1_000L)
            }
        }
    }

    fun startNew() { _draft.value = RecipeDraft() }

    suspend fun startEdit(id: String) {
        val recipe = repository.get(id) ?: return
        val foodsById = foodRepository.getByIds(recipe.ingredients.mapNotNull { it.foodId }).associateBy { it.foodId }
        _draft.value = RecipeDraft(
            id = recipe.id,
            name = recipe.name,
            category = recipe.category,
            servingsText = recipe.servings.toString(),
            imagePath = recipe.imagePath,
            ingredients = recipe.ingredients.map {
                IngredientDraft(
                    id = it.id,
                    name = it.name,
                    quantityText = formatQuantity(it.quantity),
                    unit = it.unit,
                    foodId = it.foodId,
                    gramsEquivalent = it.gramsEquivalent,
                    foodSource = it.foodId?.let(foodsById::get)?.foodSource
                )
            },
            steps = recipe.steps.map { StepDraft(it.id, it.text) },
            createdAt = recipe.createdAt
        )
    }

    fun updateDraft(transform: (RecipeDraft) -> RecipeDraft) { _draft.update(transform) }

    suspend fun save(): String {
        val d = draft.value
        val now = System.currentTimeMillis()
        val ingredients = mutableListOf<Ingredient>()
        d.ingredients.filter { it.name.isNotBlank() }.forEachIndexed { index, item ->
            val quantity = parseQuantity(item.quantityText)
            ingredients += Ingredient(
                id = item.id,
                name = item.name.trim(),
                quantity = quantity,
                unit = item.unit,
                order = index,
                foodId = item.foodId,
                gramsEquivalent = foodRepository.gramsFor(item.foodId, quantity, item.unit)
            )
        }
        repository.save(Recipe(
            id = d.id,
            name = d.name.trim(),
            category = requireNotNull(d.category),
            servings = d.servingsText.toIntOrNull()?.coerceAtLeast(1) ?: 2,
            imagePath = d.imagePath,
            ingredients = ingredients,
            steps = d.steps.filter { it.text.isNotBlank() }.mapIndexed { i, it -> RecipeStep(it.id, it.text.trim(), i) },
            createdAt = d.createdAt,
            updatedAt = now
        ))
        return d.id
    }

    fun delete(id: String) { viewModelScope.launch { repository.delete(id) } }

    suspend fun searchFoods(query: String): List<FoodEntity> = foodRepository.search(query)
    suspend fun searchFoodOptions(query: String): List<FoodSearchResult> = foodRepository.searchWithSources(query)
    suspend fun getCustomFood(foodId: String): CustomFoodEntity? = foodRepository.getCustomFood(foodId)
    suspend fun saveCustomFood(input: CustomFoodInput): FoodSearchResult = foodRepository.saveCustomFood(input)
    suspend fun resolveIngredientGrams(foodId: String?, quantityText: String, unit: IngredientUnit): Double? =
        foodRepository.gramsFor(foodId, parseQuantity(quantityText), unit)

    fun beginMeal(recipe: Recipe) {
        if (_meal.value?.recipeId == recipe.id) return
        _mealLogState.value = MealLogState.IDLE
        viewModelScope.launch {
            _meal.value = foodRepository.resolveMissingGrams(MealInstance.fromRecipe(recipe))
            refreshMealFoods()
        }
    }

    fun endMeal(recipeId: String) {
        if (_meal.value?.recipeId == recipeId) {
            _meal.value = null
            _mealFoods.value = emptyMap()
            _pinnedNutrient.value = null
            _mealLogState.value = MealLogState.IDLE
        }
    }

    fun toggleMealIngredient(id: String) {
        _meal.update { it?.let { meal -> MealSession.toggleChecked(meal, id) } }
        _mealLogState.value = MealLogState.IDLE
    }

    fun resetMealChecks() { _meal.update { it?.let(MealSession::resetChecked) } }
    fun checkAllMealIngredients() { _meal.update { it?.let(MealSession::checkAll) } }

    fun updateMealAmount(id: String, quantityText: String, unit: IngredientUnit) {
        val quantity = parseQuantity(quantityText)
        val ingredient = _meal.value?.ingredients?.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            val grams = foodRepository.gramsFor(ingredient.foodId, quantity, unit)
            _meal.update { it?.let { meal -> MealSession.updateAmount(meal, id, quantity, unit, grams) } }
            _mealLogState.value = MealLogState.IDLE
        }
    }

    fun quickAddIngredient(name: String, quantityText: String, unit: IngredientUnit, food: FoodEntity?) {
        val quantity = parseQuantity(quantityText)
        viewModelScope.launch {
            val grams = foodRepository.gramsFor(food?.foodId, quantity, unit)
            _meal.update { it?.let { meal -> MealSession.quickAdd(meal, name, quantity, unit, food?.foodId, grams) } }
            refreshMealFoods()
            _mealLogState.value = MealLogState.IDLE
        }
    }

    fun removeQuickAddedIngredient(id: String) {
        _meal.update { it?.let { meal -> MealSession.removeIngredient(meal, id) } }
    }

    fun setServingsConsumed(raw: String) {
        raw.toDoubleOrNull()?.takeIf { it > 0 }?.let { value ->
            _meal.update { it?.let { meal -> MealSession.setServingsConsumed(meal, value) } }
        }
    }

    fun pinNutrient(nutrient: NutrientKey?) { _pinnedNutrient.value = nutrient }

    fun logCurrentMeal() {
        val current = _meal.value ?: return
        val hasUnresolvedChecked = current.ingredients.any {
            it.isChecked && it.quantity != null && it.quantity > 0 && it.unit != IngredientUnit.NONE && it.gramsEquivalent == null
        }
        if (current.checkedIngredientIds.isEmpty() || hasUnresolvedChecked || current.servingsConsumed <= 0 || _mealLogState.value == MealLogState.SAVING) return
        viewModelScope.launch {
            _mealLogState.value = MealLogState.SAVING
            _mealLogState.value = runCatching { mealLogRepository.logMeal(current) }
                .fold(onSuccess = { refreshDay(); MealLogState.SAVED }, onFailure = { MealLogState.ERROR })
        }
    }

    suspend fun saveProfile(profile: NutritionProfile) {
        profileRepository.saveProfile(profile)
        refreshDay()
    }

    suspend fun logFood(food: FoodEntity, quantityText: String, unit: IngredientUnit): Boolean {
        val quantity = parseQuantity(quantityText) ?: return false
        val grams = foodRepository.gramsFor(food.foodId, quantity, unit) ?: return false
        return runCatching {
            foodLogRepository.logFood(FoodLogDraft(food.foodId, food.name, quantity, unit, grams))
            refreshDay()
        }.isSuccess
    }

    private suspend fun refreshMealFoods() {
        val ids = _meal.value?.ingredients?.mapNotNull { it.foodId }.orEmpty()
        _mealFoods.value = foodRepository.getByIds(ids).associateBy { it.foodId }
    }

    fun refreshDay() { dayBounds.value = localDayBounds() }

    companion object {
        fun localDayBounds(date: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): DayBounds {
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            return DayBounds(start, end)
        }
    }
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
