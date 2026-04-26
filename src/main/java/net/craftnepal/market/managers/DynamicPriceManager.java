package net.craftnepal.market.managers;

import net.craftnepal.market.Market;
import net.craftnepal.market.Entities.ChestShop;
import net.craftnepal.market.files.PriceData;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.DisplayUtils;
import net.craftnepal.market.utils.LocationUtils;
import net.craftnepal.market.utils.ShopUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DynamicPriceManager {

    private static File dynamicPricesFile;
    private static FileConfiguration dynamicPricesConfig;

    private static File metricsFile;
    private static FileConfiguration metricsConfig;

    private static final long UPDATE_INTERVAL_MS = 24L * 60L * 60L * 1000L; // 24 hours

    public static void setup() {
        dynamicPricesFile = new File(Market.getPlugin().getDataFolder(), "dynamic_prices.yml");
        if (!dynamicPricesFile.exists()) {
            try {
                dynamicPricesFile.createNewFile();
            } catch (IOException e) {
                Bukkit.getLogger().severe("Could not create dynamic_prices.yml");
            }
        }
        dynamicPricesConfig = YamlConfiguration.loadConfiguration(dynamicPricesFile);

        metricsFile = new File(Market.getPlugin().getDataFolder(), "market_metrics.yml");
        if (!metricsFile.exists()) {
            try {
                metricsFile.createNewFile();
            } catch (IOException e) {
                Bukkit.getLogger().severe("Could not create market_metrics.yml");
            }
        }
        metricsConfig = YamlConfiguration.loadConfiguration(metricsFile);

        // Schedule the periodic check every 5 minutes (6000 ticks)
        Bukkit.getScheduler().runTaskTimerAsynchronously(Market.getPlugin(), () -> {
            long lastUpdate = metricsConfig.getLong("last_update", 0L);
            if (System.currentTimeMillis() - lastUpdate >= UPDATE_INTERVAL_MS) {
                // Time to update!
                Bukkit.getScheduler().runTask(Market.getPlugin(), DynamicPriceManager::triggerDailyUpdate);
            }
        }, 6000L, 6000L);
    }

    public static double getDynamicPrice(Material material) {
        Integer basePrice = PriceData.getPrice(material);
        if (basePrice == null || basePrice <= 0) return 0;

        if (dynamicPricesConfig.contains(material.name() + ".price")) {
            return dynamicPricesConfig.getDouble(material.name() + ".price");
        } else if (dynamicPricesConfig.contains(material.name())) {
            // Legacy support
            return dynamicPricesConfig.getDouble(material.name());
        } else {
            // Initialize dynamic price to base price
            dynamicPricesConfig.set(material.name() + ".price", basePrice.doubleValue());
            dynamicPricesConfig.set(material.name() + ".trend", 0.0);
            saveDynamicPrices();
            return basePrice.doubleValue();
        }
    }

    public static double getTrend(Material material) {
        if (dynamicPricesConfig.contains(material.name() + ".trend")) {
            return dynamicPricesConfig.getDouble(material.name() + ".trend");
        }
        return 0.0;
    }

    public static String getTrendString(Material material) {
        double trend = getTrend(material);
        if (trend > 0.0) {
            return String.format("§a(↑ +%.1f%%)", trend * 100);
        } else if (trend < 0.0) {
            return String.format("§c(↓ %.1f%%)", trend * 100);
        }
        return "§7(-)";
    }

    public static void recordPurchase(Material material, int amount) {
        String path = "purchased_today." + material.name();
        int current = metricsConfig.getInt(path, 0);
        metricsConfig.set(path, current + amount);
        saveMetrics();
    }

    public static void triggerDailyUpdate() {
        Bukkit.getLogger().info("[Market] Running daily dynamic price update...");
        
        // 1. Gather Total Supply (Stock) across all shops
        Map<Material, Integer> totalStockMap = new HashMap<>();
        Map<String, ChestShop> allShops = ShopUtils.getAllShops();
        for (ChestShop shop : allShops.values()) {
            Material mat = shop.getItem();
            int stock = ShopUtils.getShopStock(shop);
            totalStockMap.put(mat, totalStockMap.getOrDefault(mat, 0) + stock);
        }

        ConfigurationSection purchasedSection = metricsConfig.getConfigurationSection("purchased_today");
        
        // We will process every material that has either base price or stock or purchases
        // To be safe, let's just process materials that have a base price
        boolean pricesChanged = false;
        
        for (Material material : Material.values()) {
            Integer basePrice = PriceData.getPrice(material);
            if (basePrice == null || basePrice <= 0) continue;

            int demand = 0;
            if (purchasedSection != null && purchasedSection.contains(material.name())) {
                demand = purchasedSection.getInt(material.name());
            }

            int supply = totalStockMap.getOrDefault(material, 0);

            // If there's no supply and no demand, price stays same
            if (supply == 0 && demand == 0) continue;

            double ratio = (double) demand / (supply + 1.0); // Add 1 to avoid division by zero
            
            double percentChange = 0.0;
            if (ratio >= 0.50) {
                percentChange = 0.10; // +10%
            } else if (ratio >= 0.25) {
                percentChange = 0.05; // +5%
            } else if (ratio < 0.10 && supply > 0) {
                // Only drop price if there is supply but no one is buying
                percentChange = -0.05; // -5%
            }

            if (percentChange != 0.0) {
                double currentPrice = getDynamicPrice(material);
                double newPrice = currentPrice * (1.0 + percentChange);

                // Clamp to [0.5 * Base, 3.0 * Base]
                double minPrice = basePrice * 0.5;
                double maxPrice = basePrice * 3.0;
                if (newPrice < minPrice) newPrice = minPrice;
                if (newPrice > maxPrice) newPrice = maxPrice;

                if (Math.abs(newPrice - currentPrice) > 0.01) {
                    double actualMultiplier = newPrice / currentPrice;
                    double trend = actualMultiplier - 1.0;
                    dynamicPricesConfig.set(material.name() + ".price", newPrice);
                    dynamicPricesConfig.set(material.name() + ".trend", trend);
                    pricesChanged = true;
                    
                    Bukkit.getLogger().info("[Market] " + material.name() + " price changed by " + String.format("%.1f%%", (actualMultiplier - 1.0) * 100) + " (New: " + newPrice + ")");
                    
                    // Auto-scale existing shops for this material
                    autoScaleShops(material, actualMultiplier);
                }
            }
        }

        if (pricesChanged) {
            saveDynamicPrices();
            RegionData.save();
            DisplayUtils.getInstance().updateAllDisplays();
            Bukkit.broadcastMessage(Market.getMainConfig().getString("prefix").replaceAll("&", "§") + "§aMarket prices have been updated based on daily supply and demand!");
        }

        // Reset metrics
        metricsConfig.set("purchased_today", null);
        metricsConfig.set("last_update", System.currentTimeMillis());
        saveMetrics();
    }

    private static void autoScaleShops(Material material, double multiplier) {
        ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");
        if (plots == null) return;

        for (String plotId : plots.getKeys(false)) {
            ConfigurationSection shops = plots.getConfigurationSection(plotId + ".shops");
            if (shops != null) {
                for (String shopId : shops.getKeys(false)) {
                    String path = plotId + ".shops." + shopId;
                    String itemStr = shops.getString(shopId + ".item");
                    if (itemStr != null && itemStr.equalsIgnoreCase(material.name())) {
                        double oldPrice = shops.getDouble(shopId + ".price");
                        double newShopPrice = oldPrice * multiplier;
                        RegionData.get().set("market.plots." + path + ".price", newShopPrice);
                    }
                }
            }
        }
    }

    private static void saveDynamicPrices() {
        try {
            dynamicPricesConfig.save(dynamicPricesFile);
        } catch (IOException e) {
            Bukkit.getLogger().severe("Could not save dynamic_prices.yml");
        }
    }

    private static void saveMetrics() {
        try {
            metricsConfig.save(metricsFile);
        } catch (IOException e) {
            Bukkit.getLogger().severe("Could not save market_metrics.yml");
        }
    }
}
