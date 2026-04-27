package net.craftnepal.market.menus;

import me.kodysimpson.simpapi.exceptions.MenuManagerException;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import me.kodysimpson.simpapi.menu.Menu;
import me.kodysimpson.simpapi.menu.MenuManager;
import me.kodysimpson.simpapi.menu.PlayerMenuUtility;
import net.craftnepal.market.Entities.ChestShop;
import net.craftnepal.market.utils.ShopUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class AllItemsMenu extends Menu {
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 45;
    private String searchQuery = null;
    private Map<String, Integer> itemStocks = new HashMap<>();
    private Map<String, ChestShop> representativeShops = new HashMap<>();

    public AllItemsMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query;
    }

    @Override
    public String getMenuName() {
        if (searchQuery != null && !searchQuery.isEmpty()) {
            return ChatColor.BLUE + "Search: '" + searchQuery + "'";
        }
        return ChatColor.BLUE + "All Market Items";
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
        
        // Calculate global stock for each item dynamically
        itemStocks.clear();
        representativeShops.clear();
        Map<String, ChestShop> allShops = ShopUtils.getAllShops();
        for (ChestShop shop : allShops.values()) {
            String key = ShopUtils.getItemKey(shop);
            int stock = ShopUtils.getShopStock(shop);
            if (stock > 0) {
                itemStocks.put(key, itemStocks.getOrDefault(key, 0) + stock);
                representativeShops.putIfAbsent(key, shop);
            }
        }

        // Border
        ItemStack border = makeItem(Material.BLUE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        List<String> sortedKeys = new ArrayList<>(itemStocks.keySet());
        
        // Filter by search query if any
        if (searchQuery != null && !searchQuery.isEmpty()) {
            String lowerQuery = searchQuery.toLowerCase();
            sortedKeys.removeIf(key -> !key.replace("_", " ").toLowerCase().contains(lowerQuery));
        }

        // Sort alphabetically
        sortedKeys.sort(String::compareTo);

        int totalPages = (int) Math.ceil((double) sortedKeys.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, sortedKeys.size());

        for (int i = startIndex; i < endIndex; i++) {
            String key = sortedKeys.get(i);
            int count = itemStocks.get(key);
            ChestShop representative = representativeShops.get(key);

            ItemStack itemStack = new ItemStack(representative.getItem());
            ItemMeta meta = itemStack.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + ShopUtils.getShopDisplayName(representative));
            
            String itemKey = net.craftnepal.market.utils.ShopUtils.getItemKey(representative.getItem());
            double dynamicPrice = net.craftnepal.market.managers.DynamicPriceManager.getDynamicPrice(itemKey);
            String trendStr = net.craftnepal.market.managers.DynamicPriceManager.getTrendString(itemKey);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Global Stock: " + ChatColor.GREEN + count);
            if (dynamicPrice > 0) {
                lore.add(ChatColor.GRAY + "Market Price: " + ChatColor.GOLD + net.craftnepal.market.utils.EconomyUtils.format(dynamicPrice) + " " + trendStr);
            }
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "Click to find sellers");

            meta.setLore(lore);
            itemStack.setItemMeta(meta);
            inventory.addItem(itemStack);
        }

        // Navigation
        if (currentPage > 0) {
            inventory.setItem(48, makeItem(Material.ARROW, ChatColor.GREEN + "Previous Page"));
        }

        if (currentPage < totalPages - 1) {
            inventory.setItem(50, makeItem(Material.ARROW, ChatColor.GREEN + "Next Page"));
        }

        // Info
        inventory.setItem(49, makeItem(Material.BOOK,
                ChatColor.YELLOW + "Page Info",
                ChatColor.GRAY + "Current: " + ChatColor.YELLOW + (currentPage + 1),
                ChatColor.GRAY + "Total: " + ChatColor.YELLOW + totalPages));

        // Back
        inventory.setItem(53, makeItem(Material.BARRIER, ChatColor.RED + "Back to Main Menu"));
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

        if (displayName.equals("Previous Page")) {
            currentPage--;
            setMenuItems();
        } else if (displayName.equals("Next Page")) {
            currentPage++;
            setMenuItems();
        } else if (displayName.equals("Back to Main Menu")) {
            try {
                MenuManager.openMenu(ShopMainMenu.class, playerMenuUtility.getOwner());
            } catch (MenuManagerException | MenuManagerNotSetupException e) {
                e.printStackTrace();
            }
        } else if (!clickedItem.getType().equals(Material.BLUE_STAINED_GLASS_PANE) &&
                !clickedItem.getType().equals(Material.BOOK)) {
            
            // Get the display name to match back to our keys (a bit hacky but works with SimpAPI)
            String name = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
            String matchedKey = null;
            for (String key : representativeShops.keySet()) {
                if (ShopUtils.getShopDisplayName(representativeShops.get(key)).equals(name)) {
                    matchedKey = key;
                    break;
                }
            }

            if (matchedKey != null) {
                try {
                    PlotsSellingItemMenu nextMenu = new PlotsSellingItemMenu(playerMenuUtility);
                    nextMenu.setTargetProduct(matchedKey);
                    nextMenu.open();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
