package net.craftnepal.market.menus;

import me.kodysimpson.simpapi.exceptions.MenuManagerException;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import me.kodysimpson.simpapi.menu.Menu;
import me.kodysimpson.simpapi.menu.PlayerMenuUtility;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.*;
import org.bukkit.*;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class ShopListMenu extends Menu {
    private int currentPage = 0;
    private static final int PLOTS_PER_PAGE = 45;
    private final List<String> activePlots;

    public ShopListMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
        this.activePlots = PlotUtils.getActivePlotIds();
    }

    @Override
    public String getMenuName() {
        return ChatColor.BLUE + "Market Shops (Page " + (currentPage + 1) + ")";
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

        int totalPages = (int) Math.ceil((double) activePlots.size() / PLOTS_PER_PAGE);

        // Add plot items for current page
        int startIndex = currentPage * PLOTS_PER_PAGE;
        int endIndex = Math.min(startIndex + PLOTS_PER_PAGE, activePlots.size());

        for (int i = startIndex; i < endIndex; i++) {
            inventory.addItem(createPlotItem(activePlots.get(i)));
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
    public void handleMenu(InventoryClickEvent event) throws MenuManagerException, MenuManagerNotSetupException {
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
        else if (displayName.startsWith("Plot ")) {
            String plotId = displayName.replace("Plot ", "");
            new ShopItemListMenu(playerMenuUtility, Integer.parseInt(plotId)).open();
        }
    }

    private ItemStack createPlotItem(String plotId) {
        String ownerUUID = RegionData.get().getString("market.plots." + plotId + ".owner");
        ItemStack plotItem = PlayerUtils.getPlayerHead(UUID.fromString(ownerUUID));
        ItemMeta meta = plotItem.getItemMeta();

        String ownerName = "Unknown";
        if (ownerUUID != null) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID));
            ownerName = owner.getName() != null ? owner.getName() : "Unknown";
        }

        meta.setDisplayName(ChatColor.GREEN + "Plot " + plotId);

        ConfigurationSection shops = RegionData.get().getConfigurationSection("market.plots." + plotId + ".shops");
        int shopCount = shops != null ? shops.getKeys(false).size() : 0;

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Owner: " + ChatColor.YELLOW + ownerName);
        lore.add(ChatColor.GRAY + "Shops: " + ChatColor.YELLOW + shopCount);
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Click to view items");

        meta.setLore(lore);
        plotItem.setItemMeta(meta);

        return plotItem;
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