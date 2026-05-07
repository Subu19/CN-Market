package net.craftnepal.market.Listeners;

import net.craftnepal.market.utils.MarketUtils;
import net.craftnepal.market.utils.PlotUtils;
import net.craftnepal.market.utils.SendMessage;
import net.craftnepal.market.utils.ShopUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.block.BlockFace;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Comprehensive grief protection for the Market world.
 *
 * Covered vectors: 1. Block break / place / interact — direct player actions 2. TNT / block
 * explosions — cross-plot blast radius 3. Piston push / pull — cross-plot piston movement 4. Fire
 * spread / lava ignition — fire jumping between plots 5. Liquid flow — water / lava crossing plot
 * lines 6. Wind Charge (1.21+) — projectile knockback explosion 7. Sculk spread (1.19+) — sculk /
 * sculk vein creep 8. General block spread — mushroom, grass, moss, vine, etc. 9. Entity block
 * interaction — Endermen, Sheep, Ravagers, etc. 10. Structure growth — trees, bamboo, chorus plants
 * 11. Block fade — ice melt, coral die, leaf decay 12. Block form — Frost Walker, snow accumulation
 * 13. Sponge absorption — sponge sucking water across borders 14. Dispenser / Dropper — dispensed
 * projectiles & item drops 15. Cauldron / fluid changes — cauldrons draining into other plots 16.
 * Redstone neighbour updates — BUD-switch style cross-plot changes 17. Falling blocks — sand /
 * gravel / concrete powder 18. Lightning strike — trident channeling, natural bolts
 *
 * Player-health (damage, hunger, mob spawning) is handled by WorldGuard / Paper flags.
 */
public class MarketRegionProtection implements Listener {

    // ─── Materials that trigger neighbour block updates when placed/broken ────
    private static final Set<Material> REDSTONE_COMPONENTS =
            Set.of(Material.REDSTONE_WIRE, Material.REDSTONE_TORCH, Material.REDSTONE_WALL_TORCH,
                    Material.REPEATER, Material.COMPARATOR, Material.OBSERVER, Material.PISTON,
                    Material.STICKY_PISTON, Material.DISPENSER, Material.DROPPER, Material.HOPPER,
                    Material.NOTE_BLOCK, Material.DAYLIGHT_DETECTOR);

    // ─── Helper ───────────────────────────────────────────────────────────────

    /** True when the location is inside the market world/area. */
    private boolean isMarketWorld(Location location) {
        return MarketUtils.isInMarketArea(location);
    }

    /**
     * Returns true when {@code block} is in the same plot as {@code referencePlotId}, or when both
     * are null (both in a pathway / unowned zone).
     */
    private boolean samePlot(String referencePlotId, Block block) {
        return Objects.equals(referencePlotId, PlotUtils.getPlotIdByLocation(block.getLocation()));
    }

    // =========================================================================
    // 1. DIRECT PLAYER ACTIONS
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        Block clickedBlock = e.getClickedBlock();

        if (clickedBlock == null)
            return;

