package net.craftnepal.market.managers;

import net.craftnepal.market.Entities.ChestShop;
import net.craftnepal.market.plot.Plot;
import net.craftnepal.market.plot.PlotIndex;
import net.craftnepal.market.shop.ShopIndex;
import net.craftnepal.market.utils.DisplayUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Storage facade. Plots, shops, last locations and the market spawn live in memory and are read
 * from there; every change updates memory on the server thread, then queues the SQL on the
 * {@link Database} thread. Only offline earnings are read from SQL, asynchronously.
 */
public final class DatabaseManager {

    private static Database db;
    private static final PlotIndex plotIndex = new PlotIndex();
    private static final ShopIndex shopIndex = new ShopIndex();
    /** uuid → serialized location. Deserialized on read so unloaded worlds do not matter. */
    private static final Map<String, String> lastLocations = new HashMap<>();
    private static volatile String marketSpawn;

    private DatabaseManager() {}

    public static void initialize(Plugin plugin, File dbFile) {
        try {
            db = new Database(plugin, dbFile);
            db.await("create schema", DatabaseManager::createSchema);
        } catch (IllegalStateException e) {
            // Running on with empty memory would let new writes clobber the real data; Bukkit
            // disables the plugin when onEnable throws.
            if (db != null) db.close();
            db = null;
            throw new IllegalStateException("Could not open market.db; Market will not start", e);
        }
        // Shop locations need the world, so load it before reading shops.
        MarketWorldManager.loadWorld(false);
        // Queues its writes on the database thread; the loads below run after them.
        YamlMigration.migrateFromYaml();
        loadAll();
    }

    public static void close() {
        if (db != null) db.close();
    }

    /** Bump when the tables change, and add the migration next to {@link #createSchema}. */
    static final int SCHEMA_VERSION = 1;

    private static Void createSchema(Connection c) throws SQLException {
        try (Statement stmt = c.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA journal_mode = WAL;");
            stmt.execute("PRAGMA synchronous = NORMAL;");
            stmt.execute("PRAGMA busy_timeout = 5000;");
            stmt.execute("CREATE TABLE IF NOT EXISTS plots (id TEXT PRIMARY KEY, owner TEXT, pos_min TEXT, pos_max TEXT, spawn TEXT);");
            stmt.execute("CREATE TABLE IF NOT EXISTS plot_members (plot_id TEXT, player_uuid TEXT, PRIMARY KEY (plot_id, player_uuid),"
                    + " FOREIGN KEY (plot_id) REFERENCES plots(id) ON DELETE CASCADE);");
            stmt.execute("CREATE TABLE IF NOT EXISTS shops (id TEXT PRIMARY KEY, plot_id TEXT, location TEXT NOT NULL,"
                    + " item_bytes TEXT NOT NULL, item_material TEXT NOT NULL, owner TEXT NOT NULL, price REAL NOT NULL,"
                    + " is_buying_shop INTEGER NOT NULL, is_admin INTEGER NOT NULL, stock INTEGER NOT NULL DEFAULT 0,"
                    + " FOREIGN KEY (plot_id) REFERENCES plots(id) ON DELETE CASCADE);");
            stmt.execute("CREATE TABLE IF NOT EXISTS players (uuid TEXT PRIMARY KEY, offline_earnings REAL NOT NULL DEFAULT 0.0);");
            stmt.execute("CREATE TABLE IF NOT EXISTS player_locations (uuid TEXT PRIMARY KEY, location TEXT NOT NULL);");
            stmt.execute("CREATE TABLE IF NOT EXISTS global_metadata (key TEXT PRIMARY KEY, value TEXT);");
            stmt.execute("CREATE TABLE IF NOT EXISTS offline_sales (uuid TEXT NOT NULL, item_key TEXT NOT NULL,"
                    + " item_display_name TEXT NOT NULL, quantity INTEGER NOT NULL DEFAULT 0,"
                    + " earnings REAL NOT NULL DEFAULT 0.0, PRIMARY KEY (uuid, item_key));");
            try (ResultSet rs = stmt.executeQuery("SELECT value FROM global_metadata WHERE key = 'schema_version'")) {
                int stored = rs.next() ? Integer.parseInt(rs.getString(1)) : 0;
                if (stored > SCHEMA_VERSION) {
                    throw new SQLException("market.db was written by a newer Market (schema " + stored
                            + ", this build knows " + SCHEMA_VERSION + "); refusing to touch it.");
                }
            }
            stmt.execute("INSERT OR REPLACE INTO global_metadata (key, value) VALUES ('schema_version', '" + SCHEMA_VERSION + "')");
        }
        return null;
    }

