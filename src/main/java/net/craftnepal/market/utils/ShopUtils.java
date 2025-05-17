package net.craftnepal.market.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.Entities.ChestShop;
import net.craftnepal.market.Entities.EnchantedBookChestShop;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.AbstractMap;

public class ShopUtils {

    public static Map<Material, Integer> getAllShopItemsAndCountsByPlotID(String plotId) {
        Map<Material, Integer> itemCounts = new HashMap<>();
        ConfigurationSection shops = RegionData.get().getConfigurationSection("market.plots." + plotId + ".shops");

        if (shops != null) {
            for (String shopId : shops.getKeys(false)) {
                String materialName = shops.getString(shopId + ".item");
                Material material = Material.matchMaterial(materialName);

                if (material == null)
                    continue;

                Location loc = RegionData.get()
                        .getLocation("market.plots." + plotId + ".shops." + shopId + ".location");
                Block block = loc.getBlock();

                if (block.getType() == Material.BARREL) {
                    Barrel chest = (Barrel) block.getState();
                    int count = 0;

                    for (ItemStack item : chest.getInventory().getContents()) {
                        if (item != null && item.getType() == material) {
                            count += item.getAmount();
                        }
                    }

                    itemCounts.put(material, itemCounts.getOrDefault(material, 0) + count);
                }
            }
        }
        return itemCounts;
    }

    private static ChestShop createShopFromConfig(String shopId, String path) {
        Location location = RegionData.get().getLocation(path + ".location");
        String materialName = RegionData.get().getString(path + ".item");
        Material material = Material.matchMaterial(materialName);
        String ownerString = RegionData.get().getString(path + ".owner");
        UUID owner = UUID.fromString(ownerString);
        double price = RegionData.get().getDouble(path + ".price");

        if (material != null && location != null) {
            if (material == Material.ENCHANTED_BOOK) {
                String enchantKey = RegionData.get().getString(path + ".enchantment.key");
                int level = RegionData.get().getInt(path + ".enchantment.level");
                if (enchantKey != null) {
                    enchantKey = enchantKey.toLowerCase();
                    Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantKey));
                    if (enchantment != null) {
                        return new EnchantedBookChestShop(
                                shopId,
                                location,
                                material,
                                owner,
                                price,
                                new AbstractMap.SimpleEntry<>(enchantment, level));
                    }
                }
            }
            return new ChestShop(shopId, location, material, owner, price);
        }
        return null;
    }

    public static Map<String, ChestShop> getAllShops() {
        Map<String, ChestShop> shops = new HashMap<>();
        ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");

        if (plots != null) {
            for (String plotId : plots.getKeys(false)) {
                ConfigurationSection plotShops = plots.getConfigurationSection(plotId + ".shops");
                if (plotShops != null) {
                    for (String shopId : plotShops.getKeys(false)) {
                        String path = "market.plots." + plotId + ".shops." + shopId;
                        ChestShop shop = createShopFromConfig(shopId, path);
                        if (shop != null) {
                            shops.put(shopId, shop);
                        }
                    }
                }
            }
        }
        return shops;
    }

    public static List<ChestShop> getPlotShopsByItemName(String plotId, String itemName) {
        List<ChestShop> matchingShops = new ArrayList<>();
        ConfigurationSection shops = RegionData.get().getConfigurationSection("market.plots." + plotId + ".shops");

        if (shops != null) {
            for (String shopId : shops.getKeys(false)) {
                String path = "market.plots." + plotId + ".shops." + shopId;
                String materialName = RegionData.get().getString(path + ".item");

                // Case insensitive comparison and support for both UPPER_CASE and normal names
                if (materialName != null &&
                        (materialName.equalsIgnoreCase(itemName) ||
                                materialName.replace("_", " ").equalsIgnoreCase(itemName))) {

                    ChestShop shop = createShopFromConfig(shopId, path);
                    if (shop != null) {
                        matchingShops.add(shop);
                    }
                }
            }
        }
        return matchingShops;
    }

    public static List<ChestShop> getAllShopsByItemName(String itemName) {
        List<ChestShop> matchingShops = new ArrayList<>();
        ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");

        if (plots != null) {
            for (String plotId : plots.getKeys(false)) {
                matchingShops.addAll(getPlotShopsByItemName(plotId, itemName));
            }
        }
        return matchingShops;
    }
}