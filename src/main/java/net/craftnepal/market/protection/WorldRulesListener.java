package net.craftnepal.market.protection;

import net.craftnepal.market.Market;
import net.craftnepal.market.MarketSettings;
import org.bukkit.World;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

/**
 * Optional market-world rules ({@code market-world.rules} in config.yml), all off by default.
 * Plain events rather than game rules, whose names differ between Spigot and Paper builds.
 */
public class WorldRulesListener implements Listener {

    private static boolean inMarket(World world) {
        return world != null && world.getName().equals(Market.settings().worldName());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent e) {
        if (!Market.settings().noPvp() || !(e.getEntity() instanceof Player)) return;
        Object attacker = e.getDamager() instanceof Projectile p ? p.getShooter() : e.getDamager();
        // ponytail: players and their projectiles only; TNT, potions and clouds are not traced back
        if (attacker instanceof Player && attacker != e.getEntity() && inMarket(e.getEntity().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFall(EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL && Market.settings().noFallDamage()
                && e.getEntity() instanceof Player && inMarket(e.getEntity().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent e) {
        if (Market.settings().noHunger() && e.getFoodLevel() < e.getEntity().getFoodLevel()
                && inMarket(e.getEntity().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (!Market.settings().noMonsters() || !(e.getEntity() instanceof Enemy)) return;
        // Spawn eggs and commands are deliberate; only block spawns the world makes on its own.
        switch (e.getSpawnReason()) {
            case SPAWNER_EGG, COMMAND, CUSTOM, DISPENSE_EGG, BUILD_WITHER, BUILD_SNOWMAN, BUILD_IRONGOLEM -> { return; }
            default -> { }
        }
        if (inMarket(e.getLocation().getWorld())) e.setCancelled(true);
    }
}
