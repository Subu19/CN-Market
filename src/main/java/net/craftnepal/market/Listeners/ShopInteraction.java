package net.craftnepal.market.Listeners;

import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import me.kodysimpson.simpapi.menu.MenuManager;
import me.kodysimpson.simpapi.menu.PlayerMenuUtility;
import net.craftnepal.market.Entities.ChestShop;
import net.craftnepal.market.menus.ShopAdminMenu;
import net.craftnepal.market.menus.ShopBuyerMenu;
import net.craftnepal.market.menus.ShopCreateMenu;
import net.craftnepal.market.utils.PlotUtils;
import net.craftnepal.market.utils.SendMessage;
import net.craftnepal.market.utils.ShopUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ShopInteraction implements Listener {

    /**
     * Interaction matrix for BARREL blocks in a market plot:
     *
     * SNEAKING                  → pass-through always (normal MC behavior)
     * NOT SNEAKING:
     *   Barrel == Shop:
     *     Right-click:
     *       Owner / Admin      → open barrel inventory (restock)
     *       Others             → open ShopBuyerMenu (buy/sell)
     *     Left-click:
     *       Owner / Admin      → open ShopAdminMenu (stats, remove)
     *       Others             → info message in chat
     *   Barrel != Shop (empty):
     *     Right-click          → open barrel inventory normally + hint message
     *     Left-click:
     *       Plot owner / admin → open ShopCreateMenu
     *       Others             → "no permission" message
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onChestInteraction(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BARREL)
            return;

        Player player = event.getPlayer();

        // Sneaking always bypasses our handling – let Minecraft do its normal thing.
        if (player.isSneaking()) return;

        UUID uuid = player.getUniqueId();
        String plotId = PlotUtils.getPlotIdByLocation(block.getLocation());

        // Outside any market plot – ignore entirely.
        if (plotId == null) return;

        // If the event was cancelled by another plugin (like protection), 
        // we might still want to handle it if it's a market shop interaction.
        // However, if we are in a plot, the market plugin should be the authority.
        // We'll proceed even if cancelled, but we'll be careful.

        ChestShop shop = ShopUtils.getShopAt(block.getLocation());

        boolean isOwner = shop != null && shop.getOwner() != null && shop.getOwner().equals(uuid);
        boolean isAdmin  = player.hasPermission("market.admin");
        boolean isBypass = net.craftnepal.market.subcommands.admin.Bypass.bypassPlayers.containsKey(uuid);
        boolean isRight  = event.getAction() == Action.RIGHT_CLICK_BLOCK;
        boolean isLeft   = event.getAction() == Action.LEFT_CLICK_BLOCK;

        // ── SHOP EXISTS ────────────────────────────────────────────────────────────
        if (shop != null) {
            if (isRight) {
                if (isOwner || isBypass) {
                    // Owner/Admin right-click → open the actual barrel inventory to restock.
                    // Do NOT cancel – Minecraft will open the barrel normally.
                    SendMessage.sendPlayerMessage(player, "&7Restocking your shop…");
                    event.setCancelled(false); // Ensure it's not cancelled so they can open it
                    return; // Let the event proceed normally.
                } else {
                    // Customer right-click → open buyer menu.
                    event.setCancelled(true);
                    openGui(player, plotId, shop.getId(), false);
                }
            } else { // LEFT CLICK
                // Always cancel left-click on a shop barrel to avoid block damage.
                event.setCancelled(true);
                if (isOwner || isBypass) {
                    openGui(player, plotId, shop.getId(), true);
                } else {
                    // Info message for non-owners.
                    String priceLabel = shop.isBuyingShop() ? "&eBuying for" : "&eSelling at";
                    SendMessage.sendPlayerMessage(player, priceLabel + " &6" + shop.getPrice());
                }
            }
            return;
        }

        // ── EMPTY BARREL (NOT A SHOP) ──────────────────────────────────────────────
        if (isRight) {
            if (PlotUtils.canPlayerInteract(player, block.getLocation())) {
                // Let owner/member/admin open the barrel inventory normally.
                SendMessage.sendPlayerMessage(player, "&7This barrel is not a shop. Left-click with an item to create one.");
                event.setCancelled(false); // Ensure it's not cancelled for the owner
            } else {
                // Deny for everyone else
                event.setCancelled(true);
                SendMessage.sendPlayerMessage(player, "&cYou are not allowed to open barrels in this plot.");
            }
            return;
        }

        // LEFT CLICK on empty barrel → shop creation flow.
        // Cancel to prevent the block-break animation while not sneaking.
        event.setCancelled(true);

        String plotOwner = PlotUtils.getPlotOwner(plotId);
        boolean isPlotOwner   = plotOwner != null && plotOwner.equals(uuid.toString());
        boolean isAdminInMode  = isBypass;
        boolean isAdminInSpawn = isAdmin && PlotUtils.isSpawnPlot(plotId);

        if (isPlotOwner || isAdminInMode || isAdminInSpawn) {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held == null || held.getType() == Material.AIR) {
                SendMessage.sendPlayerMessage(player, "&cHold the item you want to sell/buy, then left-click the barrel.");
                return;
            }
            
            try {
                PlayerMenuUtility pmu = MenuManager.getPlayerMenuUtility(player);
                if (pmu == null) {
                    SendMessage.sendPlayerMessage(player, "&cError: Menu data not found. Please rejoin.");
                    Bukkit.getLogger().warning("[Market] PlayerMenuUtility is null for " + player.getName());
                    return;
                }
                new ShopCreateMenu(pmu, plotId, block.getLocation(), held).open();
            } catch (MenuManagerNotSetupException e) {
                SendMessage.sendPlayerMessage(player, "&cMenu system error. Please rejoin and try again.");
                Bukkit.getLogger().severe("[Market] MenuManagerNotSetupException for " + player.getName());
            } catch (Exception e) {
                SendMessage.sendPlayerMessage(player, "&cUnexpected error opening menu.");
                e.printStackTrace();
            }
        } else {
            SendMessage.sendPlayerMessage(player, "&cYou do not own this plot and cannot create shops here.");
        }
    }

    /**
     * Crouch + break = remove shop barrel (owner or admin only).
     * Normal breaks (non-sneaking) on shop barrels are blocked with guidance.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChestBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.BARREL) return;

        ChestShop shop = ShopUtils.getShopAt(block.getLocation());
        if (shop == null) return; // Not a registered shop barrel – leave alone.

        Player player = event.getPlayer();
        boolean isBypass = net.craftnepal.market.subcommands.admin.Bypass.bypassPlayers.containsKey(player.getUniqueId());
        boolean isOwner = shop.getOwner() != null && shop.getOwner().equals(player.getUniqueId());

        if (!isOwner && !isBypass) {
            event.setCancelled(true);
            SendMessage.sendPlayerMessage(player, "&cYou cannot break a shop you don't own.");
            return;
        }

        if (!player.isSneaking()) {
            event.setCancelled(true);
            SendMessage.sendPlayerMessage(player, "&eSneak and break to remove your shop barrel.");
            return;
        }

        // Sneak + break → remove shop data, then let the block break normally.
        String plotId = PlotUtils.getPlotIdByLocation(block.getLocation());
        ShopUtils.removeShop(plotId, shop.getId());
        SendMessage.sendPlayerMessage(player, "&aShop removed. The barrel has been dropped.");
    }

    // ── Helper ────────────────────────────────────────────────────────────────────

    /** Opens an admin or buyer menu, handling the MenuManager checked exception. */
    private void openGui(Player player, String plotId, String shopId, boolean admin) {
        try {
            PlayerMenuUtility pmu = MenuManager.getPlayerMenuUtility(player);
            if (pmu == null) {
                SendMessage.sendPlayerMessage(player, "&cError: Menu data not found. Please rejoin.");
                Bukkit.getLogger().warning("[Market] PlayerMenuUtility is null for " + player.getName());
                return;
            }
            
            if (admin) {
                new ShopAdminMenu(pmu, plotId, shopId).open();
            } else {
                new ShopBuyerMenu(pmu, plotId, shopId).open();
            }
        } catch (MenuManagerNotSetupException e) {
            SendMessage.sendPlayerMessage(player, "&cMenu system error. Please rejoin and try again.");
            org.bukkit.Bukkit.getLogger().severe("[Market] MenuManagerNotSetupException for " + player.getName());
        } catch (Exception e) {
            SendMessage.sendPlayerMessage(player, "&cUnexpected error opening menu.");
            e.printStackTrace();
        }
    }
}
