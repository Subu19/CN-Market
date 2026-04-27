package net.craftnepal.market.files;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PriceData {
    private static File file;
    private static FileConfiguration config;
    private static final Map<String, Integer> priceMap = new HashMap<>();

    public static void setup() {
        file = new File(Bukkit.getServer().getPluginManager().getPlugin("Market").getDataFolder(),
                "price.yml");
        if (!file.exists()) {
            Bukkit.getServer().getPluginManager().getPlugin("Market").saveResource("price.yml",
                    false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        loadPrices();
    }

    private static void loadPrices() {
        priceMap.clear();

        // Use getKeys(true) to handle nested structures correctly (e.g., POTION:SPEED)
        // Bukkit's YamlConfiguration may interpret colons as nested sections depending on
        // formatting.
        for (String key : config.getKeys(true)) {
            if (config.isConfigurationSection(key))
                continue;

            int price = config.getInt(key);
            // Replace internal Bukkit separators (.) with colons (:) to match our internal key
            // format
            String normalizedKey = normalizeKey(key.replace(".", ":"));
            priceMap.put(normalizedKey, price);
        }
    }

    private static String normalizeKey(String key) {
        if (key == null)
            return null;
        String[] parts = key.toUpperCase().split(":");
        for (int i = 0; i < parts.length; i++) {
            switch (parts[i]) {
                case "SWIFTNESS":
                    parts[i] = "SPEED";
                    break;
                case "LEAPING":
                    parts[i] = "JUMP_BOOST";
                    break;
                case "HEALING":
                    parts[i] = "INSTANT_HEALTH";
                    break;
                case "HARMING":
                    parts[i] = "INSTANT_DAMAGE";
                    break;
                case "REGEN":
                    parts[i] = "REGENERATION";
                    break;
                case "ENANTED_BOOK":
                    parts[i] = "ENCHANTED_BOOK";
                    break; // Fix typo found in price.yml
                case "SLOW":
                    parts[i] = "SLOWNESS";
                    break;
                case "STRENGTH_POTION":
                    parts[i] = "STRENGTH";
                    break;
            }
        }
        return String.join(":", parts);
    }

    public static Integer getPrice(String key) {
        if (key == null)
            return null;
        return priceMap.get(normalizeKey(key));
    }

    public static void setPrice(String key, int price) {
        if (key == null)
            return;
        String normalized = normalizeKey(key);
        priceMap.put(normalized, price);
        config.set(normalized.replace(":", "."), price); // Save as nested in YAML for better
                                                         // readability
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
