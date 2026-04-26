package net.craftnepal.market.menus;

import me.kodysimpson.simpapi.exceptions.MenuManagerException;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import me.kodysimpson.simpapi.menu.Menu;
import me.kodysimpson.simpapi.menu.MenuManager;
import me.kodysimpson.simpapi.menu.PlayerMenuUtility;
import net.craftnepal.market.Market;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;

public class ShopMainMenu extends Menu {

    public ShopMainMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
    }

    @Override
    public String getMenuName() {
        return ChatColor.BLUE + "Market Menu";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public boolean cancelAllClicks() {
        return true;
    }

    @Override
    public void setMenuItems() {
        inventory.clear();

        // Border
        ItemStack border = makeItem(Material.BLUE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 18; i < 27; i++) inventory.setItem(i, border);
        inventory.setItem(9, border);
        inventory.setItem(17, border);

        // Browse All Items
        ItemStack browse = makeItem(Material.CHEST, 
                ChatColor.GREEN + "Browse All Items", 
                ChatColor.GRAY + "View every item currently",
                ChatColor.GRAY + "being sold in the market.");
        inventory.setItem(11, browse);

        // Search Item
        ItemStack search = makeItem(Material.NAME_TAG, 
                ChatColor.YELLOW + "Search for an Item", 
                ChatColor.GRAY + "Looking for something specific?",
                ChatColor.GRAY + "Click here to search by name.");
        inventory.setItem(15, search);

        // Close
        ItemStack close = makeItem(Material.BARRIER, ChatColor.RED + "Close Menu");
        inventory.setItem(22, close);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        Player player = playerMenuUtility.getOwner();

        if (name.equals("Browse All Items")) {
            try {
                MenuManager.openMenu(AllItemsMenu.class, player);
            } catch (MenuManagerException | MenuManagerNotSetupException e) {
                e.printStackTrace();
            }
        } else if (name.equals("Search for an Item")) {
            player.closeInventory();
            net.craftnepal.market.Listeners.SearchListener.addSearcher(player);
        } else if (name.equals("Close Menu")) {
            player.closeInventory();
        }
    }
}
