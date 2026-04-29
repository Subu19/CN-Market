package net.craftnepal.market.Listeners;

import net.craftnepal.market.Market;
import net.craftnepal.market.utils.PlotUtils;
import net.craftnepal.market.world.SchematicManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Market World listener — handles only plugin-specific logic. Player protection (damage, hunger,
 * mob spawning) is delegated to WorldGuard + Paper.
 */
public class MarketWorldListener implements Listener {

    private final Map<UUID, String> playerPlotCache = new HashMap<>();

    private boolean isMarketWorld(World world) {
        String worldName = Market.getMainConfig().getString("market-world.name", "market");
        World mw = org.bukkit.Bukkit.getWorld(worldName);
        return mw != null && mw.equals(world);
    }

    // ─── Schematic Pathway Pasting ──────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!isMarketWorld(event.getWorld()))
            return;

        SchematicManager sm = SchematicManager.getInstance();
        if (!sm.isAvailable())
            return;

        FileConfiguration cfg = Market.getMainConfig();
        int plotSize = cfg.getInt("market-world.plot-size", 16);
        int pathwayWidth = cfg.getInt("market-world.pathway-width", 3);

        // Just enqueue — no runTaskLater needed.
        // SchematicManager's drain task handles the actual paste safely off the event.
        sm.enqueueChunk(event.getWorld(), event.getChunk(), plotSize, pathwayWidth);
    }



    // ─── Action Bar – Plot Name on Enter ─────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()
                && event.getFrom().getBlockY() == event.getTo().getBlockY())
            return;

        Player player = event.getPlayer();
        if (!isMarketWorld(player.getWorld()))
            return;

        String currentPlot = PlotUtils.getPlotIdByLocation(event.getTo());
        String lastPlot = playerPlotCache.get(player.getUniqueId());

        if (java.util.Objects.equals(currentPlot, lastPlot))
            return;

        playerPlotCache.put(player.getUniqueId(), currentPlot);

        if (currentPlot == null || currentPlot.isEmpty()) {
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§7Market Pathway"));
        } else {
            String ownerUUID = PlotUtils.getPlotOwner(currentPlot);
            String label;
            if (ownerUUID != null && !ownerUUID.isEmpty()) {
                org.bukkit.OfflinePlayer owner =
                        org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(ownerUUID));
                String name = owner.getName() != null ? owner.getName() : "Unknown";
                label = "§a§l" + name + "'s Plot";
            } else {
                label = "§e§lUnclaimed Plot";
            }
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(label));
        }
    }
}
