# Freshman’s Cookbook

A native Android recipe-first MVP built with Kotlin, Jetpack Compose, Navigation Compose, and Room.

## Run

1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Run the `app` configuration on an Android 8.0+ emulator or device.

Recipes and imported image copies are stored locally on the device. Ingredient checkbox state is intentionally session-only and resets whenever the detail screen is reopened.

## Refresh the USDA food database

Download the USDA FoodData Central Foundation Foods, SR Legacy, and FNDDS JSON releases, then run:

```powershell
node scripts/convert-usda-foundation.js `
  "C:\path\to\FoodData_Central_foundation_food_json.json" `
  "C:\path\to\FoodData_Central_sr_legacy_food_json.json" `
  "C:\path\to\surveyDownload.json"
```

The converter streams the USDA array and writes the compact runtime asset to
`app/src/main/assets/foods.json`. Foundation per-100-gram nutrients remain the
primary values. Missing core fields and household portions are filled only from
an exact-name USDA record or a reviewed FDC-ID mapping in
`scripts/usda-enrichment-matches.json`; existing Foundation values are never
overwritten. Cup/tablespoon/teaspoon variants may be scaled from a USDA
food-specific volume weight using exact US customary volume ratios. No generic
volume-to-gram density is used.

The app reapplies the generated USDA asset once per process so existing installs
receive new portions without changing saved recipes. Saved ingredients with a
null gram equivalent are resolved from those portions when opened; unresolved
amounts remain null and are shown as incomplete nutrition.
