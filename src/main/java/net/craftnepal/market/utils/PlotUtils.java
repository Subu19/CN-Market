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
        return net.craftnepal.market.managers.DatabaseManager.getActivePlotIds();
    }

    public static List<String> getAvailablePlotIds() {
        return net.craftnepal.market.managers.DatabaseManager.getAvailablePlotIds();
    }

    public static String getPlotIdByLocation(Location location) {
        List<String> plots = net.craftnepal.market.managers.DatabaseManager.getAllPlotIds();
        
        // First check manually registered plots
        for (String plotId : plots) {
            Location min = net.craftnepal.market.managers.DatabaseManager.getPlotPosMin(plotId);
            Location max = net.craftnepal.market.managers.DatabaseManager.getPlotPosMax(plotId);

            if (min != null && max != null &&
                    RegionUtils.isLocationInsideRegion(location, min, max)) {
                return plotId;
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
        if (net.craftnepal.market.managers.DatabaseManager.plotExists(plotId)) {
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

        net.craftnepal.market.managers.DatabaseManager.savePlot(plotId, null, min, max, null);
    }

    public static String getPlotOwner(String plotId) {
        return net.craftnepal.market.managers.DatabaseManager.getPlotOwner(plotId);
    }

    public static String getPlotIdByPlayer(Player player) {
        List<String> plots = net.craftnepal.market.managers.DatabaseManager.getAllPlotIds();
        String playerUuid = player.getUniqueId().toString();

        for (String plotId : plots) {
            String owner = getPlotOwner(plotId);
            if (owner != null && owner.equals(playerUuid)) {
                return plotId;
            }
        }
        return null;
    }

    public static int getPlotCount(Player player) {
        List<String> plots = net.craftnepal.market.managers.DatabaseManager.getAllPlotIds();
        int count = 0;
        String playerUuid = player.getUniqueId().toString();
        for (String plotId : plots) {
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

    public static boolean isSpawnPlot(String plotId) {
        if (plotId == null || !plotId.startsWith("plot_")) return false;
        try {
            String[] parts = plotId.split("_");
            if (parts.length != 3) return false;
            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            int radius = Market.getMainConfig().getInt("market-world.spawn-radius", 1);
            return x >= -radius && x < radius && z >= -radius && z < radius;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isSpawnLocation(Location location) {
        World marketWorld = Market.getPlugin().getMarketWorld();
        if (marketWorld == null || !location.getWorld().equals(marketWorld)) {
            return false;
        }

        FileConfiguration config = Market.getMainConfig();
        int plotSize = config.getInt("market-world.plot-size", 16);
        int pathwayWidth = config.getInt("market-world.pathway-width", 3);
        int totalSize = plotSize + pathwayWidth;
        int halfPath = pathwayWidth / 2;
        int spawnRadius = config.getInt("market-world.spawn-radius", 1);
        int symmetricRadius = spawnRadius * totalSize - (pathwayWidth - halfPath);

        return Math.abs(location.getBlockX()) <= symmetricRadius && Math.abs(location.getBlockZ()) <= symmetricRadius;
    }

    public static boolean isPlotAvailable(String plotId) {
        return getPlotOwner(plotId) == null;
    }

    public static Location getPlotCenter(String plotId) {
        if (!net.craftnepal.market.managers.DatabaseManager.plotExists(plotId) && plotId.startsWith("plot_")) {
            registerAutomaticPlot(plotId);
        }
        Location min = net.craftnepal.market.managers.DatabaseManager.getPlotPosMin(plotId);
        Location max = net.craftnepal.market.managers.DatabaseManager.getPlotPosMax(plotId);
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
    }

    public static Location getPlotSpawn(String plotId) {
        if (!net.craftnepal.market.managers.DatabaseManager.plotExists(plotId) && plotId.startsWith("plot_")) {
            registerAutomaticPlot(plotId);
        }
        return net.craftnepal.market.managers.DatabaseManager.getPlotSpawn(plotId);
    }

    public static Location getPlotPosMin(String plotId) {
        if (!net.craftnepal.market.managers.DatabaseManager.plotExists(plotId) && plotId.startsWith("plot_")) {
            registerAutomaticPlot(plotId);
        }
        return net.craftnepal.market.managers.DatabaseManager.getPlotPosMin(plotId);
    }

    public static Location getPlotPosMax(String plotId) {
        if (!net.craftnepal.market.managers.DatabaseManager.plotExists(plotId) && plotId.startsWith("plot_")) {
            registerAutomaticPlot(plotId);
        }
        return net.craftnepal.market.managers.DatabaseManager.getPlotPosMax(plotId);
    }

    /**
     * Set the owner of a plot and update displays accordingly
     * @param plotId The ID of the plot
     * @param ownerUUID The UUID of the new owner, or null to unclaim
     */
    public static void setPlotOwner(String plotId, String ownerUUID) {
        if (!net.craftnepal.market.managers.DatabaseManager.plotExists(plotId) && plotId.startsWith("plot_")) {
            registerAutomaticPlot(plotId);
        }
        net.craftnepal.market.managers.DatabaseManager.setPlotOwner(plotId, ownerUUID);
    }

    public static void setPlotSpawn(String plotId, Location location) {
        if (!net.craftnepal.market.managers.DatabaseManager.plotExists(plotId) && plotId.startsWith("plot_")) {
            registerAutomaticPlot(plotId);
        }
        net.craftnepal.market.managers.DatabaseManager.setPlotSpawn(plotId, location);
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

    /**
     * Comprehensive check if a player can interact with or modify a block at a given location.
     *
     * @param player The player performing the action
     * @param location The location being interacted with
     * @return true if allowed, false if denied
     */
    public static boolean canPlayerInteract(Player player, Location location) {
        if (net.craftnepal.market.subcommands.admin.Bypass.bypassPlayers.containsKey(player.getUniqueId())) {
            return true;
        }
        if (!MarketUtils.isInMarketArea(location)) {
            return true;
        }

        // Max-height restriction
        int maxHeight = Market.getMainConfig().getInt("market-world.max-height", 255);
        if (location.getBlockY() > maxHeight) {
            return false;
        }

        // Must be inside own plot (or be a member)
        String plot = getPlotIdByLocation(location);
        if (plot == null) {
            // Pathway or spawn — no one builds here
            return false;
        }

        String owner = getPlotOwner(plot);
        if (owner != null && owner.equals(player.getUniqueId().toString())) {
            return true;
        }

        List<String> members = net.craftnepal.market.managers.DatabaseManager.getPlotMembers(plot);
        return members != null && members.contains(player.getUniqueId().toString());
    }
}
