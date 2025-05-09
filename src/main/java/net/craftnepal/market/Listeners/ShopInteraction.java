package net.craftnepal.market.Listeners;

import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.MarketUtils;
import net.craftnepal.market.utils.PlotUtils;
import net.craftnepal.market.utils.RegionUtils;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ShopInteraction implements Listener {
    @EventHandler
    public void onChestInteraction(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (event.getAction() != Action.LEFT_CLICK_BLOCK || event.getClickedBlock() == null) return;

        Block clickedBlock = event.getClickedBlock();
        Material blockType = clickedBlock.getType();

        if (blockType != Material.CHEST && blockType != Material.TRAPPED_CHEST) return;

        Location chestLocation = clickedBlock.getLocation();

        // First check if this is inside market area
        if (!MarketUtils.isInMarketArea(chestLocation)) {
            return; // Do nothing if not in market area
        }

        // Check if chest is inside the player's plot
        String plot = PlotUtils.getPlotIdByLocation(chestLocation);
        String playerPlot = null;

        if(PlotUtils.getPlotOwner(plot).equals(uuid.toString())){
            playerPlot = plot;
        }

        if (playerPlot == null) {
            SendMessage.sendPlayerMessage(player,"§cYou must click a chest inside your own plot.");
            event.setCancelled(true);
            return;
        }

        // Check the item in hand
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            SendMessage.sendPlayerMessage(player,"§cYou must be holding an item to create a shop.");
            event.setCancelled(true);
            return;
        }

        // Check if this chest is already a shop
        ConfigurationSection shops = RegionData.get().getConfigurationSection("market.plots." + playerPlot + ".shops");
        if (shops != null) {
            for (String shopId : shops.getKeys(false)) {
                Location shopLoc = RegionData.get().getLocation("market.plots." + playerPlot + ".shops." + shopId + ".location");
                if (shopLoc != null && shopLoc.equals(chestLocation)) {
                    SendMessage.sendPlayerMessage(player,"§cThis chest is already a shop!");
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Generate a new shop ID
        String shopId = UUID.randomUUID().toString();

        // Save shop data to YAML
        RegionData.get().set("market.plots." + playerPlot + ".shops." + shopId + ".location", chestLocation);
        RegionData.get().set("market.plots." + playerPlot + ".shops." + shopId + ".item", item.getType().toString());
        RegionData.get().set("market.plots." + playerPlot + ".shops." + shopId + ".owner", uuid.toString());

        // Save the file
        RegionData.save();

        SendMessage.sendPlayerMessage(player,"§aShop created successfully!");
        event.setCancelled(true);
    }
    @EventHandler
    public void onChestBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material blockType = block.getType();

        if (blockType != Material.CHEST && blockType != Material.TRAPPED_CHEST) return;

        Location chestLocation = block.getLocation();

        // First check if this is inside market area
        if (!MarketUtils.isInMarketArea(chestLocation)) {
            return; // Do nothing if not in market area
        }

        // Check all plots for this chest
        ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");
        if (plots != null) {
            for (String plot : plots.getKeys(false)) {
                ConfigurationSection shops = RegionData.get().getConfigurationSection("market.plots." + plot + ".shops");
                if (shops != null) {
                    for (String shopId : shops.getKeys(false)) {
                        Location shopLoc = RegionData.get().getLocation("market.plots." + plot + ".shops." + shopId + ".location");
                        if (shopLoc != null && shopLoc.equals(chestLocation)) {
                            // Check if the breaker is the owner or has permission
                            Player player = event.getPlayer();
                            String owner = RegionData.get().getString("market.plots." + plot + ".shops." + shopId + ".owner");

                            if (owner != null && owner.equals(player.getUniqueId().toString())) {
                                // Owner breaking their own shop - remove it
                                RegionData.get().set("market.plots." + plot + ".shops." + shopId, null);
                                RegionData.save();
                                SendMessage.sendPlayerMessage(player,"§aShop removed successfully.");
                            } else {
                                // Someone else trying to break the shop
                                SendMessage.sendPlayerMessage(player,"§cYou cannot break someone else's shop!");
                                event.setCancelled(true);
                            }
                            return;
                        }
                    }
                }
            }
        }
    }
}
