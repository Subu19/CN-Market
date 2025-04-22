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
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class RegionUtils {
    private static HashMap<UUID,Boolean> particleVisiblePlayers = new HashMap<>();
    private static HashMap<UUID,Integer> particleTimer = new HashMap<UUID, Integer>();
    private static HashMap<UUID,Integer> tasks = new HashMap<>();
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


    public static void removeLastLocation(Player player){
        LocationData.get().set("players."+player.getUniqueId().toString(),null);
        LocationData.save();
    }

    public static List<String> getActivePlotIds() {
        List<String> activePlots = new ArrayList<>();
        ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");

        if (plots != null) {
            for (String plotId : plots.getKeys(false)) {
                if (RegionData.get().getString("market.plots." + plotId + ".owner") != null) {
                    activePlots.add(plotId);
                }
            }
        }

        return activePlots;
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

    public static boolean isInMarketArea(Location location) {
        Location marketMin = RegionData.get().getLocation("market.posMin");
        Location marketMax = RegionData.get().getLocation("market.posMax");

        if (marketMin == null || marketMax == null) return false;
        return isLocationInsideRegion(location, marketMin, marketMax);
    }

    public static String getPlotIdByLocation(Location location) {
        ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");
        if (plots == null) return null;

        for (String plotId : plots.getKeys(false)) {
            Location min = RegionData.get().getLocation("market.plots." + plotId + ".posMin");
            Location max = RegionData.get().getLocation("market.plots." + plotId + ".posMax");

            if (min != null && max != null && isLocationInsideRegion(location, min, max)) {
                return plotId;
            }
        }
        return null;
    }

    public static String getPlotOwner(String plotId) {
        return RegionData.get().getString("market.plots." + plotId + ".owner");
    }

    public static String getPlotIdByPlayer(Player player) {
        ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");
        if (plots == null) return null;

        for (String plotId : plots.getKeys(false)) {
            String owner = getPlotOwner(plotId);
            if (owner != null && owner.equals(player.getUniqueId().toString())) {
                return plotId;
            }
        }
        return null;
    }

    public static boolean isPlotAvailable(String plotId) {
        return getPlotOwner(plotId) == null;
    }

    public static Location getPlotCenter(String plotId) {
        Location min = RegionData.get().getLocation("market.plots." + plotId + ".posMin");
        Location max = RegionData.get().getLocation("market.plots." + plotId + ".posMax");
        if (min == null || max == null) return null;

        return new Location(
                min.getWorld(),
                (min.getX() + max.getX()) / 2,
                (min.getY() + max.getY()) / 2,
                (min.getZ() + max.getZ()) / 2
        );
    }

    public static boolean isPlayerInOwnPlot(Player player) {
        String plotId = getPlotIdByLocation(player.getLocation());
        if (plotId == null) return false;

        String owner = getPlotOwner(plotId);
        return owner != null && owner.equals(player.getUniqueId().toString());
    }

    public static int getAvailablePlotCount() {
        ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");
        if (plots == null) return 0;

        int count = 0;
        for (String plotId : plots.getKeys(false)) {
            if (isPlotAvailable(plotId)) {
                count++;
            }
        }
        return count;
    }
    public static Map<Material, Integer> getAllShopItemsAndCountsByPlotID(Integer plotId) {
        Map<Material, Integer> itemCounts = new HashMap<>();

        ConfigurationSection shops = RegionData.get().getConfigurationSection("market.plots." + plotId + ".shops");
        System.out.println("Fetching shop items for plot ID: " + plotId);

        if (shops != null) {
            for (String shopId : shops.getKeys(false)) {
                System.out.println("Fetching items for shop ID: " + shopId);
                String materialName = shops.getString(shopId + ".item");
                Material material = Material.matchMaterial(materialName);

                if (material == null) continue;

                Location loc = RegionData.get().getLocation("market.plots." + plotId + ".shops."+shopId+".location");
                // Check if the block at location is a chest
                Block block = loc.getBlock();
                if (block.getType() == Material.CHEST) {
                    Chest chest = (Chest) block.getState();

                    int count = 0;
                    for (ItemStack item : chest.getInventory().getContents()) {
                        if (item != null && item.getType() == material) {
                            count += item.getAmount();
                        }
                    }

                    // Store the count
                    itemCounts.put(material, itemCounts.getOrDefault(material, 0) + count);
                }
            }
        }
        return itemCounts;
    }
    public static ItemStack getPlayerHead(UUID playerUUID) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        // For Minecraft 1.13+
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerUUID));

        head.setItemMeta(meta);
        return head;
    }

}
