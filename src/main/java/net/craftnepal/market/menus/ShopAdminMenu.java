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

public class ShopAdminMenu extends Menu {

    private final String plotId;
    private final String shopId;
    private final ChestShop shop;

    public ShopAdminMenu(PlayerMenuUtility playerMenuUtility, String plotId, String shopId) {
        super(playerMenuUtility);
        this.plotId = plotId;
        this.shopId = shopId;
        this.shop = ShopUtils.getShop(plotId, shopId);
    }

    @Override
    public String getMenuName() {
        return ChatColor.DARK_RED + "Shop Management";
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

        // Outer border (Red for Admin)
        ItemStack outerBorder = makeItem(Material.RED_STAINED_GLASS_PANE, " ");
        for (int i : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 41, 42, 43, 44}) {
            inventory.setItem(i, outerBorder);
        }

        // Inner border
        ItemStack innerBorder = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i : new int[]{10, 11, 12, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 32, 33, 34}) {
            inventory.setItem(i, innerBorder);
        }

        if (shop == null) {
            inventory.setItem(22, makeItem(Material.BARRIER, ChatColor.RED + "Shop Error", ChatColor.GRAY + "Shop data not found!"));
            return;
        }

        // Show the actual item in the center with detailed stats
        ItemStack displayItem = shop.getItem().clone();
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            String itemName = ShopUtils.getShopDisplayName(shop);
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + itemName + " - Stats");
            
            String itemKey = ShopUtils.getItemKey(shop);
            String trendStr = net.craftnepal.market.managers.DynamicPriceManager.getTrendString(itemKey);
            int stock = ShopUtils.getShopStock(shop);
            
            List<String> lore = meta.getLore();
            if (lore == null) lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Current Price: " + ChatColor.YELLOW + EconomyUtils.format(shop.getPrice()) + " " + trendStr);
            lore.add(ChatColor.GRAY + "Current Stock: " + ChatColor.GREEN + stock);
            lore.add("");
            lore.add(ChatColor.GRAY + "Shop ID: " + ChatColor.DARK_GRAY + shopId);
            lore.add(ChatColor.GRAY + "Owner: " + ChatColor.WHITE + org.bukkit.Bukkit.getOfflinePlayer(shop.getOwner()).getName());
            
            meta.setLore(lore);
            displayItem.setItemMeta(meta);
        }
        inventory.setItem(13, displayItem);

        // Remove Shop Button - Only show to owner or admins
        if (playerMenuUtility.getOwner().hasPermission("market.admin") || (shop.getOwner() != null && shop.getOwner().equals(playerMenuUtility.getOwner().getUniqueId()))) {
            inventory.setItem(31, makeItem(Material.TNT, ChatColor.RED + "" + ChatColor.BOLD + "REMOVE SHOP", 
                    ChatColor.GRAY + "Click to permanently remove", 
                    ChatColor.GRAY + "this shop from the market.",
                    "",
                    ChatColor.DARK_RED + "Warning: This cannot be undone!"));
        }

        // Close button
        inventory.setItem(40, makeItem(Material.DARK_OAK_DOOR, ChatColor.RED + "Close Menu"));
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        if (name.equals("Close Menu")) {
            playerMenuUtility.getOwner().closeInventory();
        } else if (name.equals("REMOVE SHOP")) {
            if (playerMenuUtility.getOwner().hasPermission("market.admin") || (shop.getOwner() != null && shop.getOwner().equals(playerMenuUtility.getOwner().getUniqueId()))) {
                ShopUtils.removeShop(plotId, shopId);
                playerMenuUtility.getOwner().closeInventory();
                net.craftnepal.market.utils.SendMessage.sendPlayerMessage(playerMenuUtility.getOwner(), "§aShop removed successfully.");
            } else {
                net.craftnepal.market.utils.SendMessage.sendPlayerMessage(playerMenuUtility.getOwner(), "§cYou do not have permission to remove this shop.");
                playerMenuUtility.getOwner().closeInventory();
            }
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
