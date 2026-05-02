package net.craftnepal.market.Listeners;

import net.craftnepal.market.subcommands.admin.Bypass;
import net.craftnepal.market.utils.MarketUtils;
import net.craftnepal.market.utils.PlotUtils;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;
import java.util.Objects;

/**
 * Comprehensive grief protection for the Market world.
 *
 * Core rule: ANY modification to a block is only permitted inside the plot owned
 * by the acting player (or a member of that plot). Secondary modification vectors
 * — explosions, pistons, fire spread, liquid flow — are also restricted so that
 * one player can never damage another player's plot, even indirectly (e.g. a TNT
 * cannon fired from their own plot into a neighbour's plot).
 *
 * Player-health protection (damage, hunger, mob spawning) is intentionally NOT
 * handled here — that is delegated to WorldGuard + Paper.
 */
public class MarketRegionProtection implements Listener {

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** Returns true when the location is in the market world. */
    private boolean isMarketWorld(Location location) {
        return MarketUtils.isInMarketArea(location);
    }

    /**
     * Core permission check for a player wanting to modify a block.
     *
     * Allow if:
     *  - Player has bypass
     *  - Location is outside the market world entirely
     *  - Location is inside a plot the player owns or is a member of
     *
     * Deny (and optionally message the player) if:
     *  - In market world but on a pathway / spawn area
     *  - In market world but inside another player's plot
     *  - In market world but above the configured max-height
     */
    private boolean isActionAllowed(Player player, Location location) {
        if (Bypass.bypassPlayers.containsKey(player.getUniqueId())) {
            return true;
        }
        if (!isMarketWorld(location)) {
            return true;
        }

        // Max-height restriction
        int maxHeight = net.craftnepal.market.Market.getMainConfig().getInt("market-world.max-height", 255);
        if (location.getBlockY() > maxHeight) {
            SendMessage.sendPlayerMessage(player, "&cYou cannot build above height " + maxHeight + "!");
            return false;
        }

        // Must be inside own plot (or be a member)
        String plot = PlotUtils.getPlotIdByLocation(location);
        if (plot == null) {
            // Pathway or spawn — no one builds here
            return false;
        }

        String owner = PlotUtils.getPlotOwner(plot);
        if (owner != null && owner.equals(player.getUniqueId().toString())) {
            return true;
        }
        List<String> members = net.craftnepal.market.files.RegionData.get()
                .getStringList("market.plots." + plot + ".members");
        return members.contains(player.getUniqueId().toString());
    }

    // ─── Direct Player Actions ────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        Block clickedBlock = e.getClickedBlock();

        if (clickedBlock == null) return;

        // Allow interacting with configurable global blocks (like Ender Chests) anywhere
        if (e.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            List<String> allowedBlocks = net.craftnepal.market.Market.getMainConfig().getStringList("market-world.allowed-interact-blocks");
            if (allowedBlocks.contains(clickedBlock.getType().name())) {
                return;
            }
        }

        // Always allow interacting with shop blocks (chest-shops, etc.)
        if (net.craftnepal.market.utils.ShopUtils.isShopLocation(clickedBlock.getLocation())) {
            return;
        }

        if (!isActionAllowed(player, clickedBlock.getLocation())) {
            e.setCancelled(true);
            SendMessage.sendPlayerMessage(player, "&cYou are not allowed to interact here.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (!isActionAllowed(e.getPlayer(), e.getBlock().getLocation())) {
            e.setCancelled(true);
            SendMessage.sendPlayerMessage(e.getPlayer(), "&cYou are not allowed to build here.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (!isActionAllowed(e.getPlayer(), e.getBlock().getLocation())) {
            e.setCancelled(true);
            SendMessage.sendPlayerMessage(e.getPlayer(), "&cYou are not allowed to build here.");
        }
    }

    // ─── Explosion Protection ─────────────────────────────────────────────────
    //
    // Handles TNT cannons: a player places TNT on their own plot, lights it, and
    // the explosion radius crosses into a neighbour's plot. We strip any affected
    // block that is NOT in the same plot as the explosion origin.

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        if (!isMarketWorld(e.getLocation())) return;

        String sourcePlot = PlotUtils.getPlotIdByLocation(e.getLocation());

        // Explosion in a pathway / spawn zone → no block damage at all
        if (sourcePlot == null) {
            e.blockList().clear();
            return;
        }

        // Only blocks within the same plot as the explosion source are affected
        e.blockList().removeIf(block ->
                !Objects.equals(sourcePlot, PlotUtils.getPlotIdByLocation(block.getLocation())));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        if (!isMarketWorld(e.getBlock().getLocation())) return;

        String sourcePlot = PlotUtils.getPlotIdByLocation(e.getBlock().getLocation());

        if (sourcePlot == null) {
            e.blockList().clear();
            return;
        }

        e.blockList().removeIf(block ->
                !Objects.equals(sourcePlot, PlotUtils.getPlotIdByLocation(block.getLocation())));
    }

