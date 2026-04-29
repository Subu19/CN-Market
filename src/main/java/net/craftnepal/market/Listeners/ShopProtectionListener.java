package net.craftnepal.market.Listeners;

import net.craftnepal.market.utils.PlotUtils;
import net.craftnepal.market.utils.ShopUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import net.craftnepal.market.files.RegionData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.List;

public class ShopProtectionListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleBlockDestruction(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleBlockDestruction(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        checkAndRemoveShop(event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        handlePiston(event.getBlocks());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        handlePiston(event.getBlocks());
    }

    private void handleBlockDestruction(List<Block> blocks) {
        for (Block block : blocks) {
            checkAndRemoveShop(block);
        }
    }

    private void handlePiston(List<Block> blocks) {
        for (Block block : blocks) {
            if (block.getType() == Material.BARREL) {
                if (ShopUtils.isShopLocation(block.getLocation())) {
                    // We don't want shops to be moved by pistons
                    // Actually, if they are moved, the location in config becomes invalid.
                    // So we remove the shop if it's moved.
                    checkAndRemoveShop(block);
                }
            }
        }
    }

    private void checkAndRemoveShop(Block block) {
        if (block.getType() != Material.BARREL) return;

        Location loc = block.getLocation();
        String plotId = PlotUtils.getPlotIdByLocation(loc);
        if (plotId == null) return;

        ConfigurationSection shops = RegionData.get().getConfigurationSection("market.plots." + plotId + ".shops");
        if (shops == null) return;

        for (String shopId : shops.getKeys(false)) {
            Location shopLoc = net.craftnepal.market.utils.LocationUtils.loadLocation(RegionData.get(), "market.plots." + plotId + ".shops." + shopId + ".location");
            if (shopLoc != null && shopLoc.equals(loc)) {
                ShopUtils.removeShop(plotId, shopId);
                return;
            }
        }
    }
}
