package net.craftnepal.market.menus;

import me.kodysimpson.simpapi.menu.Menu;
import me.kodysimpson.simpapi.menu.PlayerMenuUtility;
import net.craftnepal.market.Entities.ChestShop;
import net.craftnepal.market.utils.ShopUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarketManagementMenu extends Menu {

    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 28;
    private List<ChestShop> outOfStockShops = new ArrayList<>();
    private Map<Integer, ChestShop> slotToShopMap = new HashMap<>();

    public MarketManagementMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
    }

    @Override
    public String getMenuName() {
        return ChatColor.BLUE + "Market Management";
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
        outOfStockShops.clear();
        slotToShopMap.clear();

        // Get out of stock shops for this player
        Map<String, ChestShop> allShops = ShopUtils.getAllShops();
        for (ChestShop shop : allShops.values()) {
            if (shop.getOwner() != null && shop.getOwner().equals(playerMenuUtility.getOwner().getUniqueId()) 
                    && !shop.isAdmin() && !shop.isBuyingShop()) {
                if (ShopUtils.getShopStock(shop) == 0) {
                    outOfStockShops.add(shop);
                }
            }
        }

        // Add Border
        ItemStack border = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 10; i++) inventory.setItem(i, border);
        inventory.setItem(17, border);
        inventory.setItem(18, border);
        inventory.setItem(26, border);
        inventory.setItem(27, border);
        inventory.setItem(35, border);
        inventory.setItem(36, border);
        for (int i = 44; i < 54; i++) inventory.setItem(i, border);

        // Add Out of stock items
        int totalPages = (int) Math.ceil((double) outOfStockShops.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, outOfStockShops.size());

        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            while ((slot % 9 == 0 || slot % 9 == 8) && slot < 44) {
                slot++;
            }
            if (slot >= 44) break;

            ChestShop shop = outOfStockShops.get(i);
            ItemStack item = new ItemStack(shop.getItem());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "Out of Stock: " + ShopUtils.getShopDisplayName(shop));
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Price: " + ChatColor.GOLD + net.craftnepal.market.utils.EconomyUtils.format(shop.getPrice()));
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to see location!");
            meta.setLore(lore);
            item.setItemMeta(meta);
            
            inventory.setItem(slot, item);
            slotToShopMap.put(slot, shop);
            slot++;
        }

        // Coming soon feature
        inventory.setItem(49, makeItem(Material.DIAMOND, 
            ChatColor.AQUA + "Plot Analytics (Coming Soon)", 
            ChatColor.GRAY + "Detailed stats about your sales",
            ChatColor.GRAY + "and earnings will appear here!"));

        // Navigation
        if (currentPage > 0) {
            inventory.setItem(48, makeItem(Material.ARROW, ChatColor.GREEN + "Previous Page"));
        }

        if (currentPage < totalPages - 1) {
            inventory.setItem(50, makeItem(Material.ARROW, ChatColor.GREEN + "Next Page"));
        }
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        int slot = event.getSlot();
        if (slotToShopMap.containsKey(slot)) {
            ChestShop shop = slotToShopMap.get(slot);
            String plotId = shop.getPlotId();
            if (plotId != null) {
                org.bukkit.entity.Player player = playerMenuUtility.getOwner();
                org.bukkit.Location origin = player.getLocation();
                org.bukkit.Location shopSpawn = net.craftnepal.market.utils.PlotUtils.getPlotSpawn(plotId);
                org.bukkit.Location tpLoc = shopSpawn != null ? shopSpawn : net.craftnepal.market.utils.PlotUtils.getPlotCenter(plotId);
                
                if (tpLoc != null) {
                    net.craftnepal.market.utils.SendMessage.sendPlayerMessage(player, "Teleporting to the shop in 5 seconds! Don't move..");
                    player.closeInventory();
                    
                    net.craftnepal.market.utils.TeleportUtils.scheduleTeleport(player, tpLoc, () -> {
                        if (!net.craftnepal.market.utils.MarketUtils.isInMarketArea(origin)) {
                            net.craftnepal.market.utils.PlayerUtils.saveLastLocation(player, origin);
                        }
                        net.craftnepal.market.utils.SendMessage.sendPlayerMessage(player, "Teleported to your out-of-stock shop!");
                        
                        // Highlight the shop
                        net.craftnepal.market.utils.RegionUtils.showVerticalParticleLine(player, shop.getLocation().clone().add(0, 2, 0), null, net.craftnepal.market.Market.getPlugin());
                    });
                }
            } else {
                net.craftnepal.market.utils.SendMessage.sendPlayerMessage(playerMenuUtility.getOwner(), "§cCould not find the plot for this shop.");
            }
            return;
        }

        String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

        if (displayName.equals("Previous Page")) {
            currentPage--;
            setMenuItems();
        } else if (displayName.equals("Next Page")) {
            currentPage++;
            setMenuItems();
        }
    }
}