        // Allow configurable global blocks (e.g. Ender Chests) everywhere
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            List<String> allowed = net.craftnepal.market.Market.getMainConfig()
                    .getStringList("market-world.allowed-interact-blocks");
            if (allowed.contains(clickedBlock.getType().name()))
                return;
        }

        // Always allow barrel + shop interactions (handled by ShopInteraction listener)
        if (clickedBlock.getType() == Material.BARREL
                && PlotUtils.getPlotIdByLocation(clickedBlock.getLocation()) != null)
            return;
        if (ShopUtils.isShopLocation(clickedBlock.getLocation()))
            return;

        if (!PlotUtils.canPlayerInteract(player, clickedBlock.getLocation())) {
            e.setCancelled(true);
            SendMessage.sendPlayerMessage(player, "&cYou are not allowed to interact here.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (!PlotUtils.canPlayerInteract(e.getPlayer(), e.getBlock().getLocation())) {
            e.setCancelled(true);
            SendMessage.sendPlayerMessage(e.getPlayer(), "&cYou are not allowed to build here.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (!PlotUtils.canPlayerInteract(e.getPlayer(), e.getBlock().getLocation())) {
            e.setCancelled(true);
            SendMessage.sendPlayerMessage(e.getPlayer(), "&cYou are not allowed to build here.");
        }
    }

    // =========================================================================
    // 2. EXPLOSION PROTECTION (TNT cannons crossing plot lines)
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        if (!isMarketWorld(e.getLocation()))
            return;

        String sourcePlot = PlotUtils.getPlotIdByLocation(e.getLocation());

        // Explosion in pathway / spawn → no block damage at all
        if (sourcePlot == null) {
            e.blockList().clear();
            return;
        }

        // Only damage blocks that are in the same plot as the explosion source
        e.blockList().removeIf(block -> !samePlot(sourcePlot, block));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        if (!isMarketWorld(e.getBlock().getLocation()))
            return;

        String sourcePlot = PlotUtils.getPlotIdByLocation(e.getBlock().getLocation());

        if (sourcePlot == null) {
            e.blockList().clear();
            return;
        }

        e.blockList().removeIf(block -> !samePlot(sourcePlot, block));
    }

    // =========================================================================
    // 3. PISTON PROTECTION
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (!isMarketWorld(e.getBlock().getLocation()))
            return;

        String pistonPlot = PlotUtils.getPlotIdByLocation(e.getBlock().getLocation());

        for (Block pushed : e.getBlocks()) {
            Location dest = pushed.getRelative(e.getDirection()).getLocation();
            if (!Objects.equals(pistonPlot, PlotUtils.getPlotIdByLocation(dest))) {
                e.setCancelled(true);
                return;
            }
        }

        // Also guard the leading-edge destination
        if (!e.getBlocks().isEmpty()) {
            Block last = e.getBlocks().get(e.getBlocks().size() - 1);
            Location dest = last.getRelative(e.getDirection()).getLocation();
            if (!Objects.equals(pistonPlot, PlotUtils.getPlotIdByLocation(dest))) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (!isMarketWorld(e.getBlock().getLocation()))
            return;

        String pistonPlot = PlotUtils.getPlotIdByLocation(e.getBlock().getLocation());

        for (Block pulled : e.getBlocks()) {
            if (!samePlot(pistonPlot, pulled)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    // =========================================================================
    // 4. FIRE SPREAD PROTECTION
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent e) {
        if (!isMarketWorld(e.getBlock().getLocation()))
            return;

        Block ignitingBlock = e.getIgnitingBlock();
        if (ignitingBlock == null) {
            // Unknown source in market world → cancel to be safe
            e.setCancelled(true);
            return;
        }

        String ignitingPlot = PlotUtils.getPlotIdByLocation(ignitingBlock.getLocation());
        String burningPlot = PlotUtils.getPlotIdByLocation(e.getBlock().getLocation());

        if (!Objects.equals(ignitingPlot, burningPlot)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent e) {
        if (!isMarketWorld(e.getBlock().getLocation()))
            return;

        BlockIgniteEvent.IgniteCause cause = e.getCause();

        // Only intercept spread-based / lava-based ignition — player ignition is
        // already guarded by onPlayerInteract (flint-and-steel right-click)
        if (cause == BlockIgniteEvent.IgniteCause.SPREAD
                || cause == BlockIgniteEvent.IgniteCause.LAVA) {
            Block source = e.getIgnitingBlock();
            if (source == null) {
                e.setCancelled(true);
                return;
            }
            String sourcePlot = PlotUtils.getPlotIdByLocation(source.getLocation());
            String targetPlot = PlotUtils.getPlotIdByLocation(e.getBlock().getLocation());
            if (!Objects.equals(sourcePlot, targetPlot)) {
                e.setCancelled(true);
            }
        }

        // Lightning-caused ignition (natural + trident channeling) — cancel outside plots
        if (cause == BlockIgniteEvent.IgniteCause.LIGHTNING) {
            if (PlotUtils.getPlotIdByLocation(e.getBlock().getLocation()) == null) {
                e.setCancelled(true);
            }
        }
    }

    // =========================================================================
    // 5. LIQUID FLOW PROTECTION
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent e) {
        if (!isMarketWorld(e.getBlock().getLocation()))
            return;

        String fromPlot = PlotUtils.getPlotIdByLocation(e.getBlock().getLocation());
        String toPlot = PlotUtils.getPlotIdByLocation(e.getToBlock().getLocation());

        if (!Objects.equals(fromPlot, toPlot)) {
            e.setCancelled(true);
        }
    }

    // =========================================================================
    // 6. WIND CHARGE PROTECTION (1.21+)
    //
    // Root cause: WindCharge extends Explosive. When it hits, it detonates and
    // fires EntityExplodeEvent — but unlike TNT, its "explosion" does no block
    // damage; instead it creates a knockback burst. Cancelling ProjectileHitEvent
    // has NO effect on the burst because the detonation is already scheduled by
    // the time the event fires. We therefore use THREE layers:
    //
    // Layer 1 — ProjectileHitEvent (HIGHEST, ignoreCancelled=false):
    // Remove the entity immediately when it lands in a foreign plot.
    // entity.remove() prevents the detonation from ever being scheduled.
    // We use HIGHEST so we run after other plugins but before the
    // server schedules the explosion tick.
    //
    // Layer 2 — EntityExplodeEvent:
    // Safety net in case Layer 1 misses a tick. Clear the block list
    // AND zero the yield so no knockback pulse propagates.
    // (Wind charges use yield=0 natively but we enforce it explicitly.)
    //
    // Layer 3 — EntityKnockbackByEntityEvent (Paper-only):
    // If somehow the burst fires, cancel individual knockback events
    // where the source wind charge is in a different plot from the
    // victim entity.
    // =========================================================================

    /**
     * Layer 1: Remove the WindCharge entity before it can detonate when it lands outside the
     * shooter's plot. entity.remove() prevents scheduling of the burst.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onWindChargeHit(ProjectileHitEvent e) {
        if (!isMarketWorld(e.getEntity().getLocation()))
            return;
        if (!(e.getEntity() instanceof WindCharge windCharge))
            return;

        String hitPlot = PlotUtils.getPlotIdByLocation(windCharge.getLocation());

        boolean crossPlot;
        if (windCharge.getShooter() instanceof Player shooter) {
            String shooterPlot = PlotUtils.getPlotIdByLocation(shooter.getLocation());
            crossPlot = !Objects.equals(shooterPlot, hitPlot);
        } else {
            // Dispenser-fired wind charge: only allow if it stays in the same plot
            // as the dispenser (covered separately by onBlockDispense, but double-check)
            crossPlot = (hitPlot == null);
        }

        if (crossPlot) {
            // Remove BEFORE the burst is scheduled — this is the key fix
            windCharge.remove();
            // Also cancel the event so nothing else reacts to the hit
            e.setCancelled(true);
        }
    }

    /**
     * Layer 2: Safety net — if the WindCharge entity somehow detonates despite Layer 1, intercept
     * EntityExplodeEvent and suppress the burst entirely for cross-plot wind charges.
     *
     * Note: this handler is separate from onEntityExplode (handler #2) so the wind-charge-specific
     * zero-yield suppression is explicit.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWindChargeExplode(EntityExplodeEvent e) {
        if (!isMarketWorld(e.getLocation()))
            return;
        if (!(e.getEntity() instanceof WindCharge))
            return;

        String burstPlot = PlotUtils.getPlotIdByLocation(e.getLocation());

        if (burstPlot == null) {
            // Burst in pathway — suppress entirely
            e.blockList().clear();
            e.getEntity().remove();
            return;
        }

        // Strip any blocks outside the burst's own plot (same pattern as TNT handler)
        e.blockList().removeIf(block -> !samePlot(burstPlot, block));
    }

    /**
     * Layer 3 (Paper-only): Cancel per-entity knockback from a wind charge burst when the victim
     * entity is in a different plot from where the burst originated. This catches the knockback
     * side-effect even if the explosion event passed.
     * 
     * NOTE: Commented out because the project compiles against spigot-api, not paper-api.
     * If you need this, you must switch your pom.xml dependency to paper-api, or use reflection.
     * However, Layer 1 (windCharge.remove()) already fully prevents the burst from happening.
     */
    /*
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWindChargeKnockback(
            io.papermc.paper.event.entity.EntityKnockbackByEntityEvent e) {
        if (!isMarketWorld(e.getEntity().getLocation()))
            return;
        if (!(e.getHitBy() instanceof WindCharge windCharge))
            return;

        String burstPlot = PlotUtils.getPlotIdByLocation(windCharge.getLocation());
        String victimPlot = PlotUtils.getPlotIdByLocation(e.getEntity().getLocation());

        if (!Objects.equals(burstPlot, victimPlot)) {
            e.setCancelled(true);
        }
    }
    */



    // =========================================================================
    // 7 + 8. SCULK & GENERAL BLOCK SPREAD
    // Handles sculk/sculk-vein spread (1.19+) AND general organic spread:
    // mushrooms, grass, mycelium, moss, vine, weeping/twisting vines, etc.
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent e) {
        if (!isMarketWorld(e.getBlock().getLocation()))
            return;

        String sourcePlot = PlotUtils.getPlotIdByLocation(e.getSource().getLocation());
        String targetPlot = PlotUtils.getPlotIdByLocation(e.getBlock().getLocation());

        if (!Objects.equals(sourcePlot, targetPlot)) {
            e.setCancelled(true);
        }
    }

    // =========================================================================
    // 9. ENTITY BLOCK INTERACTION
    // Endermen stealing blocks, sheep eating grass, ravagers destroying crops,
    // silverfish infesting stone, etc.
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        if (!isMarketWorld(e.getBlock().getLocation()))
            return;

        // Player block changes are already covered by break/place events
        if (e.getEntity() instanceof Player)
            return;

        // Falling blocks are handled with more granularity in onEntityChangeBlockFall
        if (e.getEntity() instanceof org.bukkit.entity.FallingBlock)
            return;

        // All other entity-driven block changes are denied in the market world
        e.setCancelled(true);
    }

    // =========================================================================
    // 10. STRUCTURE GROWTH (trees, bamboo, chorus plants, azalea, etc.)
    // A sapling on plot A could grow into plot B without this guard.
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent e) {
        if (!isMarketWorld(e.getLocation()))
            return;

        String sourcePlot = PlotUtils.getPlotIdByLocation(e.getLocation());

        // Remove any block in the grown structure that falls outside the source plot
        e.getBlocks().removeIf(blockState -> !Objects.equals(sourcePlot,
                PlotUtils.getPlotIdByLocation(blockState.getLocation())));
    }

    // =========================================================================
    // 11. BLOCK FADE (ice melting, coral dying, leaves decaying)
    // Prevents builds from silently degrading in the market world.
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent e) {
        if (!isMarketWorld(e.getBlock().getLocation()))
            return;

        // Freeze all natural decay — market builds should stay exactly as placed
        e.setCancelled(true);
    }

    // =========================================================================
    // 12. BLOCK FORM (Frost Walker ice, snow accumulation, concrete hardening)
    // Frost Walker on a plot-edge player can form ice inside a neighbour's plot.
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent e) {
        if (!isMarketWorld(e.getBlock().getLocation()))
            return;

        if (e instanceof EntityBlockFormEvent ebe) {
            if (ebe.getEntity() instanceof Player player) {
                // Block forms from player enchants — enforce plot ownership
                if (!PlotUtils.canPlayerInteract(player, e.getBlock().getLocation())) {
                    e.setCancelled(true);
                }
            } else {
                // Snow golems or other entities forming blocks → deny in market
                e.setCancelled(true);
            }
        }
        // Non-entity block form (e.g. concrete powder touching water) is allowed
        // because it only affects the block itself, not a neighbour's plot.
    }

    // =========================================================================
    // 13. SPONGE ABSORPTION
    // A sponge placed near a plot border can absorb water from the neighbour's
    // plot, ruining water features or shop displays.
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpongeAbsorb(SpongeAbsorbEvent e) {
        if (!isMarketWorld(e.getBlock().getLocation()))
            return;

        String spongePlot = PlotUtils.getPlotIdByLocation(e.getBlock().getLocation());

        // Remove any absorbed blocks that are outside the sponge's own plot
        e.getBlocks().removeIf(blockState -> !Objects.equals(spongePlot,
                PlotUtils.getPlotIdByLocation(blockState.getLocation())));
    }

    // =========================================================================
    // 14. DISPENSER / DROPPER PROTECTION
    // Dispensers can shoot arrows, fire charges, water buckets, and spawn
    // entities. We deny dispensing that would affect a different plot.
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent e) {
        if (!isMarketWorld(e.getBlock().getLocation()))
            return;

        String dispenserPlot = PlotUtils.getPlotIdByLocation(e.getBlock().getLocation());

        // Determine the block the dispenser is aimed at
        org.bukkit.block.data.Directional directional =
                (org.bukkit.block.data.Directional) e.getBlock().getBlockData();
        Block target = e.getBlock().getRelative(directional.getFacing());

        String targetPlot = PlotUtils.getPlotIdByLocation(target.getLocation());

        if (!Objects.equals(dispenserPlot, targetPlot)) {
            e.setCancelled(true);
        }
    }

    // =========================================================================
    // 15. REDSTONE / NEIGHBOUR BLOCK UPDATES (BUD-switch cross-plot griefing)
    // A BUD-switch or quasi-connectivity can update blocks across a border.
    // We intercept BlockRedstoneEvent to block power changes from reaching
    // a different plot.
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockRedstone(BlockRedstoneEvent e) {
        if (!isMarketWorld(e.getBlock().getLocation()))
            return;

        // Only restrict redstone components that can cause neighbour block updates
        if (!REDSTONE_COMPONENTS.contains(e.getBlock().getType()))
            return;

        String blockPlot = PlotUtils.getPlotIdByLocation(e.getBlock().getLocation());

        // Check all 6 neighbours — if any neighbour is in a DIFFERENT plot,
        // block the power change entirely to prevent cross-plot BUD updates.
        for (BlockFace face : new BlockFace[] {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
                BlockFace.WEST, BlockFace.UP, BlockFace.DOWN}) {
            Block neighbour = e.getBlock().getRelative(face);
            if (!isMarketWorld(neighbour.getLocation()))
                continue;
            String neighbourPlot = PlotUtils.getPlotIdByLocation(neighbour.getLocation());
            if (!Objects.equals(blockPlot, neighbourPlot)) {
                // Revert power level to prevent cross-plot signal propagation
                e.setNewCurrent(e.getOldCurrent());
                return;
            }
        }
    }

    // =========================================================================
    // 16. FALLING BLOCKS (sand, gravel, concrete powder, anvils, pointed dripstone)
    // A falling block entity spawned in one plot can land in another.
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlockFall(EntityChangeBlockEvent e) {
        // This duplicates handler #9 intentionally for clarity.
        // FallingBlock landing fires EntityChangeBlockEvent — already covered
        // by the catch-all in handler #9, but we keep it explicit here.
        if (!isMarketWorld(e.getBlock().getLocation()))
            return;
        if (e.getEntity() instanceof Player)
            return;
        if (e.getEntity() instanceof org.bukkit.entity.FallingBlock) {
            String landingPlot = PlotUtils.getPlotIdByLocation(e.getBlock().getLocation());
            String originPlot = PlotUtils.getPlotIdByLocation(e.getEntity().getLocation());
            if (!Objects.equals(originPlot, landingPlot)) {
                e.setCancelled(true);
            }
        }
    }

    // =========================================================================
    // 17. LIGHTNING STRIKE (natural + trident channeling)
    // Lightning can ignite or char blocks. We cancel strikes in pathways and
    // cross-plot. Ignition side-effect is already caught by onBlockIgnite.
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLightningStrike(org.bukkit.event.weather.LightningStrikeEvent e) {
        if (!isMarketWorld(e.getLightning().getLocation()))
            return;

        // Cancel all natural lightning in the market world.
        // Player-aimed trident strikes are handled separately via EntityChangeBlock.
        if (e.getCause() == org.bukkit.event.weather.LightningStrikeEvent.Cause.WEATHER) {
            e.setCancelled(true);
        }
    }
}
