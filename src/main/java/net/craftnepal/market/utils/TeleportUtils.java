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
                    player.teleport(targetLocation);
                    if (onSuccess != null) onSuccess.run();
                },
                delayTicks
        );

        teleportTasks.put(player.getUniqueId(), taskId);
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
        Player player = event.getPlayer();

        if (!event.getFrom().getBlock().equals(event.getTo().getBlock())) {
            if (teleportTasks.containsKey(player.getUniqueId())) {
                cancelTeleport(player);
                SendMessage.sendPlayerMessage(player, "&cTeleport canceled due to movement.");
            }
        }
    }
}
