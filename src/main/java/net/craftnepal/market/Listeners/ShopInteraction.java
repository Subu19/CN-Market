package net.craftnepal.market.Listeners;

import net.craftnepal.market.Entities.ChestShop;
import net.craftnepal.market.Market;
import net.craftnepal.market.files.PriceData;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.*;
import net.craftnepal.market.menus.*;
import org.bukkit.*;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.UUID;

public class ShopInteraction implements Listener {
    private final DisplayUtils displayUtils;

    public ShopInteraction() {
        this.displayUtils = DisplayUtils.getInstance();
    }

    @EventHandler
    public void onChestInteraction(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Check if the player is not in survival mode.
        if (player.getGameMode() != GameMode.SURVIVAL)
            return;

        // Check for block and click
        if (event.getAction() != Action.LEFT_CLICK_BLOCK || event.getClickedBlock() == null)
            return;

        Block clickedBlock = event.getClickedBlock();
        Material blockType = clickedBlock.getType();

        if (blockType != Material.BARREL) {
            return;
        }

        Location chestLocation = clickedBlock.getLocation();

        // First check if this is inside market area
        if (!MarketUtils.isInMarketArea(chestLocation)) {
            return; // Do nothing if not in market area
        }

        // Check if chest is inside a plot
        String plot = PlotUtils.getPlotIdByLocation(chestLocation);
        if (plot == null) {
            SendMessage.sendPlayerMessage(player, "§cThis barrel is not inside a market plot.");
            event.setCancelled(true);
            return;
        }

        // Members can also see their own shop stats (treated as co-owners)
        String owner = PlotUtils.getPlotOwner(plot);
        if (owner == null || !owner.equals(uuid.toString())) {
            // check members
            java.util.List<String> members = RegionData.get().getStringList("market.plots." + plot + ".members");
            if (!player.hasPermission("market.admin") && !members.contains(uuid.toString())) {
                SendMessage.sendPlayerMessage(player, "§cYou can only create shops in your own plot.");
                event.setCancelled(true);
                return;
            }
        }

        String playerPlot = plot;

        // Check if this chest is already a shop
        ConfigurationSection shops = RegionData.get().getConfigurationSection("market.plots." + playerPlot + ".shops");
        if (shops != null) {
            for (String shopId : shops.getKeys(false)) {
                Location shopLoc = LocationUtils.loadLocation(RegionData.get(),
                        "market.plots." + playerPlot + ".shops." + shopId + ".location");
                if (shopLoc != null && shopLoc.equals(chestLocation)) {
                    String shopOwner = RegionData.get().getString("market.plots." + playerPlot + ".shops." + shopId + ".owner");
                    java.util.List<String> members = RegionData.get().getStringList("market.plots." + playerPlot + ".members");
                    if (player.hasPermission("market.admin") || (shopOwner != null && shopOwner.equals(player.getUniqueId().toString())) || members.contains(player.getUniqueId().toString())) {
                        showShopStats(player, playerPlot, shopId);
                    } else {
                        SendMessage.sendPlayerMessage(player, "§cThis chest is already a shop!");
                    }
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Check the item in hand
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            SendMessage.sendPlayerMessage(player, "§cYou must be holding an item to create a shop.");
            event.setCancelled(true);
            return;
        }

        // Open creation GUI
        try {
            new ShopCreateMenu(me.kodysimpson.simpapi.menu.MenuManager.getPlayerMenuUtility(player), playerPlot, chestLocation, item).open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        event.setCancelled(true);
    }


    @EventHandler
    public void onVisitorShopClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null)
            return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock.getType() != Material.BARREL)
            return;

        Location chestLocation = clickedBlock.getLocation();

        if (!MarketUtils.isInMarketArea(chestLocation)) {
            return;
        }

        String plot = PlotUtils.getPlotIdByLocation(chestLocation);
        if (plot == null) return;

        ConfigurationSection shops = RegionData.get().getConfigurationSection("market.plots." + plot + ".shops");
        if (shops == null) return;

        for (String shopId : shops.getKeys(false)) {
            Location shopLoc = LocationUtils.loadLocation(RegionData.get(), "market.plots." + plot + ".shops." + shopId + ".location");
            if (shopLoc != null && shopLoc.equals(chestLocation)) {
                String owner = RegionData.get().getString("market.plots." + plot + ".shops." + shopId + ".owner");
                
                // Owner and members can open the barrel freely
                java.util.List<String> members = RegionData.get().getStringList("market.plots." + plot + ".members");
                if (owner != null && (owner.equals(player.getUniqueId().toString()) || members.contains(player.getUniqueId().toString()))) {
                    return;
                }

                // It's a visitor! Cancel opening the barrel.
                event.setCancelled(true);

                // Open Buyer GUI
                try {
                    new ShopBuyerMenu(me.kodysimpson.simpapi.menu.MenuManager.getPlayerMenuUtility(player), plot, shopId).open();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
        }
    }

    private void showShopStats(Player player, String plotId, String shopId) {
        // Open Admin GUI
        try {
            new ShopAdminMenu(me.kodysimpson.simpapi.menu.MenuManager.getPlayerMenuUtility(player), plotId, shopId).open();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onChestBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material blockType = block.getType();

        if (blockType != Material.BARREL)
            return;

        Location chestLocation = block.getLocation();

        // First check if this is inside market area
        if (!MarketUtils.isInMarketArea(chestLocation)) {
            return; // Do nothing if not in market area
        }

        // Get plot at chest location
        String plot = PlotUtils.getPlotIdByLocation(chestLocation);
        if (plot == null) return;

        ConfigurationSection shops = RegionData.get().getConfigurationSection("market.plots." + plot + ".shops");
        if (shops == null) return;

        for (String shopId : shops.getKeys(false)) {
            Location shopLoc = LocationUtils.loadLocation(RegionData.get(), "market.plots." + plot + ".shops." + shopId + ".location");
            if (shopLoc != null && shopLoc.equals(chestLocation)) {
                Player player = event.getPlayer();
                String owner = RegionData.get().getString("market.plots." + plot + ".shops." + shopId + ".owner");

                if (player.hasPermission("market.admin") || (owner != null && owner.equals(player.getUniqueId().toString()))) {
                    ShopUtils.removeShop(plot, shopId);
                    SendMessage.sendPlayerMessage(player, "§aShop removed successfully.");
                } else {
                    SendMessage.sendPlayerMessage(player, "§cYou cannot break someone else's shop!");
                    event.setCancelled(true);
                }
                return;
            }
        }
    }

}
