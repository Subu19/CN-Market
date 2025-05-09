package net.craftnepal.market.utils;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import net.craftnepal.market.files.RegionData;

public class MarketUtils {

    public static boolean isInMarketArea(Location location) {
        Location marketMin = RegionData.get().getLocation("market.posMin");
        Location marketMax = RegionData.get().getLocation("market.posMax");

        if (marketMin == null || marketMax == null) return false;
        return RegionUtils.isLocationInsideRegion(location, marketMin, marketMax);
    }

    public static Location getMarketSpawn() {
        return RegionData.get().getLocation("market.spawn");
    }

    public static int getAvailablePlotCount() {
        ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");
        if (plots == null) return 0;

        int count = 0;
        for (String plotId : plots.getKeys(false)) {
            if (PlotUtils.getPlotOwner(plotId) == null) {
                count++;
            }
        }
        return count;
    }
}