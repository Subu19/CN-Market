package net.craftnepal.market.Listeners;

import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.subcommands.Bypass;
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

        ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");
        if (plots != null) {
            for (String plot : plots.getKeys(false)) {
                Location minPos = RegionData.get().getLocation("market.plots." + plot + ".posMin");
                Location maxPos = RegionData.get().getLocation("market.plots." + plot + ".posMax");

                if (RegionUtils.isLocationInsideRegion(location, minPos, maxPos)) {
                    String owner = RegionData.get().getString("market.plots." + plot + ".owner");
                    return owner != null && owner.equals(player.getUniqueId().toString());
                }
            }
        }

        Location marketMin = RegionData.get().getLocation("market.posMin");
        Location marketMax = RegionData.get().getLocation("market.posMax");

        return !RegionUtils.isLocationInsideRegion(location, marketMin, marketMax);
    }
}
