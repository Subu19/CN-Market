package net.craftnepal.market.files;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PriceData {
    private static File file;
    private static FileConfiguration config;
    private static final Map<Material, Integer> priceMap = new HashMap<>();

    public static void setup() {
        file = new File(Bukkit.getServer().getPluginManager().getPlugin("Market").getDataFolder(), "price.yml");
        if (!file.exists()) {
            Bukkit.getServer().getPluginManager().getPlugin("Market").saveResource("price.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        loadPrices();
    }

    private static void loadPrices() {
        priceMap.clear();
        boolean modified = false;
        for (String key : config.getKeys(false)) {
            String originalKey = key.toUpperCase();
            Material material = null;
            
            try {
                material = Material.valueOf(originalKey);
            } catch (IllegalArgumentException e) {
                // Try to resolve legacy or common alias names
                material = resolveLegacyMaterial(originalKey);
            }

            if (material != null) {
                int price = config.getInt(key);
                priceMap.put(material, price);
                
                // If the key was legacy, update it in the config
                if (!material.name().equals(key)) {
                    config.set(key, null);
                    config.set(material.name(), price);
                    modified = true;
                }
            } else {
                Bukkit.getLogger().warning("Invalid material in prices.yml: " + key);
            }
        }
        if (modified) {
            save();
        }
    }

    private static Material resolveLegacyMaterial(String key) {
        // Handle UNWAXED_BLOCK_OF_X -> X_BLOCK
        if (key.startsWith("UNWAXED_BLOCK_OF_")) {
            String base = key.substring(17);
            try { return Material.valueOf(base + "_BLOCK"); } catch (IllegalArgumentException ignored) {}
        }

        // Handle UNWAXED_X -> X
        if (key.startsWith("UNWAXED_")) {
            String base = key.substring(8);
            try { return Material.valueOf(base); } catch (IllegalArgumentException ignored) {}
        }

        // Handle BLOCK_OF_RAW_X -> RAW_X_BLOCK
        if (key.startsWith("BLOCK_OF_RAW_")) {
            String base = key.substring(13);
            try { return Material.valueOf("RAW_" + base + "_BLOCK"); } catch (IllegalArgumentException ignored) {}
        }

        // Handle BLOCK_OF_X -> X_BLOCK
        if (key.startsWith("BLOCK_OF_")) {
            String base = key.substring(9);
            try { return Material.valueOf(base + "_BLOCK"); } catch (IllegalArgumentException ignored) {}
        }
        
        // Handle X_BOAT_WITH_CHEST -> X_CHEST_BOAT
        if (key.endsWith("_BOAT_WITH_CHEST")) {
            String base = key.substring(0, key.length() - 16);
            try { return Material.valueOf(base + "_CHEST_BOAT"); } catch (IllegalArgumentException ignored) {}
        }

        // Common manual mappings
        switch (key) {
            case "BOTTLE_O'_ENCHANTING": return Material.EXPERIENCE_BOTTLE;
            case "JACK_O'LANTERN": return Material.JACK_O_LANTERN;
            case "REDSTONE_DUST": return Material.REDSTONE;
            case "REDSTONE_REPEATER": return Material.REPEATER;
            case "REDSTONE_COMPARATOR": return Material.COMPARATOR;
            case "BOOK_AND_QUILL": return Material.WRITABLE_BOOK;
            case "EYE_OF_ENDER": return Material.ENDER_EYE;
            case "HAY_BALE": return Material.HAY_BLOCK;
            case "SKULL": return Material.SKELETON_SKULL;
            case "WEB": return Material.COBWEB;
            case "WOODEN_DOOR": return Material.OAK_DOOR;
            case "WOODEN_BUTTON": return Material.OAK_BUTTON;
            case "WOODEN_PRESSURE_PLATE": return Material.OAK_PRESSURE_PLATE;
            case "WOODEN_SLAB": return Material.OAK_SLAB;
            case "WOODEN_STAIRS": return Material.OAK_STAIRS;
            case "STEAK": return Material.COOKED_BEEF;
            case "RAW_BEEF": return Material.BEEF;
            case "RAW_CHICKEN": return Material.CHICKEN;
            case "RAW_PORKCHOP": return Material.PORKCHOP;
            case "RAW_RABBIT": return Material.RABBIT;
            case "RAW_MUTTON": return Material.MUTTON;
            case "RAW_SALMON": return Material.SALMON;
            case "RAW_COD": return Material.COD;
        }

        return null;
    }

    public static Integer getPrice(Material material) {
        return priceMap.get(material);
    }

    public static void setPrice(Material material, int price) {
        priceMap.put(material, price);
        config.set(material.name(), price);
        save();
    }

    public static void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().severe("Could not save prices.yml!");
        }
    }

    public static void reload() {
        config = YamlConfiguration.loadConfiguration(file);
        loadPrices();
    }
}
