package net.craftnepal.market.protection;

import net.craftnepal.market.Market;
import net.craftnepal.market.utils.PlotUtils;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractWindCharge;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.inventory.Merchant;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import static net.craftnepal.market.protection.PlotProtection.*;

/**
 * Entities: item frames, armor stands, animals, vehicles, projectiles, falling blocks.
 *
 * <p>Item frames, paintings and armor stands are decoration and follow the strict actor rule, as
 * before. Other entities (animals, vehicles, golems) are protected only inside plots, so public
 * space stays public. Players, hostile mobs and traders are never protected here.
 */
public class EntityProtectionListener implements Listener {

    private final NamespacedKey fallOrigin = new NamespacedKey(Market.getPlugin(), "fall_origin");

    private static boolean isDecoration(Entity entity) {
        return entity instanceof Hanging || entity instanceof ArmorStand;
    }

    private static boolean isUnprotected(Entity entity) {
        return entity instanceof Player || entity instanceof Enemy || entity instanceof Merchant;
    }

    /** Actor rule for one entity; null player means not a player action. */
    private static boolean mayAffect(Player player, Entity entity) {
        if (isDecoration(entity)) return canBuild(player, entity.getLocation());
        if (isUnprotected(entity)) return true;
        return canTouch(player, entity.getLocation());
    }

