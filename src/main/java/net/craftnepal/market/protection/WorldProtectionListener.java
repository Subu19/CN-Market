package net.craftnepal.market.protection;

import net.craftnepal.market.utils.PlotUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.AbstractWindCharge;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.projectiles.BlockProjectileSource;

import java.util.List;
import java.util.Set;

import static net.craftnepal.market.protection.PlotProtection.*;

/**
 * Block and world causes: explosions, pistons, fire, fluids, growth, dispensers, redstone. All use
 * the boundary rule: a change may not cross from one plot (or pathway) into another.
 */
public class WorldProtectionListener implements Listener {

    /** Components that cause neighbour block updates when their power changes. */
    private static final Set<Material> REDSTONE_COMPONENTS =
            Set.of(Material.REDSTONE_WIRE, Material.REDSTONE_TORCH, Material.REDSTONE_WALL_TORCH,
                    Material.REPEATER, Material.COMPARATOR, Material.OBSERVER, Material.PISTON,
                    Material.STICKY_PISTON, Material.DISPENSER, Material.DROPPER, Material.HOPPER,
                    Material.NOTE_BLOCK, Material.DAYLIGHT_DETECTOR);

    private static final int[][] NEIGHBOURS = {{0, 0, -1}, {0, 0, 1}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}};

    // ── Explosions (TNT, creepers, beds, wind charges) ───────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        Location source = e.getLocation();
        if (!inMarket(source)) return;

        if (PlotUtils.getPlotIdByLocation(source) == null) {
            // Explosion in a pathway or the spawn area: no block damage at all.
            e.blockList().clear();
            if (e.getEntity() instanceof AbstractWindCharge) e.getEntity().remove();
            return;
        }
        e.blockList().removeIf(block -> !sameZone(source, block));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        Block source = e.getBlock();
        if (!inMarket(source.getWorld())) return;

        if (PlotUtils.getPlotIdByBlock(source) == null) {
            e.blockList().clear();
            return;
        }
        e.blockList().removeIf(block -> !sameZone(source, block));
    }

    // ── Pistons ──────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        Block piston = e.getBlock();
        if (!inMarket(piston.getWorld())) return;

        for (Block pushed : e.getBlocks()) {
            if (!sameZone(piston, pushed.getRelative(e.getDirection()))) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        Block piston = e.getBlock();
        if (!inMarket(piston.getWorld())) return;

        for (Block pulled : e.getBlocks()) {
            if (!sameZone(piston, pulled)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    // ── Fire ─────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent e) {
        Block block = e.getBlock();
        if (!inMarket(block.getWorld())) return;

        Block igniting = e.getIgnitingBlock();
        if (igniting == null || !sameZone(igniting, block)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent e) {
        Block block = e.getBlock();
        if (!inMarket(block.getWorld())) return;

        switch (e.getCause()) {
            case SPREAD, LAVA -> {
                Block source = e.getIgnitingBlock();
                if (source == null || !sameZone(source, block)) e.setCancelled(true);
            }
            case LIGHTNING -> {
                if (PlotUtils.getPlotIdByBlock(block) == null) e.setCancelled(true);
            }
            default -> {
                // Flint and steel, fire charges, flaming arrows, fireballs, end crystals.
                Entity entity = e.getIgnitingEntity();
                Player player = playerSource(entity);
                if (player != null) {
                    if (!canBuild(player, block)) e.setCancelled(true);
                } else if (entity instanceof Projectile projectile
                        && projectile.getShooter() instanceof BlockProjectileSource dispenser) {
                    if (!sameZone(dispenser.getBlock(), block)) e.setCancelled(true);
                } else if (entity != null) {
                    if (!sameZone(entity.getLocation(), block)) e.setCancelled(true);
                } else if (e.getIgnitingBlock() != null && !sameZone(e.getIgnitingBlock(), block)) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTntPrime(TNTPrimeEvent e) {
        Block tnt = e.getBlock();
        if (!inMarket(tnt.getWorld())) return;

        Entity entity = e.getPrimingEntity();
        Player player = playerSource(entity);
        if (player != null) {
            if (!canBuild(player, tnt)) e.setCancelled(true);
        } else if (entity != null) {
            if (!sameZone(entity.getLocation(), tnt)) e.setCancelled(true);
        } else if (e.getPrimingBlock() != null && !sameZone(e.getPrimingBlock(), tnt)) {
            e.setCancelled(true);
        }
    }

    // ── Fluids ───────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent e) {
        Block from = e.getBlock();
        if (!inMarket(from.getWorld())) return;
        if (PlotUtils.isGridInterior(from)) return;

        if (!sameZone(from, e.getToBlock())) {
            e.setCancelled(true);
        }
    }

    // ── Growth and spread ────────────────────────────────────────────────────

    /** Sculk, grass, mycelium, moss, vines, mushrooms and other organic spread. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent e) {
        Block block = e.getBlock();
        if (!inMarket(block.getWorld())) return;

        if (!sameZone(e.getSource(), block)) {
            e.setCancelled(true);
        }
    }

    /** Trees, bamboo, chorus plants, azaleas: drop the parts that would grow into another plot. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent e) {
        Location source = e.getLocation();
        if (!inMarket(source)) return;

        World world = source.getWorld();
        int x = source.getBlockX(), y = source.getBlockY(), z = source.getBlockZ();
        e.getBlocks().removeIf(state -> !PlotUtils.sameZone(world, x, y, z, state.getX(), state.getY(), state.getZ()));
    }

    /** Bone meal: a player may only grow into blocks they could build on; dispensers stay in their plot. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFertilize(BlockFertilizeEvent e) {
        Block source = e.getBlock();
        if (!inMarket(source.getWorld())) return;

        Player player = e.getPlayer();
        List<BlockState> blocks = e.getBlocks();
        if (player != null) {
            blocks.removeIf(state -> !canBuild(player, state.getBlock()));
        } else {
            blocks.removeIf(state -> !sameZone(source, state.getBlock()));
        }
    }

    /** Ice melting, coral dying, leaves decaying: market builds stay exactly as placed. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent e) {
        if (inMarket(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    /**
     * Frost Walker and snow golems. Blocks forming on their own (concrete powder touching water)
     * only change the block itself and are allowed.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent e) {
        if (!(e instanceof EntityBlockFormEvent ebe)) return;
        Block block = e.getBlock();
        if (!inMarket(block.getWorld())) return;

        if (ebe.getEntity() instanceof Player player) {
            if (!canBuild(player, block)) e.setCancelled(true);
        } else {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpongeAbsorb(SpongeAbsorbEvent e) {
        Block sponge = e.getBlock();
        if (!inMarket(sponge.getWorld())) return;

        World world = sponge.getWorld();
        int x = sponge.getX(), y = sponge.getY(), z = sponge.getZ();
        e.getBlocks().removeIf(state -> !PlotUtils.sameZone(world, x, y, z, state.getX(), state.getY(), state.getZ()));
    }

    // ── Dispensers and redstone ──────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent e) {
        Block dispenser = e.getBlock();
        if (!inMarket(dispenser.getWorld())) return;

        BlockData data = dispenser.getBlockData();
        if (data instanceof Directional directional
                && !sameZone(dispenser, dispenser.getRelative(directional.getFacing()))) {
            e.setCancelled(true);
        }
    }

    /**
     * Stops BUD switches and quasi-connectivity from updating blocks across a plot border. This
     * event is not Cancellable: resetting the current is the only lever.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockRedstone(BlockRedstoneEvent e) {
        Block block = e.getBlock();
        if (!inMarket(block.getWorld())) return;
        if (!REDSTONE_COMPONENTS.contains(block.getType())) return;

        // Fast path: a block away from every plot edge cannot reach another plot.
        if (PlotUtils.isGridInterior(block)) return;

        World world = block.getWorld();
        int x = block.getX(), y = block.getY(), z = block.getZ();
        for (int[] d : NEIGHBOURS) {
            if (!PlotUtils.sameZone(world, x, y, z, x + d[0], y + d[1], z + d[2])) {
                e.setNewCurrent(e.getOldCurrent());
                return;
            }
        }
    }
}
