package net.craftnepal.market.utils;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.*;

public class RegionUtils {
    private static HashMap<UUID, Boolean> particleVisiblePlayers = new HashMap<>();
    private static HashMap<UUID, Integer> particleTimer = new HashMap<>();
    private static HashMap<UUID, Integer> tasks = new HashMap<>();

    public static boolean isLocationInsideRegion(Location location, Location min, Location max) {
        if (location == null || min == null || max == null)
            return false;
        if (!location.getWorld().equals(min.getWorld()) || !location.getWorld().equals(max.getWorld()))
            return false;

        double locX = location.getX();
        double locY = location.getY();
        double locZ = location.getZ();

        double minX = Math.min(min.getX(), max.getX());
        double minY = Math.min(min.getY(), max.getY());
        double minZ = Math.min(min.getZ(), max.getZ());

        double maxX = Math.max(min.getX(), max.getX());
        double maxY = Math.max(min.getY(), max.getY());
        double maxZ = Math.max(min.getZ(), max.getZ());

        return locX >= minX && locX <= maxX &&
                locY >= minY && locY <= maxY &&
                locZ >= minZ && locZ <= maxZ;
    }

    public static void visibleRegionBorders(Player player, Location min, Location max, Plugin plugin, Color color) {
        if (particleVisiblePlayers.containsKey(player.getUniqueId())) {

            Bukkit.getScheduler().cancelTask(tasks.get(player.getUniqueId()));
            tasks.remove(player.getUniqueId());
            particleVisiblePlayers.remove(player.getUniqueId());
            player.sendMessage("Turned off borders!");

        } else {
            particleVisiblePlayers.put(player.getUniqueId(), true);
            tasks.put(player.getUniqueId(), Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                for (double x = min.getX(); x <= max.getX(); x++) {
                    showBorderParticle(player, new Location(min.getWorld(), x, min.getY(), min.getZ()), color);
                    showBorderParticle(player, new Location(min.getWorld(), x, min.getY(), max.getZ()), color);
                    showBorderParticle(player, new Location(min.getWorld(), x, max.getY(), min.getZ()), color);
                    showBorderParticle(player, new Location(min.getWorld(), x, max.getY(), max.getZ()), color);
                }
                for (double y = min.getY(); y <= max.getY(); y++) {
                    showBorderParticle(player, new Location(min.getWorld(), min.getX(), y, min.getZ()), color);
                    showBorderParticle(player, new Location(min.getWorld(), max.getX(), y, min.getZ()), color);
                    showBorderParticle(player, new Location(min.getWorld(), min.getX(), y, max.getZ()), color);
                    showBorderParticle(player, new Location(min.getWorld(), max.getX(), y, max.getZ()), color);
                }
                for (double z = min.getZ(); z <= max.getZ(); z++) {
                    showBorderParticle(player, new Location(min.getWorld(), min.getX(), min.getY(), z), color);
                    showBorderParticle(player, new Location(min.getWorld(), max.getX(), min.getY(), z), color);
                    showBorderParticle(player, new Location(min.getWorld(), min.getX(), max.getY(), z), color);
                    showBorderParticle(player, new Location(min.getWorld(), max.getX(), max.getY(), z), color);
                }
            }, 0, 10L));
            player.sendMessage("Turned on borders.");
        }
    }

    public static void visibleRegionBorders(Player player, Location min, Location max, Plugin plugin, Color color,
            int time) {
        particleTimer.put(player.getUniqueId(), time);
        if (particleVisiblePlayers.containsKey(player.getUniqueId())) {
            Bukkit.getScheduler().cancelTask(tasks.get(player.getUniqueId()));
            tasks.remove(player.getUniqueId());
            particleVisiblePlayers.remove(player.getUniqueId());
        } else {
            particleVisiblePlayers.put(player.getUniqueId(), true);
            tasks.put(player.getUniqueId(), Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                for (double x = min.getX(); x <= max.getX(); x++) {
                    showBorderParticle(player, new Location(min.getWorld(), x, min.getY(), min.getZ()), color);
                    showBorderParticle(player, new Location(min.getWorld(), x, min.getY(), max.getZ()), color);
                    showBorderParticle(player, new Location(min.getWorld(), x, max.getY(), min.getZ()), color);
                    showBorderParticle(player, new Location(min.getWorld(), x, max.getY(), max.getZ()), color);
                }
                for (double y = min.getY(); y <= max.getY(); y++) {
                    showBorderParticle(player, new Location(min.getWorld(), min.getX(), y, min.getZ()), color);
                    showBorderParticle(player, new Location(min.getWorld(), max.getX(), y, min.getZ()), color);
                    showBorderParticle(player, new Location(min.getWorld(), min.getX(), y, max.getZ()), color);
                    showBorderParticle(player, new Location(min.getWorld(), max.getX(), y, max.getZ()), color);
                }
                for (double z = min.getZ(); z <= max.getZ(); z++) {
                    showBorderParticle(player, new Location(min.getWorld(), min.getX(), min.getY(), z), color);
                    showBorderParticle(player, new Location(min.getWorld(), max.getX(), min.getY(), z), color);
                    showBorderParticle(player, new Location(min.getWorld(), min.getX(), max.getY(), z), color);
                    showBorderParticle(player, new Location(min.getWorld(), max.getX(), max.getY(), z), color);
                }
                if (particleTimer.get(player.getUniqueId()) > 0) {
                    particleTimer.put(player.getUniqueId(), particleTimer.get(player.getUniqueId()) - 1);
                } else {
                    Bukkit.getScheduler().cancelTask(tasks.get(player.getUniqueId()));
                    tasks.remove(player.getUniqueId());
                    particleVisiblePlayers.remove(player.getUniqueId());
                    particleTimer.remove(player.getUniqueId());
                }
            }, 0, 10L));
        }
    }

    private static void showBorderParticle(Player player, Location location, Color color) {
        Location centerLocation = new Location(player.getWorld(),
                location.getX() + 0.5,
                location.getY() + 0.5,
                location.getZ() + 0.5);
        player.spawnParticle(Particle.REDSTONE, centerLocation, 3, new Particle.DustOptions(
                color, 1));
    }

    public static boolean isChunkLoaded(Location location) {
        return location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public static boolean isLocationInChunk(Location location, Chunk chunk) {
        return location.getChunk().equals(chunk);
    }

    public static void showVerticalParticleLine(Location startLoc, Location endLoc, Plugin plugin) {
        // If no end location provided, create one 3 blocks above start
        if (endLoc == null) {
            endLoc = startLoc.clone().add(0, 3, 0);
        }

        // Center the start and end locations in their respective blocks
        Location centeredStart = startLoc.clone().add(0.5, 0, 0.5);
        Location centeredEnd = endLoc.clone().add(0.5, 0, 0.5);

        Location finalEndLoc = centeredEnd;
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            // Calculate the vector between start and end
            double distance = centeredStart.distance(finalEndLoc);
            org.bukkit.util.Vector direction = finalEndLoc.toVector().subtract(centeredStart.toVector()).normalize();

            // Create particles along the line
            for (double d = 0; d <= distance; d += 0.2) {
                Location particleLoc = centeredStart.clone().add(direction.clone().multiply(d));

                // Create a helix effect around the main line
                double radius = 0.3;
                double angle = (d * Math.PI * 2) + (System.currentTimeMillis() / 100.0);
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                Location helixLoc = particleLoc.clone().add(x, 0, z);

                // Main beam particles
                particleLoc.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);

                // Helix particles with color
                particleLoc.getWorld().spawnParticle(
                        Particle.REDSTONE,
                        helixLoc,
                        1,
                        new Particle.DustOptions(Color.fromRGB(30, 144, 255), 0.7f));
            }
        }, 0L, 2L);

        // Cancel the task after 5 seconds (100 ticks)
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            Bukkit.getScheduler().cancelTask(taskId);
        }, 200L);
    }

}
