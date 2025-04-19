package net.craftnepal.market.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import net.craftnepal.market.Listeners.Movement;
import net.craftnepal.market.Market;
import net.craftnepal.market.files.LocationData;
import net.craftnepal.market.files.RegionData;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.UUID;

public class RegionUtils {
    private static HashMap<UUID,Boolean> particleVisiblePlayers = new HashMap<>();
    private static HashMap<UUID,Integer> particleTimer = new HashMap<UUID, Integer>();
    private static HashMap<UUID,Integer> tasks = new HashMap<>();
    public static boolean isLocationInsideRegion(Location location, Location min, Location max) {
        double locX = location.getX();
        double locY = location.getY();
        double locZ = location.getZ();

        double minX = min.getX();
        double minY = min.getY();
        double minZ = min.getZ();

        double maxX = max.getX();
        double maxY = max.getY();
        double maxZ = max.getZ();

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
//        PacketContainer packetContainer = Market.getProtocolManager().createPacket(PacketType.Play.Server.BLOCK_CHANGE);
//        packetContainer.getBlockPositionModifier().write(0,new BlockPosition(location.toVector()));
//        packetContainer.getBlockData().write(0, WrappedBlockData.createData(Material.GLASS));
//        Market.getProtocolManager().sendServerPacket(player,packetContainer);
//        PacketContainer packetContainer = Market.getProtocolManager().createPacket(PacketType.Play.Server.WORLD_PARTICLES);
//        packetContainer.getParticles().write(0, EnumWrappers.Particle.HEART);
//        packetContainer.getBooleans().write(0,false);
//        packetContainer.getDoubles().write(0,(double) location.getX());//x
//        packetContainer.getDoubles().write(1,(double)location.getY());//y
//        packetContainer.getDoubles().write(2,(double)location.getZ());//z
//        packetContainer.getFloat().write(0, (float)0); // Offset X
//        packetContainer.getFloat().write(1, (float)0); // Offset Y
//        packetContainer.getFloat().write(2, (float)0); // Offset Z
//        packetContainer.getFloat().write(3, (float)1);//max speed
//        packetContainer.getIntegers().write(0,1);//particle count
//        Market.getProtocolManager().sendServerPacket(player,packetContainer);
        Location centerLocation = new Location(player.getWorld(),
                location.getX()+0.5,
                location.getY()+0.5,
                location.getZ()+0.5
        );
        player.spawnParticle(Particle.REDSTONE,centerLocation,3,new Particle.DustOptions(
                color, 1));
    }


    public static void removeLastLocation(Player player){
        LocationData.get().set("players."+player.getUniqueId().toString(),null);
        LocationData.save();
    }
    public static void addLastLocation(Player player){
        Location lastLocation = player.getLocation();
        LocationData.get().set("players."+player.getUniqueId().toString(),lastLocation);
        LocationData.save();
    }
    public static Location getLastLocation(Player player){
        Location lastLocation = LocationData.get().getLocation("players."+player.getUniqueId().toString());
        return lastLocation;
    }
    public static void teleportToSpawn(Player player){
        Location spawn = RegionData.get().getLocation("market.spawn");

        if(spawn == null )
            return;
        player.teleport(spawn);
    }

}
