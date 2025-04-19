package net.craftnepal.market.Listeners;

import net.craftnepal.market.Market;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.RegionUtils;
import net.craftnepal.market.utils.SendMessage;
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
    private static final Map<UUID, BukkitRunnable> pendingFlightDisables = new ConcurrentHashMap<>();
    private static final int FLIGHT_DISABLE_WARNING_TIME = 5; // seconds

    private final Market plugin;

    public Movement(Market plugin) {
        this.plugin = plugin;
    }

    public static Map<UUID, Boolean> getPlayersInMarket() {
        return new HashMap<>(playersInMarket);
    }

    @EventHandler
    public void onMarketMovement(PlayerMoveEvent e) {
        // Only process if player actually moved blocks
        if (e.getFrom().getBlock().equals(e.getTo().getBlock())) {
            return;
        }

        Player player = e.getPlayer();
        Location min = RegionData.get().getLocation("market.posMin");
        Location max = RegionData.get().getLocation("market.posMax");

        if (min == null || max == null) {
            return;
        }

        boolean allowFlight = Market.getMainConfig().getBoolean("allow-flight", false);
        if (!allowFlight) {
            return;
        }

        boolean isInsideMarket = RegionUtils.isLocationInsideRegion(player.getLocation(), min, max);
        UUID uuid = player.getUniqueId();

        if (isInsideMarket) {
            handleEnterMarket(player, uuid);
        } else {
            handleExitMarket(player, uuid);
        }
    }

    private void handleEnterMarket(Player player, UUID uuid) {
        // Cancel any pending flight disable
        cancelPendingFlightDisable(uuid);

        if (!player.getAllowFlight()) {
            player.setAllowFlight(true);
            playersInMarket.put(uuid, true);
            SendMessage.sendPlayerMessage(player, "&aEnabled flying!");
        }
    }

    private void handleExitMarket(Player player, UUID uuid) {
        if (playersInMarket.containsKey(uuid) && player.getAllowFlight()) {
            // If already has a pending disable, don't create another one
            if (pendingFlightDisables.containsKey(uuid)) {
                return;
            }

            SendMessage.sendPlayerMessage(player, "&eFlight will be disabled in " + FLIGHT_DISABLE_WARNING_TIME + " seconds!");

            BukkitRunnable disableTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        player.setAllowFlight(false);
                        playersInMarket.remove(uuid);
                        SendMessage.sendPlayerMessage(player, "&cDisabled flying!");
                    }
                    pendingFlightDisables.remove(uuid);
                }
            };

            disableTask.runTaskLater(plugin, FLIGHT_DISABLE_WARNING_TIME * 20L); // 20 ticks = 1 second
            pendingFlightDisables.put(uuid, disableTask);
        }
    }

    private void cancelPendingFlightDisable(UUID uuid) {
        BukkitRunnable task = pendingFlightDisables.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    public static void checkAndToggle(Player player, boolean toggle) {
        boolean allowFlight = Market.getMainConfig().getBoolean("allow-flight", false);
        if (allowFlight) {
            player.setAllowFlight(toggle);

            if (toggle) {
                playersInMarket.put(player.getUniqueId(), true);
                pendingFlightDisables.remove(player.getUniqueId());
            } else {
                playersInMarket.remove(player.getUniqueId());
            }
        }
    }
}