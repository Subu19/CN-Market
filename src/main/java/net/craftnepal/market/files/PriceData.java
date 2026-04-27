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
    private static final Map<String, Integer> priceMap = new HashMap<>();

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
            String upperKey = key.toUpperCase();
            int price = config.getInt(key);
            priceMap.put(upperKey, price);
            
            if (!key.equals(upperKey)) {
                config.set(key, null);
                config.set(upperKey, price);
                modified = true;
            }
        }
        
        // Inject base prices for generic items if missing
        if (!priceMap.containsKey("POTION")) { priceMap.put("POTION", 100); config.set("POTION", 100); modified = true; }
        if (!priceMap.containsKey("SPLASH_POTION")) { priceMap.put("SPLASH_POTION", 120); config.set("SPLASH_POTION", 120); modified = true; }
        if (!priceMap.containsKey("LINGERING_POTION")) { priceMap.put("LINGERING_POTION", 150); config.set("LINGERING_POTION", 150); modified = true; }
        if (!priceMap.containsKey("TIPPED_ARROW")) { priceMap.put("TIPPED_ARROW", 60); config.set("TIPPED_ARROW", 60); modified = true; }
        if (!priceMap.containsKey("ENCHANTED_BOOK")) { priceMap.put("ENCHANTED_BOOK", 400); config.set("ENCHANTED_BOOK", 400); modified = true; }

        if (modified) {
            save();
        }
    }

    public static Integer getPrice(String key) {
        if (key == null) return null;
        return priceMap.get(key.toUpperCase());
    }

    public static void setPrice(String key, int price) {
        if (key == null) return;
        String upper = key.toUpperCase();
        priceMap.put(upper, price);
        config.set(upper, price);
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
