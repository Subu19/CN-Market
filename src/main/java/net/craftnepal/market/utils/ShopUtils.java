package net.craftnepal.market.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
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
        ConfigurationSection shops =
                RegionData.get().getConfigurationSection("market.plots." + plotId + ".shops");

        if (shops != null) {
            for (String shopId : shops.getKeys(false)) {
                String materialName = shops.getString(shopId + ".item");
                Material material = Material.matchMaterial(materialName);

                if (material == null)
                    continue;

                Location loc = LocationUtils.loadLocation(RegionData.get(),
                        "market.plots." + plotId + ".shops." + shopId + ".location");
                if (loc == null)
                    continue;

                Block block = loc.getBlock();

                if (block.getType() == Material.BARREL) {
                    Barrel chest = (Barrel) block.getState();
                    int count = 0;

                    // Create a dummy shop to use isMatchingItem
                    ChestShop tempShop = createShopFromConfig(shopId,
                            "market.plots." + plotId + ".shops." + shopId);

                    for (ItemStack item : chest.getInventory().getContents()) {
                        if (item != null && isMatchingItem(tempShop, item)) {
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
        Location location = LocationUtils.loadLocation(RegionData.get(), path + ".location");
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
                    Enchantment enchantment =
                            Enchantment.getByKey(NamespacedKey.minecraft(enchantKey));
                    if (enchantment != null) {
                        return new EnchantedBookChestShop(shopId, location, material, owner, price,
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
        ConfigurationSection shops =
                RegionData.get().getConfigurationSection("market.plots." + plotId + ".shops");

        if (shops != null) {
            for (String shopId : shops.getKeys(false)) {
                String path = "market.plots." + plotId + ".shops." + shopId;
                String materialName = RegionData.get().getString(path + ".item");

                // Case insensitive comparison and support for both UPPER_CASE and normal names
                if (materialName != null && (materialName.equalsIgnoreCase(itemName)
                        || materialName.replace("_", " ").equalsIgnoreCase(itemName))) {

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

    public static ChestShop getShopAt(Location location) {
        String plotId = PlotUtils.getPlotIdByLocation(location);
        if (plotId == null)
            return null;

        ConfigurationSection shops =
                RegionData.get().getConfigurationSection("market.plots." + plotId + ".shops");
        if (shops == null)
            return null;

        for (String shopId : shops.getKeys(false)) {
            Location shopLoc = LocationUtils.loadLocation(RegionData.get(),
                    "market.plots." + plotId + ".shops." + shopId + ".location");
            if (shopLoc != null && shopLoc.equals(location)) {
                String path = "market.plots." + plotId + ".shops." + shopId;
                return createShopFromConfig(shopId, path);
            }
        }
        return null;
    }

    public static boolean isShopLocation(Location location) {
        String plotId = PlotUtils.getPlotIdByLocation(location);
        if (plotId == null)
            return false;

        ConfigurationSection shops =
                RegionData.get().getConfigurationSection("market.plots." + plotId + ".shops");
        if (shops == null)
            return false;

        for (String shopId : shops.getKeys(false)) {
            Location shopLoc = LocationUtils.loadLocation(RegionData.get(),
                    "market.plots." + plotId + ".shops." + shopId + ".location");
            if (shopLoc != null && shopLoc.equals(location)) {
                return true;
            }
        }
        return false;
    }

    public static int getShopStock(ChestShop shop) {
        Location loc = shop.getLocation();
        if (loc == null || loc.getBlock().getType() != Material.BARREL)
            return 0;

        Barrel barrel = (Barrel) loc.getBlock().getState();
        int stock = 0;
        for (ItemStack item : barrel.getInventory().getContents()) {
            if (item != null && isMatchingItem(shop, item)) {
                stock += item.getAmount();
            }
        }
        return stock;
    }

    public static boolean isMatchingItem(ChestShop shop, ItemStack item) {
        if (item == null || item.getType() != shop.getItem()) {
            return false;
        }

        if (shop instanceof EnchantedBookChestShop) {
            EnchantedBookChestShop enchantedShop = (EnchantedBookChestShop) shop;
            Map.Entry<Enchantment, Integer> targetEnchant = enchantedShop.getEnchantment();

            if (targetEnchant != null) {
                if (item.getItemMeta() instanceof EnchantmentStorageMeta) {
                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                    // Books must have EXACTLY one enchantment and it must match target
                    return meta.getStoredEnchants().size() == 1
                            && meta.getStoredEnchantLevel(targetEnchant.getKey()) == targetEnchant
                                    .getValue();
                }
                return false;
            }
        }
        return true;
    }

    public static void processPurchase(org.bukkit.entity.Player player, String plotId,
            String shopId, int amount) {
        String basePath = "market.plots." + plotId + ".shops." + shopId;

        // --- Validate shop config ---
        if (!RegionData.get().contains(basePath)) {
            SendMessage.sendPlayerMessage(player, "§cThis shop no longer exists.");
            return;
        }

        String ownerStr = RegionData.get().getString(basePath + ".owner");
        if (ownerStr == null) {
            SendMessage.sendPlayerMessage(player, "§cShop owner data is missing.");
            return;
        }

        UUID ownerUUID;
        try {
            ownerUUID = UUID.fromString(ownerStr);
        } catch (IllegalArgumentException e) {
            SendMessage.sendPlayerMessage(player, "§cShop owner data is corrupted.");
            return;
        }

        if (player.getUniqueId().equals(ownerUUID)) {
            SendMessage.sendPlayerMessage(player, "§cYou cannot buy from your own shop.");
            return;
        }

        String itemNameStr = RegionData.get().getString(basePath + ".item");
        Material itemType = Material.matchMaterial(itemNameStr != null ? itemNameStr : "");
        if (itemType == null) {
            SendMessage.sendPlayerMessage(player, "§cInvalid item type in shop data.");
            return;
        }

        double pricePerItem = RegionData.get().getDouble(basePath + ".price");
        double totalPrice = pricePerItem * amount;

        if (!EconomyUtils.hasBalance(player.getUniqueId(), totalPrice)) {
            SendMessage.sendPlayerMessage(player,
                    "§cYou do not have enough money. You need " + EconomyUtils.format(totalPrice));
            return;
        }

        // --- Validate barrel ---
        Location shopLoc = LocationUtils.loadLocation(RegionData.get(), basePath + ".location");
        if (shopLoc == null || shopLoc.getBlock().getType() != Material.BARREL) {
            SendMessage.sendPlayerMessage(player, "§cShop barrel is missing or corrupted.");
            return;
        }

        ChestShop shop = getShopAt(shopLoc);
        if (shop == null) {
            SendMessage.sendPlayerMessage(player, "§cShop data not found.");
            return;
        }

        // Always re-fetch block state fresh to get live inventory
        org.bukkit.block.BlockState blockState = shopLoc.getBlock().getState();
        if (!(blockState instanceof Barrel)) {
            SendMessage.sendPlayerMessage(player, "§cShop barrel is missing or corrupted.");
            return;
        }
        Barrel barrel = (Barrel) blockState;
        org.bukkit.inventory.Inventory barrelInv = barrel.getInventory();

        // --- Count matching stock ---
        int stock = 0;
        for (ItemStack item : barrelInv.getContents()) {
            if (item != null && isMatchingItem(shop, item)) {
                stock += item.getAmount();
            }
        }

        if (stock < amount) {
            SendMessage.sendPlayerMessage(player,
                    "§cNot enough stock! Only " + stock + " available.");
            return;
        }

        // --- Check player inventory space ---
        int freeSpace = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType() == Material.AIR) {
                freeSpace += itemType.getMaxStackSize();
            } else if (item.getType() == itemType && item.getAmount() < item.getMaxStackSize()) {
                freeSpace += (item.getMaxStackSize() - item.getAmount());
            }
        }

        if (freeSpace < amount) {
            SendMessage.sendPlayerMessage(player, "§cYou do not have enough inventory space.");
            return;
        }

        // =========================================================
        // STEP 1: Remove items from barrel FIRST.
        // The barrel is the source of truth. Nothing is given to the
        // player until items are physically removed from the chest.
        // =========================================================
        java.util.List<ItemStack> removedItems = new java.util.ArrayList<>();
        int remaining = amount;

        for (int i = 0; i < barrelInv.getSize() && remaining > 0; i++) {
            ItemStack slotItem = barrelInv.getItem(i);
            if (slotItem == null || !isMatchingItem(shop, slotItem))
                continue;

            int slotAmount = slotItem.getAmount();

            if (slotAmount <= remaining) {
                removedItems.add(slotItem.clone());
                barrelInv.setItem(i, null);
                remaining -= slotAmount;
            } else {
                ItemStack taken = slotItem.clone();
                taken.setAmount(remaining);
                removedItems.add(taken);
                slotItem.setAmount(slotAmount - remaining);
                barrelInv.setItem(i, slotItem);
                remaining = 0;
            }
        }

        // Sanity check: confirm we actually removed the right amount
        int totalRemoved = removedItems.stream().mapToInt(ItemStack::getAmount).sum();
        if (totalRemoved != amount) {
            // Mismatch — return everything to barrel and abort
            for (ItemStack item : removedItems)
                barrelInv.addItem(item);
            SendMessage.sendPlayerMessage(player,
                    "§cTransaction failed: could not remove items from shop.");
            return;
        }

        // Commit barrel removal to the world
        // barrel.update(true, false); // Removed: update() overwrites live inventory with snapshot

        // =========================================================
        // STEP 2: Give items to player.
        // addItem() returns a map of items it FAILED to add (overflow).
        // If overflow exists, those items go back into the barrel.
        // =========================================================
        java.util.HashMap<Integer, ItemStack> overflow = new java.util.HashMap<>();
        for (ItemStack item : removedItems) {
            overflow.putAll(player.getInventory().addItem(item));
        }

        int overflowAmount = overflow.values().stream().mapToInt(ItemStack::getAmount).sum();
        int actualGiven = amount - overflowAmount;

        // Return any overflow items to the barrel immediately
        if (!overflow.isEmpty()) {
            for (ItemStack leftover : overflow.values())
                barrelInv.addItem(leftover);
        }

        if (actualGiven <= 0) {
            // Gave the player nothing — cancel entirely, no economy charge
            SendMessage.sendPlayerMessage(player,
                    "§cTransaction failed: your inventory is full. No items were taken.");
            return;
        }

        // =========================================================
        // STEP 3: Economy — only charge for what was actually given.
        // =========================================================
        double actualPrice = pricePerItem * actualGiven;

        if (!EconomyUtils.withdraw(player.getUniqueId(), actualPrice)) {
            // Can't charge — roll back items from player to barrel
            for (ItemStack item : removedItems) {
                java.util.Map<Integer, ItemStack> notRemoved =
                        player.getInventory().removeItem(item.clone());
                // If removeItem couldn't take it all back, still return what we can to barrel
                // (edge case, but better than items vanishing)
                for (ItemStack rb : notRemoved.values())
                    barrelInv.addItem(rb);
            }
            SendMessage.sendPlayerMessage(player,
                    "§cCould not process payment. Transaction cancelled.");
            return;
        }

        if (!EconomyUtils.deposit(ownerUUID, actualPrice)) {
            // Deposit failed — refund buyer but keep item state (owner loses out, not the buyer)
            EconomyUtils.deposit(player.getUniqueId(), actualPrice);
        }

        // =========================================================
        // STEP 4: Notify and update display.
        // =========================================================
        net.craftnepal.market.managers.DynamicPriceManager.recordPurchase(itemType, actualGiven);
        
        String itemDisplayName = itemType.name().toLowerCase().replace("_", " ");

        if (overflowAmount > 0) {
            SendMessage.sendPlayerMessage(player,
                    "§eOnly " + actualGiven + " fit in your inventory. Bought " + actualGiven + " "
                            + itemDisplayName + " for " + EconomyUtils.format(actualPrice) + ".");
        } else {
            SendMessage.sendPlayerMessage(player, "§aBought " + amount + " " + itemDisplayName
                    + " for " + EconomyUtils.format(actualPrice) + ".");
        }

        org.bukkit.entity.Player owner = org.bukkit.Bukkit.getPlayer(ownerUUID);
        if (owner != null && owner.isOnline()) {
            SendMessage.sendPlayerMessage(owner,
                    "§a" + player.getName() + " bought " + actualGiven + " " + itemDisplayName
                            + " from your shop for " + EconomyUtils.format(actualPrice) + ".");
        }

        org.bukkit.Bukkit.getScheduler().runTask(net.craftnepal.market.Market.getPlugin(), () -> {
            DisplayUtils.getInstance().updateDisplay(shop);
        });
    }
}
