package net.craftnepal.market.utils;

import net.craftnepal.market.files.PriceData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PriceCalculator {

    private static final Set<Material> processing = new HashSet<>();

    /**
     * Attempts to calculate prices for all materials that have recipes but no price yet.
     * @return The number of items updated.
     */
    public static int calculateMissingPrices() {
        int count = 0;
        for (Material material : Material.values()) {
            if (material.isAir() || !material.isItem()) continue;
            
            if (PriceData.getPrice(material) == null) {
                Integer calculatedPrice = calculatePrice(material, 0);
                if (calculatedPrice != null && calculatedPrice > 0) {
                    PriceData.setPrice(material, calculatedPrice);
                    count++;
                }
            }
        }
        if (count > 0) {
            PriceData.save();
        }
        return count;
    }

    /**
     * Recursively calculates the price of a material based on its recipes.
     * @param material The material to calculate.
     * @param depth Current recursion depth to prevent infinite loops.
     * @return The calculated price, or null if it cannot be determined.
     */
    public static Integer calculatePrice(Material material, int depth) {
        if (depth > 5) return null; // Prevent deep recursion
        
        // Check if we already have a price
        Integer existingPrice = PriceData.getPrice(material);
        if (existingPrice != null) return existingPrice;

        if (processing.contains(material)) return null; // Circular dependency
        processing.add(material);

        try {
            Recipe recipe = Bukkit.getRecipesFor(new ItemStack(material)).stream().findFirst().orElse(null);
            if (recipe == null) return null;

            double totalPrice = 0;
            int resultAmount = recipe.getResult().getAmount();

            if (recipe instanceof ShapedRecipe) {
                ShapedRecipe shaped = (ShapedRecipe) recipe;
                for (ItemStack ingredient : shaped.getIngredientMap().values()) {
                    if (ingredient == null || ingredient.getType().isAir()) continue;
                    Integer ingredientPrice = calculatePrice(ingredient.getType(), depth + 1);
                    if (ingredientPrice == null) return null; // Cannot calculate if one ingredient is missing price
                    totalPrice += ingredientPrice;
                }
            } else if (recipe instanceof ShapelessRecipe) {
                ShapelessRecipe shapeless = (ShapelessRecipe) recipe;
                for (ItemStack ingredient : shapeless.getIngredientList()) {
                    if (ingredient == null || ingredient.getType().isAir()) continue;
                    Integer ingredientPrice = calculatePrice(ingredient.getType(), depth + 1);
                    if (ingredientPrice == null) return null;
                    totalPrice += ingredientPrice;
                }
            } else {
                // Handle other recipe types (Smelting, Blasting, etc.) if needed
                return null;
            }

            if (totalPrice == 0) return null;
            
            // Result price is (total ingredients / result amount)
            return (int) Math.ceil(totalPrice / resultAmount);
        } finally {
            processing.remove(material);
        }
    }
}
