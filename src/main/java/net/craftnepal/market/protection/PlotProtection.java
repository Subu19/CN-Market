package net.craftnepal.market.protection;

import net.craftnepal.market.subcommands.admin.Bypass;
import net.craftnepal.market.utils.MarketUtils;
import net.craftnepal.market.utils.PlotUtils;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;

/**
 * The two rules every protection handler is built from, in the style of GriefPrevention:
 *
 * <ul>
 *   <li><b>Actor rule</b> ({@link #canBuild}): a player may act at a block only when trusted in the
 *       plot there. Pathways, spawn and the space above the height limit are denied.</li>
 *   <li><b>Boundary rule</b> ({@link #sameZone}): anything that is not a player may only affect
 *       blocks in the same plot it started in (or move between two non-plot blocks).</li>
 * </ul>
 */
public final class PlotProtection {

    static final String DENY_BUILD = "&cYou are not allowed to build here.";
    static final String DENY_INTERACT = "&cYou are not allowed to interact here.";

    private PlotProtection() {}

    static boolean inMarket(World world) {
        return MarketUtils.isInMarketWorld(world);
    }

    static boolean inMarket(Location location) {
        return location != null && MarketUtils.isInMarketWorld(location.getWorld());
    }

    static boolean bypass(Player player) {
        return Bypass.bypassPlayers.containsKey(player.getUniqueId());
    }

    /** Actor rule. */
    static boolean canBuild(Player player, Block block) {
        return PlotUtils.canPlayerInteract(player, block);
    }

    /** Actor rule. */
    static boolean canBuild(Player player, Location location) {
        return PlotUtils.canPlayerInteract(player, location);
    }

    /**
     * Actor rule for things that belong to a plot, such as animals and vehicles: inside a plot the
     * player must be trusted, but pathways and other public space stay usable by everyone.
     */
    static boolean canTouch(Player player, Location location) {
        if (!inMarket(location) || PlotUtils.getPlotIdByLocation(location) == null) return true;
        return PlotUtils.canPlayerInteract(player, location);
    }

    /** Boundary rule. */
    static boolean sameZone(Block a, Block b) {
        return PlotUtils.sameZone(a, b);
    }

    /** Boundary rule between an arbitrary location (e.g. an entity) and a block. */
    static boolean sameZone(Location a, Block b) {
        return PlotUtils.sameZone(b.getWorld(), a.getBlockX(), a.getBlockY(), a.getBlockZ(), b.getX(), b.getY(), b.getZ());
    }

    static boolean sameZone(Location a, Location b) {
        return PlotUtils.sameZone(a.getWorld(), a.getBlockX(), a.getBlockY(), a.getBlockZ(),
                b.getBlockX(), b.getBlockY(), b.getBlockZ());
    }

    /** The player behind an entity: the entity itself, or the shooter of a projectile. */
    static Player playerSource(Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }

    static void deny(Player player, String message) {
        SendMessage.sendPlayerMessage(player, message);
    }

    /** Stepping on a pressure plate fires every tick; do not spam the denial message for it. */
    static void deny(Player player, String message, Action action) {
        if (action != Action.PHYSICAL) deny(player, message);
    }
}
