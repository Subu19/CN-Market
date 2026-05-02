package net.craftnepal.market.Listeners;

import net.craftnepal.market.Market;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.*;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Movement implements Listener {
    private static final Map<UUID, Boolean> playersInMarket = new ConcurrentHashMap<>();

    private final Market plugin;

    public Movement(Market plugin) {
        this.plugin = plugin;
    }

    public static Map<UUID, Boolean> getPlayersInMarket() {
        return new HashMap<>(playersInMarket);
    }

    public static boolean isPlayerInMarket(UUID uuid) {
        Boolean inMarket = playersInMarket.get(uuid);
        return inMarket != null && inMarket;
    }

    @EventHandler
    public void onMarketMovement(PlayerMoveEvent e) {
        //Cancel any teleports if any pending for that player!
        TeleportUtils.handleMovement(e);

        // Only process if player actually moved blocks
        if (e.getFrom().getBlockX() == e.getTo().getBlockX() && 
            e.getFrom().getBlockY() == e.getTo().getBlockY() && 
            e.getFrom().getBlockZ() == e.getTo().getBlockZ()) {
            return;
        }

        Player player = e.getPlayer();

        boolean allowFlight = Market.getMainConfig().getBoolean("allow-flight", false);
        if (!allowFlight) {
            return;
        }

        boolean isInsideMarket = MarketUtils.isInMarketArea(player.getLocation());

        UUID uuid = player.getUniqueId();
        if (isInsideMarket) {
            handleEnterMarket(player, uuid);
        } else {
            handleExitMarket(player, uuid);
        }
    }    private void handleEnterMarket(Player player, UUID uuid) {
        if (!player.getAllowFlight()) {
            player.setAllowFlight(true);
            playersInMarket.put(uuid, true);
            SendMessage.sendPlayerMessage(player, "&aEnabled flying!");
        }
    }

    private void handleExitMarket(Player player, UUID uuid) {
        if (playersInMarket.containsKey(uuid) && player.getAllowFlight()) {
            player.setAllowFlight(false);
            playersInMarket.remove(uuid);
            SendMessage.sendPlayerMessage(player, "&cDisabled flying!");
        }
    }

    public static void checkAndToggle(Player player, boolean toggle) {
        boolean allowFlight = Market.getMainConfig().getBoolean("allow-flight", false);
        if (allowFlight) {
            player.setAllowFlight(toggle);

            if (toggle) {
                playersInMarket.put(player.getUniqueId(), true);
            } else {
                playersInMarket.remove(player.getUniqueId());
            }
        }
    }
}