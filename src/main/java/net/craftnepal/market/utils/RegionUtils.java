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
        if (location == null || min == null || max == null) return false;
        if (!location.getWorld().equals(min.getWorld()) || !location.getWorld().equals(max.getWorld())) return false;

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

    public static void visibleRegionBorders(Player player, Location min, Location max, Plugin plugin, Color color){
        if(particleVisiblePlayers.containsKey(player.getUniqueId())){

            Bukkit.getScheduler().cancelTask(tasks.get(player.getUniqueId()));
            tasks.remove(player.getUniqueId());
            particleVisiblePlayers.remove(player.getUniqueId());
            player.sendMessage("Turned off borders!");

        }else{
            particleVisiblePlayers.put(player.getUniqueId(),true);
            tasks.put(player.getUniqueId(),Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, ()->{
                for (double x = min.getX(); x <= max.getX(); x++) {
                    showBorderParticle(player, new Location(min.getWorld(), x, min.getY(), min.getZ()),color);
                    showBorderParticle(player, new Location(min.getWorld(), x, min.getY(), max.getZ()),color);
                    showBorderParticle(player, new Location(min.getWorld(), x, max.getY(), min.getZ()),color);
                    showBorderParticle(player, new Location(min.getWorld(), x, max.getY(), max.getZ()),color);
                }
                for (double y = min.getY(); y <= max.getY(); y++) {
                    showBorderParticle(player, new Location(min.getWorld(), min.getX(), y, min.getZ()),color);
                    showBorderParticle(player, new Location(min.getWorld(), max.getX(), y, min.getZ()),color);
                    showBorderParticle(player, new Location(min.getWorld(), min.getX(), y, max.getZ()),color);
                    showBorderParticle(player, new Location(min.getWorld(), max.getX(), y, max.getZ()),color);
                }
                for (double z = min.getZ(); z <= max.getZ(); z++) {
                    showBorderParticle(player, new Location(min.getWorld(), min.getX(), min.getY(), z),color);
                    showBorderParticle(player, new Location(min.getWorld(), max.getX(), min.getY(), z),color);
                    showBorderParticle(player, new Location(min.getWorld(), min.getX(), max.getY(), z),color);
                    showBorderParticle(player, new Location(min.getWorld(), max.getX(), max.getY(), z),color);
                }
            }, 0, 10L));
            player.sendMessage("Turned on borders.");
        }
    }
    public static void visibleRegionBorders(Player player, Location min, Location max, Plugin plugin, Color color,int time){
        particleTimer.put(player.getUniqueId(),time);
        if(particleVisiblePlayers.containsKey(player.getUniqueId())){
            Bukkit.getScheduler().cancelTask(tasks.get(player.getUniqueId()));
            tasks.remove(player.getUniqueId());
            particleVisiblePlayers.remove(player.getUniqueId());
        }else{
            particleVisiblePlayers.put(player.getUniqueId(),true);
            tasks.put(player.getUniqueId(),Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, ()->{
                for (double x = min.getX(); x <= max.getX(); x++) {
                    showBorderParticle(player, new Location(min.getWorld(), x, min.getY(), min.getZ()),color);
                    showBorderParticle(player, new Location(min.getWorld(), x, min.getY(), max.getZ()),color);
                    showBorderParticle(player, new Location(min.getWorld(), x, max.getY(), min.getZ()),color);
                    showBorderParticle(player, new Location(min.getWorld(), x, max.getY(), max.getZ()),color);
                }
                for (double y = min.getY(); y <= max.getY(); y++) {
                    showBorderParticle(player, new Location(min.getWorld(), min.getX(), y, min.getZ()),color);
                    showBorderParticle(player, new Location(min.getWorld(), max.getX(), y, min.getZ()),color);
                    showBorderParticle(player, new Location(min.getWorld(), min.getX(), y, max.getZ()),color);
                    showBorderParticle(player, new Location(min.getWorld(), max.getX(), y, max.getZ()),color);
                }
                for (double z = min.getZ(); z <= max.getZ(); z++) {
                    showBorderParticle(player, new Location(min.getWorld(), min.getX(), min.getY(), z),color);
                    showBorderParticle(player, new Location(min.getWorld(), max.getX(), min.getY(), z),color);
                    showBorderParticle(player, new Location(min.getWorld(), min.getX(), max.getY(), z),color);
                    showBorderParticle(player, new Location(min.getWorld(), max.getX(), max.getY(), z),color);
                }
                if(particleTimer.get(player.getUniqueId()) > 0){
                    particleTimer.put(player.getUniqueId(),particleTimer.get(player.getUniqueId())-1);
                }else{
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
                location.getX()+0.5,
                location.getY()+0.5,
                location.getZ()+0.5
        );
        player.spawnParticle(Particle.REDSTONE,centerLocation,3,new Particle.DustOptions(
                color, 1));
    }




}
