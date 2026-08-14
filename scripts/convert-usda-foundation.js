#!/usr/bin/env node

/**
 * Convert USDA FoodData Central Foundation Foods into the compact JSON shape
 * consumed by FoodAssetSeeder on Android.
 *
 * The parser walks the FoundationFoods array one element at a time. It never
 * retains the full USDA document or the full converted catalog in memory.
 */

const fs = require("node:fs");
const path = require("node:path");
const { StringDecoder } = require("node:string_decoder");

const inputPath = process.argv[2] && path.resolve(process.argv[2]);
const enrichmentPaths = process.argv.slice(3).map((value) => path.resolve(value));
const outputPath = path.resolve(__dirname, "../app/src/main/assets/foods.json");
const matchPath = path.resolve(__dirname, "usda-enrichment-matches.json");

if (require.main === module && !inputPath) {
  console.error('Usage: node scripts/convert-usda-foundation.js "C:\\path\\to\\foundation-foods.json" ["C:\\path\\to\\sr-legacy.json" ...]');
  process.exit(1);
}
for (const enrichmentPath of require.main === module ? enrichmentPaths : []) {
  if (!fs.existsSync(enrichmentPath) || !fs.statSync(enrichmentPath).isFile()) {
    console.error(`Enrichment input file does not exist: ${enrichmentPath}`);
    process.exit(1);
  }
}
if (require.main === module && (!fs.existsSync(inputPath) || !fs.statSync(inputPath).isFile())) {
  console.error(`Input file does not exist: ${inputPath}`);
  process.exit(1);
}

/*
 * USDA nutrient ID mapping. IDs are preferred over names because USDA names
 * have changed between FoodData Central releases. Every value below is the
 * USDA amount per 100 g in the unit encoded by the destination field.
 *
 * 1003 protein (g)                 1004 total lipid/fat (g)
 * 1005 carbohydrate by difference 1050 carbohydrate by summation (g fallback)
 * 1008 energy (kcal)               2048/2047 Atwater energy (kcal fallbacks)
 * 1062 energy (kJ final fallback)  1063 total sugars (g)
 * 1079 dietary fiber (g)           2033 AOAC dietary fiber (g fallback)
 * 1087..1103 minerals              1106 vitamin A RAE (micrograms)
 * 1109 vitamin E (mg)              1114 vitamin D D2+D3 (micrograms)
 * 1110 vitamin D (IU fallback; converted using 1 IU = 0.025 micrograms)
 * 1162 vitamin C                   1165/1166/1167 B1/B2/B3 (mg)
 * 1170 B5, 1175 B6                 1177 total folate (micrograms)
 * 1190 folate DFE (when present)   1178 B12 (micrograms)
 * 1180 choline (mg)                1185 vitamin K (micrograms)
 * 1253 cholesterol (mg)            1258/1292/1293 saturated/mono/poly fat (g)
 */
const DIRECT_NUTRIENTS = new Map([
  [1003, "proteinG"],
  [1004, "fatG"],
  [1063, "totalSugarsG"],
  [1079, "fiberG"],
  [1087, "calciumMg"],
  [1089, "ironMg"],
  [1090, "magnesiumMg"],
  [1091, "phosphorusMg"],
  [1092, "potassiumMg"],
  [1093, "sodiumMg"],
  [1095, "zincMg"],
  [1098, "copperMg"],
  [1101, "manganeseMg"],
  [1103, "seleniumMcg"],
  [1106, "vitaminAMcgRae"],
  [1109, "vitaminEMg"],
  [1114, "vitaminDMcg"],
  [1162, "vitaminCMg"],
  [1165, "thiaminB1Mg"],
  [1166, "riboflavinB2Mg"],
  [1167, "niacinB3Mg"],
  [1170, "pantothenicAcidB5Mg"],
  [1175, "vitaminB6Mg"],
  [1177, "folateMcg"],
  [1190, "folateMcgDfe"],
  [1178, "vitaminB12Mcg"],
  [1180, "cholineMg"],
  [1185, "vitaminKMcg"],
  [1253, "cholesterolMg"],
  [1258, "saturatedFatG"],
  [1292, "monounsaturatedFatG"],
  [1293, "polyunsaturatedFatG"],
]);