    // ── Player ↔ entity ──────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        checkInteract(e.getPlayer(), e.getRightClicked(), e);
    }

    /** Has its own handler list, so {@link #onInteractEntity} does not see it. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent e) {
        checkInteract(e.getPlayer(), e.getRightClicked(), e);
    }

    /** Taking or swapping armor stand items. Also has its own handler list. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        checkInteract(e.getPlayer(), e.getRightClicked(), e);
    }

    private static void checkInteract(Player player, Entity entity, PlayerInteractEntityEvent e) {
        if (!inMarket(entity.getWorld())) return;
        if (!mayAffect(player, entity)) {
            e.setCancelled(true);
            deny(player, DENY_INTERACT);
        }
    }

    /** Scooping fish or axolotls into a bucket. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEntity(PlayerBucketEntityEvent e) {
        if (!canTouch(e.getPlayer(), e.getEntity().getLocation())) {
            e.setCancelled(true);
            deny(e.getPlayer(), DENY_INTERACT);
        }
    }

    /** Pulling entities with a fishing rod. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        Entity caught = e.getCaught();
        if (e.getState() != PlayerFishEvent.State.CAUGHT_ENTITY || caught == null) return;
        if (!inMarket(caught.getWorld())) return;
        if (!mayAffect(e.getPlayer(), caught)) {
            e.setCancelled(true);
            deny(e.getPlayer(), DENY_INTERACT);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        Entity victim = e.getEntity();
        if (!inMarket(victim.getWorld()) || isUnprotected(victim)) return;

        Entity damager = e.getDamager();
        Player player = playerSource(damager);
        if (player != null) {
            if (!mayAffect(player, victim)) {
                e.setCancelled(true);
                deny(player, DENY_INTERACT);
            }
            return;
        }
        // Mobs fight as usual; TNT, dispenser arrows and the like must stay in their plot.
        boolean shotByMob = damager instanceof Projectile p && p.getShooter() instanceof LivingEntity;
        if (!(damager instanceof LivingEntity) && !shotByMob && !sameZone(damager.getLocation(), victim.getLocation())) {
            e.setCancelled(true);
        }
    }

    /** Also receives {@link HangingBreakByEntityEvent}, which has no handler list of its own. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent e) {
        Entity hanging = e.getEntity();
        if (!inMarket(hanging.getWorld())) return;

        if (e instanceof HangingBreakByEntityEvent byEntity && byEntity.getRemover() != null) {
            Entity remover = byEntity.getRemover();
            Player player = playerSource(remover);
            if (player != null) {
                if (!canBuild(player, hanging.getLocation())) {
                    e.setCancelled(true);
                    deny(player, DENY_INTERACT);
                }
            } else if (!sameZone(remover.getLocation(), hanging.getLocation())) {
                e.setCancelled(true);
            }
        } else if (e.getCause() == HangingBreakEvent.RemoveCause.EXPLOSION) {
            // An explosion without a known source (beds, respawn anchors).
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleDamage(VehicleDamageEvent e) {
        if (!mayHurtVehicle(e.getVehicle(), e.getAttacker())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleDestroy(VehicleDestroyEvent e) {
        if (!mayHurtVehicle(e.getVehicle(), e.getAttacker())) e.setCancelled(true);
    }

    private static boolean mayHurtVehicle(Vehicle vehicle, Entity attacker) {
        if (attacker == null || !inMarket(vehicle.getWorld())) return true;
        Player player = playerSource(attacker);
        if (player != null) {
            if (canTouch(player, vehicle.getLocation())) return true;
            deny(player, DENY_INTERACT);
            return false;
        }
        return sameZone(attacker.getLocation(), vehicle.getLocation());
    }

    /** Splash potions thrown by a player do nothing to entities they could not touch. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent e) {
        if (!inMarket(e.getEntity().getWorld())) return;
        Player player = playerSource(e.getEntity());
        if (player == null) return;
        for (LivingEntity affected : e.getAffectedEntities()) {
            if (!isUnprotected(affected) && !mayAffect(player, affected)) {
                e.setIntensity(affected, 0);
            }
        }
    }

    // ── Wind charges ─────────────────────────────────────────────────────────
    // A wind charge's burst is scheduled before ProjectileHitEvent finishes, so cancelling the
    // hit is not enough: the entity is removed before it can burst. The explosion handler clears
    // pathway bursts. (No knockback handler: Paper deprecates the Bukkit knockback event and
    // warns on every start; players cannot throw wind charges here anyway.)

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWindChargeHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof AbstractWindCharge windCharge)) return;
        if (!inMarket(windCharge.getWorld())) return;

        String hitPlot = PlotUtils.getPlotIdByLocation(windCharge.getLocation());
        boolean crossPlot;
        if (windCharge.getShooter() instanceof Player shooter) {
            crossPlot = !sameZone(shooter.getLocation(), windCharge.getLocation());
        } else {
            // Dispenser-fired: only allowed when it lands inside a plot (dispensers are
            // separately kept from firing across borders).
            crossPlot = hitPlot == null;
        }
        if (crossPlot) {
            windCharge.remove();
            e.setCancelled(true);
        }
    }

    // ── Entities changing blocks ─────────────────────────────────────────────

    /**
     * Endermen, sheep, ravagers, silverfish, projectiles and so on may not change blocks in the
     * market world. Falling blocks may land only in the zone they started falling from.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        Block block = e.getBlock();
        if (!inMarket(block.getWorld())) return;

        Entity entity = e.getEntity();
        if (entity instanceof Player) return; // covered by break/place events

        if (entity instanceof FallingBlock falling) {
            PersistentDataContainer data = falling.getPersistentDataContainer();
            if (e.getTo().isAir()) {
                // The block is starting to fall: remember where from.
                data.set(fallOrigin, PersistentDataType.INTEGER_ARRAY,
                        new int[] {block.getX(), block.getY(), block.getZ()});
                return;
            }
            int[] origin = data.get(fallOrigin, PersistentDataType.INTEGER_ARRAY);
            boolean allowed = origin == null
                    ? sameZone(falling.getLocation(), block)
                    : PlotUtils.sameZone(block.getWorld(), origin[0], origin[1], origin[2], block.getX(), block.getY(), block.getZ());
            if (!allowed) e.setCancelled(true);
            return;
        }

        e.setCancelled(true);
    }

    /** Natural lightning is switched off in the market world. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLightningStrike(LightningStrikeEvent e) {
        Location location = e.getLightning().getLocation();
        if (inMarket(location) && e.getCause() == LightningStrikeEvent.Cause.WEATHER) {
            e.setCancelled(true);
        }
    }
}
