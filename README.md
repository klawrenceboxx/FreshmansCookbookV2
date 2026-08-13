# Freshman’s Cookbook

A native Android recipe-first MVP built with Kotlin, Jetpack Compose, Navigation Compose, and Room.

## Run

1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Run the `app` configuration on an Android 8.0+ emulator or device.

Recipes and imported image copies are stored locally on the device. Ingredient checkbox state is intentionally session-only and resets whenever the detail screen is reopened.

## Refresh the USDA food database

Download the FoodData Central Foundation Foods JSON release, then run:

```powershell
node scripts/convert-usda-foundation.js "C:\path\to\FoodData_Central_foundation_food_json.json"
```

The converter streams the USDA array and writes the compact runtime asset to
`app/src/main/assets/foods.json`. The app imports that asset into Room on first
launch. USDA nutrient amounts are stored per 100 grams; household conversions
are only present where the source release provides a usable gram weight.
