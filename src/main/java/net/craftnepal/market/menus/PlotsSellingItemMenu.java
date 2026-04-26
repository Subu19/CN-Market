package net.craftnepal.market.menus;

import me.kodysimpson.simpapi.exceptions.MenuManagerException;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import me.kodysimpson.simpapi.menu.Menu;
import me.kodysimpson.simpapi.menu.MenuManager;
import me.kodysimpson.simpapi.menu.PlayerMenuUtility;
import net.craftnepal.market.Entities.ChestShop;
import net.craftnepal.market.Market;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.*;
import org.bukkit.*;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class PlotsSellingItemMenu extends Menu {
    private Material targetMaterial;
    private int currentPage = 0;
    private static final int PLOTS_PER_PAGE = 45;
    private List<String> plotsSellingItem = new ArrayList<>();

    public PlotsSellingItemMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
    }

    public void setTargetMaterial(Material material) {
        this.targetMaterial = material;
        // Find all plots selling this item with stock > 0
        Set<String> plotSet = new HashSet<>();
        Map<String, ChestShop> allShops = ShopUtils.getAllShops();
        for (ChestShop shop : allShops.values()) {
            if (shop.getItem() == material && ShopUtils.getShopStock(shop) > 0) {
                // Find plot ID from shop location
                String plotId = PlotUtils.getPlotIdByLocation(shop.getLocation());
                if (plotId != null) {
                    plotSet.add(plotId);
                }
            }
        }
        this.plotsSellingItem.addAll(plotSet);
    }

    @Override
    public String getMenuName() {
        return ChatColor.BLUE + "Sellers for " + targetMaterial.name();
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

        // Border
        ItemStack border = makeItem(Material.BLUE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        int totalPages = (int) Math.ceil((double) plotsSellingItem.size() / PLOTS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        int startIndex = currentPage * PLOTS_PER_PAGE;
        int endIndex = Math.min(startIndex + PLOTS_PER_PAGE, plotsSellingItem.size());

        for (int i = startIndex; i < endIndex; i++) {
            String plotId = plotsSellingItem.get(i);
            inventory.addItem(createPlotItem(plotId));
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
        inventory.setItem(53, makeItem(Material.BARRIER, ChatColor.RED + "Back to Items"));
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
        } else if (displayName.equals("Back to Items")) {
            try {
                // Just go back to the browse menu (without search query for simplicity, or we could pass it)
                MenuManager.openMenu(AllItemsMenu.class, playerMenuUtility.getOwner());
            } catch (MenuManagerException | MenuManagerNotSetupException e) {
                e.printStackTrace();
            }
        } else if (displayName.startsWith("Plot ")) {
            String plotId = displayName.replace("Plot ", "");
            
            Location shopSpawn = PlotUtils.getPlotSpawn(plotId);
            Location tpLoc = shopSpawn != null ? shopSpawn : PlotUtils.getPlotCenter(plotId);
            
            if (tpLoc != null) {
                SendMessage.sendPlayerMessage(playerMenuUtility.getOwner(), "Teleporting to the shop in 5 seconds! Don't move..");
                playerMenuUtility.getOwner().closeInventory();
                
                TeleportUtils.scheduleTeleport(playerMenuUtility.getOwner(), tpLoc, () -> {
                    SendMessage.sendPlayerMessage(playerMenuUtility.getOwner(), "Teleported to shop selling: " +
                            ChatColor.YELLOW + targetMaterial.name());
                    
                    List<ChestShop> allShops = ShopUtils.getPlotShopsByItemName(plotId, targetMaterial.name());
                    for (ChestShop shop : allShops) {
                        RegionUtils.showVerticalParticleLine(playerMenuUtility.getOwner(), shop.getLocation().clone().add(0, 2, 0), null, Market.getPlugin());
                    }
                });
            }
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

        // Find cheapest price in this plot for this item
        double cheapest = Double.MAX_VALUE;
        int totalStock = 0;
        List<ChestShop> plotShops = ShopUtils.getPlotShopsByItemName(plotId, targetMaterial.name());
        for (ChestShop shop : plotShops) {
            int stock = ShopUtils.getShopStock(shop);
            if (stock > 0) {
                totalStock += stock;
                if (shop.getPrice() < cheapest) cheapest = shop.getPrice();
            }
        }

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Owner: " + ChatColor.YELLOW + ownerName);
        lore.add(ChatColor.GRAY + "In Stock: " + ChatColor.GREEN + totalStock);
        if (cheapest != Double.MAX_VALUE) {
            String trendStr = net.craftnepal.market.managers.DynamicPriceManager.getTrendString(targetMaterial);
            lore.add(ChatColor.GRAY + "Price: " + ChatColor.GOLD + EconomyUtils.format(cheapest) + " " + trendStr);
        }
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Click to teleport");

        meta.setLore(lore);
        plotItem.setItemMeta(meta);

        return plotItem;
    }
}
