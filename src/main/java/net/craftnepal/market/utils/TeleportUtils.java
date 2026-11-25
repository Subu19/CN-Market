package net.craftnepal.market.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportUtils {

    private static final Map<UUID, Integer> teleportTasks = new HashMap<>();
    private static final long DELAY_TICKS = 100L; // 5 seconds

    /**
     * Schedule a delayed teleport for a player.
     * Cancels any existing teleport task if present.
     */
    public static void scheduleTeleport(Player player, Location targetLocation, Runnable onSuccess) {
        scheduleTeleport(player, targetLocation, onSuccess, DELAY_TICKS);
    }

    public static void scheduleTeleport(Player player, Location targetLocation, Runnable onSuccess, long delayTicks) {
        cancelTeleport(player); // Prevent multiple tasks for one player

        int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(
                Bukkit.getPluginManager().getPlugin("Market"),
                () -> {
                    teleportTasks.remove(player.getUniqueId());
                    if (!player.isOnline()) return;
                    // Another plugin may cancel the teleport; then nothing else should happen.
                    if (player.teleport(targetLocation)) {
                        if (onSuccess != null) onSuccess.run();
                    } else {
                        SendMessage.sendPlayerMessage(player, "&cTeleport was blocked.");
                    }
                },
                delayTicks
        );

        teleportTasks.put(player.getUniqueId(), taskId);
    }

    /**
     * The usual market teleport: wait, teleport, and if the player came from outside the market,
     * remember where so {@code /market leave} can take them back.
     */
    public static void teleportFromOrigin(Player player, Location targetLocation, Runnable onSuccess) {
        Location origin = player.getLocation();
        scheduleTeleport(player, targetLocation, () -> {
            if (!MarketUtils.isInMarketArea(origin)) {
                PlayerUtils.saveLastLocation(player, origin);
            }
            if (onSuccess != null) onSuccess.run();
        });
    }

    /**
     * Cancel a player's scheduled teleport task, if any.
     */
    public static void cancelTeleport(Player player) {
        UUID uuid = player.getUniqueId();
        Integer taskId = teleportTasks.remove(uuid);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    /**
     * Check and cancel teleport if the player moves.
     */
    public static void handleMovement(PlayerMoveEvent event) {
        // Runs for every move of every player: cheapest checks first, no Block lookups.
        if (teleportTasks.isEmpty()) return;
        Player player = event.getPlayer();
        if (!teleportTasks.containsKey(player.getUniqueId())) return;
        Location from = event.getFrom(), to = event.getTo();
        if (to == null || (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ())) return;
        cancelTeleport(player);
        SendMessage.sendPlayerMessage(player, "&cTeleport canceled due to movement.");
    }
}
