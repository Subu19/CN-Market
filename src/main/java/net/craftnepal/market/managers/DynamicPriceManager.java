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

    public static double getDynamicPrice(String itemKey) {
        Integer basePrice = PriceData.getPrice(itemKey);
        if (basePrice == null || basePrice <= 0) return 0;

        if (dynamicPricesConfig.contains(itemKey + ".price")) {
            return dynamicPricesConfig.getDouble(itemKey + ".price");
        } else if (dynamicPricesConfig.contains(itemKey)) {
            // Legacy support
            return dynamicPricesConfig.getDouble(itemKey);
        } else {
            // Initialize dynamic price to base price
            dynamicPricesConfig.set(itemKey + ".price", basePrice.doubleValue());
            dynamicPricesConfig.set(itemKey + ".trend", 0.0);
            saveDynamicPrices();
            return basePrice.doubleValue();
        }
    }

    public static double getTrend(String itemKey) {
        if (dynamicPricesConfig.contains(itemKey + ".trend")) {
            return dynamicPricesConfig.getDouble(itemKey + ".trend");
        }
        return 0.0;
    }

    public static String getTrendString(String itemKey) {
        double trend = getTrend(itemKey);
        if (trend > 0.0) {
            return String.format("§a(↑ +%.1f%%)", trend * 100);
        } else if (trend < 0.0) {
            return String.format("§c(↓ %.1f%%)", trend * 100);
        }
        return "§7(-)";
    }

    public static void recordPurchase(String itemKey, int amount) {
        String path = "purchased_today." + itemKey;
        int current = metricsConfig.getInt(path, 0);
        metricsConfig.set(path, current + amount);
        saveMetrics();
    }

    public static void triggerDailyUpdate() {
        Bukkit.getLogger().info("[Market] Running daily dynamic price update...");
        
        // 1. Gather Total Supply (Stock) across all shops
        Map<String, Integer> totalStockMap = new HashMap<>();
        Map<String, ChestShop> allShops = ShopUtils.getAllShops();
        for (ChestShop shop : allShops.values()) {
            String key = ShopUtils.getItemKey(shop);
            int stock = ShopUtils.getShopStock(shop);
            totalStockMap.put(key, totalStockMap.getOrDefault(key, 0) + stock);
        }

        ConfigurationSection purchasedSection = metricsConfig.getConfigurationSection("purchased_today");
        
        // We will process every material that has either base price or stock or purchases
        boolean pricesChanged = false;
        
        // To compute over all active keys, we take materials + any recorded stock/purchases
        java.util.Set<String> allKeys = new java.util.HashSet<>(totalStockMap.keySet());
        if (purchasedSection != null) allKeys.addAll(purchasedSection.getKeys(false));
        for (Material m : Material.values()) {
            if (m.isItem() && !m.isAir()) allKeys.add(m.name());
        }
        
        for (String itemKey : allKeys) {
            Integer basePrice = PriceData.getPrice(itemKey);
            if (basePrice == null || basePrice <= 0) continue;

            int demand = 0;
            if (purchasedSection != null && purchasedSection.contains(itemKey)) {
                demand = purchasedSection.getInt(itemKey);
            }

            int supply = totalStockMap.getOrDefault(itemKey, 0);

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
                double currentPrice = getDynamicPrice(itemKey);
                double newPrice = currentPrice * (1.0 + percentChange);

                // Clamp to [0.5 * Base, 3.0 * Base]
                double minPrice = basePrice * 0.5;
                double maxPrice = basePrice * 3.0;
                if (newPrice < minPrice) newPrice = minPrice;
                if (newPrice > maxPrice) newPrice = maxPrice;

                if (Math.abs(newPrice - currentPrice) > 0.01) {
                    double actualMultiplier = newPrice / currentPrice;
                    double trend = actualMultiplier - 1.0;
                    dynamicPricesConfig.set(itemKey + ".price", newPrice);
                    dynamicPricesConfig.set(itemKey + ".trend", trend);
                    pricesChanged = true;
                    
                    Bukkit.getLogger().info("[Market] " + itemKey + " price changed by " + String.format("%.1f%%", (actualMultiplier - 1.0) * 100) + " (New: " + newPrice + ")");
                    
                    // Auto-scale existing shops for this material
                    autoScaleShops(itemKey, actualMultiplier);
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

    private static void autoScaleShops(String itemKey, double multiplier) {
        ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");
        if (plots == null) return;

        for (String plotId : plots.getKeys(false)) {
            ConfigurationSection shops = plots.getConfigurationSection(plotId + ".shops");
            if (shops != null) {
                for (String shopId : shops.getKeys(false)) {
                    ChestShop shop = ShopUtils.getShop(plotId, shopId);
                    if (shop != null) {
                        if (ShopUtils.getItemKey(shop).equalsIgnoreCase(itemKey)) {
                            double oldPrice = shop.getPrice();
                            double newShopPrice = oldPrice * multiplier;
                            shop.setPrice(newShopPrice);
                            String path = plotId + ".shops." + shopId;
                            RegionData.get().set("market.plots." + path + ".price", newShopPrice);
                        }
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
