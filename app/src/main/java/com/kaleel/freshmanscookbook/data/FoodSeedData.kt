package com.kaleel.freshmanscookbook.data

/**
 * Defines the starter-food catalog we want to populate from an authoritative
 * food-composition source (USDA FoodData Central).
 *
 * IMPORTANT:
 * This file intentionally does NOT hard-code nutrient numbers. The actual
 * nutrient records should be generated from USDA data and inserted as
 * FoodEntity rows. Keeping the catalog separate from nutrient values makes it
 * much easier to refresh the source dataset without rewriting app logic.
 */
object FoodSeedData {

    const val SOURCE_NAME = "USDA FoodData Central"

    /**
     * Human-friendly foods we want represented in the first local database.
     *
     * searchTerms are used while preparing/importing the USDA-backed dataset.
     * preferredName is what the app should normally show to the user.
     * aliases are additional autocomplete terms.
     */
    val starterCatalog: List<SeedFoodSpec> = listOf(

        // Poultry / meat
        SeedFoodSpec("Chicken breast", FoodCategory.POULTRY, listOf("chicken breast boneless skinless"), listOf("chicken", "chicken breast")),
        SeedFoodSpec("Chicken thigh", FoodCategory.POULTRY, listOf("chicken thigh meat"), listOf("chicken thighs")),
        SeedFoodSpec("Ground chicken", FoodCategory.POULTRY, listOf("ground chicken"), emptyList()),
        SeedFoodSpec("Turkey breast", FoodCategory.POULTRY, listOf("turkey breast"), listOf("turkey")),
        SeedFoodSpec("Ground turkey", FoodCategory.POULTRY, listOf("ground turkey"), emptyList()),
        SeedFoodSpec("Ground beef", FoodCategory.MEAT, listOf("ground beef"), listOf("minced beef")),
        SeedFoodSpec("Beef sirloin", FoodCategory.MEAT, listOf("beef sirloin"), listOf("sirloin steak")),
        SeedFoodSpec("Pork loin", FoodCategory.MEAT, listOf("pork loin"), emptyList()),

        // Seafood
        SeedFoodSpec("Salmon", FoodCategory.SEAFOOD, listOf("salmon raw"), listOf("salmon fillet")),
        SeedFoodSpec("Tuna", FoodCategory.SEAFOOD, listOf("tuna canned water"), listOf("canned tuna")),
        SeedFoodSpec("Sardines", FoodCategory.SEAFOOD, listOf("sardines canned"), listOf("canned sardines")),
        SeedFoodSpec("Cod", FoodCategory.SEAFOOD, listOf("cod raw"), listOf("cod fillet")),
        SeedFoodSpec("Shrimp", FoodCategory.SEAFOOD, listOf("shrimp raw"), listOf("prawns")),

        // Eggs / dairy
        SeedFoodSpec("Egg", FoodCategory.EGGS, listOf("egg whole raw"), listOf("eggs", "whole egg")),
        SeedFoodSpec("Egg white", FoodCategory.EGGS, listOf("egg white raw"), listOf("egg whites")),
        SeedFoodSpec("Whole milk", FoodCategory.DAIRY, listOf("milk whole"), listOf("milk")),
        SeedFoodSpec("Greek yogurt", FoodCategory.DAIRY, listOf("greek yogurt plain"), listOf("yogurt")),
        SeedFoodSpec("Cottage cheese", FoodCategory.DAIRY, listOf("cottage cheese"), emptyList()),
        SeedFoodSpec("Cheddar cheese", FoodCategory.DAIRY, listOf("cheddar cheese"), listOf("cheddar")),
        SeedFoodSpec("Parmesan cheese", FoodCategory.DAIRY, listOf("parmesan cheese"), listOf("parmesan")),
        SeedFoodSpec("Heavy cream", FoodCategory.DAIRY, listOf("cream heavy"), listOf("heavy whipping cream")),

        // Grains / starches
        SeedFoodSpec("White rice, cooked", FoodCategory.GRAINS, listOf("rice white cooked"), listOf("white rice")),
        SeedFoodSpec("Brown rice, cooked", FoodCategory.GRAINS, listOf("rice brown cooked"), listOf("brown rice")),
        SeedFoodSpec("Oats", FoodCategory.GRAINS, listOf("oats rolled old fashioned"), listOf("rolled oats", "oatmeal")),
        SeedFoodSpec("Quinoa, cooked", FoodCategory.GRAINS, listOf("quinoa cooked"), listOf("quinoa")),
        SeedFoodSpec("Pasta, cooked", FoodCategory.GRAINS, listOf("pasta cooked"), listOf("pasta")),
        SeedFoodSpec("Whole wheat bread", FoodCategory.GRAINS, listOf("bread whole wheat"), listOf("whole grain bread")),
        SeedFoodSpec("Potato", FoodCategory.VEGETABLES, listOf("potato raw flesh skin"), listOf("potatoes")),
        SeedFoodSpec("Sweet potato", FoodCategory.VEGETABLES, listOf("sweet potato raw"), listOf("sweet potatoes")),

        // Legumes
        SeedFoodSpec("Red lentils, cooked", FoodCategory.LEGUMES, listOf("lentils cooked"), listOf("red lentils", "lentils")),
        SeedFoodSpec("Chickpeas, cooked", FoodCategory.LEGUMES, listOf("chickpeas cooked"), listOf("garbanzo beans", "chickpeas")),
        SeedFoodSpec("Black beans, cooked", FoodCategory.LEGUMES, listOf("black beans cooked"), listOf("black beans")),
        SeedFoodSpec("Kidney beans, cooked", FoodCategory.LEGUMES, listOf("kidney beans cooked"), listOf("kidney beans")),
        SeedFoodSpec("Peas", FoodCategory.LEGUMES, listOf("peas green"), listOf("green peas")),

        // Vegetables
        SeedFoodSpec("Broccoli", FoodCategory.VEGETABLES, listOf("broccoli raw"), emptyList()),
        SeedFoodSpec("Kale", FoodCategory.VEGETABLES, listOf("kale raw"), emptyList()),
        SeedFoodSpec("Spinach", FoodCategory.VEGETABLES, listOf("spinach raw"), emptyList()),
        SeedFoodSpec("Carrot", FoodCategory.VEGETABLES, listOf("carrots raw"), listOf("carrots")),
        SeedFoodSpec("Bell pepper", FoodCategory.VEGETABLES, listOf("pepper sweet raw"), listOf("bell peppers")),
        SeedFoodSpec("Onion", FoodCategory.VEGETABLES, listOf("onion raw"), listOf("onions")),
        SeedFoodSpec("Garlic", FoodCategory.VEGETABLES, listOf("garlic raw"), emptyList()),
        SeedFoodSpec("Tomato", FoodCategory.VEGETABLES, listOf("tomato raw"), listOf("tomatoes")),
        SeedFoodSpec("Cucumber", FoodCategory.VEGETABLES, listOf("cucumber with peel raw"), emptyList()),
        SeedFoodSpec("Cauliflower", FoodCategory.VEGETABLES, listOf("cauliflower raw"), emptyList()),
        SeedFoodSpec("Cabbage", FoodCategory.VEGETABLES, listOf("cabbage green raw"), emptyList()),
        SeedFoodSpec("Mushrooms", FoodCategory.VEGETABLES, listOf("mushrooms white raw"), listOf("mushroom")),

        // Fruit
        SeedFoodSpec("Banana", FoodCategory.FRUIT, listOf("banana raw"), listOf("bananas")),
        SeedFoodSpec("Apple", FoodCategory.FRUIT, listOf("apple raw with skin"), listOf("apples")),
        SeedFoodSpec("Orange", FoodCategory.FRUIT, listOf("orange raw"), listOf("oranges")),
        SeedFoodSpec("Blueberries", FoodCategory.FRUIT, listOf("blueberries raw"), listOf("blueberry")),
        SeedFoodSpec("Strawberries", FoodCategory.FRUIT, listOf("strawberries raw"), listOf("strawberry")),
        SeedFoodSpec("Raspberries", FoodCategory.FRUIT, listOf("raspberries raw"), listOf("raspberry")),
        SeedFoodSpec("Avocado", FoodCategory.FRUIT, listOf("avocado raw"), listOf("avocados")),

        // Nuts / seeds
        SeedFoodSpec("Almonds", FoodCategory.NUTS_SEEDS, listOf("almonds raw"), listOf("almond")),
        SeedFoodSpec("Walnuts", FoodCategory.NUTS_SEEDS, listOf("walnuts raw"), listOf("walnut")),
        SeedFoodSpec("Macadamia nuts", FoodCategory.NUTS_SEEDS, listOf("macadamia nuts raw"), listOf("macadamia")),
        SeedFoodSpec("Peanuts", FoodCategory.NUTS_SEEDS, listOf("peanuts raw"), listOf("peanut")),
        SeedFoodSpec("Peanut butter", FoodCategory.NUTS_SEEDS, listOf("peanut butter smooth"), emptyList()),
        SeedFoodSpec("Chia seeds", FoodCategory.NUTS_SEEDS, listOf("chia seeds dried"), listOf("chia")),
        SeedFoodSpec("Flaxseed", FoodCategory.NUTS_SEEDS, listOf("flaxseed"), listOf("flax seeds")),
        SeedFoodSpec("Pumpkin seeds", FoodCategory.NUTS_SEEDS, listOf("pumpkin seeds"), listOf("pepitas")),
        SeedFoodSpec("Sunflower seeds", FoodCategory.NUTS_SEEDS, listOf("sunflower seed kernels"), emptyList()),

        // Oils / fats
        SeedFoodSpec("Olive oil", FoodCategory.OILS_FATS, listOf("olive oil"), listOf("extra virgin olive oil", "EVOO")),
        SeedFoodSpec("Avocado oil", FoodCategory.OILS_FATS, listOf("avocado oil"), emptyList()),
        SeedFoodSpec("Coconut oil", FoodCategory.OILS_FATS, listOf("coconut oil"), emptyList()),
        SeedFoodSpec("Butter", FoodCategory.OILS_FATS, listOf("butter salted"), listOf("salted butter")),

        // Common cooking / baking
        SeedFoodSpec("Coconut milk", FoodCategory.BAKING, listOf("coconut milk canned"), listOf("canned coconut milk")),
        SeedFoodSpec("Coconut cream", FoodCategory.BAKING, listOf("coconut cream"), emptyList()),
        SeedFoodSpec("Almond flour", FoodCategory.BAKING, listOf("almond flour"), emptyList()),
        SeedFoodSpec("Cocoa powder", FoodCategory.BAKING, listOf("cocoa powder unsweetened"), listOf("cacao powder")),
        SeedFoodSpec("Honey", FoodCategory.BAKING, listOf("honey"), emptyList()),
        SeedFoodSpec("Maple syrup", FoodCategory.BAKING, listOf("maple syrup"), emptyList()),

        // Herbs / spices / condiments
        SeedFoodSpec("Salt", FoodCategory.HERBS_SPICES, listOf("salt table"), listOf("table salt")),
        SeedFoodSpec("Black pepper", FoodCategory.HERBS_SPICES, listOf("pepper black"), emptyList()),
        SeedFoodSpec("Garlic powder", FoodCategory.HERBS_SPICES, listOf("garlic powder"), emptyList()),
        SeedFoodSpec("Onion powder", FoodCategory.HERBS_SPICES, listOf("onion powder"), emptyList()),
        SeedFoodSpec("Paprika", FoodCategory.HERBS_SPICES, listOf("paprika"), emptyList()),
        SeedFoodSpec("Cumin", FoodCategory.HERBS_SPICES, listOf("cumin seed"), emptyList()),
        SeedFoodSpec("Turmeric", FoodCategory.HERBS_SPICES, listOf("turmeric ground"), emptyList()),
        SeedFoodSpec("Cinnamon", FoodCategory.HERBS_SPICES, listOf("cinnamon ground"), emptyList()),
        SeedFoodSpec("Soy sauce", FoodCategory.SAUCES_CONDIMENTS, listOf("soy sauce"), emptyList()),
        SeedFoodSpec("Tomato sauce", FoodCategory.SAUCES_CONDIMENTS, listOf("tomato sauce canned"), listOf("pasta sauce")),
        SeedFoodSpec("Mustard", FoodCategory.SAUCES_CONDIMENTS, listOf("mustard prepared yellow"), listOf("yellow mustard"))
    )

    /**
     * Portion conversions that are sufficiently generic to define up front.
     *
     * Food-specific household weights (e.g. "1 medium banana") should come
     * from the source dataset rather than being guessed here.
     */
    val commonUnitLabels: Map<IngredientUnit, List<String>> = mapOf(
        IngredientUnit.G to listOf("g", "gram", "grams"),
        IngredientUnit.KG to listOf("kg", "kilogram", "kilograms"),
        IngredientUnit.ML to listOf("ml", "milliliter", "milliliters"),
        IngredientUnit.L to listOf("l", "liter", "liters"),
        IngredientUnit.TSP to listOf("tsp", "teaspoon", "teaspoons"),
        IngredientUnit.TBSP to listOf("tbsp", "tablespoon", "tablespoons"),
        IngredientUnit.CUP to listOf("cup", "cups"),
        IngredientUnit.OZ to listOf("oz", "ounce", "ounces"),
        IngredientUnit.LB to listOf("lb", "pound", "pounds"),
        IngredientUnit.PIECE to listOf("piece", "pieces")
    )
}

data class SeedFoodSpec(
    val preferredName: String,
    val category: FoodCategory,
    val searchTerms: List<String>,
    val aliases: List<String>
)
