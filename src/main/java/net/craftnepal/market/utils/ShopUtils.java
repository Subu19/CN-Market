package net.craftnepal.market.utils;

import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import net.craftnepal.market.Entities.ChestShop;
import net.craftnepal.market.Market;
import net.craftnepal.market.managers.DatabaseManager;
import net.craftnepal.market.managers.DynamicPriceManager;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ShopUtils {

    public static Map<String, Integer> getAllShopItemKeysAndCountsByPlotID(String plotId) {
        Map<String, Integer> itemCounts = new HashMap<>();
        List<ChestShop> shops = DatabaseManager.getShopsByPlot(plotId);

        for (ChestShop shop : shops) {
            String key = getItemKey(shop);
            int stock = getShopStock(shop); // Uses the database stock cache — fast!

            if (stock > 0) {
                itemCounts.put(key, itemCounts.getOrDefault(key, 0) + stock);
            }
        }
        return itemCounts;
    }

    public static Map<Material, Integer> getAllShopItemsAndCountsByPlotID(String plotId) {
        Map<Material, Integer> itemCounts = new HashMap<>();
        Map<String, Integer> keyCounts = getAllShopItemKeysAndCountsByPlotID(plotId);
        
        for (Map.Entry<String, Integer> entry : keyCounts.entrySet()) {
            Material mat = Material.matchMaterial(entry.getKey().split(":")[0]);
            if (mat != null) {
                itemCounts.put(mat, mat.getMaxStackSize() > 0 ? itemCounts.getOrDefault(mat, 0) + entry.getValue() : 0);
            }
        }
        return itemCounts;
    }

    public static Map<String, ChestShop> getAllShops() {
        Map<String, ChestShop> shops = new HashMap<>();
        List<ChestShop> list = DatabaseManager.getAllShops();
        for (ChestShop shop : list) {
            shops.put(shop.getId(), shop);
        }
        return shops;
    }

    public static List<ChestShop> getPlayerSellingShops(UUID ownerUUID) {
        List<ChestShop> shops = new ArrayList<>();
        List<ChestShop> allOwnerShops = DatabaseManager.getShopsByOwner(ownerUUID.toString());
        for (ChestShop shop : allOwnerShops) {
            if (!shop.isAdmin() && !shop.isBuyingShop()) {
                shops.add(shop);
            }
        }
        return shops;
    }

    public static List<ChestShop> getPlotShopsByItemName(String plotId, String itemName) {
        List<ChestShop> matchingShops = new ArrayList<>();
        List<ChestShop> plotShops = DatabaseManager.getShopsByPlot(plotId);
        for (ChestShop shop : plotShops) {
            String materialName = shop.getItem().getType().toString();
            if (materialName.equalsIgnoreCase(itemName) || materialName.replace("_", " ").equalsIgnoreCase(itemName)) {
                matchingShops.add(shop);
            }
        }
        return matchingShops;
    }

    public static List<ChestShop> getAllShopsByItemName(String itemName) {
        List<ChestShop> matchingShops = new ArrayList<>();
        List<ChestShop> allShops = DatabaseManager.getAllShops();
        for (ChestShop shop : allShops) {
            String materialName = shop.getItem().getType().toString();
            if (materialName.equalsIgnoreCase(itemName) || materialName.replace("_", " ").equalsIgnoreCase(itemName)) {
                matchingShops.add(shop);
            }
        }
        return matchingShops;
    }

    public static ChestShop getShopAt(Location location) {
        return DatabaseManager.getShopAt(location);
    }

    public static ChestShop getShop(String plotId, String shopId) {
        return DatabaseManager.getShop(shopId);
    }

    public static boolean isShopLocation(Location location) {
        return DatabaseManager.getShopAt(location) != null;
    }

    public static void removeShop(String plotId, String shopId) {
        DatabaseManager.removeShop(shopId);
        DisplayUtils.getInstance().removeDisplayPair(plotId, shopId);
    }

    /**
     * Get stock from SQLite cache rather than loading chunk and counting barrel.
     * Prevents server lag entirely.
     */
    public static int getShopStock(ChestShop shop) {
        if (shop.isAdmin()) return 9999;
        return shop.getStock();
    }

    /**
     * Physically count items inside the barrel. Only used when updating cache or processing purchases.
     */
    public static int getPhysicalBarrelStock(ChestShop shop) {
        if (shop.isAdmin()) return 9999;
        
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

    /**
     * Sync physical barrel stock with the SQLite database.
     */
    public static void syncShopStockWithDatabase(ChestShop shop) {
        if (shop.isAdmin()) return;
        int physicalStock = getPhysicalBarrelStock(shop);
        DatabaseManager.updateShopStock(shop.getId(), physicalStock);
    }

    public static boolean isItemBlacklisted(Material material) {
        if (material == null) return false;
        List<String> blacklist = Market.getMainConfig().getStringList("player-shop-blacklist");
        if (blacklist == null) return false;
        String matName = material.name();
        for (String item : blacklist) {
            if (item.equalsIgnoreCase(matName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMatchingItem(ChestShop shop, ItemStack item) {
        if (item == null) return false;
        return item.isSimilar(shop.getItem());
    }

    public static void processPurchase(Player player, String plotId, String shopId, int amount) {
        ChestShop shop = DatabaseManager.getShop(shopId);
        if (shop == null) {
            SendMessage.sendPlayerMessage(player, "§cThis shop no longer exists.");
            return;
        }

        if (!shop.isAdmin() && isItemBlacklisted(shop.getItem().getType())) {
            SendMessage.sendPlayerMessage(player, "§cThis item is blacklisted and transactions for it are disabled.");
            return;
        }

        if (shop.isBuyingShop()) {
            processPlayerSale(player, plotId, shopId, amount);
            return;
        }

        UUID ownerUUID = shop.getOwner();
        if (!shop.isAdmin() && player.getUniqueId().equals(ownerUUID)) {
            SendMessage.sendPlayerMessage(player, "§cYou cannot buy from your own shop.");
            return;
        }

        Location shopLoc = shop.getLocation();
        if (!shop.isAdmin() && (shopLoc == null || shopLoc.getBlock().getType() != Material.BARREL)) {
            SendMessage.sendPlayerMessage(player, "§cShop barrel is missing or corrupted.");
            return;
        }

        Material itemType = shop.getItem().getType();
        double pricePerItem = shop.getPrice();
        double totalPrice = pricePerItem * amount;

        if (!EconomyUtils.hasBalance(player.getUniqueId(), totalPrice)) {
            SendMessage.sendPlayerMessage(player, "§cYou do not have enough money. You need " + EconomyUtils.format(totalPrice));
            return;
        }

        // Query stock using SQLite cache
        int stock = getShopStock(shop);
        if (stock < amount) {
            SendMessage.sendPlayerMessage(player, "§cNot enough stock! Only " + stock + " available.");
            return;
        }

        // Check player inventory space
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

        // STEP 1: Remove items from barrel FIRST. (SKIP FOR ADMIN)
        List<ItemStack> removedItems = new ArrayList<>();
        
        if (shop.isAdmin()) {
            ItemStack itemToGive = shop.getItem().clone();
            itemToGive.setAmount(amount);
            removedItems.add(itemToGive);
        } else {
            Barrel barrel = (Barrel) shopLoc.getBlock().getState();
            org.bukkit.inventory.Inventory barrelInv = barrel.getInventory();
            
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

            int totalRemoved = removedItems.stream().mapToInt(ItemStack::getAmount).sum();
            if (totalRemoved != amount) {
                for (ItemStack item : removedItems)
                    barrelInv.addItem(item);
                SendMessage.sendPlayerMessage(player, "§cTransaction failed: could not remove items from shop.");
                return;
            }
        }

        // STEP 2: Give items to player.
        HashMap<Integer, ItemStack> overflow = new HashMap<>();
        for (ItemStack item : removedItems) {
            overflow.putAll(player.getInventory().addItem(item));
        }

        int overflowAmount = overflow.values().stream().mapToInt(ItemStack::getAmount).sum();
        int actualGiven = amount - overflowAmount;

        if (!overflow.isEmpty() && !shop.isAdmin()) {
            Barrel barrel = (Barrel) shopLoc.getBlock().getState();
            for (ItemStack leftover : overflow.values())
                barrel.getInventory().addItem(leftover);
        }

        if (actualGiven <= 0) {
            SendMessage.sendPlayerMessage(player, "§cTransaction failed: your inventory is full. No items were taken.");
            return;
        }

        // STEP 3: Economy
        double actualPrice = pricePerItem * actualGiven;

        if (!EconomyUtils.withdraw(player.getUniqueId(), actualPrice)) {
            for (ItemStack item : removedItems) {
                Map<Integer, ItemStack> notRemoved = player.getInventory().removeItem(item.clone());
                if (!shop.isAdmin()) {
                    Barrel barrel = (Barrel) shopLoc.getBlock().getState();
                    for (ItemStack rb : notRemoved.values())
                        barrel.getInventory().addItem(rb);
                }
            }
            SendMessage.sendPlayerMessage(player, "§cCould not process payment. Transaction cancelled.");
            return;
        }

        if (!shop.isAdmin() && !EconomyUtils.deposit(ownerUUID, actualPrice)) {
            EconomyUtils.deposit(player.getUniqueId(), actualPrice);
        }

        // STEP 4: Update SQLite Database Stock Cache (arithmetic — no chunk load needed)
        if (!shop.isAdmin()) {
            int newStock = Math.max(0, stock - actualGiven);
            shop.setStock(newStock);
            DatabaseManager.updateShopStock(shop.getId(), newStock);
        }

        // STEP 5: Notify and update display.
        String itemKey = getItemKey(shop);
        if (!shop.isAdmin()) {
            DynamicPriceManager.recordPurchase(itemKey, actualGiven);
        } else {
            DynamicPriceManager.recordAdminPurchase(itemKey, actualGiven);
        }
        
        String itemDisplayName = getShopDisplayName(shop);

        if (overflowAmount > 0) {
            SendMessage.sendPlayerMessage(player, "§eOnly " + actualGiven + " fit in your inventory. Bought " + actualGiven + " "
                            + itemDisplayName + " for " + EconomyUtils.format(actualPrice) + ".");
        } else {
            SendMessage.sendPlayerMessage(player, "§aBought " + amount + " " + itemDisplayName
                    + " for " + EconomyUtils.format(actualPrice) + ".");
        }

        Player owner = Bukkit.getPlayer(ownerUUID);
        if (owner != null && owner.isOnline()) {
            SendMessage.sendPlayerMessage(owner, "§a" + player.getName() + " bought " + actualGiven + " " + itemDisplayName
                            + " from your shop for " + EconomyUtils.format(actualPrice) + ".");
            if (!shop.isAdmin() && getShopStock(shop) == 0) {
                SendMessage.sendPlayerMessage(owner, "§c[Reminder] Your shop selling " + itemDisplayName + " is now out of stock!");
            }
        } else if (!shop.isAdmin()) {
            double currentOffline = DatabaseManager.getOfflineEarnings(ownerUUID.toString());
            DatabaseManager.setOfflineEarnings(ownerUUID.toString(), currentOffline + actualPrice);
        }
        TransactionLogUtils.log("BUY: " + player.getName() + " bought " + actualGiven + "x " + itemDisplayName + " from shop " + shopId + " (Owner: " + ownerUUID.toString() + ") for " + actualPrice);

        Bukkit.getScheduler().runTask(Market.getPlugin(), () -> {
            DisplayUtils.getInstance().updateDisplay(shop);
        });
    }

    public static void processPlayerSale(Player player, String plotId, String shopId, int amount) {
        ChestShop shop = DatabaseManager.getShop(shopId);
        if (shop == null || !shop.isBuyingShop()) return;

        if (!shop.isAdmin() && isItemBlacklisted(shop.getItem().getType())) {
            SendMessage.sendPlayerMessage(player, "§cThis item is blacklisted and transactions for it are disabled.");
            return;
        }

        double pricePerItem = shop.getPrice();
        double totalPayout = pricePerItem * amount;

        int playerHas = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && isMatchingItem(shop, item)) {
                playerHas += item.getAmount();
            }
        }

        if (playerHas < amount) {
            SendMessage.sendPlayerMessage(player, "§cYou do not have enough " + getShopDisplayName(shop) + " to sell.");
            return;
        }

        if (!shop.isAdmin()) {
            if (!EconomyUtils.hasBalance(shop.getOwner(), totalPayout)) {
                SendMessage.sendPlayerMessage(player, "§cThe shop owner does not have enough money to buy your items.");
                return;
            }
        }

        ItemStack toRemove = shop.getItem().clone();
        toRemove.setAmount(amount);
        player.getInventory().removeItem(toRemove);

        if (!shop.isAdmin()) {
            Location loc = shop.getLocation();
            if (loc != null && loc.getBlock().getType() == Material.BARREL) {
                Barrel barrel = (Barrel) loc.getBlock().getState();
                barrel.getInventory().addItem(toRemove);
                
                // Update SQLite database stock cache (arithmetic — no chunk load needed)
                int currentStock = getShopStock(shop);
                int newStock = currentStock + amount;
                shop.setStock(newStock);
                DatabaseManager.updateShopStock(shop.getId(), newStock);
            }
        }

        if (shop.isAdmin()) {
            EconomyUtils.deposit(player.getUniqueId(), totalPayout);
        } else {
            if (EconomyUtils.withdraw(shop.getOwner(), totalPayout)) {
                EconomyUtils.deposit(player.getUniqueId(), totalPayout);
            } else {
                player.getInventory().addItem(toRemove);
                SendMessage.sendPlayerMessage(player, "§cTransaction failed.");
                return;
            }
        }

        if (shop.isAdmin()) {
            DynamicPriceManager.recordAdminSale(getItemKey(shop), amount);
        }

        SendMessage.sendPlayerMessage(player, "§aSuccessfully sold " + amount + " " + getShopDisplayName(shop) + " for " + EconomyUtils.format(totalPayout));
        TransactionLogUtils.log("SELL: " + player.getName() + " sold " + amount + "x " + getShopDisplayName(shop) + " to shop " + shopId + " (Owner: " + shop.getOwner().toString() + ") for " + totalPayout);
        
        Bukkit.getScheduler().runTask(Market.getPlugin(), () -> {
            DisplayUtils.getInstance().updateDisplay(shop);
        });
    }

    public static String getItemKey(ChestShop shop) {
        return getItemKey(shop.getItem());
    }

    public static String getItemKey(ItemStack item) {
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof EnchantmentStorageMeta) {
                EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) meta;
                if (!bookMeta.getStoredEnchants().isEmpty()) {
                    Map.Entry<Enchantment, Integer> enchant = bookMeta.getStoredEnchants().entrySet().iterator().next();
                    return "ENCHANTED_BOOK:" + enchant.getKey().getKey().getKey().toUpperCase() + ":" + enchant.getValue();
                }
            } else if (meta instanceof org.bukkit.inventory.meta.PotionMeta) {
                org.bukkit.inventory.meta.PotionMeta potionMeta = (org.bukkit.inventory.meta.PotionMeta) meta;
                org.bukkit.potion.PotionData data = potionMeta.getBasePotionData();
                org.bukkit.potion.PotionType type = data.getType();
                String key = item.getType().name() + ":" + (type != null ? type.name() : "UNKNOWN");
                if (data.isUpgraded()) key += ":UPGRADED";
                else if (data.isExtended()) key += ":EXTENDED";
                return key;
            }
        }
        return item.getType().name();
    }

    public static String getShopDisplayName(ChestShop shop) {
        return getShopDisplayName(shop.getItem());
    }

    public static String getShopDisplayName(ItemStack item) {
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                return meta.getDisplayName();
            }
            if (meta instanceof EnchantmentStorageMeta) {
                EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) meta;
                if (!bookMeta.getStoredEnchants().isEmpty()) {
                    Map.Entry<Enchantment, Integer> enchant = bookMeta.getStoredEnchants().entrySet().iterator().next();
                    return formatKey(enchant.getKey().getKey().getKey()) + " " + enchant.getValue();
                }
                return "Enchanted Book";
            } else if (meta instanceof org.bukkit.inventory.meta.PotionMeta) {
                org.bukkit.inventory.meta.PotionMeta potionMeta = (org.bukkit.inventory.meta.PotionMeta) meta;
                org.bukkit.potion.PotionData data = potionMeta.getBasePotionData();
                org.bukkit.potion.PotionType type = data.getType();
                String base = type != null ? formatKey(type.name()) : "Unknown";
                
                if (type != null) {
                    base = switch (type.name()) {
                        case "SPEED" -> "Swiftness";
                        case "JUMP" -> "Leaping";
                        case "INSTANT_HEALTH" -> "Healing";
                        case "INSTANT_DAMAGE" -> "Harming";
                        default -> base;
                    };
                }
                
                String suffix = "";
                if (data.isUpgraded()) suffix = " II";
                else if (data.isExtended()) suffix = " (Extended)";

                String prefix = switch (item.getType()) {
                    case TIPPED_ARROW -> "Arrow of ";
                    case POTION -> "Potion of ";
                    case SPLASH_POTION -> "Splash Potion of ";
                    case LINGERING_POTION -> "Lingering Potion of ";
                    default -> "";
                };
                return prefix + base + suffix;
            }
        }
        return formatKey(item.getType().name());
    }

    public static String formatKey(String key) {
        return java.util.Arrays.stream(key.split("_"))
                .map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    public static byte[] serializeItem(ItemStack item) {
        try {
            java.io.ByteArrayOutputStream io = new java.io.ByteArrayOutputStream();
            org.bukkit.util.io.BukkitObjectOutputStream os = new org.bukkit.util.io.BukkitObjectOutputStream(io);
            os.writeObject(item);
            os.flush();
            return io.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public static ItemStack deserializeItem(byte[] bytes) {
        try {
            java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(bytes);
            org.bukkit.util.io.BukkitObjectInputStream is = new org.bukkit.util.io.BukkitObjectInputStream(in);
            return (ItemStack) is.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
