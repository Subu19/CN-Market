package net.craftnepal.market.Listeners;

import net.craftnepal.market.files.RegionData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RegionSelection implements Listener {
    private static final Map<UUID, String> selectionModePlayers = new ConcurrentHashMap<>();
    private static final Map<UUID, Location> playerSelections = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> selectionCooldowns = new ConcurrentHashMap<>();

    private static final long SELECTION_COOLDOWN_MS = 1000;
    private static final Material SELECTION_TOOL = Material.STICK;

    public enum SelectionMode {
        MARKET,
        MARKET_PLOT
    }

    @EventHandler
    public void onRegionSelection(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!isPlayerInSelectionMode(uuid) || !isHoldingSelectionTool(player)) {
            return;
        }

        if (!event.getAction().name().contains("RIGHT_CLICK") || event.getClickedBlock() == null) {
            player.sendMessage("Cannot find block!");
            return;
        }

        if (isOnCooldown(uuid)) {
            return;
        }

        handleSelection(player, event.getClickedBlock().getLocation());
        selectionCooldowns.put(uuid, System.currentTimeMillis());
    }

    private boolean isPlayerInSelectionMode(UUID uuid) {
        return selectionModePlayers.containsKey(uuid);
    }

    private boolean isHoldingSelectionTool(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        return item.getType() == SELECTION_TOOL;
    }

    private boolean isOnCooldown(UUID uuid) {
        long currentTime = System.currentTimeMillis();
        Long lastSelectionTime = selectionCooldowns.get(uuid);
        return lastSelectionTime != null && currentTime - lastSelectionTime < SELECTION_COOLDOWN_MS;
    }

    private void handleSelection(Player player, Location clickedLocation) {
        UUID uuid = player.getUniqueId();

        if (!playerSelections.containsKey(uuid)) {
            // First selection point
            playerSelections.put(uuid, clickedLocation);
            player.sendMessage("Position 1 selected! Please select another location.");
            return;
        }

        // Second selection point - complete the region
        Location point1 = playerSelections.get(uuid);
        Location point2 = clickedLocation;

        RegionBounds bounds = new RegionBounds(point1, point2);

        String mode = selectionModePlayers.get(uuid);

        try {
            if (SelectionMode.MARKET.name().equals(mode)) {
                saveMarketRegion(bounds, player);
            } else if (SelectionMode.MARKET_PLOT.name().equals(mode)) {
                saveMarketPlot(bounds, player);
            }
        } finally {
            cleanupSelection(uuid);
        }
    }

    private void saveMarketRegion(RegionBounds bounds, Player player) {
        RegionData.get().set("market.posMin", bounds.getMin());
        RegionData.get().set("market.posMax", bounds.getMax());
        RegionData.save();
        player.sendMessage("Region created and is protected from grief!");
    }

    private void saveMarketPlot(RegionBounds bounds, Player player) {
        ConfigurationSection plotsSection = RegionData.get().getConfigurationSection("market.plots");
        int newPlotId = 0;

        if (plotsSection != null) {
            // Find the highest existing plot ID and check for overlaps
            for (String plotId : plotsSection.getKeys(false)) {
                try {
                    int currentId = Integer.parseInt(plotId);
                    newPlotId = Math.max(newPlotId, currentId + 1);

                    RegionBounds existingPlot = getPlotBounds(plotId);
                    if (bounds.intersects(existingPlot)) {
                        player.sendMessage("Error: The selected area overlaps with existing plot " + plotId);
                        return;
                    }
                } catch (NumberFormatException e) {
                    Bukkit.getLogger().warning("Invalid plot ID found in config: " + plotId);
                }
            }
        }

        // Save the new plot
        RegionData.get().set("market.plots." + newPlotId + ".posMin", bounds.getMin());
        RegionData.get().set("market.plots." + newPlotId + ".posMax", bounds.getMax());
        RegionData.save();

        player.sendMessage("Created new plot: " + newPlotId);
    }

    private RegionBounds getPlotBounds(String plotId) {
        Location min = RegionData.get().getLocation("market.plots." + plotId + ".posMin");
        Location max = RegionData.get().getLocation("market.plots." + plotId + ".posMax");
        return new RegionBounds(min, max);
    }

    private void cleanupSelection(UUID uuid) {
        selectionModePlayers.remove(uuid);
        playerSelections.remove(uuid);
    }

    // Helper class to handle region bounds operations
    private static class RegionBounds {
        private final Location min;
        private final Location max;

        public RegionBounds(Location point1, Location point2) {
            this.min = new Location(
                    point1.getWorld(),
                    Math.min(point1.getX(), point2.getX()),
                    Math.min(point1.getY(), point2.getY()),
                    Math.min(point1.getZ(), point2.getZ())
            );
            this.max = new Location(
                    point1.getWorld(),
                    Math.max(point1.getX(), point2.getX()),
                    Math.max(point1.getY(), point2.getY()),
                    Math.max(point1.getZ(), point2.getZ())
            );
        }

        public Location getMin() {
            return min;
        }

        public Location getMax() {
            return max;
        }

        public boolean intersects(RegionBounds other) {
            return (min.getX() <= other.max.getX() && max.getX() >= other.min.getX()) &&
                    (min.getY() <= other.max.getY() && max.getY() >= other.min.getY()) &&
                    (min.getZ() <= other.max.getZ() && max.getZ() >= other.min.getZ());
        }

        public boolean contains(RegionBounds other) {
            return min.getX() <= other.min.getX() && max.getX() >= other.max.getX() &&
                    min.getY() <= other.min.getY() && max.getY() >= other.max.getY() &&
                    min.getZ() <= other.min.getZ() && max.getZ() >= other.max.getZ();
        }
    }

    // Static utility methods
    public static void addSelectionModePlayer(UUID uuid, SelectionMode mode) {
        selectionModePlayers.put(uuid, mode.name());
    }

    public static void removeSelectionModePlayer(UUID uuid) {
        selectionModePlayers.remove(uuid);
        playerSelections.remove(uuid);
    }

    public static boolean isInSelectionMode(UUID uuid) {
        return selectionModePlayers.containsKey(uuid);
    }
}