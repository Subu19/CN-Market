package net.craftnepal.market.Listeners;

import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.subcommands.admin.Bypass;
import net.craftnepal.market.utils.MarketUtils;
import net.craftnepal.market.utils.PlotUtils;
import net.craftnepal.market.utils.RegionUtils;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class MarketRegionProtection implements Listener {
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        Block clickedBlock = e.getClickedBlock();

        if (clickedBlock == null) return;

        // Allow interaction if it's a shop
        if (net.craftnepal.market.utils.ShopUtils.isShopLocation(clickedBlock.getLocation())) {
            return;
        }

        if (!isActionAllowed(player, clickedBlock.getLocation())) {
            e.setCancelled(true);
            SendMessage.sendPlayerMessage(player, "&cYou are not allowed to interact here.");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (!isActionAllowed(e.getPlayer(), e.getBlock().getLocation())) {
            e.setCancelled(true);
            SendMessage.sendPlayerMessage(e.getPlayer(), "&cYou are not allowed to build here.");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (!isActionAllowed(e.getPlayer(), e.getBlock().getLocation())) {
            e.setCancelled(true);
            SendMessage.sendPlayerMessage(e.getPlayer(), "&cYou are not allowed to build here.");
        }
    }
    private boolean isActionAllowed(Player player, Location location) {
        if (Bypass.bypassPlayers.containsKey(player.getUniqueId())) {
            return true;
        }

        if (!MarketUtils.isInMarketArea(location)) {
            return true;
        }

        // Check if inside any plot
        String plot = PlotUtils.getPlotIdByLocation(location);
        if (plot != null) {
            String owner = PlotUtils.getPlotOwner(plot);
            if (owner != null && owner.equals(player.getUniqueId().toString())) {
                return true;
            }
            java.util.List<String> members = net.craftnepal.market.files.RegionData.get().getStringList("market.plots." + plot + ".members");
            return members.contains(player.getUniqueId().toString());
        }

        // In market world but not in any plot
        return false;
    }
}
