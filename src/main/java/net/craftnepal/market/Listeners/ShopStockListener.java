package net.craftnepal.market.Listeners;

import net.craftnepal.market.Entities.ChestShop;
import net.craftnepal.market.utils.DisplayUtils;
import net.craftnepal.market.utils.MarketUtils;
import net.craftnepal.market.utils.ShopUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

public class ShopStockListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        Location loc = event.getInventory().getLocation();
        if (loc == null || !MarketUtils.isInMarketArea(loc))
            return;

        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof org.bukkit.block.Container) {
            org.bukkit.block.Container container = (org.bukkit.block.Container) holder;
            if (container.getBlock().getType() == Material.BARREL) {
                refreshShopDisplay(loc);
            }
        }
    }

    // Hopper-based stock sync removed for performance.
    // Shop stock is updated arithmetically on buy/sell, and synced
    // physically when an owner closes the barrel inventory.

    private void refreshShopDisplay(Location loc) {
        ChestShop shop = ShopUtils.getShopAt(loc);
        if (shop != null) {
            ShopUtils.syncShopStockWithDatabase(shop);
            DisplayUtils.getInstance().updateDisplay(shop);
        }
    }
}
