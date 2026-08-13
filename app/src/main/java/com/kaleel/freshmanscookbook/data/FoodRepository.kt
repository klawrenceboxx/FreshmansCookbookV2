package com.kaleel.freshmanscookbook.data

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

class FoodRepository(
    private val context: Context,
    private val database: CookbookDatabase
) {
    private val dao = database.foodDao()
    private val seedMutex = Mutex()

    suspend fun ensureSeeded() = seedMutex.withLock {
        if (dao.countFoods() > 0) return@withLock
        FoodAssetSeeder.seed(context, database)
    }

    suspend fun search(query: String, limit: Int = 8): List<FoodEntity> {
        ensureSeeded()
        val normalized = normalizeFoodSearchName(query)
        return if (normalized.length < 2) emptyList() else dao.search(normalized, limit)
    }

    /** Resolve an entered ingredient amount to grams without guessing volume. */
    suspend fun gramsFor(foodId: String?, quantity: Double?, unit: IngredientUnit): Double? {
        if (quantity == null || quantity < 0) return null
        val massMultiplier = when (unit) {
            IngredientUnit.G -> 1.0
            IngredientUnit.KG -> 1_000.0
            IngredientUnit.OZ -> 28.349523125
            IngredientUnit.LB -> 453.59237
            else -> null
        }
        if (massMultiplier != null) return quantity * massMultiplier
        if (foodId == null) return null

        val lookupUnit = if (unit == IngredientUnit.L) IngredientUnit.ML else unit
        if (lookupUnit == IngredientUnit.NONE) return null
        val portions = dao.portions(foodId, lookupUnit)
        if (portions.isEmpty()) return null

        // Prefer the unmodified household measure. If USDA only provides
        // multiple size-specific weights (for example small/large), returning
        // null is safer than silently choosing one.
        val expectedDescriptions = FoodSeedData.commonUnitLabels[lookupUnit]
            .orEmpty()
            .map { normalizeFoodSearchName("1 $it") }
            .toSet()
        val exact = portions.firstOrNull {
            normalizeFoodSearchName(it.description.orEmpty()) in expectedDescriptions
        }
        val gramsPerUnit = exact?.gramsPerUnit ?: portions.singleOrNull()?.gramsPerUnit
            ?: portions.map { it.gramsPerUnit }.takeIf { values -> values.all { abs(it - values.first()) < 0.01 } }?.first()
            ?: return null
        val adjustedQuantity = if (unit == IngredientUnit.L) quantity * 1_000.0 else quantity
        return adjustedQuantity * gramsPerUnit
    }
}

fun normalizeFoodSearchName(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKD)
    .replace(Regex("[\\u0300-\\u036f]"), "")
    .lowercase(Locale.US)
    .replace("&", " and ")
    .replace(Regex("[^a-z0-9]+"), " ")
    .trim()
    .replace(Regex("\\s+"), " ")

private object FoodAssetSeeder {
    private const val TAG = "FoodAssetSeeder"
    private const val ASSET_NAME = "foods.json"

