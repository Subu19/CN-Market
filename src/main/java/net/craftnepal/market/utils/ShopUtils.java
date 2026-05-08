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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.AbstractMap;
import java.util.stream.Collectors;

public class ShopUtils {

    public static Map<String, Integer> getAllShopItemKeysAndCountsByPlotID(String plotId) {
        Map<String, Integer> itemCounts = new HashMap<>();
        ConfigurationSection shops =
                RegionData.get().getConfigurationSection("market.plots." + plotId + ".shops");

        if (shops != null) {
            for (String shopId : shops.getKeys(false)) {
                String path = "market.plots." + plotId + ".shops." + shopId;
                ChestShop shop = createShopFromConfig(shopId, path);
                if (shop == null) continue;

                String key = getItemKey(shop);
                int stock = getShopStock(shop);

                if (stock > 0) {
                    itemCounts.put(key, itemCounts.getOrDefault(key, 0) + stock);
                }
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
                itemCounts.put(mat, itemCounts.getOrDefault(mat, 0) + entry.getValue());
            }
        }
        return itemCounts;
    }

    private static ChestShop createShopFromConfig(String shopId, String path) {
        Location location = LocationUtils.loadLocation(RegionData.get(), path + ".location");
        String ownerString = RegionData.get().getString(path + ".owner");
        UUID owner = ownerString != null ? UUID.fromString(ownerString) : null;
        double price = RegionData.get().getDouble(path + ".price");
        boolean isAdmin = RegionData.get().getBoolean(path + ".is_admin", false);
        boolean isBuyingShop = RegionData.get().getBoolean(path + ".is_buying_shop", false);
        
        // Fallback for the short-lived is_sell_shop key
        if (!isBuyingShop && RegionData.get().contains(path + ".is_sell_shop")) {
            isBuyingShop = RegionData.get().getBoolean(path + ".is_sell_shop");
        }

        ItemStack itemStack = null;

        // Try to load from Base64 bytes first
        if (RegionData.get().contains(path + ".item_bytes")) {
            String b64 = RegionData.get().getString(path + ".item_bytes");
            if (b64 != null) {
                try {
                    byte[] bytes = java.util.Base64.getDecoder().decode(b64);
                    itemStack = deserializeItem(bytes);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            // Legacy loading fallback
            String materialName = RegionData.get().getString(path + ".item");
            if (materialName != null) {
                Material material = Material.matchMaterial(materialName);
                if (material != null) {
                    itemStack = new ItemStack(material);
                    if (material == Material.ENCHANTED_BOOK) {
                        String enchantKey = RegionData.get().getString(path + ".enchantment.key");
                        int level = RegionData.get().getInt(path + ".enchantment.level");
                        if (enchantKey != null) {
                            enchantKey = enchantKey.toLowerCase();
                            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantKey));
                            if (enchantment != null) {
                                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemStack.getItemMeta();
                                if (meta != null) {
                                    meta.addStoredEnchant(enchantment, level, true);
                                    itemStack.setItemMeta(meta);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (itemStack != null && location != null && owner != null) {
            return new ChestShop(shopId, location, itemStack, owner, price, isAdmin, isBuyingShop);
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

    public static ChestShop getShop(String plotId, String shopId) {
        String path = "market.plots." + plotId + ".shops." + shopId;
        return createShopFromConfig(shopId, path);
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

    public static void removeShop(String plotId, String shopId) {
        String path = "market.plots." + plotId + ".shops." + shopId;
        if (RegionData.get().contains(path)) {
            RegionData.get().set(path, null);
            RegionData.save();
            DisplayUtils.getInstance().removeDisplayPair(plotId, shopId);
        }
    }


    public static int getShopStock(ChestShop shop) {
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

    public static boolean isMatchingItem(ChestShop shop, ItemStack item) {
        if (item == null) return false;
        
        // Deep compare using Bukkit's isSimilar (compares type, durability, and ItemMeta)
        return item.isSimilar(shop.getItem());
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

        ChestShop shop = getShop(plotId, shopId);
        if (shop == null) {
            SendMessage.sendPlayerMessage(player, "§cShop data not found.");
            return;
        }

        if (shop.isBuyingShop()) {
            processPlayerSale(player, plotId, shopId, amount);
            return;
        }

        UUID ownerUUID;
        try {
            ownerUUID = UUID.fromString(ownerStr);
        } catch (IllegalArgumentException e) {
            SendMessage.sendPlayerMessage(player, "§cShop owner data is corrupted.");
            return;
        }

        if (!shop.isAdmin() && player.getUniqueId().equals(ownerUUID)) {
            SendMessage.sendPlayerMessage(player, "§cYou cannot buy from your own shop.");
            return;
        }

        // --- Validate barrel ---
        Location shopLoc = shop.getLocation();
        if (!shop.isAdmin() && (shopLoc == null || shopLoc.getBlock().getType() != Material.BARREL)) {
            SendMessage.sendPlayerMessage(player, "§cShop barrel is missing or corrupted.");
            return;
        }

        Material itemType = shop.getItem().getType();

        double pricePerItem = RegionData.get().getDouble(basePath + ".price");
        double totalPrice = pricePerItem * amount;

        if (!EconomyUtils.hasBalance(player.getUniqueId(), totalPrice)) {
            SendMessage.sendPlayerMessage(player,
                    "§cYou do not have enough money. You need " + EconomyUtils.format(totalPrice));
            return;
        }


        // Always re-fetch block state fresh to get live inventory
        int stock = getShopStock(shop);

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
        // STEP 1: Remove items from barrel FIRST. (SKIP FOR ADMIN)
        // =========================================================
        java.util.List<ItemStack> removedItems = new java.util.ArrayList<>();
        
        if (shop.isAdmin()) {
            ItemStack itemToGive = shop.getItem().clone();
            itemToGive.setAmount(amount);
            removedItems.add(itemToGive);
        } else {
            org.bukkit.block.BlockState blockState = shopLoc.getBlock().getState();
            Barrel barrel = (Barrel) blockState;
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
        }

        // =========================================================
        // STEP 2: Give items to player.
        // =========================================================
        java.util.HashMap<Integer, ItemStack> overflow = new java.util.HashMap<>();
        for (ItemStack item : removedItems) {
            overflow.putAll(player.getInventory().addItem(item));
        }

        int overflowAmount = overflow.values().stream().mapToInt(ItemStack::getAmount).sum();
        int actualGiven = amount - overflowAmount;

        // Return any overflow items to the barrel immediately
        if (!overflow.isEmpty() && !shop.isAdmin()) {
            org.bukkit.block.BlockState blockState = shopLoc.getBlock().getState();
            Barrel barrel = (Barrel) blockState;
            for (ItemStack leftover : overflow.values())
                barrel.getInventory().addItem(leftover);
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
                if (!shop.isAdmin()) {
                    org.bukkit.block.BlockState blockState = shopLoc.getBlock().getState();
                    Barrel barrel = (Barrel) blockState;
                    for (ItemStack rb : notRemoved.values())
                        barrel.getInventory().addItem(rb);
                }
            }
            SendMessage.sendPlayerMessage(player,
                    "§cCould not process payment. Transaction cancelled.");
            return;
        }

        if (!shop.isAdmin() && !EconomyUtils.deposit(ownerUUID, actualPrice)) {
            // Deposit failed — refund buyer but keep item state (owner loses out, not the buyer)
            EconomyUtils.deposit(player.getUniqueId(), actualPrice);
        }

        // =========================================================
        // STEP 4: Notify and update display.
        // =========================================================
        String itemKey = getItemKey(shop);
        if (!shop.isAdmin()) {
            net.craftnepal.market.managers.DynamicPriceManager.recordPurchase(itemKey, actualGiven);
        }
        
        String itemDisplayName = getShopDisplayName(shop);

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

    public static void processPlayerSale(org.bukkit.entity.Player player, String plotId, String shopId, int amount) {
        ChestShop shop = getShop(plotId, shopId);
        if (shop == null || !shop.isBuyingShop()) return;

        double pricePerItem = shop.getPrice();
        double totalPayout = pricePerItem * amount;

        // Check if player has the items
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

        // Check shop funds if not admin
        if (!shop.isAdmin()) {
            if (!EconomyUtils.hasBalance(shop.getOwner(), totalPayout)) {
                SendMessage.sendPlayerMessage(player, "§cThe shop owner does not have enough money to buy your items.");
                return;
            }
        }

        // Remove items from player
        ItemStack toRemove = shop.getItem().clone();
        toRemove.setAmount(amount);
        player.getInventory().removeItem(toRemove);

        // Add items to shop barrel if not admin
        if (!shop.isAdmin()) {
            Location loc = shop.getLocation();
            if (loc != null && loc.getBlock().getType() == Material.BARREL) {
                Barrel barrel = (Barrel) loc.getBlock().getState();
                barrel.getInventory().addItem(toRemove);
            }
        }

        // Economy transfer
        if (shop.isAdmin()) {
            EconomyUtils.deposit(player.getUniqueId(), totalPayout);
        } else {
            if (EconomyUtils.withdraw(shop.getOwner(), totalPayout)) {
                EconomyUtils.deposit(player.getUniqueId(), totalPayout);
            } else {
                // Rollback if withdraw fails
                player.getInventory().addItem(toRemove);
                SendMessage.sendPlayerMessage(player, "§cTransaction failed.");
                return;
            }
        }

        SendMessage.sendPlayerMessage(player, "§aSuccessfully sold " + amount + " " + getShopDisplayName(shop) + " for " + EconomyUtils.format(totalPayout));
        
        org.bukkit.Bukkit.getScheduler().runTask(net.craftnepal.market.Market.getPlugin(), () -> {
            DisplayUtils.getInstance().updateDisplay(shop);
        });
    }

    /**
     * Generates a unique key for an item sold in a shop.
     * For enchanted books, includes enchantment type and level.
     */
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

    /**
     * Gets a user-friendly display name for a shop's item.
     */
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
                
                // Minecraft-standard names for potion effects
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
