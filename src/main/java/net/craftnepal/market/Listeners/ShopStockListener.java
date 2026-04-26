package net.craftnepal.market.Listeners;

import net.craftnepal.market.Entities.ChestShop;
import net.craftnepal.market.utils.DisplayUtils;
import net.craftnepal.market.utils.MarketUtils;
import net.craftnepal.market.utils.ShopUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent event) {
        // Check source inventory (item leaving)
        refreshInventory(event.getSource());
        // Check destination inventory (item arriving)
        refreshInventory(event.getDestination());
    }

    private void refreshInventory(org.bukkit.inventory.Inventory inv) {
        Location loc = inv.getLocation();
        if (loc == null || !MarketUtils.isInMarketArea(loc))
            return;

        InventoryHolder holder = inv.getHolder();
        if (holder instanceof org.bukkit.block.Container) {
            org.bukkit.block.Container container = (org.bukkit.block.Container) holder;
            if (container.getBlock().getType() == Material.BARREL) {
                refreshShopDisplay(loc);
            }
        }
    }

    private void refreshShopDisplay(Location loc) {
        ChestShop shop = ShopUtils.getShopAt(loc);
        if (shop != null) {
            DisplayUtils.getInstance().updateDisplay(shop);
        }
    }
}
