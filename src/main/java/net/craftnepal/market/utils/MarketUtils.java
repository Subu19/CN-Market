package net.craftnepal.market.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.bukkit.configuration.ConfigurationSection;
import net.craftnepal.market.files.RegionData;

public class MarketUtils {

    public static boolean isInMarketArea(Location location) {
        Location marketMin = RegionData.get().getLocation("market.posMin");
        Location marketMax = RegionData.get().getLocation("market.posMax");

        if (marketMin == null || marketMax == null)
            return false;
        return RegionUtils.isLocationInsideRegion(location, marketMin, marketMax);
    }

    public static Location getMarketSpawn() {
        return RegionData.get().getLocation("market.spawn");
    }

    public static int getAvailablePlotCount() {
        ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");
        if (plots == null)
            return 0;

        int count = 0;
        for (String plotId : plots.getKeys(false)) {
            if (PlotUtils.getPlotOwner(plotId) == null) {
                count++;
            }
        }
        return count;
    }

    public static boolean isMarketRegionLoaded() {
        Location marketMin = RegionData.get().getLocation("market.posMin");
        Location marketMax = RegionData.get().getLocation("market.posMax");

        if (marketMin == null || marketMax == null)
            return false;
        if (!marketMin.getWorld().equals(marketMax.getWorld()))
            return false;

        int minX = Math.min(marketMin.getBlockX(), marketMax.getBlockX());
        int maxX = Math.max(marketMin.getBlockX(), marketMax.getBlockX());
        int minZ = Math.min(marketMin.getBlockZ(), marketMax.getBlockZ());
        int maxZ = Math.max(marketMin.getBlockZ(), marketMax.getBlockZ());

        int chunkMinX = minX >> 4;
        int chunkMaxX = maxX >> 4;
        int chunkMinZ = minZ >> 4;
        int chunkMaxZ = maxZ >> 4;

        World world = marketMin.getWorld();
        for (int x = chunkMinX; x <= chunkMaxX; x++) {
            for (int z = chunkMinZ; z <= chunkMaxZ; z++) {
                if (world.isChunkLoaded(x, z)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isChunkInMarketRegion(Chunk chunk) {
        Location marketMin = RegionData.get().getLocation("market.posMin");
        Location marketMax = RegionData.get().getLocation("market.posMax");

        if (marketMin == null || marketMax == null)
            return false;
        if (!marketMin.getWorld().equals(chunk.getWorld()))
            return false;

        int minX = Math.min(marketMin.getBlockX(), marketMax.getBlockX()) >> 4;
        int maxX = Math.max(marketMin.getBlockX(), marketMax.getBlockX()) >> 4;
        int minZ = Math.min(marketMin.getBlockZ(), marketMax.getBlockZ()) >> 4;
        int maxZ = Math.max(marketMin.getBlockZ(), marketMax.getBlockZ()) >> 4;

        return chunk.getX() >= minX && chunk.getX() <= maxX &&
                chunk.getZ() >= minZ && chunk.getZ() <= maxZ;
    }
}