const NUTRIENT_FIELDS = [
  "caloriesKcal", "proteinG", "carbohydrateG", "fatG", "fiberG", "totalSugarsG",
  "calciumMg", "ironMg", "magnesiumMg", "phosphorusMg", "potassiumMg", "sodiumMg",
  "zincMg", "copperMg", "manganeseMg", "seleniumMcg", "vitaminAMcgRae", "vitaminCMg",
  "vitaminDMcg", "vitaminEMg", "vitaminKMcg", "thiaminB1Mg", "riboflavinB2Mg",
  "niacinB3Mg", "pantothenicAcidB5Mg", "vitaminB6Mg", "folateMcg", "folateMcgDfe",
  "vitaminB12Mcg", "cholineMg", "saturatedFatG", "monounsaturatedFatG",
  "polyunsaturatedFatG", "cholesterolMg",
];
const CORE_NUTRIENT_FIELDS = ["caloriesKcal", "proteinG", "carbohydrateG", "fatG", "fiberG"];

const CATEGORY_MAP = new Map([
  ["Beef Products", "MEAT"],
  ["Lamb, Veal, and Game Products", "MEAT"],
  ["Pork Products", "MEAT"],
  ["Sausages and Luncheon Meats", "MEAT"],
  ["Poultry Products", "POULTRY"],
  ["Finfish and Shellfish Products", "SEAFOOD"],
  ["Dairy and Egg Products", "DAIRY"],
  ["Cereal Grains and Pasta", "GRAINS"],
  ["Legumes and Legume Products", "LEGUMES"],
  ["Vegetables and Vegetable Products", "VEGETABLES"],
  ["Fruits and Fruit Juices", "FRUIT"],
  ["Nut and Seed Products", "NUTS_SEEDS"],
  ["Fats and Oils", "OILS_FATS"],
  ["Spices and Herbs", "HERBS_SPICES"],
  ["Soups, Sauces, and Gravies", "SAUCES_CONDIMENTS"],
  ["Baked Products", "BAKING"],
  ["Sweets", "BAKING"],
  ["Beverages", "BEVERAGES"],
]);

const COUNTABLE_UNITS = new Set([
  "each", "piece", "pieces", "slice", "link", "fruit", "roast", "wedge", "egg",
  "drumstick", "fillet", "steak", "onion", "banana", "tomatoes", "spear", "olive",
  "cookie", "can",
]);

const report = {
  read: 0,
  retained: 0,
  skipped: 0,
  nutrients: 0,
  portions: 0,
  duplicates: 0,
  malformed: 0,
  invalidNutrientValues: 0,
  invalidPortions: 0,
  foodsWithPortionsBefore: 0,
  foodsWithPortionsAfter: 0,
  portionEnrichedFoods: 0,
  coreValuesEnriched: 0,
  coreMissingAfter: Object.fromEntries(CORE_NUTRIENT_FIELDS.map((field) => [field, 0])),
  exactMatches: 0,
  reviewedMatches: 0,
};
const malformedExamples = [];

