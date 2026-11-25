package net.craftnepal.market.protection;

import net.craftnepal.market.Market;
import net.craftnepal.market.utils.MarketUtils;
import net.craftnepal.market.utils.PlotUtils;
import net.craftnepal.market.world.MarketEnvironment;
import net.craftnepal.market.world.MarketEnvironment.Explosions;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.util.BoundingBox;

/**
 * Enforces {@link MarketEnvironment} in the market world. Most settings are also set on the world
 * itself ({@link MarketEnvironment#apply}); these handlers are the backstop and cover what the
 * world has no switch for. Runs at NORMAL so the plot protection handlers (HIGH) see the result.
 */
public class EnvironmentListener implements Listener {

    private static MarketEnvironment env() {
        return Market.settings().environment();
    }

    private static boolean inMarket(World world) {
        return world != null && world.getName().equals(Market.settings().worldName());
    }

    // ── Mobs ─────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (!(e.getEntity() instanceof Mob) || !inMarket(e.getLocation().getWorld())) return;
        MarketEnvironment env = env();
        if (!env.allowsSpawn(e.getSpawnReason(), e.getEntity() instanceof Enemy)) {
            e.setCancelled(true);
        } else if (env.capped(e.getSpawnReason()) && mobsInPlot(e.getLocation()) >= env.maxMobsPerPlot()) {
            e.setCancelled(true);
        }
    }

    /** Living mobs in the plot at this location; 0 outside plots. Only runs for player-made spawns. */
    private static int mobsInPlot(Location at) {
        String plotId = PlotUtils.getPlotIdByLocation(at);
        Location min = plotId == null ? null : PlotUtils.getPlotPosMin(plotId);
        Location max = plotId == null ? null : PlotUtils.getPlotPosMax(plotId);
        if (min == null || max == null) return 0;
        World world = at.getWorld();
        BoundingBox box = new BoundingBox(min.getBlockX(), world.getMinHeight(), min.getBlockZ(),
                max.getBlockX() + 1, world.getMaxHeight(), max.getBlockZ() + 1);
        return world.getNearbyEntities(box, entity -> entity instanceof Mob).size();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onRaid(RaidTriggerEvent e) {
        if (!env().mobSpawning() && inMarket(e.getWorld())) e.setCancelled(true);
    }

    /** Mobs left from before (or from another plugin): hostile ones go when their chunk loads. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(EntitiesLoadEvent e) {
        if (env().hostileMobs() || !inMarket(e.getWorld())) return;
        for (Entity entity : e.getEntities()) {
            if (entity instanceof Mob mob && entity instanceof Enemy && isWild(mob)) mob.remove();
        }
    }

    /** Not named, tamed, leashed, riding or ridden, and the game would despawn it anyway. */
    public static boolean isWild(Mob mob) {
        return mob.getCustomName() == null && mob.getRemoveWhenFarAway() && !mob.isLeashed()
                && mob.getVehicle() == null && mob.getPassengers().isEmpty()
                && !(mob instanceof org.bukkit.entity.Tameable t && t.isTamed());
    }

    // ── PvP (the world's PvP flag is the main switch) ────────────────────────

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent e) {
        if (env().pvp() || !(e.getEntity() instanceof Player)) return;
        Object attacker = e.getDamager() instanceof Projectile p ? p.getShooter() : e.getDamager();
        if (attacker instanceof Player && attacker != e.getEntity() && inMarket(e.getEntity().getWorld())) {
            e.setCancelled(true);
        }
    }

    // ── Explosions ───────────────────────────────────────────────────────────

    private static boolean noExplosions(World world) {
        return env().explosions() == Explosions.OFF && inMarket(world);
    }

    /** TNT stays a block instead of being used up. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTntPrime(TNTPrimeEvent e) {
        if (noExplosions(e.getBlock().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent e) {
        if (noExplosions(e.getEntity().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        if (noExplosions(e.getLocation().getWorld())) e.setCancelled(true);
    }

    /** Respawn anchors and beds. The anchor itself is used up by the game before the event. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        if (noExplosions(e.getBlock().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHangingExplode(HangingBreakEvent e) {
        if (e.getCause() == HangingBreakEvent.RemoveCause.EXPLOSION && noExplosions(e.getEntity().getWorld())) {
            e.setCancelled(true);
        }
    }

    // ── Damage: explosions, falls, void ──────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        Entity entity = e.getEntity();
        DamageCause cause = e.getCause();
        if ((cause == DamageCause.ENTITY_EXPLOSION || cause == DamageCause.BLOCK_EXPLOSION)) {
            if (noExplosions(entity.getWorld())) e.setCancelled(true);
            return;
        }
        if (!(entity instanceof Player player) || !inMarket(entity.getWorld())) return;
        if (cause == DamageCause.FALL && !env().fallDamage()) {
            e.setCancelled(true);
        } else if (cause == DamageCause.VOID && env().voidRescue()) {
            e.setCancelled(true);
            player.setFallDistance(0);
            player.setVelocity(new org.bukkit.util.Vector());
            player.teleport(MarketUtils.spawnIn(entity.getWorld()));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent e) {
        if (!env().hunger() && e.getFoodLevel() < e.getEntity().getFoodLevel() && inMarket(e.getEntity().getWorld())) {
            e.setCancelled(true);
        }
    }

    // ── Fire ─────────────────────────────────────────────────────────────────

    private static boolean noFireSpread(World world) {
        return !env().fireSpread() && inMarket(world);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFireSpread(BlockSpreadEvent e) {
        Material type = e.getNewState().getType();
        if ((type == Material.FIRE || type == Material.SOUL_FIRE) && noFireSpread(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        if (noFireSpread(e.getBlock().getWorld())) e.setCancelled(true);
    }

    /** Spread and lava; a player lighting a fire on their own plot is left to plot protection. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent e) {
        switch (e.getCause()) {
            case SPREAD, LAVA, LIGHTNING -> {
                if (noFireSpread(e.getBlock().getWorld())) e.setCancelled(true);
            }
            default -> { }
        }
    }

    // ── Weather ──────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onWeather(WeatherChangeEvent e) {
        if (e.toWeatherState() && env().clearWeather() && inMarket(e.getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onThunder(ThunderChangeEvent e) {
        if (e.toThunderState() && env().clearWeather() && inMarket(e.getWorld())) e.setCancelled(true);
    }
}
