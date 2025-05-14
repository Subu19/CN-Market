package net.craftnepal.market.Listeners;

import net.craftnepal.market.Market;
import net.craftnepal.market.utils.DisplayUtils;
import net.craftnepal.market.utils.MarketUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class MarketDisplayListener implements Listener {
    private final DisplayUtils displayUtils;
    private final Market plugin;

    public MarketDisplayListener(Market plugin) {
        this.displayUtils = DisplayUtils.getInstance();
        this.plugin = plugin;
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
        // Schedule display spawn for next tick to ensure world is fully loaded
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (MarketUtils.getMarketSpawn() != null &&
                    MarketUtils.getMarketSpawn().getWorld().equals(event.getWorld())) {
                displayUtils.spawnMarketDisplays();
                Bukkit.getLogger().info("clearing all display items..");
            }else
                Bukkit.getLogger().info("something went wrong creating all display items..");

        });
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        if (MarketUtils.getMarketSpawn() != null &&
                MarketUtils.getMarketSpawn().getWorld().equals(event.getWorld())) {
            Bukkit.getLogger().info("clearing all display items..");
            displayUtils.clearAllDisplays();
        }else{
            Bukkit.getLogger().info("something went wrong clearing all display items..");
        }
    }

//    @EventHandler
//    public void onChunkLoad(ChunkLoadEvent event) {
//        World world = event.getWorld();
//        Chunk chunk = event.getChunk();
//        if (MarketUtils.getMarketSpawn() != null &&
//                MarketUtils.getMarketSpawn().getWorld().equals(world)) {
//            if (MarketUtils.isChunkInMarketRegion(chunk)) {
//               if(MarketUtils.isMarketRegionLoaded() && DisplayUtils.getInstance().getMarketDisplays().isEmpty()){
//                   displayUtils.spawnMarketDisplays();
//                   Bukkit.getLogger().info("Spawned display items in chunk: " + chunk.getX() + ", " + chunk.getZ());
//               }
//            }
//        }
//    }
//
//    @EventHandler
//    public void onChunkUnload(ChunkUnloadEvent event) {
//        World world = event.getWorld();
//        Chunk chunk = event.getChunk();
//
//        if (MarketUtils.getMarketSpawn() != null &&
//                MarketUtils.getMarketSpawn().getWorld().equals(world)) {
//            if (MarketUtils.isChunkInMarketRegion(chunk)) {
//                displayUtils.clearDisplaysInChunk(chunk);
//                Bukkit.getLogger().info("Cleared display items in chunk: " + chunk.getX() + ", " + chunk.getZ());
//            }
//        }
//    }


}