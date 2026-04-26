package net.craftnepal.market.Listeners;

import net.craftnepal.market.Market;
import net.craftnepal.market.utils.PlotUtils;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Consolidated Market World protection listener.
 * One class handles ALL world-level protection to minimize listener overhead.
 * Design inspired by Multiverse-Core's WorldManager approach — check world first,
 * then process. All events early-exit if not in the market world.
 */
public class MarketWorldListener implements Listener {

    // Track which plot a player is currently in for action bar updates
    private final Map<UUID, String> playerPlotCache = new HashMap<>();

    private boolean isMarketWorld(World world) {
        World mw = Market.getPlugin().getMarketWorld();
        return mw != null && mw.equals(world);
    }

    // ─── Entity / Damage Protection ──────────────────────────────────────────

    /** Prevent ALL damage to players in the market world (fall, void, mobs, etc.) */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!isMarketWorld(event.getEntity().getWorld())) return;
        if (!Market.getMainConfig().getBoolean("protection.prevent-damage", true)) return;
        event.setCancelled(true);
    }

    /** No hunger loss in market world */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!isMarketWorld(event.getEntity().getWorld())) return;
        if (!Market.getMainConfig().getBoolean("protection.prevent-hunger", true)) return;
        event.setCancelled(true);
    }

    /** Prevent natural mob spawning in market world */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!isMarketWorld(event.getEntity().getWorld())) return;
        if (!Market.getMainConfig().getBoolean("protection.prevent-mob-spawn", true)) return;
        EntityType type = event.getEntityType();
        // Allow item frames, armor stands, display entities (shop displays)
        if (type == EntityType.ITEM_FRAME || type == EntityType.GLOW_ITEM_FRAME
                || type == EntityType.ARMOR_STAND || type == EntityType.TEXT_DISPLAY
                || type == EntityType.ITEM_DISPLAY || type == EntityType.BLOCK_DISPLAY) {
            return;
        }
        // Block all natural + mob spawns
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason == CreatureSpawnEvent.SpawnReason.NATURAL
                || reason == CreatureSpawnEvent.SpawnReason.DEFAULT
                || reason == CreatureSpawnEvent.SpawnReason.CHUNK_GEN
                || reason == CreatureSpawnEvent.SpawnReason.RAID) {
            event.setCancelled(true);
        }
    }

    /** Prevent entity target (mobs won't aggro players in market) */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!isMarketWorld(event.getEntity().getWorld())) return;
        event.setCancelled(true);
    }

    // ─── Action Bar – Plot Name on Enter ─────────────────────────────────────

    /**
     * Shows a plot action bar when a player walks into a different plot.
     * Uses block-level check (only fires when block position changes) and
     * caches the last known plot to avoid redundant lookups.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only process block changes to prevent per-tick spam
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        Player player = event.getPlayer();
        if (!isMarketWorld(player.getWorld())) {
            playerPlotCache.remove(player.getUniqueId());
            return;
        }

        String currentPlot = PlotUtils.getPlotIdByLocation(event.getTo());
        String lastPlot = playerPlotCache.get(player.getUniqueId());

        // Only update action bar if player crossed into a different plot
        if (java.util.Objects.equals(currentPlot, lastPlot)) return;

        playerPlotCache.put(player.getUniqueId(), currentPlot);

        if (currentPlot == null) {
            // Entered pathway/spawn area
            player.spigot().sendMessage(
                net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§7Market Pathway"));
        } else {
            String ownerUUID = PlotUtils.getPlotOwner(currentPlot);
            String label;
            if (ownerUUID != null) {
                org.bukkit.OfflinePlayer owner = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(ownerUUID));
                String name = owner.getName() != null ? owner.getName() : "Unknown";
                label = "§a§l" + name + "'s Plot";
            } else {
                label = "§e§lUnclaimed Plot";
            }
            player.spigot().sendMessage(
                net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(label));
        }
    }
}