    // ── Startup load ───────────────────────────────────────────────────────────

    private record ShopRow(String id, String plotId, String location, String itemBytes, String owner,
                           double price, boolean buying, boolean admin, int stock) {}

    private record Snapshot(List<Plot> plots, List<String[]> members, List<ShopRow> shops,
                            Map<String, String> locations, String spawn) {}

    /** Reads every table on the database thread; builds Bukkit objects back on this thread. */
    private static void loadAll() {
        Snapshot s;
        try {
            s = db.await("load data", DatabaseManager::readSnapshot);
        } catch (IllegalStateException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[Market] Could not load data", e);
            return;
        }
        plotIndex.clear();
        for (Plot plot : s.plots()) plotIndex.put(plot);
        for (String[] m : s.members()) {
            Plot plot = plotIndex.get(m[0]);
            if (plot != null) plot.members().add(m[1]);
        }
        shopIndex.clear();
        for (ShopRow row : s.shops()) {
            ChestShop shop = toShop(row);
            if (shop != null) shopIndex.put(shop);
        }
        lastLocations.clear();
        lastLocations.putAll(s.locations());
        marketSpawn = s.spawn();
        Bukkit.getLogger().info("[Market] Loaded " + plotIndex.all().size() + " plots and "
                + shopIndex.size() + " shops into memory.");
    }

    private static Snapshot readSnapshot(Connection c) throws SQLException {
        List<Plot> plots = new ArrayList<>();
        List<String[]> members = new ArrayList<>();
        List<ShopRow> shops = new ArrayList<>();
        Map<String, String> locations = new HashMap<>();
        String spawn = null;
        try (Statement stmt = c.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT id, owner, pos_min, pos_max, spawn FROM plots")) {
                while (rs.next()) {
                    plots.add(new Plot(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
                }
            }
            try (ResultSet rs = stmt.executeQuery("SELECT plot_id, player_uuid FROM plot_members")) {
                while (rs.next()) members.add(new String[] {rs.getString(1), rs.getString(2)});
            }
            try (ResultSet rs = stmt.executeQuery("SELECT id, plot_id, location, item_bytes, owner, price,"
                    + " is_buying_shop, is_admin, stock FROM shops")) {
                while (rs.next()) {
                    shops.add(new ShopRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getString(5), rs.getDouble(6), rs.getInt(7) == 1, rs.getInt(8) == 1, rs.getInt(9)));
                }
            }
            try (ResultSet rs = stmt.executeQuery("SELECT uuid, location FROM player_locations")) {
                while (rs.next()) locations.put(rs.getString(1), rs.getString(2));
            }
            try (ResultSet rs = stmt.executeQuery("SELECT value FROM global_metadata WHERE key = 'market_spawn'")) {
                if (rs.next()) spawn = rs.getString(1);
            }
        }
        return new Snapshot(plots, members, shops, locations, spawn);
    }

    private static ChestShop toShop(ShopRow row) {
        Location loc = deserializeLocation(row.location());
        ItemStack item = null;
        try {
            item = row.itemBytes() == null ? null : deserializeItem(Base64.getDecoder().decode(row.itemBytes()));
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[Market] Shop " + row.id() + " has unreadable item data.");
        }
        if (loc == null || item == null) {
            Bukkit.getLogger().warning("[Market] Skipping shop " + row.id() + ": world not loaded or item unreadable.");
            return null;
        }
        ChestShop shop = new ChestShop(row.id(), loc, item, UUID.fromString(row.owner()), row.price(), row.admin(), row.buying());
        shop.setPlotId(row.plotId());
        shop.setStock(row.stock());
        return shop;
    }

    // ── Plots ──────────────────────────────────────────────────────────────────

    public static PlotIndex plotIndex() {
        return plotIndex;
    }

    public static boolean plotExists(String id) {
        return plotIndex.get(id) != null;
    }