function normalizeSearchName(value) {
  return value
    .normalize("NFKD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/&/g, " and ")
    .replace(/[^a-z0-9]+/g, " ")
    .trim()
    .replace(/\s+/g, " ");
}

function cleanDisplayName(value) {
  return value.replace(/\s+/g, " ").replace(/\s+,/g, ",").trim();
}

function finiteNonNegative(value) {
  return typeof value === "number" && Number.isFinite(value) && value >= 0 ? value : null;
}

function pick(values, ...ids) {
  for (const id of ids) if (values.has(id)) return values.get(id);
  return null;
}

function categoryFor(food) {
  const description = food.foodCategory?.description;
  if (description === "Dairy and Egg Products" && /(^|,|\s)egg(s)?([,\s]|$)/i.test(food.description)) return "EGGS";
  return CATEGORY_MAP.get(description) || "OTHER";
}

function singularize(value) {
  if (value === "tomatoes") return "tomato";
  if (value === "pieces") return "piece";
  return value.endsWith("s") && !value.endsWith("ss") ? value.slice(0, -1) : value;
}

function portionText(portion) {
  return String(portion.portionDescription || portion.modifier || "").trim().toLowerCase();
}

function parsedPortionUnit(portion) {
  const sourceUnit = String(portion.measureUnit?.name || portion.measureUnit?.abbreviation || "").toLowerCase().trim();
  const text = portionText(portion);
  if (sourceUnit === "cup" || /(^|\s|\d)cup(?:\s|,|$)/.test(text)) return "CUP";
  if (sourceUnit === "tablespoon" || /(^|\s|\d)(?:tbsp|tablespoon)(?:\s|,|$)/.test(text)) return "TBSP";
  if (sourceUnit === "teaspoon" || /(^|\s|\d)(?:tsp|teaspoon)(?:\s|,|$)/.test(text)) return "TSP";
  if (sourceUnit === "milliliter" || /(^|\s|\d)ml(?:\s|,|$)/.test(text)) return "ML";
  if (sourceUnit === "oz" || sourceUnit === "ounce" || /(^|\s|\d)oz(?:\s|,|$)/.test(text)) return "OZ";
  if (COUNTABLE_UNITS.has(sourceUnit) || /\b(each|piece|slice|fruit|berry|berries|nut|nuts)\b/.test(text)) return "PIECE";
  return null;
}

function portionAmount(portion) {
  const direct = finiteNonNegative(portion.amount ?? portion.value);
  if (direct && direct > 0) return direct;
  const match = portionText(portion).match(/^\s*(\d+(?:\.\d+)?)/);
  return match ? Number(match[1]) : 1;
}

function convertPortions(foodId, sourcePortions, options = {}) {
  const result = [];
  const seen = new Set();
  if (!Array.isArray(sourcePortions)) return result;

  for (const portion of sourcePortions) {
    if (!portion || typeof portion !== "object") continue;
    const sourceUnit = String(portion.measureUnit?.name || portion.measureUnit?.abbreviation || "").toLowerCase().trim();
    const sourceDescription = portionText(portion);
    if (options.portionIncludes?.length && !options.portionIncludes.some((value) => sourceDescription.includes(value))) continue;
    const amount = portionAmount(portion);
    const gramWeight = finiteNonNegative(portion.gramWeight);
    if (!amount || !gramWeight) {
      report.invalidPortions++;
      continue;
    }

    const unit = parsedPortionUnit(portion);
    if (!unit) continue; // RACC/serving/package/etc. are not app ingredient units.

    const gramsPerUnit = gramWeight / amount;
    if (!Number.isFinite(gramsPerUnit) || gramsPerUnit <= 0) continue;

    const modifier = String(portion.portionDescription || portion.modifier || "").trim().toLowerCase()
      .replace(/^\s*\d+(?:\.\d+)?\s*/, "")
      .replace(/^(cup|tbsp|tablespoon|tsp|teaspoon|oz)\b\s*,?\s*/, "");
    const canonicalLabel = unit === "CUP" ? "cup"
      : unit === "TBSP" ? "tablespoon"
      : unit === "TSP" ? "teaspoon"
      : unit === "ML" ? "milliliter"
      : unit === "OZ" ? (sourceDescription.includes("fl oz") ? "fl oz" : "oz")
      : sourceUnit === "each" ? "piece"
      : singularize(sourceUnit === "undetermined" ? "piece" : sourceUnit);
    const description = `1 ${canonicalLabel}${modifier ? `, ${modifier}` : ""}`.replace(/\s+/g, " ").trim();
    const roundedGrams = Number(gramsPerUnit.toFixed(4));
    const dedupeKey = `${unit}|${normalizeSearchName(description)}|${roundedGrams.toFixed(3)}`;
    if (seen.has(dedupeKey)) {
      report.duplicates++;
      continue;
    }
    seen.add(dedupeKey);
    result.push({ foodId, unit, description, gramsPerUnit: roundedGrams });
  }
  return options.deriveVolumeUnits === false ? result : deriveVolumePortions(foodId, result);
}

function deriveVolumePortions(foodId, portions) {
  const ratios = { CUP: 1, TBSP: 16, TSP: 48 };
  const result = portions.slice();
  const seen = new Set(result.map((portion) => `${portion.unit}|${normalizeSearchName(portion.description)}|${portion.gramsPerUnit.toFixed(3)}`));
  const groups = new Map();
  for (const portion of portions.filter((item) => item.unit in ratios)) {
    const suffix = portion.description.replace(/^1\s+(?:cup|tablespoon|teaspoon)/, "");
    const group = groups.get(suffix) || [];
    group.push(portion);
    groups.set(suffix, group);
  }
  for (const [suffix, sources] of groups) {
    for (const [unit, divisor] of Object.entries(ratios)) {
      if (sources.some((source) => source.unit === unit)) continue;
      const estimates = sources.map((source) => source.gramsPerUnit * ratios[source.unit] / divisor);
      if (Math.max(...estimates) / Math.min(...estimates) > 1.01) continue;
      const gramsPerUnit = Number((estimates.reduce((sum, value) => sum + value, 0) / estimates.length).toFixed(4));
      const label = unit === "CUP" ? "cup" : unit === "TBSP" ? "tablespoon" : "teaspoon";
      const description = `1 ${label}${suffix}`.replace(/\s+/g, " ").trim();
      const key = `${unit}|${normalizeSearchName(description)}|${gramsPerUnit.toFixed(3)}`;
      if (seen.has(key)) continue;
      seen.add(key);
      result.push({ foodId, unit, description, gramsPerUnit });
    }
  }
  return result;
}

function nutrientsFor(food) {
  const values = new Map();
  if (Array.isArray(food.foodNutrients)) {
    for (const record of food.foodNutrients) {
      const id = Number(record?.nutrient?.id);
      const amount = finiteNonNegative(record?.amount);
      if (Number.isSafeInteger(id) && amount !== null && !values.has(id)) values.set(id, amount);
    }
  }
  const nutrients = Object.fromEntries(NUTRIENT_FIELDS.map((field) => [field, null]));
  for (const [id, field] of DIRECT_NUTRIENTS) if (values.has(id)) nutrients[field] = values.get(id);
  nutrients.caloriesKcal = pick(values, 1008, 2048, 2047);
  if (nutrients.caloriesKcal === null && values.has(1062)) nutrients.caloriesKcal = Number((values.get(1062) / 4.184).toFixed(4));
  nutrients.carbohydrateG = pick(values, 1005, 1050);
  if (nutrients.fiberG === null) nutrients.fiberG = pick(values, 2033);
  if (nutrients.vitaminDMcg === null && values.has(1110)) nutrients.vitaminDMcg = Number((values.get(1110) * 0.025).toFixed(4));
  return nutrients;
}

function convertFood(food) {
  if (food === null) return { skip: "null entry" };
  if (!food || typeof food !== "object") return { error: "entry is not an object" };
  if (food.foodClass && food.foodClass !== "FinalFood") return { skip: "not a FinalFood" };

  const fdcId = Number(food.fdcId);
  const name = typeof food.description === "string" ? cleanDisplayName(food.description) : "";
  if (!Number.isSafeInteger(fdcId) || fdcId <= 0 || !name) return { error: "missing valid fdcId or description" };

  const values = new Map();
  if (Array.isArray(food.foodNutrients)) {
    for (const record of food.foodNutrients) {
      const id = Number(record?.nutrient?.id);
      const amount = finiteNonNegative(record?.amount);
      if (!Number.isSafeInteger(id)) continue;
      if (amount === null) {
        report.invalidNutrientValues++;
        continue;
      }
      // Keep the first valid value for a stable nutrient ID; duplicate research
      // records must not unpredictably overwrite the canonical amount.
      if (!values.has(id)) values.set(id, amount);
    }
  }

  const nutrients = nutrientsFor(food);

  const nutrientCount = Object.values(nutrients).filter((value) => value !== null).length;
  // A FinalFood with no supported nutrient is not useful to nutrition search.
  if (nutrientCount === 0) return { skip: "no supported nutrients" };

  const foodId = String(fdcId); // USDA fdcId is the app's stable external/canonical ID.
  const portions = convertPortions(foodId, food.foodPortions);
  return {
    food: {
      foodId,
      name,
      searchName: normalizeSearchName(name),
      category: categoryFor(food),
      ...nutrients,
      source: "USDA FoodData Central Foundation Foods",
      sourceFoodId: foodId,
      aliases: [],
      portions,
    },
    nutrientCount,
  };
}

/** Stream values inside a named root JSON array without retaining the dataset. */
async function* streamFoods(filePath, rootKeys) {
  const stream = fs.createReadStream(filePath, { highWaterMark: 256 * 1024 });
  const decoder = new StringDecoder("utf8");
  let locatedArray = false;
  let prefix = "";
  let collecting = false;
  let primitive = false;
  let buffer = "";
  let depth = 0;
  let inString = false;
  let escaped = false;

  for await (const chunk of stream) {
    let text = decoder.write(chunk);
    if (!locatedArray) {
      prefix += text;
      const matches = rootKeys.map((key) => ({ key, index: prefix.indexOf(`"${key}"`) })).filter((match) => match.index >= 0);
      const match = matches.sort((a, b) => a.index - b.index)[0];
      const keyIndex = match?.index ?? -1;
      if (keyIndex < 0) {
        prefix = prefix.slice(-64);
        continue;
      }
      const arrayIndex = prefix.indexOf("[", keyIndex);
      if (arrayIndex < 0) continue;
      text = prefix.slice(arrayIndex + 1);
      prefix = "";
      locatedArray = true;
    }

    for (let i = 0; i < text.length; i++) {
      const char = text[i];
      if (!collecting) {
        if (/\s|,/.test(char)) continue;
        if (char === "]") return;
        collecting = true;
        primitive = char !== "{";
        buffer = char;
        depth = char === "{" ? 1 : 0;
        inString = false;
        escaped = false;
        continue;
      }

      if (primitive) {
        if (char === "," || char === "]") {
          yield buffer.trim();
          collecting = false;
          buffer = "";
          if (char === "]") return;
        } else buffer += char;
        continue;
      }

      buffer += char;
      if (inString) {
        if (escaped) escaped = false;
        else if (char === "\\") escaped = true;
        else if (char === '"') inString = false;
      } else if (char === '"') inString = true;
      else if (char === "{") depth++;
      else if (char === "}" && --depth === 0) {
        yield buffer;
        collecting = false;
        buffer = "";
      }
    }
  }
  if (!locatedArray) throw new Error(`Could not find a supported root array (${rootKeys.join(", ")})`);
  if (collecting && buffer.trim()) yield buffer.trim();
}

async function buildEnrichmentIndex(paths) {
  const byId = new Map();
  const byExactName = new Map();
  for (const filePath of paths) {
    for await (const rawEntry of streamFoods(filePath, ["SRLegacyFoods", "SurveyFoods"])) {
      let food;
      try { food = JSON.parse(rawEntry); } catch { continue; }
      const fdcId = String(food.fdcId || "");
      const name = cleanDisplayName(String(food.description || ""));
      if (!fdcId || !name) continue;
      const donor = {
        fdcId,
        name,
        dataType: String(food.dataType || "USDA FoodData Central"),
        nutrients: nutrientsFor(food),
        sourcePortions: food.foodPortions,
      };
      byId.set(fdcId, donor);
      const key = normalizeSearchName(name);
      const matches = byExactName.get(key) || [];
      matches.push(donor);
      byExactName.set(key, matches);
    }
  }
  return { byId, byExactName };
}

function enrichFood(food, index, reviewedMatches) {
  const reviewed = reviewedMatches[food.foodId];
  let donor = reviewed ? index.byId.get(String(reviewed.donorFdcId)) : null;
  let matchType = "reviewed";
  if (!donor) {
    const exact = index.byExactName.get(food.searchName) || [];
    if (exact.length === 1) donor = exact[0];
    matchType = "exact-name";
  }
  if (!donor) return food;
  if (reviewed) report.reviewedMatches++; else report.exactMatches++;

  const nativePortionCount = food.portions.length;
  const donorPortions = convertPortions(food.foodId, donor.sourcePortions, {
    portionIncludes: reviewed?.portionIncludes?.map((value) => value.toLowerCase()),
  });
  if (!food.portions.length && donorPortions.length) {
    food.portions = donorPortions;
    report.portionEnrichedFoods++;
  }

  const filledCoreFields = [];
  for (const field of CORE_NUTRIENT_FIELDS) {
    if (food[field] === null && donor.nutrients[field] !== null) {
      food[field] = donor.nutrients[field];
      filledCoreFields.push(field);
      report.coreValuesEnriched++;
    }
  }
  if ((!nativePortionCount && food.portions.length) || filledCoreFields.length) {
    food.enrichment = {
      matchType,
      donorDataType: donor.dataType,
      donorFdcId: donor.fdcId,
      donorDescription: donor.name,
      portions: !nativePortionCount && food.portions.length > 0,
      coreNutrients: filledCoreFields,
    };
  }
  return food;
}

function write(stream, value) {
  return stream.write(value) ? Promise.resolve() : new Promise((resolve) => stream.once("drain", resolve));
}

function formatBytes(bytes) {
  const units = ["B", "KB", "MB", "GB"];
  let value = bytes;
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) { value /= 1024; unit++; }
  return `${value.toFixed(unit ? 2 : 0)} ${units[unit]}`;
}