    // ─── Piston Protection ────────────────────────────────────────────────────
    //
    // Prevents pistons from pushing or pulling blocks across plot boundaries.

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (!isMarketWorld(e.getBlock().getLocation())) return;

        String pistonPlot = PlotUtils.getPlotIdByLocation(e.getBlock().getLocation());

        for (Block pushed : e.getBlocks()) {
            // Destination = block's current location shifted one step in the piston direction
            Location dest = pushed.getRelative(e.getDirection()).getLocation();
            if (!Objects.equals(pistonPlot, PlotUtils.getPlotIdByLocation(dest))) {
                e.setCancelled(true);
                return;
            }
        }

        // Also check the leading edge block position that will become empty/pushed into
        if (!e.getBlocks().isEmpty()) {
            Block last = e.getBlocks().get(e.getBlocks().size() - 1);
            Location dest = last.getRelative(e.getDirection()).getLocation();
            if (!Objects.equals(pistonPlot, PlotUtils.getPlotIdByLocation(dest))) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (!isMarketWorld(e.getBlock().getLocation())) return;

        String pistonPlot = PlotUtils.getPlotIdByLocation(e.getBlock().getLocation());

        for (Block pulled : e.getBlocks()) {
            if (!Objects.equals(pistonPlot, PlotUtils.getPlotIdByLocation(pulled.getLocation()))) {
                e.setCancelled(true);
                return;
            }
        }
    }

    // ─── Fire Spread Protection ───────────────────────────────────────────────
    //
    // Prevents fire from jumping between plots or from a plot onto pathways.

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent e) {
        if (!isMarketWorld(e.getBlock().getLocation())) return;

        Block ignitingBlock = e.getIgnitingBlock();
        if (ignitingBlock == null) {
            // Unknown ignition source in market world — cancel to be safe
            e.setCancelled(true);
            return;
        }

        String ignitingPlot = PlotUtils.getPlotIdByLocation(ignitingBlock.getLocation());
        String burningPlot  = PlotUtils.getPlotIdByLocation(e.getBlock().getLocation());

        if (!Objects.equals(ignitingPlot, burningPlot)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent e) {
        if (!isMarketWorld(e.getBlock().getLocation())) return;

        // Allow player-initiated ignition only inside their own plot (handled by onPlayerInteract)
        // Block spread-based ignition that crosses plot lines
        BlockIgniteEvent.IgniteCause cause = e.getCause();
        if (cause == BlockIgniteEvent.IgniteCause.SPREAD || cause == BlockIgniteEvent.IgniteCause.LAVA) {
            Block source = e.getIgnitingBlock();
            if (source == null) {
                e.setCancelled(true);
                return;
            }
            String sourcePlot = PlotUtils.getPlotIdByLocation(source.getLocation());
            String targetPlot = PlotUtils.getPlotIdByLocation(e.getBlock().getLocation());
            if (!Objects.equals(sourcePlot, targetPlot)) {
                e.setCancelled(true);
            }
        }
    }

    // ─── Liquid Flow Protection ───────────────────────────────────────────────
    //
    // Prevents water / lava from flowing across plot or pathway boundaries.

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent e) {
        if (!isMarketWorld(e.getBlock().getLocation())) return;

        String fromPlot = PlotUtils.getPlotIdByLocation(e.getBlock().getLocation());
        String toPlot   = PlotUtils.getPlotIdByLocation(e.getToBlock().getLocation());

        if (!Objects.equals(fromPlot, toPlot)) {
            e.setCancelled(true);
        }
    }
}
