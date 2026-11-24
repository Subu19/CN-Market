package net.craftnepal.market.utils;

import net.craftnepal.market.Market;
import net.craftnepal.market.MarketSettings;
import net.craftnepal.market.managers.DatabaseManager;
import net.craftnepal.market.plot.Plot;
import net.craftnepal.market.plot.PlotGrid;
import net.craftnepal.market.plot.Zones;
import net.craftnepal.market.subcommands.admin.Bypass;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Plot lookups. Everything here reads the in-memory {@link net.craftnepal.market.plot.PlotIndex} and {@link PlotGrid}; no call
 * touches the database except the write helpers. Main-thread only.
 */
public final class PlotUtils {

    private PlotUtils() {}

    public static List<String> getActivePlotIds() {
        return DatabaseManager.getActivePlotIds();
    }

    // ── Location → plot ──────────────────────────────────────────────────────

    public static String getPlotIdByLocation(Location location) {
        if (location == null || location.getWorld() == null) return null;
        return plotIdAt(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static String getPlotIdByBlock(Block block) {
        return plotIdAt(block.getWorld(), block.getX(), block.getY(), block.getZ());
    }

    /** Plot id at a block: a plot with its own bounds first, then the automatic grid. */
    public static String plotIdAt(World world, int x, int y, int z) {
        return Zones.plotIdAt(DatabaseManager.plotIndex(), Market.settings().grid(), world.getName(),
                world == Market.getPlugin().getMarketWorld(), x, y, z);
    }

    /** True when both blocks are in the same plot, or both are outside every plot. */
    public static boolean sameZone(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
        return Zones.sameZone(DatabaseManager.plotIndex(), Market.settings().grid(), world.getName(),
                world == Market.getPlugin().getMarketWorld(), x1, y1, z1, x2, y2, z2);
    }

    public static boolean sameZone(Block a, Block b) {
        return sameZone(a.getWorld(), a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
    }

    /** True when the block and its six neighbours are certainly in the same plot. */
    public static boolean isGridInterior(Block block) {
        return !DatabaseManager.plotIndex().hasBoundedPlots()
                && Market.settings().grid().isInterior(block.getX(), block.getZ());
    }

    // ── Plot data ────────────────────────────────────────────────────────────

    public static Plot getPlot(String plotId) {
        return DatabaseManager.plotIndex().get(plotId);
    }

    public static void registerAutomaticPlot(String plotId) {
        if (DatabaseManager.plotExists(plotId)) return;
        int[] cell = PlotGrid.parseId(plotId);
        if (cell == null) return;

        MarketSettings settings = Market.settings();
        PlotGrid grid = settings.grid();
        World world = Market.getPlugin().getMarketWorld();
        Location min = new Location(world, grid.minBlock(cell[0]), 64, grid.minBlock(cell[1]));
        Location max = new Location(world, grid.maxBlock(cell[0]), settings.maxHeight(), grid.maxBlock(cell[1]));
        DatabaseManager.savePlot(plotId, null, min, max, null);
    }

    private static void ensureRegistered(String plotId) {
        if (plotId != null && plotId.startsWith(PlotGrid.GRID_PREFIX) && !DatabaseManager.plotExists(plotId)) {
            registerAutomaticPlot(plotId);
        }
    }

    public static String getPlotOwner(String plotId) {
        return DatabaseManager.getPlotOwner(plotId);
    }

    public static String getPlotIdByPlayer(Player player) {
        String uuid = player.getUniqueId().toString();
        for (Plot plot : DatabaseManager.plotIndex().all()) {
            if (uuid.equals(plot.owner())) return plot.id();
        }
        return null;
    }

    public static int getPlotCount(Player player) {
        String uuid = player.getUniqueId().toString();
        int count = 0;
        for (Plot plot : DatabaseManager.plotIndex().all()) {
            if (uuid.equals(plot.owner())) count++;
        }
        return count;
    }

    public static int getPlotLimit(Player player) {
        if (player.hasPermission("market.plots.limit.unlimited")) {
            return Integer.MAX_VALUE;
        }
        int limit = Market.settings().maxPlotsPerPlayer();
        // ponytail: up to 100 permission checks, but only on /market plot claim.
        for (int i = 100; i > limit; i--) {
            if (player.hasPermission("market.plots.limit." + i)) {
                return i;
            }
        }
        return limit;
    }

    public static boolean isSpawnPlot(String plotId) {
        return Market.settings().grid().isSpawnPlot(plotId);
    }

    public static boolean isSpawnLocation(Location location) {
        World marketWorld = Market.getPlugin().getMarketWorld();
        if (marketWorld == null || location.getWorld() != marketWorld) return false;
        return Market.settings().grid().isSpawnArea(location.getBlockX(), location.getBlockZ());
    }

    public static Location getPlotCenter(String plotId) {
        ensureRegistered(plotId);
        Location min = DatabaseManager.getPlotPosMin(plotId);
        Location max = DatabaseManager.getPlotPosMax(plotId);
        if (min == null || max == null) return null;
        return new Location(min.getWorld(), (min.getX() + max.getX()) / 2, min.getY() + 1,
                (min.getZ() + max.getZ()) / 2);
    }

    public static boolean isPlayerInOwnPlot(Player player) {
        Plot plot = getPlot(getPlotIdByLocation(player.getLocation()));
        return plot != null && player.getUniqueId().toString().equals(plot.owner());
    }

    public static Location getPlotSpawn(String plotId) {
        ensureRegistered(plotId);
        return DatabaseManager.getPlotSpawn(plotId);
    }

    public static Location getPlotPosMin(String plotId) {
        ensureRegistered(plotId);
        return DatabaseManager.getPlotPosMin(plotId);
    }

    public static Location getPlotPosMax(String plotId) {
        ensureRegistered(plotId);
        return DatabaseManager.getPlotPosMax(plotId);
    }

    /**
     * Set the owner of a plot.
     * @param ownerUUID the new owner's UUID, or null to unclaim
     */
    public static void setPlotOwner(String plotId, String ownerUUID) {
        ensureRegistered(plotId);
        DatabaseManager.setPlotOwner(plotId, ownerUUID);
    }

    public static void setPlotSpawn(String plotId, Location location) {
        ensureRegistered(plotId);
        DatabaseManager.setPlotSpawn(plotId, location);
    }

    /**
     * Teleport a player to their own plot's spawn point.
     * @return true if a teleport was scheduled
     */
    public static boolean teleportToOwnPlotSpawn(Player player) {
        String plotId = getPlotIdByPlayer(player);
        if (plotId == null) {
            SendMessage.sendPlayerMessage(player, "&cYou don't own a plot!");
            return false;
        }

        Location spawn = getPlotSpawn(plotId);
        if (spawn == null) {
            SendMessage.sendPlayerMessage(player, "&cYour plot doesn't have a spawn point set! Use /market plot setspawn to set one.");
            return false;
        }
        SendMessage.sendPlayerMessage(player, "&eTeleporting in 5 seconds.. don't move!");
        TeleportUtils.teleportFromOrigin(player, spawn, () -> {
            SendMessage.sendPlayerMessage(player, "&aTeleported to your plot's spawn point!");
        });
        return true;
    }

    /**
     * Whether a player may build at or interact with a location: bypass, outside the market, or
     * owner/member of the plot there. Pathways, spawn and anything above the height limit are denied.
     */
    public static boolean canPlayerInteract(Player player, Location location) {
        if (Bypass.bypassPlayers.containsKey(player.getUniqueId())) return true;
        if (!MarketUtils.isInMarketArea(location)) return true;
        return isTrusted(player, location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static boolean canPlayerInteract(Player player, Block block) {
        if (Bypass.bypassPlayers.containsKey(player.getUniqueId())) return true;
        if (!MarketUtils.isInMarketWorld(block.getWorld())) return true;
        return isTrusted(player, block.getWorld(), block.getX(), block.getY(), block.getZ());
    }

    private static boolean isTrusted(Player player, World world, int x, int y, int z) {
        return Zones.isTrusted(DatabaseManager.plotIndex(), Market.settings().grid(), world.getName(),
                Market.settings().maxHeight(), player.getUniqueId().toString(), x, y, z);
    }
}