    public static List<String> getActivePlotIds() {
        List<String> list = new ArrayList<>();
        for (Plot plot : plotIndex.all()) {
            if (plot.isOwned()) list.add(plot.id());
        }
        return list;
    }

    public static List<String> getAvailablePlotIds() {
        List<String> list = new ArrayList<>();
        for (Plot plot : plotIndex.all()) {
            if (!plot.isOwned()) list.add(plot.id());
        }
        return list;
    }

    public static List<String> getAllPlotIds() {
        return plotIndex.ids();
    }

    /**
     * Creates a plot or updates its owner, bounds and spawn. Members and shops are kept: an upsert,
     * not INSERT OR REPLACE, which would delete the row and cascade to both.
     */
    public static void savePlot(String id, String owner, Location posMin, Location posMax, Location spawn) {
        String min = serializeLocation(posMin), max = serializeLocation(posMax), sp = serializeLocation(spawn);
        Plot old = plotIndex.get(id);
        Plot plot = new Plot(id, owner, min, max, sp);
        if (old != null) plot.members().addAll(old.members());
        plotIndex.put(plot);
        db.write("save plot " + id, c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO plots (id, owner, pos_min, pos_max, spawn) VALUES (?, ?, ?, ?, ?)"
                    + " ON CONFLICT(id) DO UPDATE SET owner = excluded.owner, pos_min = excluded.pos_min,"
                    + " pos_max = excluded.pos_max, spawn = excluded.spawn")) {
                ps.setString(1, id);
                ps.setString(2, owner);
                ps.setString(3, min);
                ps.setString(4, max);
                ps.setString(5, sp);
                ps.executeUpdate();
            }
        });
    }

    public static void setPlotOwner(String id, String owner) {
        Plot plot = plotIndex.get(id);
        if (plot != null) plot.setOwner(owner);
        update("set owner of plot " + id, "UPDATE plots SET owner = ? WHERE id = ?", owner, id);
    }

    public static String getPlotOwner(String id) {
        Plot plot = plotIndex.get(id);
        return plot == null ? null : plot.owner();
    }

    public static Location getPlotPosMin(String id) {
        Plot plot = plotIndex.get(id);
        return plot == null ? null : deserializeLocation(plot.posMinText());
    }

    public static Location getPlotPosMax(String id) {
        Plot plot = plotIndex.get(id);
        return plot == null ? null : deserializeLocation(plot.posMaxText());
    }

    public static Location getPlotSpawn(String id) {
        Plot plot = plotIndex.get(id);
        return plot == null ? null : deserializeLocation(plot.spawnText());
    }

    public static void setPlotSpawn(String id, Location spawn) {
        String text = serializeLocation(spawn);
        Plot plot = plotIndex.get(id);
        if (plot != null) plot.setSpawnText(text);
        update("set spawn of plot " + id, "UPDATE plots SET spawn = ? WHERE id = ?", text, id);
    }

    /** Deletes a plot with its members and shops (the database cascades; memory and displays follow). */
    public static void deletePlot(String id) {
        plotIndex.remove(id);
        for (ChestShop shop : shopIndex.byPlot(id)) {
            shopIndex.remove(shop.getId());
            DisplayUtils.getInstance().removeDisplayPair(id, shop.getId());
        }
        update("delete plot " + id, "DELETE FROM plots WHERE id = ?", id);
    }

    /**
     * Releases a plot: no owner, no members, no shops. The plot row and its bounds stay, so a
     * manually defined plot remains claimable just like a grid plot.
     */
    public static void unclaimPlot(String id) {
        Plot plot = plotIndex.get(id);
        if (plot != null) {
            plot.setOwner(null);
            plot.members().clear();
        }
        for (ChestShop shop : shopIndex.byPlot(id)) {
            shopIndex.remove(shop.getId());
            DisplayUtils.getInstance().removeDisplayPair(id, shop.getId());
        }
        db.write("unclaim plot " + id, c -> {
            for (String sql : new String[] {"UPDATE plots SET owner = NULL WHERE id = ?",
                    "DELETE FROM plot_members WHERE plot_id = ?", "DELETE FROM shops WHERE plot_id = ?"}) {
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, id);
                    ps.executeUpdate();
                }
            }
        });
    }

    public static List<String> getPlotMembers(String plotId) {
        Plot plot = plotIndex.get(plotId);
        return plot == null ? new ArrayList<>() : new ArrayList<>(plot.members());
    }

    public static void addPlotMember(String plotId, String playerUuid) {
        Plot plot = plotIndex.get(plotId);
        if (plot != null) plot.members().add(playerUuid);
        update("add member to plot " + plotId, "INSERT OR IGNORE INTO plot_members (plot_id, player_uuid) VALUES (?, ?)", plotId, playerUuid);
    }

    public static void removePlotMember(String plotId, String playerUuid) {
        Plot plot = plotIndex.get(plotId);
        if (plot != null) plot.members().remove(playerUuid);
        update("remove member from plot " + plotId, "DELETE FROM plot_members WHERE plot_id = ? AND player_uuid = ?", plotId, playerUuid);
    }

    // ── Shops ──────────────────────────────────────────────────────────────────

    public static ShopIndex shopIndex() {
        return shopIndex;
    }

    public static void saveShop(ChestShop shop, String plotId, int stock) {
        shop.setPlotId(plotId);
        shop.setStock(stock);
        shopIndex.put(shop);
        String location = serializeLocation(shop.getLocation());
        String item = Base64.getEncoder().encodeToString(serializeItem(shop.getItem()));
        String material = shop.getItem().getType().toString();
        String owner = shop.getOwner().toString();
        double price = shop.getPrice();
        int buying = shop.isBuyingShop() ? 1 : 0, admin = shop.isAdmin() ? 1 : 0;
        db.write("save shop " + shop.getId(), c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT OR REPLACE INTO shops (id, plot_id, location, item_bytes,"
                    + " item_material, owner, price, is_buying_shop, is_admin, stock) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, shop.getId());
                ps.setString(2, plotId);
                ps.setString(3, location);
                ps.setString(4, item);
                ps.setString(5, material);
                ps.setString(6, owner);
                ps.setDouble(7, price);
                ps.setInt(8, buying);
                ps.setInt(9, admin);
                ps.setInt(10, stock);
                ps.executeUpdate();
            }
        });
    }

    public static void removeShop(String id) {
        shopIndex.remove(id);
        update("remove shop " + id, "DELETE FROM shops WHERE id = ?", id);
    }

    public static void updateShopPrice(String id, double price) {
        ChestShop shop = shopIndex.get(id);
        if (shop != null) shop.setPrice(price);
        update("update price of shop " + id, "UPDATE shops SET price = ? WHERE id = ?", price, id);
    }

    public static void updateShopStock(String id, int stock) {
        ChestShop shop = shopIndex.get(id);
        if (shop != null) shop.setStock(stock);
        update("update stock of shop " + id, "UPDATE shops SET stock = ? WHERE id = ?", stock, id);
    }

    public static ChestShop getShop(String id) {
        return shopIndex.get(id);
    }

    public static ChestShop getShopAt(Location location) {
        return shopIndex.at(location);
    }

    public static List<ChestShop> getShopsByPlot(String plotId) {
        return shopIndex.byPlot(plotId);
    }

    public static List<ChestShop> getShopsByOwner(String ownerUuid) {
        return shopIndex.byOwner(ownerUuid);
    }

    // ── Offline earnings ───────────────────────────────────────────────────────

    public record OfflineSale(String getItemKey, String getItemDisplayName, int getQuantity, double getEarnings) {}

    /** What a player earned while offline: per-item sales plus the legacy lump sum. */
    public record OfflineEarnings(List<OfflineSale> sales, double legacy) {}

    public static void setOfflineEarnings(String uuid, double amount) {
        update("set offline earnings of " + uuid, "INSERT OR REPLACE INTO players (uuid, offline_earnings) VALUES (?, ?)", uuid, amount);
    }

    public static void addOfflineSale(String uuid, String itemKey, String itemDisplayName, int quantity, double earnings) {
        update("add offline sale for " + uuid, "INSERT INTO offline_sales (uuid, item_key, item_display_name, quantity, earnings)"
                + " VALUES (?, ?, ?, ?, ?) ON CONFLICT(uuid, item_key) DO UPDATE SET quantity = quantity + excluded.quantity,"
                + " earnings = earnings + excluded.earnings, item_display_name = excluded.item_display_name",
                uuid, itemKey, itemDisplayName, quantity, earnings);
    }

    /** Reads offline earnings on the database thread and calls back on the server thread. */
    public static void readOfflineEarnings(String uuid, Consumer<OfflineEarnings> callback) {
        db.read("read offline earnings of " + uuid, c -> {
            List<OfflineSale> sales = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT item_key, item_display_name, quantity, earnings FROM offline_sales WHERE uuid = ?")) {
                ps.setString(1, uuid);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) sales.add(new OfflineSale(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getDouble(4)));
                }
            }
            double legacy = 0;
            try (PreparedStatement ps = c.prepareStatement("SELECT offline_earnings FROM players WHERE uuid = ?")) {
                ps.setString(1, uuid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) legacy = rs.getDouble(1);
                }
            }
            return new OfflineEarnings(sales, legacy);
        }, callback);
    }

    /** Marks offline earnings as delivered. */
    public static void clearOfflineEarnings(String uuid) {
        setOfflineEarnings(uuid, 0.0);
        update("clear offline sales of " + uuid, "DELETE FROM offline_sales WHERE uuid = ?", uuid);
    }

    // ── Last locations (/market back) ──────────────────────────────────────────

    public static void saveLastLocation(String uuid, Location location) {
        if (location == null) {
            clearLastLocation(uuid);
            return;
        }
        String text = serializeLocation(location);
        lastLocations.put(uuid, text);
        update("save last location of " + uuid, "INSERT OR REPLACE INTO player_locations (uuid, location) VALUES (?, ?)", uuid, text);
    }

    public static Location getLastLocation(String uuid) {
        return deserializeLocation(lastLocations.get(uuid));
    }

    public static void clearLastLocation(String uuid) {
        if (lastLocations.remove(uuid) == null) return;
        update("clear last location of " + uuid, "DELETE FROM player_locations WHERE uuid = ?", uuid);
    }

    // ── Market spawn ───────────────────────────────────────────────────────────

    public static void setMarketSpawn(Location spawn) {
        String text = serializeLocation(spawn);
        marketSpawn = text;
        update("set market spawn", "INSERT OR REPLACE INTO global_metadata (key, value) VALUES ('market_spawn', ?)", text);
    }

    public static Location getMarketSpawn() {
        return deserializeLocation(marketSpawn);
    }

    // ── Reset ──────────────────────────────────────────────────────────────────

    /** Wipes plots, shops, players, locations and metadata. Offline sales are kept: that money is owed. */
    public static void clearAllData() {
        plotIndex.clear();
        shopIndex.clear();
        DisplayUtils.getInstance().clearAllDisplays();
        lastLocations.clear();
        marketSpawn = null;
        db.write("clear all data", c -> {
            try (Statement stmt = c.createStatement()) {
                for (String table : new String[] {"plot_members", "shops", "plots", "players", "player_locations"}) {
                    stmt.executeUpdate("DELETE FROM " + table);
                }
                stmt.executeUpdate("DELETE FROM global_metadata WHERE key <> 'schema_version'");
            }
        });
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Queues a single parameterised statement. */
    private static void update(String operation, String sql, Object... params) {
        db.write(operation, c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
                ps.executeUpdate();
            }
        });
    }

    public static String serializeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return loc.getWorld().getName() + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ() + ";" + loc.getYaw() + ";" + loc.getPitch();
    }

    public static Location deserializeLocation(String str) {
        if (str == null || str.isBlank()) return null;
        try {
            String[] parts = str.split(";");
            if (parts.length < 4) return null;
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) return null;
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0.0f;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0.0f;
            return new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]), yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static byte[] serializeItem(ItemStack item) {
        try (ByteArrayOutputStream io = new ByteArrayOutputStream();
             BukkitObjectOutputStream os = new BukkitObjectOutputStream(io)) {
            os.writeObject(item);
            os.flush();
            return io.toByteArray();
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "[Market] Could not serialize item " + item, e);
            return new byte[0];
        }
    }

    static ItemStack deserializeItem(byte[] bytes) {
        try (BukkitObjectInputStream is = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (ItemStack) is.readObject();
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[Market] Could not deserialize an item", e);
            return null;
        }
    }
}
