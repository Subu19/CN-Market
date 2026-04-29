package net.craftnepal.market.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import net.craftnepal.market.Market;
import net.craftnepal.market.files.RegionData;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class PlotUtils {

    public static List<String> getActivePlotIds() {
        List<String> activePlots = new ArrayList<>();
        ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");

        if (plots != null) {
            for (String plotId : plots.getKeys(false)) {
                if (getPlotOwner(plotId) != null) {
                    activePlots.add(plotId);
                }
            }
        }
        return activePlots;
    }

    public static List<String> getAvailablePlotIds() {
        List<String> availablePlots = new ArrayList<>();
        ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");

        if (plots != null) {
            for (String plotId : plots.getKeys(false)) {
                if (getPlotOwner(plotId) == null) {
                    availablePlots.add(plotId);
                }
            }
        }
        return availablePlots;
    }

    public static String getPlotIdByLocation(Location location) {
        ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");
        
        // First check manually registered plots
        if (plots != null) {
            for (String plotId : plots.getKeys(false)) {
                Location min = LocationUtils.loadLocation(plots, plotId + ".posMin");
                Location max = LocationUtils.loadLocation(plots, plotId + ".posMax");

                if (min != null && max != null &&
                        RegionUtils.isLocationInsideRegion(location, min, max)) {
                    return plotId;
                }
            }
        }

        // If not found, check if it's an automatic plot in the market world
        return getAutomaticPlotIdAt(location);
    }

    public static String getAutomaticPlotIdAt(Location location) {
        World marketWorld = Market.getPlugin().getMarketWorld();
        if (marketWorld == null || !location.getWorld().equals(marketWorld)) {
            return null;
        }

        FileConfiguration config = Market.getMainConfig();
        int plotSize = config.getInt("market-world.plot-size", 16);
        int pathwayWidth = config.getInt("market-world.pathway-width", 3);
        int totalSize = plotSize + pathwayWidth;

        int worldX = location.getBlockX();
        int worldZ = location.getBlockZ();

        int adjustedX = worldX + (pathwayWidth / 2);
        int adjustedZ = worldZ + (pathwayWidth / 2);

        int modX = Math.abs(adjustedX) % totalSize;
        int modZ = Math.abs(adjustedZ) % totalSize;

        if (adjustedX < 0 && modX != 0) modX = totalSize - modX;
        if (adjustedZ < 0 && modZ != 0) modZ = totalSize - modZ;

        if (modX < pathwayWidth || modZ < pathwayWidth) {
            return null; // Standing on a pathway
        }

        // Calculate plot ID based on grid coordinates
        int plotX = (int) Math.floor((double) adjustedX / totalSize);
        int plotZ = (int) Math.floor((double) adjustedZ / totalSize);
        
        return "plot_" + plotX + "_" + plotZ;
    }

    public static void registerAutomaticPlot(String plotId) {
        if (RegionData.get().contains("market.plots." + plotId + ".posMin")) {
            return; // Already registered with boundaries
        }

        String[] parts = plotId.split("_");
        if (parts.length != 3) return;

        int plotX = Integer.parseInt(parts[1]);
        int plotZ = Integer.parseInt(parts[2]);

        FileConfiguration config = Market.getMainConfig();
        int plotSize = config.getInt("market-world.plot-size", 16);
        int pathwayWidth = config.getInt("market-world.pathway-width", 3);
        int totalSize = plotSize + pathwayWidth;

        int startX = (plotX * totalSize) - (pathwayWidth / 2) + pathwayWidth;
        int startZ = (plotZ * totalSize) - (pathwayWidth / 2) + pathwayWidth;

        World world = Market.getPlugin().getMarketWorld();
        int maxHeight = config.getInt("market-world.max-height", 255);
        Location min = new Location(world, startX, 64, startZ);
        Location max = new Location(world, startX + plotSize - 1, maxHeight, startZ + plotSize - 1);

        ConfigurationSection plotSection = RegionData.get().getConfigurationSection("market.plots." + plotId);
        if (plotSection == null) {
            plotSection = RegionData.get().createSection("market.plots." + plotId);
        }

        LocationUtils.saveLocation(RegionData.get(), "market.plots." + plotId + ".posMin", min);
        LocationUtils.saveLocation(RegionData.get(), "market.plots." + plotId + ".posMax", max);
        RegionData.save();
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

    public static int getPlotCount(Player player) {
        ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");
        if (plots == null) return 0;

        int count = 0;
        String playerUuid = player.getUniqueId().toString();
        for (String plotId : plots.getKeys(false)) {
            String owner = getPlotOwner(plotId);
            if (owner != null && owner.equals(playerUuid)) {
                count++;
            }
        }
        return count;
    }

    public static int getPlotLimit(Player player) {
        if (player.hasPermission("market.plots.limit.unlimited")) {
            return Integer.MAX_VALUE;
        }

        // Check for permission-based limits (highest one wins)
        int limit = Market.getMainConfig().getInt("market-world.max-plots-per-player", 1);
        
        // This is a bit expensive but common for plot plugins
        // We check from 100 down to the config limit
        for (int i = 100; i > limit; i--) {
            if (player.hasPermission("market.plots.limit." + i)) {
                return i;
            }
        }

        return limit;
    }


    public static boolean isPlotAvailable(String plotId) {
        return getPlotOwner(plotId) == null;
    }

    public static Location getPlotCenter(String plotId) {
        if (!RegionData.get().contains("market.plots." + plotId + ".posMin") && plotId.startsWith("plot_")) {
            registerAutomaticPlot(plotId);
        }
        Location min = LocationUtils.loadLocation(RegionData.get(), "market.plots." + plotId + ".posMin");
        Location max = LocationUtils.loadLocation(RegionData.get(), "market.plots." + plotId + ".posMax");
        if (min == null || max == null) return null;

        return new Location(
                min.getWorld(),
                (min.getX() + max.getX()) / 2,
                (min.getY()) + 1, // +1 to be above ground
                (min.getZ() + max.getZ()) / 2
        );
    }

    public static boolean isPlayerInOwnPlot(Player player) {
        String plotId = getPlotIdByLocation(player.getLocation());
        if (plotId == null) return false;

        String owner = getPlotOwner(plotId);
        return owner != null && owner.equals(player.getUniqueId().toString());
    }    public static Location getPlotSpawn(String plotId) {
        if (!RegionData.get().contains("market.plots." + plotId + ".posMin") && plotId.startsWith("plot_")) {
            registerAutomaticPlot(plotId);
        }
        return LocationUtils.loadLocation(RegionData.get(), "market.plots." + plotId + ".spawn");
    }

    /**
     * Set the owner of a plot and update displays accordingly
     * @param plotId The ID of the plot
     * @param ownerUUID The UUID of the new owner, or null to unclaim
     */
    public static void setPlotOwner(String plotId, String ownerUUID) {

        // Update the owner in the config
        if (ownerUUID != null) {
            RegionData.get().set("market.plots." + plotId + ".owner", ownerUUID);
        } else {
            // If unclaiming, remove all plot data
            RegionData.get().set("market.plots." + plotId + ".owner", null);
        }
        RegionData.save();

    }public static void setPlotSpawn(String plotId, Location location) {
        LocationUtils.saveLocation(RegionData.get(), "market.plots." + plotId + ".spawn", location);
        RegionData.save();
    }

    public static void teleportToPlotSpawn(Player player, String plotId) {
        Location spawn = getPlotSpawn(plotId);
        if (spawn != null) {
            player.teleport(spawn);
            SendMessage.sendPlayerMessage(player, "&aTeleported to plot spawn point!");
        } else {
            SendMessage.sendPlayerMessage(player, "&cThis plot doesn't have a spawn point set!");
        }
    }

    /**
     * Teleport a player to their own plot's spawn point
     * @param player The player to teleport
     * @return true if teleported successfully, false if player doesn't own a plot or plot has no spawn
     */
    public static boolean teleportToOwnPlotSpawn(Player player) {
        String plotId = getPlotIdByPlayer(player);
        if (plotId == null) {
            SendMessage.sendPlayerMessage(player, "&cYou don't own a plot!");
            return false;
        }
        
        Location spawn = getPlotSpawn(plotId);
        if (spawn == null) {
            SendMessage.sendPlayerMessage(player, "&cYour plot doesn't have a spawn point set! Use /market setplotspawn to set one.");
            return false;
        }
        Location origin = player.getLocation();
        SendMessage.sendPlayerMessage(player,"&eTeleporting in 5 seconds.. don't move!");
        TeleportUtils.scheduleTeleport(player,spawn,()->{
            if (!MarketUtils.isInMarketArea(origin)) {
                PlayerUtils.saveLastLocation(player, origin);
            }
            SendMessage.sendPlayerMessage(player, "&aTeleported to your plot's spawn point!");
        });
        return true;
    }
}