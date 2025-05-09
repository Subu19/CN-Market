package net.craftnepal.market.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;
import net.craftnepal.market.files.RegionData;

import java.util.HashMap;
import java.util.Map;

public class ShopUtils {

    public static Map<Material, Integer> getAllShopItemsAndCountsByPlotID(String plotId) {
        Map<Material, Integer> itemCounts = new HashMap<>();
        ConfigurationSection shops = RegionData.get().getConfigurationSection("market.plots." + plotId + ".shops");

        if (shops != null) {
            for (String shopId : shops.getKeys(false)) {
                String materialName = shops.getString(shopId + ".item");
                Material material = Material.matchMaterial(materialName);

                if (material == null) continue;

                Location loc = RegionData.get().getLocation("market.plots." + plotId + ".shops."+shopId+".location");
                Block block = loc.getBlock();

                if (block.getType() == Material.CHEST) {
                    Chest chest = (Chest) block.getState();
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
}