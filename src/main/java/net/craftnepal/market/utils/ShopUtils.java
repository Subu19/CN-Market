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
            if (shop.isBuyingShop()) continue; // items a shop bought are not for sale
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

    /** Live, read-only view of every shop by id. */
    public static Map<String, ChestShop> getAllShops() {
        return DatabaseManager.shopIndex().asMap();
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
        for (ChestShop shop : DatabaseManager.shopIndex().all()) {
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
        return material != null && Market.settings().shopBlacklist().contains(material);
    }

    public static boolean isMatchingItem(ChestShop shop, ItemStack item) {
        if (item == null) return false;
        return item.isSimilar(shop.getItem());
    }

    /** Trades are only opened from a barrel click; the player must still be near it. */
    private static final double MAX_TRADE_DISTANCE_SQUARED = 8 * 8;

    private static boolean canTrade(Player player, ChestShop shop, int amount) {
        if (amount <= 0) return false;
        Location loc = shop.getLocation();
        if (loc == null || loc.getWorld() != player.getWorld()
                || loc.distanceSquared(player.getLocation()) > MAX_TRADE_DISTANCE_SQUARED) {
            SendMessage.sendPlayerMessage(player, "§cYou are too far from this shop.");
            return false;
        }
        return true;
    }

    /** Puts items back into a barrel; anything that does not fit is dropped on top so nothing is lost. */
    private static void returnToBarrel(Location shopLoc, Iterable<ItemStack> items) {
        Barrel barrel = (Barrel) shopLoc.getBlock().getState();
        for (ItemStack item : items) {
            for (ItemStack left : barrel.getInventory().addItem(item).values()) {
                shopLoc.getWorld().dropItemNaturally(shopLoc.clone().add(0.5, 1.0, 0.5), left);
            }
        }
    }

    /** How many more of the shop's item fit into an inventory's storage slots. */
    private static int spaceFor(ChestShop shop, ItemStack[] contents) {
        int max = shop.getItem().getMaxStackSize();
        int space = 0;
        for (ItemStack slot : contents) {
            if (slot == null || slot.getType() == Material.AIR) space += max;
            else if (isMatchingItem(shop, slot)) space += Math.max(0, max - slot.getAmount());
        }
        return space;
    }

    public static void processPurchase(Player player, String plotId, String shopId, int amount) {
        ChestShop shop = DatabaseManager.getShop(shopId);
        if (shop == null) {
            SendMessage.sendPlayerMessage(player, "§cThis shop no longer exists.");
            return;
        }
        if (!canTrade(player, shop, amount)) return;

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
                returnToBarrel(shopLoc, removedItems);
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
            returnToBarrel(shopLoc, overflow.values());
        }

        if (actualGiven <= 0) {
            SendMessage.sendPlayerMessage(player, "§cTransaction failed: your inventory is full. No items were taken.");
            return;
        }

        // STEP 3: Economy
        double actualPrice = pricePerItem * actualGiven;

        if (!EconomyUtils.withdraw(player.getUniqueId(), actualPrice)) {
            // Take back what was given and return it to the barrel.
            for (ItemStack item : removedItems) {
                ItemStack given = item.clone();
                int missing = player.getInventory().removeItem(given).values().stream().mapToInt(ItemStack::getAmount).sum();
                given.setAmount(given.getAmount() - missing);
                if (!shop.isAdmin() && given.getAmount() > 0) returnToBarrel(shopLoc, List.of(given));
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
            DatabaseManager.addOfflineSale(ownerUUID.toString(), itemKey, itemDisplayName, actualGiven, actualPrice);
        }
        TransactionLogUtils.log("BUY: " + player.getName() + " bought " + actualGiven + "x " + itemDisplayName + " from shop " + shopId + " (Owner: " + ownerUUID.toString() + ") for " + actualPrice);

        Bukkit.getScheduler().runTask(Market.getPlugin(), () -> {
            DisplayUtils.getInstance().updateDisplay(shop);
        });
    }

    /**
     * A player sells to a buying shop. Order matters: everything that can fail (stock, space,
     * owner's money) is checked or done before the player's items move, so a failure never
     * leaves items in two places or in none.
     */
    public static void processPlayerSale(Player player, String plotId, String shopId, int amount) {
        ChestShop shop = DatabaseManager.getShop(shopId);
        if (shop == null || !shop.isBuyingShop()) return;
        if (!canTrade(player, shop, amount)) return;

        if (!shop.isAdmin() && isItemBlacklisted(shop.getItem().getType())) {
            SendMessage.sendPlayerMessage(player, "§cThis item is blacklisted and transactions for it are disabled.");
            return;
        }

        double pricePerItem = shop.getPrice();
        double totalPayout = pricePerItem * amount;
        String displayName = getShopDisplayName(shop);

        int playerHas = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && isMatchingItem(shop, item)) {
                playerHas += item.getAmount();
            }
        }
        if (playerHas < amount) {
            SendMessage.sendPlayerMessage(player, "§cYou do not have enough " + displayName + " to sell.");
            return;
        }

        Location loc = shop.getLocation();
        Barrel barrel = null;
        if (!shop.isAdmin()) {
            if (loc == null || loc.getBlock().getType() != Material.BARREL) {
                SendMessage.sendPlayerMessage(player, "§cShop barrel is missing or corrupted.");
                return;
            }
            barrel = (Barrel) loc.getBlock().getState();
            if (spaceFor(shop, barrel.getInventory().getStorageContents()) < amount) {
                SendMessage.sendPlayerMessage(player, "§cThis shop's barrel is full.");
                return;
            }
            if (!EconomyUtils.hasBalance(shop.getOwner(), totalPayout)
                    || !EconomyUtils.withdraw(shop.getOwner(), totalPayout)) {
                SendMessage.sendPlayerMessage(player, "§cThe shop owner does not have enough money to buy your items.");
                return;
            }
        }

        ItemStack toRemove = shop.getItem().clone();
        toRemove.setAmount(amount);
        int notTaken = player.getInventory().removeItem(toRemove).values().stream().mapToInt(ItemStack::getAmount).sum();
        int sold = amount - notTaken;
        if (sold <= 0) {
            if (!shop.isAdmin()) EconomyUtils.deposit(shop.getOwner(), totalPayout);
            SendMessage.sendPlayerMessage(player, "§cTransaction failed.");
            return;
        }
        double payout = pricePerItem * sold;
        if (!shop.isAdmin() && sold < amount) {
            EconomyUtils.deposit(shop.getOwner(), totalPayout - payout); // refund the part not sold
        }

        if (!shop.isAdmin()) {
            ItemStack stored = shop.getItem().clone();
            stored.setAmount(sold);
            returnToBarrel(loc, List.of(stored));
            int newStock = getShopStock(shop) + sold;
            DatabaseManager.updateShopStock(shop.getId(), newStock);
        }
        EconomyUtils.deposit(player.getUniqueId(), payout);

        if (shop.isAdmin()) {
            DynamicPriceManager.recordAdminSale(getItemKey(shop), sold);
        }

        SendMessage.sendPlayerMessage(player, "§aSuccessfully sold " + sold + " " + displayName + " for " + EconomyUtils.format(payout));
        TransactionLogUtils.log("SELL: " + player.getName() + " sold " + sold + "x " + displayName + " to shop " + shopId + " (Owner: " + shop.getOwner() + ") for " + payout);

        DisplayUtils.getInstance().updateDisplay(shop);
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
}
