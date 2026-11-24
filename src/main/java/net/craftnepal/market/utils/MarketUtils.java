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

    /** Where {@code /market} sends players. Null when the market world is not loaded. */
    public static Location getMarketSpawn() {
        World marketWorld = Market.getPlugin().getMarketWorld();
        return marketWorld == null ? null : spawnIn(marketWorld);
    }

    /**
     * The spawn set with {@code /market admin setspawn} if it belongs to this world; otherwise a
     * safe spot on top of the blocks at 0,0 (the centre of the protected spawn area).
     */
    public static Location spawnIn(World world) {
        Location saved = net.craftnepal.market.managers.DatabaseManager.getMarketSpawn();
        if (saved != null && saved.getWorld() == world) return saved;
        return new Location(world, 0.5, world.getHighestBlockYAt(0, 0) + 1, 0.5);
    }

    public static boolean isChunkInMarketRegion(Chunk chunk) {
        return isInMarketWorld(chunk.getWorld());
    }
}
