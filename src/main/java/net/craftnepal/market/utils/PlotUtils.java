package net.craftnepal.market.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import net.craftnepal.market.files.RegionData;

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
        if (plots == null) return null;

        for (String plotId : plots.getKeys(false)) {
            Location min = RegionData.get().getLocation("market.plots." + plotId + ".posMin");
            Location max = RegionData.get().getLocation("market.plots." + plotId + ".posMax");

            if (min != null && max != null &&
                    RegionUtils.isLocationInsideRegion(location, min, max)) {
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
        return RegionData.get().getLocation("market.plots." + plotId + ".spawn");
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
        RegionData.get().set("market.plots." + plotId + ".spawn", location);
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
        SendMessage.sendPlayerMessage(player,"&eTeleporting in 5 seconds.. don't move!");
        TeleportUtils.scheduleTeleport(player,spawn,()->{
            SendMessage.sendPlayerMessage(player, "&aTeleported to your plot's spawn point!");
        });
        return true;
    }
}