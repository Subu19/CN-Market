package net.craftnepal.market.menus;

import me.kodysimpson.simpapi.menu.Menu;
import me.kodysimpson.simpapi.menu.PlayerMenuUtility;
import net.craftnepal.market.utils.*;
import org.bukkit.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ShopItemListMenu extends Menu {

    private final int plotId;
    private int currentPage = 0;
    private final Map<Material, Integer> itemCounts;
    private static final int ITEMS_PER_PAGE = 45;

    public ShopItemListMenu(PlayerMenuUtility playerMenuUtility, int plotId) {
        super(playerMenuUtility);
        this.plotId = plotId;
        this.itemCounts = ShopUtils.getAllShopItemsAndCountsByPlotID(String.valueOf(plotId));
    }

    @Override
    public String getMenuName() {
        return ChatColor.BLUE + "Items For Sale - Plot " + plotId;
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public boolean cancelAllClicks() {
        return true;
    }

    @Override
    public void setMenuItems() {
        inventory.clear();

        // Add border glass panes
        ItemStack border = makeItem(Material.BLUE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
        }
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, border);
        }

        // Calculate pagination
        List<Material> materials = new ArrayList<>(itemCounts.keySet());
        int totalPages = (int) Math.ceil((double) materials.size() / ITEMS_PER_PAGE);

        // Add items for current page
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, materials.size());

        for (int i = startIndex; i < endIndex; i++) {
            Material material = materials.get(i);
            int count = itemCounts.get(material);

            ItemStack itemStack = new ItemStack(material);
            ItemMeta meta = itemStack.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + material.name());
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "In Stock: " + ChatColor.GREEN + count,
                    ChatColor.DARK_GRAY + "Click to teleport to shop"
            ));
            itemStack.setItemMeta(meta);

            inventory.addItem(itemStack);
        }

        // Add navigation items
        if (currentPage > 0) {
            ItemStack back = makeItem(Material.ARROW, ChatColor.GREEN + "Previous Page");
            inventory.setItem(48, back);
        }

        if (currentPage < totalPages - 1) {
            ItemStack next = makeItem(Material.ARROW, ChatColor.GREEN + "Next Page");
            inventory.setItem(50, next);
        }

        // Add page info
        ItemStack pageInfo = makeItem(Material.BOOK,
                ChatColor.YELLOW + "Page Info",
                ChatColor.GRAY + "Current: " + ChatColor.YELLOW + (currentPage + 1),
                ChatColor.GRAY + "Total: " + ChatColor.YELLOW + totalPages);
        inventory.setItem(49, pageInfo);

        // Add close button
        ItemStack close = makeItem(Material.BARRIER, ChatColor.RED + "Close Menu");
        inventory.setItem(53, close);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

        if (displayName.equals("Previous Page")) {
            currentPage--;
            setMenuItems();
        }
        else if (displayName.equals("Next Page")) {
            currentPage++;
            setMenuItems();
        }
        else if (displayName.equals("Close Menu")) {
            playerMenuUtility.getOwner().closeInventory();
        }
        else if (!clickedItem.getType().equals(Material.BLUE_STAINED_GLASS_PANE) &&
                !clickedItem.getType().equals(Material.BOOK)) {
            // Handle item click
            Material clickedMaterial = clickedItem.getType();
            Location shopLocation = PlotUtils.getPlotCenter(String.valueOf(plotId));

            if (shopLocation != null) {
                playerMenuUtility.getOwner().teleport(shopLocation);
                playerMenuUtility.getOwner().sendMessage(ChatColor.GREEN + "Teleported to shop selling: " +
                        ChatColor.YELLOW + clickedMaterial.name());
            } else {
                playerMenuUtility.getOwner().sendMessage(ChatColor.RED + "No shop found selling this item.");
            }
        }
    }

    public ItemStack makeItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        if (lore.length > 0) {
            meta.setLore(Arrays.asList(lore));
        }

        item.setItemMeta(meta);
        return item;
    }

}