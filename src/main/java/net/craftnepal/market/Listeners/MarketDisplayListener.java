package net.craftnepal.market.Listeners;

import net.craftnepal.market.Market;
import net.craftnepal.market.utils.DisplayUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

/** Spawns and removes shop displays as market chunks and the market world load and unload. */
public class MarketDisplayListener implements Listener {
    private final DisplayUtils displayUtils;
    private final Market plugin;

    public MarketDisplayListener(Market plugin) {
        this.displayUtils = DisplayUtils.getInstance();
        this.plugin = plugin;
    }

    private static boolean isMarketWorld(World world) {
        return world.getName().equals(Market.settings().worldName());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        displayUtils.handleChunkLoad(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        displayUtils.handleChunkUnload(event.getChunk());
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (!isMarketWorld(event.getWorld())) return;
        // Next tick, once the world is fully registered.
        Bukkit.getScheduler().runTask(plugin, displayUtils::spawnMarketDisplays);
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        if (isMarketWorld(event.getWorld())) displayUtils.clearAllDisplays();
    }
}
