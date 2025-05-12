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
        for (String key : config.getKeys(false)) {
            try {
                Material material = Material.valueOf(key.toUpperCase());
                int price = config.getInt(key);
                priceMap.put(material, price);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("Invalid material in prices.yml: " + key);
            }
        }
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
