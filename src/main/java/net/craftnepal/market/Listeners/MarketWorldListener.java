package net.craftnepal.market.Listeners;

import net.craftnepal.market.Market;
import net.craftnepal.market.MarketSettings;
import net.craftnepal.market.plot.Plot;
import net.craftnepal.market.utils.MarketUtils;
import net.craftnepal.market.utils.PlayerUtils;
import net.craftnepal.market.utils.PlotUtils;
import net.craftnepal.market.world.SchematicManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Market World listener — handles only plugin-specific logic. Player protection (damage, hunger,
 * mob spawning) is delegated to WorldGuard + Paper.
 */
public class MarketWorldListener implements Listener {

    private final Map<UUID, String> playerPlotCache = new HashMap<>();

    // ─── Cached market world reference ──────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWorldLoad(WorldLoadEvent event) {
        Market.getPlugin().onWorldChanged();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        if (!event.isCancelled()) Market.getPlugin().onWorldChanged();
    }

    // ─── Schematic Pathway Pasting ──────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!MarketUtils.isInMarketWorld(event.getWorld()))
            return;

        SchematicManager sm = SchematicManager.getInstance();
        if (!sm.isAvailable())
            return;

        MarketSettings settings = Market.settings();
        // Just enqueue; SchematicManager's drain task does the paste outside the event.
        sm.enqueueChunk(event.getWorld(), event.getChunk(), settings.grid().plotSize(),
                settings.grid().pathwayWidth());
    }

    // ─── Action Bar – Plot Name on Enter ─────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()
                && from.getBlockY() == to.getBlockY()))
            return;

        Player player = event.getPlayer();
        if (!MarketUtils.isInMarketWorld(to.getWorld()))
            return;

        String currentPlot = PlotUtils.getPlotIdByLocation(to);
        String lastPlot = playerPlotCache.get(player.getUniqueId());

        if (Objects.equals(currentPlot, lastPlot))
            return;

        playerPlotCache.put(player.getUniqueId(), currentPlot);

        String label;
        if (PlotUtils.isSpawnLocation(to)) {
            label = "§b§lMarket Spawn";
        } else if (currentPlot == null || currentPlot.isEmpty()) {
            label = "§7Market Pathway";
        } else {
            Plot plot = PlotUtils.getPlot(currentPlot);
            if (plot != null && plot.isOwned()) {
                label = "§a§l" + ownerName(plot.owner()) + "'s Plot";
            } else {
                label = "§e§lUnclaimed Plot";
            }
        }
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(label));
    }

    private static String ownerName(String uuid) {
        try {
            return PlayerUtils.getName(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            return "Unknown";
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        playerPlotCache.remove(uuid);
        net.craftnepal.market.utils.RegionUtils.stopBorders(uuid);
        net.craftnepal.market.utils.TeleportUtils.cancelTeleport(event.getPlayer());
        RegionSelection.forget(uuid);
        Movement.forget(uuid);
        // Bypass mode is kept on purpose: admins may rely on it surviving a relog.
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        if (event.isBedSpawn() || event.isAnchorSpawn()) return;
        if (!MarketUtils.isInMarketWorld(event.getPlayer().getWorld())) return;
        if (!Market.getMainConfig().getBoolean("market-world.respawn-at-market-spawn", true)) return;
        Location spawn = MarketUtils.getMarketSpawn();
        if (spawn != null) event.setRespawnLocation(spawn);
    }
}
