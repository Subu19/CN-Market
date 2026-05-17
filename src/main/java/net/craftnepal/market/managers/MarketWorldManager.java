package net.craftnepal.market.managers;

import net.craftnepal.market.Market;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.world.MarketGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;

/**
 * Manages the lifecycle of the market world: creation and spawn configuration.
 */
public class MarketWorldManager {

    /**
     * Loads or creates the market world.
     *
     * @param forceCreate If true, creates the world even if the config entry is missing
     *                    (used during the initial admin setup flow).
     */
    public static void initialize(boolean forceCreate) {
        if (!Market.getMainConfig().contains("market-world.name") && !forceCreate) {
            return;
        }

        String worldName = Market.getMainConfig().getString("market-world.name", "market");
        World marketWorld = Bukkit.getWorld(worldName);

        if (marketWorld == null) {
            Bukkit.getLogger().info((forceCreate ? "Creating new" : "Loading existing")
                    + " Market world: " + worldName);
            WorldCreator creator = new WorldCreator(worldName);
            creator.generator(new MarketGenerator());
            marketWorld = creator.createWorld();
        }

        if (marketWorld != null) {
            Location savedSpawn = RegionData.get().getLocation("market.spawn");
            if (savedSpawn != null) {
                marketWorld.setSpawnLocation(
                        savedSpawn.getBlockX(),
                        savedSpawn.getBlockY(),
                        savedSpawn.getBlockZ());
            } else {
                marketWorld.setSpawnLocation(0, 65, 0);
            }
        }
    }
}
