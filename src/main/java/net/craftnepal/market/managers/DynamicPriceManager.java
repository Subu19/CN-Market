package net.craftnepal.market.managers;

import net.craftnepal.market.Market;
import net.craftnepal.market.Entities.ChestShop;
import net.craftnepal.market.files.PriceData;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
            // Check if the admin has changed the base price in price.yml since
            // this entry was last seeded. We store the base that was used to seed
            // the dynamic price as a ".base" field. If it differs from the current
            // PriceData value, the admin edited price.yml — reset the dynamic price.
            int storedBase = dynamicPricesConfig.getInt(itemKey + ".base", -1);
            if (storedBase == -1) {
                // Migration: Existing config has .price but no .base. 
                // Set .base to current basePrice to track future changes, but do NOT reset the economy.
                dynamicPricesConfig.set(itemKey + ".base", basePrice);
                saveDynamicPrices();
            } else if (storedBase != basePrice) {
                dynamicPricesConfig.set(itemKey + ".price", basePrice.doubleValue());
                dynamicPricesConfig.set(itemKey + ".trend", 0.0);
                dynamicPricesConfig.set(itemKey + ".base", basePrice);
                saveDynamicPrices();
                return basePrice.doubleValue();
            }

            return dynamicPricesConfig.getDouble(itemKey + ".price");

        } else if (dynamicPricesConfig.contains(itemKey)) {
            // Legacy support — migrate to new format
            double legacyPrice = dynamicPricesConfig.getDouble(itemKey);
            dynamicPricesConfig.set(itemKey, null);
            dynamicPricesConfig.set(itemKey + ".price", legacyPrice);
            dynamicPricesConfig.set(itemKey + ".trend", 0.0);
            dynamicPricesConfig.set(itemKey + ".base", basePrice);
            saveDynamicPrices();
            return legacyPrice;

        } else {
            // First time we see this item — seed with base price
            dynamicPricesConfig.set(itemKey + ".price", basePrice.doubleValue());
            dynamicPricesConfig.set(itemKey + ".trend", 0.0);
            dynamicPricesConfig.set(itemKey + ".base", basePrice);
            saveDynamicPrices();
            return basePrice.doubleValue();
        }
    }

    /**
     * Returns the total dynamic price for an ItemStack that may carry enchantments.
     * <p>
     * The returned value is the sum of:
     * <ul>
     *   <li>The dynamic market price of the base item (fluctuates with supply/demand).</li>
     *   <li>The static book price of every enchantment on the item at its level.</li>
     * </ul>
     * Enchanted books use the existing single-key path and are NOT double-counted.
     */
    public static double getDynamicPrice(org.bukkit.inventory.ItemStack item) {
        if (item == null) return 0;

        // Enchanted books already encode the enchant in their key — use the normal path.
        if (item.getType() == org.bukkit.Material.ENCHANTED_BOOK) {
            return getDynamicPrice(net.craftnepal.market.utils.ShopUtils.getItemKey(item));
        }

        // Use the full composite key (handles TIPPED_ARROW:SPEED, POTION:SPEED, etc.)
        String baseKey = net.craftnepal.market.utils.ShopUtils.getItemKey(item);
        double basePrice = getDynamicPrice(baseKey);
        if (basePrice <= 0) return 0;

        // Add static enchantment prices on top
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasEnchants()) {
            for (java.util.Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry
                    : meta.getEnchants().entrySet()) {
                String enchantName = entry.getKey().getKey().getKey().toUpperCase();
                int level = entry.getValue();
                basePrice += net.craftnepal.market.files.PriceData.getEnchantmentPrice(enchantName, level);
            }
        }

        return basePrice;
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

    public static void recordAdminPurchase(String itemKey, int amount) {
        String path = "admin_purchased_today." + itemKey;
        int current = metricsConfig.getInt(path, 0);
        metricsConfig.set(path, current + amount);
        saveMetrics();
    }

    public static void recordAdminSale(String itemKey, int amount) {
        String path = "admin_sold_today." + itemKey;
        int current = metricsConfig.getInt(path, 0);
        metricsConfig.set(path, current + amount);
        saveMetrics();
    }

    public static void triggerDailyUpdate() {
        Bukkit.getLogger().info("[Market] Running daily dynamic price update...");
        
        // 1. Gather Total Supply (Stock) across all shops and partition keys
        Map<String, Integer> totalStockMap = new HashMap<>();
        Map<String, ChestShop> allShops = ShopUtils.getAllShops();
        java.util.Set<String> adminShopKeys = new java.util.HashSet<>();

        for (ChestShop shop : allShops.values()) {
            String key = ShopUtils.getItemKey(shop);
            if (shop.isAdmin()) {
                adminShopKeys.add(key);
                continue; // Admin shops have infinite supply, ignore for market balance
            }
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

            double currentPrice = getDynamicPrice(itemKey);

            if (adminShopKeys.contains(itemKey)) {
                double newPrice = currentPrice;
                boolean isCurrencyExchange = false;

                if (Market.getMainConfig().getBoolean("currency-exchange.enabled", false)
                        && itemKey.equalsIgnoreCase(Market.getMainConfig().getString("currency-exchange.currency-item"))) {
                    
                    String model = Market.getMainConfig().getString("currency-exchange.pricing-model", "CPI_PEG");
                    if (model.equalsIgnoreCase("CPI_PEG")) {
                        newPrice = calculateCPIPrice(itemKey, basePrice);
                        isCurrencyExchange = true;
                    }
                }

                if (!isCurrencyExchange) {
                    // Admin Shop Net-Flow pricing model
                    int bought = metricsConfig.getInt("admin_purchased_today." + itemKey, 0);
                    int sold = metricsConfig.getInt("admin_sold_today." + itemKey, 0);
                    int netFlow = bought - sold;

                    double maxChange = Market.getMainConfig().getDouble("admin-shops.max-price-change-percent", 0.03);
                    double percentChange;
                    if (netFlow > 10) {
                        percentChange = maxChange;
                    } else if (netFlow > 0) {
                        percentChange = maxChange / 3.0;
                    } else if (netFlow < -10) {
                        percentChange = -maxChange;
                    } else if (netFlow < 0) {
                        percentChange = -maxChange / 3.0;
                    } else {
                        // Decay toward base price: moves 1% of the distance to the base price
                        percentChange = (basePrice - currentPrice) / currentPrice * 0.01;
                    }

                    if (percentChange != 0.0) {
                        newPrice = currentPrice * (1.0 + percentChange);

                        // Tight clamp: [min-price-multiplier * base, max-price-multiplier * base]
                        double minMult = Market.getMainConfig().getDouble("admin-shops.min-price-multiplier", 0.75);
                        double maxMult = Market.getMainConfig().getDouble("admin-shops.max-price-multiplier", 1.50);
                        double minPrice = basePrice * minMult;
                        double maxPrice = basePrice * maxMult;
                        
                        if (newPrice < minPrice) newPrice = minPrice;
                        if (newPrice > maxPrice) newPrice = maxPrice;
                    }
                }

                if (Math.abs(newPrice - currentPrice) > 0.01) {
                    double actualMultiplier = newPrice / currentPrice;
                    double trend = actualMultiplier - 1.0;
                    dynamicPricesConfig.set(itemKey + ".price", newPrice);
                    dynamicPricesConfig.set(itemKey + ".trend", trend);
                    pricesChanged = true;

                    Bukkit.getLogger().info("[Market] Admin item " + itemKey + " price changed by " + String.format("%.1f%%", trend * 100) + " (New: " + newPrice + ")");
                    
                    // Auto-scale existing player shops (if any exist)
                    autoScaleShops(itemKey, actualMultiplier);
                }
            } else {
                // Normal supply/demand ratio model for player shops
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
                        
                        Bukkit.getLogger().info("[Market] " + itemKey + " price changed by " + String.format("%.1f%%", trend * 100) + " (New: " + newPrice + ")");
                        
                        // Auto-scale existing shops for this material
                        autoScaleShops(itemKey, actualMultiplier);
                    }
                }
            }
        }

        if (pricesChanged) {
            saveDynamicPrices();
            DisplayUtils.getInstance().updateAllDisplays();
            Bukkit.broadcastMessage(Market.getMainConfig().getString("prefix").replaceAll("&", "§") + "§aMarket prices have been updated based on daily supply and demand!");
        }

        // Reset metrics
        metricsConfig.set("purchased_today", null);
        metricsConfig.set("admin_purchased_today", null);
        metricsConfig.set("admin_sold_today", null);
        metricsConfig.set("last_update", System.currentTimeMillis());
        saveMetrics();
    }

    private static void autoScaleShops(String itemKey, double multiplier) {
        for (ChestShop shop : DatabaseManager.getAllShops()) {
            if (ShopUtils.getItemKey(shop).equalsIgnoreCase(itemKey)) {
                double oldPrice = shop.getPrice();
                double newShopPrice = oldPrice * multiplier;
                shop.setPrice(newShopPrice);
                DatabaseManager.updateShopPrice(shop.getId(), newShopPrice);
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

    private static double calculateCPIPrice(String itemKey, double basePrice) {
        FileConfiguration mainConfig = Market.getMainConfig();
        if (mainConfig == null) return basePrice;

        List<String> basketItems = mainConfig.getStringList("currency-exchange.cpi-settings.basket-items");
        if (basketItems == null || basketItems.isEmpty()) {
            return basePrice;
        }

        double baselineIndex = 0;
        double currentIndex = 0;

        // Fetch all active shops
        Map<String, ChestShop> allShops = ShopUtils.getAllShops();

        for (String basketItem : basketItems) {
            Integer itemBase = PriceData.getPrice(basketItem);
            if (itemBase == null || itemBase <= 0) continue;

            baselineIndex += itemBase;

            // Get median price in player shops with stock > 0
            List<Double> prices = new ArrayList<>();
            for (ChestShop shop : allShops.values()) {
                if (!shop.isAdmin() && !shop.isBuyingShop() && ShopUtils.getItemKey(shop).equalsIgnoreCase(basketItem)) {
                    int stock = ShopUtils.getShopStock(shop);
                    if (stock > 0) {
                        prices.add(shop.getPrice());
                    }
                }
            }

            double itemMarketPrice;
            if (prices.isEmpty()) {
                // Fallback to current dynamic price of the item if no player shops exist
                itemMarketPrice = getDynamicPrice(basketItem);
                if (itemMarketPrice <= 0) {
                    itemMarketPrice = itemBase;
                }
            } else {
                // Compute median to avoid manipulation by outlier shops
                Collections.sort(prices);
                int size = prices.size();
                if (size % 2 == 0) {
                    itemMarketPrice = (prices.get(size / 2 - 1) + prices.get(size / 2)) / 2.0;
                } else {
                    itemMarketPrice = prices.get(size / 2);
                }
            }

            currentIndex += itemMarketPrice;
        }

        if (baselineIndex <= 0) return basePrice;

        double cpiRatio = currentIndex / baselineIndex;
        double newPrice = basePrice * cpiRatio;

        // Clamping based on currency-exchange multipliers, falling back to admin-shops
        double minMult = mainConfig.getDouble("currency-exchange.min-price-multiplier", 
                mainConfig.getDouble("admin-shops.min-price-multiplier", 0.75));
        double maxMult = mainConfig.getDouble("currency-exchange.max-price-multiplier", 
                mainConfig.getDouble("admin-shops.max-price-multiplier", 1.50));
        
        double minPrice = basePrice * minMult;
        double maxPrice = basePrice * maxMult;

        if (newPrice < minPrice) newPrice = minPrice;
        if (newPrice > maxPrice) newPrice = maxPrice;

        return newPrice;
    }

    /**
     * Reloads both dynamic_prices.yml and market_metrics.yml from disk.
     * Call this after an admin manually edits either file.
     */
    public static void reload() {
        if (dynamicPricesFile != null) {
            dynamicPricesConfig = YamlConfiguration.loadConfiguration(dynamicPricesFile);
        }
        if (metricsFile != null) {
            metricsConfig = YamlConfiguration.loadConfiguration(metricsFile);
        }
    }
}
