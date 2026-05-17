package net.craftnepal.market.handlers;

import net.craftnepal.market.Market;
import net.craftnepal.market.Listeners.Movement;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.*;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the base /market command, which teleports the player to the market spawn.
 */
public class MarketCommandHandler {

    public static void handle(CommandSender sender) {
        if (!(sender instanceof Player)) return;

        Player p = (Player) sender;

        if (!p.hasPermission("market.use")) {
            SendMessage.sendPlayerMessage(p, "§cYou do not have permission to use the market.");
            return;
        }

        World marketWorld = Market.getPlugin().getMarketWorld();

        if (marketWorld == null) {
            SendMessage.sendPlayerMessage(p, "§cThe market world has not been set up yet.");
            if (p.hasPermission("market.admin")) {
                SendMessage.sendPlayerMessage(p,
                        "§eAdmin: §7Use §f/market admin setup <world> <plotSize> <pathwayWidth> §7to initialize the market.");
            }
            return;
        }

        Location location = RegionData.get().getLocation("market.spawn");
        if (location == null) {
            location = marketWorld.getSpawnLocation();
        }

        Location origin = p.getLocation();
        SendMessage.sendPlayerMessage(p, "&eTeleporting to the market in 5 seconds... don't move!");

        TeleportUtils.scheduleTeleport(p, location, () -> {
            if (!MarketUtils.isInMarketArea(origin)) {
                PlayerUtils.saveLastLocation(p, origin);
            }
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            Movement.checkAndToggle(p, true);
        });
    }
}