async function main() {
  const reviewedMatches = fs.existsSync(matchPath) ? JSON.parse(fs.readFileSync(matchPath, "utf8")).matches : {};
  const enrichmentIndex = await buildEnrichmentIndex(enrichmentPaths);
  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  const temporaryPath = `${outputPath}.tmp`;
  const output = fs.createWriteStream(temporaryPath, { encoding: "utf8" });
  await write(output, '{"schemaVersion":2,"source":"USDA FoodData Central Foundation Foods enriched with reviewed USDA FoodData Central records","foods":[');
  let first = true;

  try {
    for await (const rawEntry of streamFoods(inputPath, ["FoundationFoods"])) {
      report.read++;
      let sourceFood;
      try {
        sourceFood = JSON.parse(rawEntry);
      } catch (error) {
        report.skipped++;
        report.malformed++;
        if (malformedExamples.length < 5) malformedExamples.push(`entry ${report.read}: invalid JSON (${error.message})`);
        continue;
      }

      let converted;
      try {
        converted = convertFood(sourceFood);
      } catch (error) {
        converted = { error: error.message };
      }
      if (!converted.food) {
        report.skipped++;
        if (converted.error) {
          report.malformed++;
          if (malformedExamples.length < 5) malformedExamples.push(`entry ${report.read}: ${converted.error}`);
        }
        continue;
      }

      if (converted.food.portions.length) report.foodsWithPortionsBefore++;
      enrichFood(converted.food, enrichmentIndex, reviewedMatches);
      if (converted.food.portions.length) report.foodsWithPortionsAfter++;
      for (const field of CORE_NUTRIENT_FIELDS) if (converted.food[field] === null) report.coreMissingAfter[field]++;
      await write(output, `${first ? "" : ","}${JSON.stringify(converted.food)}`);
      first = false;
      report.retained++;
      report.nutrients += converted.nutrientCount;
      report.portions += converted.food.portions.length;
    }
    await write(output, "]}");
    await new Promise((resolve, reject) => output.end((error) => error ? reject(error) : resolve()));
    fs.renameSync(temporaryPath, outputPath);
  } catch (error) {
    output.destroy();
    fs.rmSync(temporaryPath, { force: true });
    throw error;
  }

  const inputSize = fs.statSync(inputPath).size;
  const outputSize = fs.statSync(outputPath).size;
  console.log("\nUSDA Foundation Foods conversion complete");
  console.log(`  USDA foods read:             ${report.read}`);
  console.log(`  Foods retained:              ${report.retained}`);
  console.log(`  Foods skipped:               ${report.skipped}`);
  console.log(`  Malformed foods/values:      ${report.malformed}`);
  console.log(`  Invalid nutrient values:     ${report.invalidNutrientValues}`);
  console.log(`  Invalid source portions:     ${report.invalidPortions}`);
  console.log(`  Nutrient values retained:    ${report.nutrients}`);
  console.log(`  Portions retained:           ${report.portions}`);
  console.log(`  Duplicate portions removed:  ${report.duplicates}`);
  console.log(`  Foods with portions before:  ${report.foodsWithPortionsBefore}`);
  console.log(`  Foods with portions after:   ${report.foodsWithPortionsAfter}`);
  console.log(`  Foods still without portions:${report.retained - report.foodsWithPortionsAfter}`);
  console.log(`  Portion-enriched foods:      ${report.portionEnrichedFoods}`);
  console.log(`  Core values enriched:        ${report.coreValuesEnriched}`);
  console.log(`  Core fields missing after:   ${JSON.stringify(report.coreMissingAfter)}`);
  console.log(`  Exact/reviewed matches:      ${report.exactMatches}/${report.reviewedMatches}`);
  console.log(`  Input file size:              ${formatBytes(inputSize)} (${inputSize} bytes)`);
  console.log(`  Output file size:             ${formatBytes(outputSize)} (${outputSize} bytes)`);
  console.log(`  Output:                       ${outputPath}`);
  if (malformedExamples.length) console.log(`  Validation examples:          ${malformedExamples.join("; ")}`);
}

if (require.main === module) {
  main().catch((error) => {
    console.error(`Conversion failed: ${error.stack || error.message}`);
    process.exit(1);
  });
}

module.exports = { convertPortions, deriveVolumePortions, enrichFood, nutrientsFor, normalizeSearchName, streamFoods };
