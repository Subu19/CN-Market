package net.craftnepal.market.protection;

import net.craftnepal.market.Market;
import net.craftnepal.market.utils.PlotUtils;
import net.craftnepal.market.utils.ShopUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.ItemStack;

import static net.craftnepal.market.protection.PlotProtection.*;

/** Things a player does directly. All use the actor rule. */
public class PlayerProtectionListener implements Listener {

    private static final String DENY_WIND_CHARGE = "&cYou are not allowed to use Wind Charges here.";

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        Action action = e.getAction();

        if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
                && inMarket(player.getWorld()) && !bypass(player)) {
            ItemStack item = e.getItem();
            if (item != null && item.getType() == Material.WIND_CHARGE) {
                e.setCancelled(true);
                deny(player, DENY_WIND_CHARGE);
                return;
            }
        }

        Block clicked = e.getClickedBlock();
        if (clicked == null) return;

        // Blocks the config opens up to everyone (e.g. ender chests, crafting tables).
        if (action == Action.RIGHT_CLICK_BLOCK
                && Market.settings().allowedInteractBlocks().contains(clicked.getType())) return;

        // Barrels in a plot and shops are handled by ShopInteraction.
        if (clicked.getType() == Material.BARREL && PlotUtils.getPlotIdByBlock(clicked) != null) return;
        if (ShopUtils.isShopLocation(clicked.getLocation())) return;

        if (!canBuild(player, clicked)) {
            e.setCancelled(true);
            deny(player, DENY_INTERACT, action);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (!canBuild(e.getPlayer(), e.getBlock())) {
            e.setCancelled(true);
            deny(e.getPlayer(), DENY_BUILD);
        }
    }

    /** Also receives {@link BlockMultiPlaceEvent} (beds, doors), which has no handler list of its own. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        boolean allowed = canBuild(player, e.getBlock());
        if (allowed && e instanceof BlockMultiPlaceEvent multi) {
            for (BlockState state : multi.getReplacedBlockStates()) {
                if (!canBuild(player, state.getBlock())) {
                    allowed = false;
                    break;
                }
            }
        }
        if (!allowed) {
            e.setCancelled(true);
            deny(player, DENY_BUILD);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (!canBuild(e.getPlayer(), e.getBlock())) {
            e.setCancelled(true);
            deny(e.getPlayer(), DENY_BUILD);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent e) {
        Block block = e.getBlock();
        // Milking a cow also fires this event, with no liquid involved.
        boolean liquid = block.isLiquid() || block.getType() == Material.POWDER_SNOW
                || (block.getBlockData() instanceof Waterlogged w && w.isWaterlogged());
        if (liquid && !canBuild(e.getPlayer(), block)) {
            e.setCancelled(true);
            deny(e.getPlayer(), DENY_BUILD);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent e) {
        if (!canBuild(e.getPlayer(), e.getBlock())) {
            e.setCancelled(true);
            deny(e.getPlayer(), DENY_BUILD);
        }
    }

    /** Boats, minecarts, armor stands and end crystals. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent e) {
        Player player = e.getPlayer();
        if (player != null && !canBuild(player, e.getBlock())) {
            e.setCancelled(true);
            deny(player, DENY_BUILD);
        }
    }

    /** Paintings and item frames. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent e) {
        Player player = e.getPlayer();
        if (player != null && !canBuild(player, e.getBlock().getRelative(e.getBlockFace()))) {
            e.setCancelled(true);
            deny(player, DENY_BUILD);
        }
    }

    /** A thrown egg may not hatch chickens inside someone else's plot. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEggThrow(PlayerEggThrowEvent e) {
        if (e.isHatching() && !canTouch(e.getPlayer(), e.getEgg().getLocation())) {
            e.setHatching(false);
        }
    }

    /** Nether portals may only be lit where the player can build; nothing else may create one. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent e) {
        if (!inMarket(e.getWorld())) return;

        if (!(e.getEntity() instanceof Player player)) {
            e.setCancelled(true);
            return;
        }
        for (BlockState state : e.getBlocks()) {
            if (!canBuild(player, state.getBlock())) {
                e.setCancelled(true);
                deny(player, DENY_BUILD);
                return;
            }
        }
    }

    /**
     * Fallback for wind charges thrown at the air: the interact event for air clicks arrives
     * already cancelled, so the check in {@link #onPlayerInteract} does not see it.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWindChargeLaunch(ProjectileLaunchEvent e) {
        if (!(e.getEntity() instanceof WindCharge)) return;
        if (e.getEntity().getShooter() instanceof Player player && inMarket(player.getWorld()) && !bypass(player)) {
            e.setCancelled(true);
            deny(player, DENY_WIND_CHARGE);
        }
    }
}
