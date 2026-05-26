package net.craftnepal.market.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.bukkit.configuration.ConfigurationSection;
import net.craftnepal.market.Market;
import net.craftnepal.market.files.RegionData;

public class MarketUtils {

    public static boolean isInMarketArea(Location location) {
        World marketWorld = Market.getPlugin().getMarketWorld();
        return marketWorld != null && location.getWorld().equals(marketWorld);
    }

    public static Location getMarketSpawn() {
        World marketWorld = Market.getPlugin().getMarketWorld();
        return marketWorld != null ? marketWorld.getSpawnLocation() : null;
    }


    public static int getAvailablePlotCount() {
        return net.craftnepal.market.managers.DatabaseManager.getAvailablePlotIds().size();
    }

    public static boolean isMarketRegionLoaded() {
        World marketWorld = Market.getPlugin().getMarketWorld();
        return marketWorld != null;
    }

    public static boolean isChunkInMarketRegion(Chunk chunk) {
        World marketWorld = Market.getPlugin().getMarketWorld();
        return marketWorld != null && chunk.getWorld().equals(marketWorld);
    }
}