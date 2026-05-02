package net.craftnepal.market.menus;

import me.kodysimpson.simpapi.menu.Menu;
import me.kodysimpson.simpapi.menu.PlayerMenuUtility;
import net.craftnepal.market.Entities.ChestShop;
import net.craftnepal.market.Market;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.managers.DynamicPriceManager;
import net.craftnepal.market.utils.DisplayUtils;
import net.craftnepal.market.utils.LocationUtils;
import net.craftnepal.market.utils.ShopUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ShopCreateMenu extends Menu {

    private final String plotId;
    private final Location chestLocation;
    private final ItemStack itemToSell;
    private double currentPrice;
    private final double minPrice;
    private final double maxPrice;
    private final double fairPrice;

    public ShopCreateMenu(PlayerMenuUtility playerMenuUtility, String plotId,
            Location chestLocation, ItemStack itemToSell) {
        super(playerMenuUtility);
        this.plotId = plotId;
        this.chestLocation = chestLocation;
        this.itemToSell = itemToSell.clone();
        this.itemToSell.setAmount(1);

        String itemKey = ShopUtils.getItemKey(itemToSell);
        this.fairPrice = DynamicPriceManager.getDynamicPrice(itemToSell);
        this.currentPrice = fairPrice;
        this.minPrice = fairPrice * 0.85;
        this.maxPrice = fairPrice * 1.50;
    }

    @Override
    public String getMenuName() {
        return ChatColor.DARK_GREEN + "Create Shop";
    }

    @Override
    public int getSlots() {
        return 45;
    }

    @Override
    public boolean cancelAllClicks() {
        return true;
    }

    @Override
    public void setMenuItems() {
        inventory.clear();

        // Outer border
        ItemStack outerBorder = makeItem(Material.GREEN_STAINED_GLASS_PANE, " ");
        for (int i : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 41, 42, 43, 44}) {
            inventory.setItem(i, outerBorder);
        }

        // Inner border
        ItemStack innerBorder = makeItem(Material.LIME_STAINED_GLASS_PANE, " ");
        for (int i : new int[]{10, 11, 12, 14, 15, 16, 19, 25, 28, 29, 30, 32, 33, 34}) {
            inventory.setItem(i, innerBorder);
        }

        // Show item being sold
        ItemStack displayItem = itemToSell.clone();
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            String itemName = ShopUtils.getShopDisplayName(itemToSell);
            meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Selling: " + itemName);

            String itemKey = ShopUtils.getItemKey(itemToSell);
            String trendStr = DynamicPriceManager.getTrendString(itemKey);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Market Fair Price: " + ChatColor.GOLD
                    + String.format("%.2f", fairPrice) + " " + trendStr);

            // Show per-enchant price breakdown if the item has enchantments
            org.bukkit.inventory.meta.ItemMeta realMeta = itemToSell.getItemMeta();
            if (realMeta != null && realMeta.hasEnchants()) {
                double baseOnly = net.craftnepal.market.managers.DynamicPriceManager.getDynamicPrice(itemKey);
                lore.add(ChatColor.DARK_GRAY + "  Base item: " + ChatColor.GRAY + String.format("%.2f", baseOnly));
                for (java.util.Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry
                        : realMeta.getEnchants().entrySet()) {
                    String enchantName = entry.getKey().getKey().getKey().toUpperCase();
                    int enchantLevel = entry.getValue();
                    int enchantPrice = net.craftnepal.market.files.PriceData.getEnchantmentPrice(enchantName, enchantLevel);
                    String displayName = net.craftnepal.market.utils.ShopUtils.formatKey(enchantName)
                            + " " + toRoman(enchantLevel);
                    lore.add(ChatColor.DARK_GRAY + "  + " + displayName + ": "
                            + ChatColor.GRAY + String.format("%.2f", (double) enchantPrice));
                }
            }

            lore.add(ChatColor.GRAY + "Allowed Range: " + ChatColor.RED
                    + String.format("%.2f", minPrice) + ChatColor.GRAY + " to " + ChatColor.GREEN
                    + String.format("%.2f", maxPrice));
            lore.add("");
            lore.add(ChatColor.GRAY + "Your Set Price: " + ChatColor.AQUA + ChatColor.BOLD
                    + String.format("%.2f", currentPrice));

            meta.setLore(lore);
            displayItem.setItemMeta(meta);
        }
        inventory.setItem(13, displayItem);

        // Price adjustment buttons
        inventory.setItem(20, makeItem(Material.RED_DYE, ChatColor.RED + "-10%",
                ChatColor.GRAY + "New price: " + String.format("%.2f", currentPrice * 0.90)));
        inventory.setItem(21, makeItem(Material.ORANGE_DYE, ChatColor.GOLD + "-5%",
                ChatColor.GRAY + "New price: " + String.format("%.2f", currentPrice * 0.95)));
        inventory.setItem(22, makeItem(Material.SUNFLOWER, ChatColor.YELLOW + "Reset to Fair Price",
                ChatColor.GRAY + "Set price to: " + String.format("%.2f", fairPrice)));
        inventory.setItem(23, makeItem(Material.LIGHT_BLUE_DYE, ChatColor.AQUA + "+5%",
                ChatColor.GRAY + "New price: " + String.format("%.2f", currentPrice * 1.05)));
        inventory.setItem(24, makeItem(Material.LIME_DYE, ChatColor.GREEN + "+10%",
                ChatColor.GRAY + "New price: " + String.format("%.2f", currentPrice * 1.10)));

        // Confirm / Cancel
        inventory.setItem(31, makeItem(Material.EMERALD_BLOCK,
                ChatColor.GREEN + "" + ChatColor.BOLD + "CONFIRM AND CREATE",
                ChatColor.GRAY + "Price: " + ChatColor.GOLD + String.format("%.2f", currentPrice)));

        inventory.setItem(40, makeItem(Material.DARK_OAK_DOOR, ChatColor.RED + "Cancel"));
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta())
            return;

        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        if (name.equals("Cancel")) {
            playerMenuUtility.getOwner().closeInventory();
            net.craftnepal.market.utils.SendMessage.sendPlayerMessage(playerMenuUtility.getOwner(),
                    "§cShop creation cancelled.");
        } else if (name.equals("-10%")) {
            adjustPrice(0.90);
        } else if (name.equals("-5%")) {
            adjustPrice(0.95);
        } else if (name.equals("Reset to Fair Price")) {
            currentPrice = fairPrice;
            setMenuItems();
        } else if (name.equals("+5%")) {
            adjustPrice(1.05);
        } else if (name.equals("+10%")) {
            adjustPrice(1.10);
        } else if (name.equals("CONFIRM AND CREATE")) {
            createShop();
        }
    }

    private void adjustPrice(double multiplier) {
        double newPrice = currentPrice * multiplier;
        if (newPrice < minPrice)
            newPrice = minPrice;
        if (newPrice > maxPrice)
            newPrice = maxPrice;
        currentPrice = newPrice;
        setMenuItems();
    }

    private void createShop() {
        UUID uuid = playerMenuUtility.getOwner().getUniqueId();
        String shopId = UUID.randomUUID().toString();

        // Define the base path in the YAML configuration
        String basePath = "market.plots." + plotId + ".shops." + shopId;

        // Serialize and save the ChestShop data
        LocationUtils.saveLocation(RegionData.get(), basePath + ".location", chestLocation);
        String base64Item =
                java.util.Base64.getEncoder().encodeToString(ShopUtils.serializeItem(itemToSell));
        RegionData.get().set(basePath + ".item_bytes", base64Item);
        RegionData.get().set(basePath + ".item", itemToSell.getType().toString());
        RegionData.get().set(basePath + ".owner", uuid.toString());
        RegionData.get().set(basePath + ".price", currentPrice);

        RegionData.save();

        playerMenuUtility.getOwner().closeInventory();
        playerMenuUtility.getOwner()
                .sendMessage(ChatColor.GREEN + "Shop created successfully with price: "
                        + ChatColor.GOLD + String.format("%.2f", currentPrice));

        // Spawn display
        ChestShop chestShop = new ChestShop(shopId, chestLocation, itemToSell, uuid, currentPrice);
        DisplayUtils.getInstance().spawnDisplayPair(chestShop);
    }

    public ItemStack makeItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String toRoman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(level);
        };
    }
}
