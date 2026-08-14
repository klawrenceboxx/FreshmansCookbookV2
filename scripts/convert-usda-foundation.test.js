const test = require("node:test");
const assert = require("node:assert/strict");
const path = require("node:path");

const { convertPortions, displayNameFor, enrichFood } = require("./convert-usda-foundation");

test("generates conservative reviewed and category-safe display names", () => {
  assert.equal(displayNameFor("2346394", "Nuts, walnuts, English, halves, raw", "NUTS_SEEDS"), "Walnuts");
  assert.equal(displayNameFor("2515378", "Nuts, macadamia nuts, raw", "NUTS_SEEDS"), "Macadamia nuts");
  assert.equal(displayNameFor("2262075", "Flaxseed, ground", "NUTS_SEEDS"), "Ground flaxseed");
  assert.equal(displayNameFor("746778", "Milk, reduced fat, fluid, 2% milkfat", "DAIRY"), "2% milk");
  assert.equal(displayNameFor("2727588", "Juice, pomegranate, from concentrate", "FRUIT"), "Pomegranate juice");
  assert.equal(displayNameFor("other", "Ambiguous, food, description", "OTHER"), "Ambiguous, food, description");
});

test("retains native USDA tablespoon and teaspoon portions", () => {
  const portions = convertPortions("food", [
    { amount: 1, gramWeight: 7, measureUnit: { name: "tablespoon" }, modifier: "ground" },
    { amount: 1, gramWeight: 2.5, measureUnit: { name: "teaspoon" }, modifier: "ground" },
  ]);
  assert.equal(portions.find((item) => item.unit === "TBSP").gramsPerUnit, 7);
  assert.equal(portions.find((item) => item.unit === "TSP").gramsPerUnit, 2.5);
});

test("derives household volume units only from a USDA food-specific cup weight", () => {
  const portions = convertPortions("food", [
    { amount: 1, gramWeight: 148, measureUnit: { name: "cup" } },
  ]);
  assert.equal(portions.find((item) => item.unit === "CUP").gramsPerUnit, 148);
  assert.equal(portions.find((item) => item.unit === "TBSP").gramsPerUnit, 9.25);
  assert.equal(portions.find((item) => item.unit === "TSP").gramsPerUnit, 3.0833);
});

test("parses SR Legacy portions whose household unit is in the modifier", () => {
  const portions = convertPortions("food", [
    { amount: 1, gramWeight: 7, measureUnit: { name: "undetermined" }, modifier: "tbsp, ground" },
  ]);
  assert.deepEqual(portions.find((item) => item.unit === "TBSP"), {
    foodId: "food", unit: "TBSP", description: "1 tablespoon, ground", gramsPerUnit: 7,
  });
});

test("fills only missing core nutrients and leaves unresolved foods untouched", () => {
  const base = {
    foodId: "foundation", searchName: "same food", caloriesKcal: null,
    proteinG: 3, carbohydrateG: null, fatG: 2, fiberG: null, portions: [],
  };
  const donor = {
    fdcId: "legacy", name: "Same food", dataType: "SR Legacy",
    nutrients: { caloriesKcal: 100, proteinG: 99, carbohydrateG: 20, fatG: 99, fiberG: 4 },
    sourcePortions: [{ amount: 1, gramWeight: 80, measureUnit: { name: "cup" } }],
  };
  const enriched = enrichFood({ ...base }, {
    byId: new Map(), byExactName: new Map([["same food", [donor]]]),
  }, {});
  assert.deepEqual(
    [enriched.caloriesKcal, enriched.proteinG, enriched.carbohydrateG, enriched.fatG, enriched.fiberG],
    [100, 3, 20, 2, 4]
  );
  assert.equal(enriched.portions.find((item) => item.unit === "CUP").gramsPerUnit, 80);

  const unresolved = enrichFood({ ...base, searchName: "different food" }, {
    byId: new Map(), byExactName: new Map(),
  }, {});
  assert.equal(unresolved.caloriesKcal, null);
  assert.deepEqual(unresolved.portions, []);
});

test("generated asset contains reviewed Nutty Pudding enrichments", () => {
  const asset = require(path.resolve(__dirname, "../app/src/main/assets/foods.json"));
  assert.equal(asset.schemaVersion, 3);
  const byId = new Map(asset.foods.map((food) => [food.foodId, food]));
  for (const id of ["2346394", "2515378", "2262075", "2710819", "2346411", "2727588"]) {
    assert.ok(byId.get(id).portions.length > 0, `${byId.get(id).name} should have portions`);
  }
  assert.equal(byId.get("2727588").caloriesKcal, 54);
  assert.equal(byId.get("2727588").carbohydrateG, 13.1);
  assert.equal(byId.get("2346394").displayName, "Walnuts");
  assert.equal(byId.get("2515378").displayName, "Macadamia nuts");
  assert.equal(byId.get("2262075").displayName, "Ground flaxseed");
  assert.equal(byId.get("746778").displayName, "2% milk");
  assert.equal(byId.get("2727588").displayName, "Pomegranate juice");
  assert.equal(byId.get("2346411").displayName, "Blueberries");
});

test("Nutty Pudding USDA ingredients produce realistic non-zero macros", () => {
  const asset = require(path.resolve(__dirname, "../app/src/main/assets/foods.json"));
  const byId = new Map(asset.foods.map((food) => [food.foodId, food]));
  const ingredients = [
    ["2346394", 2, "TSP"], ["2515378", 3, "TBSP"], ["2262075", 1, "TSP"],
    ["2710819", 2, "TBSP"], ["2346411", 0.25, "CUP"],
    ["746778", 0.25, "CUP"], ["2727588", 0.25, "CUP"],
  ];
  const totals = { caloriesKcal: 0, proteinG: 0, carbohydrateG: 0, fatG: 0, fiberG: 0 };
  for (const [foodId, quantity, unit] of ingredients) {
    const food = byId.get(foodId);
    const matching = food.portions.filter((portion) => portion.unit === unit);
    const portion = matching.length === 1
      ? matching[0]
      : matching.find((item) => /^1 (cup|tablespoon|teaspoon)$/.test(item.description));
    assert.ok(portion, `${food.name} should resolve ${unit}`);
    const factor = quantity * portion.gramsPerUnit / 100;
    for (const field of Object.keys(totals)) totals[field] += food[field] * factor;
  }
  assert.ok(Math.abs(totals.caloriesKcal - 397.6055) < 0.001);
  assert.ok(Math.abs(totals.proteinG - 8.9037) < 0.001);
  assert.ok(Math.abs(totals.carbohydrateG - 31.9703) < 0.001);
  assert.ok(Math.abs(totals.fatG - 28.5092) < 0.001);
  assert.ok(Math.abs(totals.fiberG - 10.8683) < 0.001);
});
