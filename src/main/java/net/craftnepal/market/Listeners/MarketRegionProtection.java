package net.craftnepal.market.Listeners;

import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.subcommands.Bypass;
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

        if (!isActionAllowed(player, clickedBlock.getLocation())) {
            e.setCancelled(true);
            SendMessage.sendPlayerMessage(player, "&cYou are not allowed to interact here.");
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
            return owner != null && owner.equals(player.getUniqueId().toString());
        }

        // In market world but not in any plot
        return false;
    }
}
