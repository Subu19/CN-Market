package net.craftnepal.market.menus;

import me.kodysimpson.simpapi.menu.Menu;
import me.kodysimpson.simpapi.menu.PlayerMenuUtility;
import net.craftnepal.market.Entities.ChestShop;
import net.craftnepal.market.utils.EconomyUtils;
import net.craftnepal.market.utils.ShopUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShopBuyerMenu extends Menu {

    private final String plotId;
    private final String shopId;
    private final ChestShop shop;

    public ShopBuyerMenu(PlayerMenuUtility playerMenuUtility, String plotId, String shopId) {
        super(playerMenuUtility);
        this.plotId = plotId;
        this.shopId = shopId;
        this.shop = ShopUtils.getShop(plotId, shopId);
    }

    @Override
    public String getMenuName() {
        return ChatColor.DARK_BLUE + "Buy Item";
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
        ItemStack outerBorder = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 41, 42, 43, 44}) {
            inventory.setItem(i, outerBorder);
        }

        // Inner border
        ItemStack innerBorder = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i : new int[]{10, 11, 12, 14, 15, 16, 19, 20, 21, 23, 24, 25, 28, 30, 32, 34}) {
            inventory.setItem(i, innerBorder);
        }

        if (shop == null) {
            inventory.setItem(22, makeItem(Material.BARRIER, ChatColor.RED + "Shop Error",
                    ChatColor.GRAY + "Shop data not found!"));
            return;
        }

        // Show the actual item in the center
        ItemStack displayItem = shop.getItem().clone();
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            String itemName = ShopUtils.getShopDisplayName(shop);
            meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + itemName);

            String itemKey = ShopUtils.getItemKey(shop);
            String trendStr =
                    net.craftnepal.market.managers.DynamicPriceManager.getTrendString(itemKey);
            int stock = ShopUtils.getShopStock(shop);

            List<String> lore = meta.getLore();
            if (lore == null)
                lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Price per item: " + ChatColor.GOLD
                    + EconomyUtils.format(shop.getPrice()) + " " + trendStr);
            lore.add(ChatColor.GRAY + "Stock available: "
                    + (stock > 0 ? ChatColor.GREEN : ChatColor.RED) + stock);
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "Owner: " + ChatColor.WHITE
                    + org.bukkit.Bukkit.getOfflinePlayer(shop.getOwner()).getName());

            meta.setLore(lore);
            displayItem.setItemMeta(meta);
        }
        inventory.setItem(13, displayItem);

        // Buying buttons
        int stock = ShopUtils.getShopStock(shop);

        inventory.setItem(29, makeBuyButton(1, stock));
        inventory.setItem(31, makeBuyButton(16, stock));
        inventory.setItem(33, makeBuyButton(64, stock));

        // Close button
        inventory.setItem(40, makeItem(Material.DARK_OAK_DOOR, ChatColor.RED + "Close Menu"));
    }

    private ItemStack makeBuyButton(int amount, int stock) {
        Material mat = (stock >= amount) ? ((amount == 64) ? Material.EMERALD_BLOCK : Material.EMERALD) : Material.RED_DYE;
        String color = (stock >= amount) ? ChatColor.GREEN + "" : ChatColor.RED + "";
        String title = color + ChatColor.BOLD + "Buy " + amount;
        String priceLine = ChatColor.GRAY + "Cost: " + ChatColor.GOLD + EconomyUtils.format(shop.getPrice() * amount);
        String stockLine = (stock >= amount) ? ChatColor.GRAY + "Status: " + ChatColor.GREEN + "In Stock"
                        : ChatColor.GRAY + "Status: " + ChatColor.RED + "Out of Stock";

        ItemStack item = makeItem(mat, title, "", priceLine, stockLine, "", ChatColor.YELLOW + "► Click to purchase");
        item.setAmount(Math.max(1, amount)); // set stack size for visual flair
        return item;
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta())
            return;

        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        if (name.equals("Close Menu")) {
            playerMenuUtility.getOwner().closeInventory();
        } else if (name.startsWith("Buy ")) {
            int amount = Integer.parseInt(name.split(" ")[1]);
            ShopUtils.processPurchase(playerMenuUtility.getOwner(), plotId, shopId, amount);
            // Refresh menu to show new stock
            setMenuItems();
        }
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
}