    suspend fun seed(context: Context, database: CookbookDatabase) {
        val root = context.assets.open(ASSET_NAME).bufferedReader().use { JSONObject(it.readText()) }
        val foodsJson = root.getJSONArray("foods")
        var skipped = 0

        database.withTransaction {
            val dao = database.foodDao()
            val foods = ArrayList<FoodEntity>(100)
            val aliases = ArrayList<FoodAliasEntity>(100)
            val portions = ArrayList<FoodPortionEntity>(100)

            suspend fun flush() {
                if (foods.isNotEmpty()) dao.insertFoods(foods.toList())
                if (aliases.isNotEmpty()) dao.insertAliases(aliases.toList())
                if (portions.isNotEmpty()) dao.insertPortions(portions.toList())
                foods.clear()
                aliases.clear()
                portions.clear()
            }

            for (index in 0 until foodsJson.length()) {
                try {
                    val item = foodsJson.getJSONObject(index)
                    val foodId = item.getString("foodId")
                    foods += FoodEntity(
                        foodId = foodId,
                        name = item.getString("name"),
                        searchName = item.getString("searchName"),
                        category = FoodCategory.valueOf(item.getString("category")),
                        caloriesKcal = item.nullableDouble("caloriesKcal"),
                        proteinG = item.nullableDouble("proteinG"),
                        carbohydrateG = item.nullableDouble("carbohydrateG"),
                        fatG = item.nullableDouble("fatG"),
                        fiberG = item.nullableDouble("fiberG"),
                        totalSugarsG = item.nullableDouble("totalSugarsG"),
                        calciumMg = item.nullableDouble("calciumMg"),
                        ironMg = item.nullableDouble("ironMg"),
                        magnesiumMg = item.nullableDouble("magnesiumMg"),
                        phosphorusMg = item.nullableDouble("phosphorusMg"),
                        potassiumMg = item.nullableDouble("potassiumMg"),
                        sodiumMg = item.nullableDouble("sodiumMg"),
                        zincMg = item.nullableDouble("zincMg"),
                        copperMg = item.nullableDouble("copperMg"),
                        manganeseMg = item.nullableDouble("manganeseMg"),
                        seleniumMcg = item.nullableDouble("seleniumMcg"),
                        vitaminAMcgRae = item.nullableDouble("vitaminAMcgRae"),
                        vitaminCMg = item.nullableDouble("vitaminCMg"),
                        vitaminDMcg = item.nullableDouble("vitaminDMcg"),
                        vitaminEMg = item.nullableDouble("vitaminEMg"),
                        vitaminKMcg = item.nullableDouble("vitaminKMcg"),
                        thiaminB1Mg = item.nullableDouble("thiaminB1Mg"),
                        riboflavinB2Mg = item.nullableDouble("riboflavinB2Mg"),
                        niacinB3Mg = item.nullableDouble("niacinB3Mg"),
                        pantothenicAcidB5Mg = item.nullableDouble("pantothenicAcidB5Mg"),
                        vitaminB6Mg = item.nullableDouble("vitaminB6Mg"),
                        folateMcg = item.nullableDouble("folateMcg"),
                        folateMcgDfe = item.nullableDouble("folateMcgDfe"),
                        vitaminB12Mcg = item.nullableDouble("vitaminB12Mcg"),
                        cholineMg = item.nullableDouble("cholineMg"),
                        saturatedFatG = item.nullableDouble("saturatedFatG"),
                        monounsaturatedFatG = item.nullableDouble("monounsaturatedFatG"),
                        polyunsaturatedFatG = item.nullableDouble("polyunsaturatedFatG"),
                        cholesterolMg = item.nullableDouble("cholesterolMg"),
                        source = item.getString("source"),
                        sourceFoodId = item.optString("sourceFoodId").takeIf(String::isNotBlank)
                    )

                    item.optJSONArray("aliases")?.let { values ->
                        for (aliasIndex in 0 until values.length()) {
                            val alias = normalizeFoodSearchName(values.optString(aliasIndex))
                            if (alias.isNotBlank()) aliases += FoodAliasEntity(alias = alias, foodId = foodId)
                        }
                    }
                    item.optJSONArray("portions")?.let { values ->
                        for (portionIndex in 0 until values.length()) {
                            val portion = values.getJSONObject(portionIndex)
                            val grams = portion.optDouble("gramsPerUnit", Double.NaN)
                            if (grams.isFinite() && grams > 0) portions += FoodPortionEntity(
                                foodId = foodId,
                                unit = IngredientUnit.valueOf(portion.getString("unit")),
                                description = portion.optString("description").takeIf(String::isNotBlank),
                                gramsPerUnit = grams
                            )
                        }
                    }
                    if (foods.size >= 100) flush()
                } catch (error: Exception) {
                    skipped++
                    Log.w(TAG, "Skipping malformed food at asset index $index", error)
                }
            }
            flush()

            // Reuse the existing starter catalog to add user-language aliases
            // without changing the authoritative USDA display names.
            val allFoods = dao.getAllFoods()
            val seedAliases = FoodSeedData.starterCatalog.flatMap { spec ->
                val match = spec.searchTerms.asSequence()
                    .map(::normalizeFoodSearchName)
                    .flatMap { term -> allFoods.asSequence().filter { it.searchName.contains(term) } }
                    .firstOrNull()
                    ?: return@flatMap emptyList()
                (listOf(spec.preferredName) + spec.aliases)
                    .map(::normalizeFoodSearchName)
                    .filter { it.isNotBlank() && it != match.searchName }
                    .map { FoodAliasEntity(alias = it, foodId = match.foodId) }
            }
            seedAliases.chunked(100).forEach { dao.insertAliases(it) }
        }
        Log.i(TAG, "Imported ${foodsJson.length() - skipped} USDA foods; skipped $skipped malformed asset rows")
    }

    private fun JSONObject.nullableDouble(name: String): Double? {
        if (!has(name) || isNull(name)) return null
        return optDouble(name, Double.NaN).takeIf { it.isFinite() && it >= 0 }
    }
}
