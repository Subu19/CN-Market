package net.craftnepal.market.utils;

import net.craftnepal.market.Market;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

public final class MarketUtils {

    private MarketUtils() {}

    public static boolean isInMarketArea(Location location) {
        return location != null && isInMarketWorld(location.getWorld());
    }

    public static boolean isInMarketWorld(World world) {
        World marketWorld = Market.getPlugin().getMarketWorld();
        return marketWorld != null && world == marketWorld;
    }

    public static Location getMarketSpawn() {
        World marketWorld = Market.getPlugin().getMarketWorld();
        return marketWorld != null ? marketWorld.getSpawnLocation() : null;
    }

    public static boolean isChunkInMarketRegion(Chunk chunk) {
        return isInMarketWorld(chunk.getWorld());
    }
}
