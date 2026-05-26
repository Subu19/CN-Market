package net.craftnepal.market.managers;

import net.craftnepal.market.Entities.ChestShop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {

    private static Connection connection;
    private static File databaseFile;

    public static void initialize(File dbFile) {
        databaseFile = dbFile;
        try {
            // Ensure connection is established and tables exist
            Connection conn = getConnection();
            try (Statement stmt = conn.createStatement()) {
                // Enable foreign keys
                stmt.execute("PRAGMA foreign_keys = ON;");

                // Plots table
                stmt.execute("CREATE TABLE IF NOT EXISTS plots (" +
                        "id TEXT PRIMARY KEY," +
                        "owner TEXT," +
                        "pos_min TEXT," +
                        "pos_max TEXT," +
                        "spawn TEXT" +
                        ");");

                // Plot members table
                stmt.execute("CREATE TABLE IF NOT EXISTS plot_members (" +
                        "plot_id TEXT," +
                        "player_uuid TEXT," +
                        "PRIMARY KEY (plot_id, player_uuid)," +
                        "FOREIGN KEY (plot_id) REFERENCES plots(id) ON DELETE CASCADE" +
                        ");");

                // Shops table (with stock column to avoid accesses that cause lag)
                stmt.execute("CREATE TABLE IF NOT EXISTS shops (" +
                        "id TEXT PRIMARY KEY," +
                        "plot_id TEXT," +
                        "location TEXT NOT NULL," +
                        "item_bytes TEXT NOT NULL," +
                        "item_material TEXT NOT NULL," +
                        "owner TEXT NOT NULL," +
                        "price REAL NOT NULL," +
                        "is_buying_shop INTEGER NOT NULL," +
                        "is_admin INTEGER NOT NULL," +
                        "stock INTEGER NOT NULL DEFAULT 0," +
                        "FOREIGN KEY (plot_id) REFERENCES plots(id) ON DELETE CASCADE" +
                        ");");

                // Players table (offline earnings)
                stmt.execute("CREATE TABLE IF NOT EXISTS players (" +
                        "uuid TEXT PRIMARY KEY," +
                        "offline_earnings REAL NOT NULL DEFAULT 0.0" +
                        ");");

                // Player locations table (last locations before tp to market)
                stmt.execute("CREATE TABLE IF NOT EXISTS player_locations (" +
                        "uuid TEXT PRIMARY KEY," +
                        "location TEXT NOT NULL" +
                        ");");

                // Global metadata table (e.g. for market spawn or other global state)
                stmt.execute("CREATE TABLE IF NOT EXISTS global_metadata (" +
                        "key TEXT PRIMARY KEY," +
                        "value TEXT" +
                        ");");
            }
            
            // Perform automatic one-time migration if legacy YAML data files exist
            migrateFromYaml();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Could not initialize SQLite database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void migrateFromYaml() {
        // Ensure the market world is loaded so that Location instances can be created correctly
        String worldName = net.craftnepal.market.Market.getMainConfig().getString("market-world.name", "market");
        if (Bukkit.getWorld(worldName) == null) {
            Bukkit.getLogger().info("[Market] Loading world '" + worldName + "' for migration...");
            org.bukkit.WorldCreator creator = new org.bukkit.WorldCreator(worldName);
            creator.generator(new net.craftnepal.market.world.MarketGenerator());
            creator.createWorld();
        }

        File dataFolder = Bukkit.getServer().getPluginManager().getPlugin("Market").getDataFolder();
        File regionFile = new File(dataFolder, "RegionData.yml");
        File locationFile = new File(dataFolder, "LocationData.yml");

        if (!regionFile.exists() && !locationFile.exists()) {
            return; // No files to migrate
        }

        org.bukkit.configuration.file.FileConfiguration regionConfig = regionFile.exists() ? org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(regionFile) : null;
        org.bukkit.configuration.file.FileConfiguration locationConfig = locationFile.exists() ? org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(locationFile) : null;

        org.bukkit.configuration.ConfigurationSection plotsSection = regionConfig != null ? regionConfig.getConfigurationSection("market.plots") : null;
        org.bukkit.configuration.ConfigurationSection playersSection = regionConfig != null ? regionConfig.getConfigurationSection("market.players") : null;
        org.bukkit.configuration.ConfigurationSection locationsSection = locationConfig != null ? locationConfig.getConfigurationSection("players") : null;

        boolean hasPlots = plotsSection != null && !plotsSection.getKeys(false).isEmpty();
        boolean hasPlayers = playersSection != null && !playersSection.getKeys(false).isEmpty();
        boolean hasLocations = locationsSection != null && !locationsSection.getKeys(false).isEmpty();

        if (!hasPlots && !hasPlayers && !hasLocations) {
            return; // Nothing to migrate
        }

        Bukkit.getLogger().info("[Market] Starting automatic migration of data from YAML to SQLite database...");

        // 1. Migrate Plots & Shops
        if (hasPlots) {
            int plotCount = 0;
            int shopCount = 0;
            for (String plotId : plotsSection.getKeys(false)) {
                String path = "market.plots." + plotId;
                Location posMin = net.craftnepal.market.utils.LocationUtils.loadLocation(regionConfig, path + ".posMin");
                Location posMax = net.craftnepal.market.utils.LocationUtils.loadLocation(regionConfig, path + ".posMax");
                Location spawn = net.craftnepal.market.utils.LocationUtils.loadLocation(regionConfig, path + ".spawn");
                String owner = regionConfig.getString(path + ".owner");

                savePlot(plotId, owner, posMin, posMax, spawn);
                plotCount++;

                List<String> members = regionConfig.getStringList(path + ".members");
                if (members != null) {
                    for (String member : members) {
                        addPlotMember(plotId, member);
                    }
                }

                org.bukkit.configuration.ConfigurationSection shops = regionConfig.getConfigurationSection(path + ".shops");
                if (shops != null) {
                    for (String shopId : shops.getKeys(false)) {
                        String shopPath = path + ".shops." + shopId;
                        ChestShop shop = createShopFromYaml(regionConfig, shopId, shopPath);
                        if (shop != null) {
                            int stock = 0;
                            try {
                                stock = net.craftnepal.market.utils.ShopUtils.getShopStock(shop);
                            } catch (Exception e) {
                                // Default to 0 if we can't read the block state
                            }
                            saveShop(shop, plotId, stock);
                            shopCount++;
                        }
                    }
                }
            }
            Bukkit.getLogger().info("[Market] Migrated " + plotCount + " plots and " + shopCount + " shops.");
        }

        // 2. Migrate Player Offline Earnings
        if (hasPlayers) {
            int playerEarningsCount = 0;
            for (String uuidStr : playersSection.getKeys(false)) {
                double earnings = regionConfig.getDouble("market.players." + uuidStr + ".offline_earnings", 0.0);
                if (earnings > 0) {
                    setOfflineEarnings(uuidStr, earnings);
                    playerEarningsCount++;
                }
            }
            Bukkit.getLogger().info("[Market] Migrated offline earnings for " + playerEarningsCount + " players.");
        }

        // 3. Migrate Player Back Locations
        if (hasLocations) {
            int locationCount = 0;
            for (String uuidStr : locationsSection.getKeys(false)) {
                Location lastLoc = net.craftnepal.market.utils.LocationUtils.loadLocation(locationConfig, "players." + uuidStr);
                if (lastLoc != null) {
                    saveLastLocation(uuidStr, lastLoc);
                    locationCount++;
                }
            }
            Bukkit.getLogger().info("[Market] Migrated " + locationCount + " player back locations.");
        }

        // 4. Migrate Global Spawn
        if (regionConfig != null && regionConfig.contains("market.spawn")) {
            Location spawn = regionConfig.getLocation("market.spawn");
            if (spawn != null) {
                setMarketSpawn(spawn);
                Bukkit.getLogger().info("[Market] Migrated global market spawn location.");
            }
        }

        Bukkit.getLogger().info("[Market] Data migration to SQLite completed successfully!");

        // 4. Backup the old YAML files by renaming them so we don't migrate again on next startup
        backupYamlFiles();
    }

    private static ChestShop createShopFromYaml(org.bukkit.configuration.file.FileConfiguration regionConfig, String shopId, String path) {
        Location location = net.craftnepal.market.utils.LocationUtils.loadLocation(regionConfig, path + ".location");
        String ownerString = regionConfig.getString(path + ".owner");
        UUID owner = ownerString != null ? UUID.fromString(ownerString) : null;
        double price = regionConfig.getDouble(path + ".price");
        boolean isAdmin = regionConfig.getBoolean(path + ".is_admin", false);
        boolean isBuyingShop = regionConfig.getBoolean(path + ".is_buying_shop", false);
        
        // Fallback for the short-lived is_sell_shop key
        if (!isBuyingShop && regionConfig.contains(path + ".is_sell_shop")) {
            isBuyingShop = regionConfig.getBoolean(path + ".is_sell_shop");
        }

        ItemStack itemStack = null;

        // Try to load from Base64 bytes first
        if (regionConfig.contains(path + ".item_bytes")) {
            String b64 = regionConfig.getString(path + ".item_bytes");
            if (b64 != null) {
                try {
                    byte[] bytes = java.util.Base64.getDecoder().decode(b64);
                    itemStack = deserializeItem(bytes);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            // Legacy loading fallback
            String materialName = regionConfig.getString(path + ".item");
            if (materialName != null) {
                org.bukkit.Material material = org.bukkit.Material.matchMaterial(materialName);
                if (material != null) {
                    itemStack = new ItemStack(material);
                    if (material == org.bukkit.Material.ENCHANTED_BOOK) {
                        String enchantKey = regionConfig.getString(path + ".enchantment.key");
                        int level = regionConfig.getInt(path + ".enchantment.level");
                        if (enchantKey != null) {
                            enchantKey = enchantKey.toLowerCase();
                            org.bukkit.enchantments.Enchantment enchantment = org.bukkit.enchantments.Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchantKey));
                            if (enchantment != null) {
                                org.bukkit.inventory.meta.EnchantmentStorageMeta meta = (org.bukkit.inventory.meta.EnchantmentStorageMeta) itemStack.getItemMeta();
                                if (meta != null) {
                                    meta.addStoredEnchant(enchantment, level, true);
                                    itemStack.setItemMeta(meta);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (itemStack != null && location != null && owner != null) {
            return new ChestShop(shopId, location, itemStack, owner, price, isAdmin, isBuyingShop);
        }
        return null;
    }

    private static void backupYamlFiles() {
        try {
            File dataFolder = Bukkit.getServer().getPluginManager().getPlugin("Market").getDataFolder();
            File regionFile = new File(dataFolder, "RegionData.yml");
            if (regionFile.exists()) {
                File regionBak = new File(dataFolder, "RegionData.yml.bak");
                if (regionBak.exists()) regionBak.delete();
                if (regionFile.renameTo(regionBak)) {
                    Bukkit.getLogger().info("[Market] Renamed RegionData.yml to RegionData.yml.bak");
                }
            }
            File locationFile = new File(dataFolder, "LocationData.yml");
            if (locationFile.exists()) {
                File locationBak = new File(dataFolder, "LocationData.yml.bak");
                if (locationBak.exists()) locationBak.delete();
                if (locationFile.renameTo(locationBak)) {
                    Bukkit.getLogger().info("[Market] Renamed LocationData.yml to LocationData.yml.bak");
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[Market] Failed to rename old YAML storage files: " + e.getMessage());
        }
    }

    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                Bukkit.getLogger().severe("SQLite JDBC Driver not found!");
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        }
        return connection;
    }

    public static synchronized void close() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                connection = null;
            }
        }
    }

    // ── PLOTS CRUD ─────────────────────────────────────────────────────────────

    public static boolean plotExists(String id) {
        String sql = "SELECT 1 FROM plots WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static List<String> getActivePlotIds() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT id FROM plots WHERE owner IS NOT NULL AND owner != ''";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String> getAvailablePlotIds() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT id FROM plots WHERE owner IS NULL OR owner = ''";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String> getAllPlotIds() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT id FROM plots";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void savePlot(String id, String owner, Location posMin, Location posMax, Location spawn) {
        String sql = "INSERT OR REPLACE INTO plots (id, owner, pos_min, pos_max, spawn) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.setString(2, owner);
            pstmt.setString(3, serializeLocation(posMin));
            pstmt.setString(4, serializeLocation(posMax));
            pstmt.setString(5, serializeLocation(spawn));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Failed to save plot " + id + ": " + e.getMessage());
        }
    }

    public static void setPlotOwner(String id, String owner) {
        String sql = "UPDATE plots SET owner = ? WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, owner);
            pstmt.setString(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Failed to set owner for plot " + id + ": " + e.getMessage());
        }
    }

    public static String getPlotOwner(String id) {
        String sql = "SELECT owner FROM plots WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("owner");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Location getPlotPosMin(String id) {
        String sql = "SELECT pos_min FROM plots WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return deserializeLocation(rs.getString("pos_min"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Location getPlotPosMax(String id) {
        String sql = "SELECT pos_max FROM plots WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return deserializeLocation(rs.getString("pos_max"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Location getPlotSpawn(String id) {
        String sql = "SELECT spawn FROM plots WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return deserializeLocation(rs.getString("spawn"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setPlotSpawn(String id, Location spawn) {
        String sql = "UPDATE plots SET spawn = ? WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, serializeLocation(spawn));
            pstmt.setString(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Failed to set spawn for plot " + id + ": " + e.getMessage());
        }
    }

    public static void deletePlot(String id) {
        String sql = "DELETE FROM plots WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Failed to delete plot " + id + ": " + e.getMessage());
        }
    }

    public static void resetAllPlots() {
        String sql = "DELETE FROM plots";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── PLOT MEMBERS ───────────────────────────────────────────────────────────

    public static List<String> getPlotMembers(String plotId) {
        List<String> members = new ArrayList<>();
        String sql = "SELECT player_uuid FROM plot_members WHERE plot_id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, plotId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    members.add(rs.getString("player_uuid"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }

    public static void addPlotMember(String plotId, String playerUuid) {
        String sql = "INSERT OR IGNORE INTO plot_members (plot_id, player_uuid) VALUES (?, ?)";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, plotId);
            pstmt.setString(2, playerUuid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removePlotMember(String plotId, String playerUuid) {
        String sql = "DELETE FROM plot_members WHERE plot_id = ? AND player_uuid = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, plotId);
            pstmt.setString(2, playerUuid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void clearPlotMembers(String plotId) {
        String sql = "DELETE FROM plot_members WHERE plot_id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, plotId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── SHOPS CRUD ─────────────────────────────────────────────────────────────

    public static void saveShop(ChestShop shop, String plotId, int stock) {
        String sql = "INSERT OR REPLACE INTO shops (id, plot_id, location, item_bytes, item_material, owner, price, is_buying_shop, is_admin, stock) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, shop.getId());
            pstmt.setString(2, plotId);
            pstmt.setString(3, serializeLocation(shop.getLocation()));
            pstmt.setString(4, Base64.getEncoder().encodeToString(serializeItem(shop.getItem())));
            pstmt.setString(5, shop.getItem().getType().toString());
            pstmt.setString(6, shop.getOwner().toString());
            pstmt.setDouble(7, shop.getPrice());
            pstmt.setInt(8, shop.isBuyingShop() ? 1 : 0);
            pstmt.setInt(9, shop.isAdmin() ? 1 : 0);
            pstmt.setInt(10, stock);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Failed to save shop " + shop.getId() + ": " + e.getMessage());
        }
    }

    public static void removeShop(String id) {
        String sql = "DELETE FROM shops WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Failed to remove shop " + id + ": " + e.getMessage());
        }
    }

    public static void updateShopPrice(String id, double price) {
        String sql = "UPDATE shops SET price = ? WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setDouble(1, price);
            pstmt.setString(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Failed to update shop price " + id + ": " + e.getMessage());
        }
    }

    public static ChestShop getShop(String id) {
        String sql = "SELECT * FROM shops WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToShop(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ChestShop getShopAt(Location location) {
        String sql = "SELECT * FROM shops WHERE location = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, serializeLocation(location));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToShop(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<ChestShop> getAllShops() {
        List<ChestShop> shops = new ArrayList<>();
        String sql = "SELECT * FROM shops";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                ChestShop shop = mapRowToShop(rs);
                if (shop != null) {
                    shops.add(shop);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return shops;
    }

    public static List<ChestShop> getShopsByPlot(String plotId) {
        List<ChestShop> shops = new ArrayList<>();
        String sql = "SELECT * FROM shops WHERE plot_id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, plotId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ChestShop shop = mapRowToShop(rs);
                    if (shop != null) {
                        shops.add(shop);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return shops;
    }

    public static List<ChestShop> getShopsByOwner(String ownerUuid) {
        List<ChestShop> shops = new ArrayList<>();
        String sql = "SELECT * FROM shops WHERE owner = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, ownerUuid);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ChestShop shop = mapRowToShop(rs);
                    if (shop != null) {
                        shops.add(shop);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return shops;
    }

    public static String getPlotIdOfShop(String shopId) {
        String sql = "SELECT plot_id FROM shops WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, shopId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("plot_id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getShopStock(String id) {
        String sql = "SELECT stock FROM shops WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("stock");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void updateShopStock(String id, int stock) {
        String sql = "UPDATE shops SET stock = ? WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, stock);
            pstmt.setString(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Failed to update shop stock for " + id + ": " + e.getMessage());
        }
    }

    // ── PLAYERS (OFFLINE EARNINGS) ─────────────────────────────────────────────

    public static double getOfflineEarnings(String uuid) {
        String sql = "SELECT offline_earnings FROM players WHERE uuid = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("offline_earnings");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public static void setOfflineEarnings(String uuid, double amount) {
        String sql = "INSERT OR REPLACE INTO players (uuid, offline_earnings) VALUES (?, ?)";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.setDouble(2, amount);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── PLAYER LOCATIONS (BACK) ────────────────────────────────────────────────

    public static void saveLastLocation(String uuid, Location location) {
        if (location == null) {
            clearLastLocation(uuid);
            return;
        }
        String sql = "INSERT OR REPLACE INTO player_locations (uuid, location) VALUES (?, ?)";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.setString(2, serializeLocation(location));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Location getLastLocation(String uuid) {
        String sql = "SELECT location FROM player_locations WHERE uuid = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return deserializeLocation(rs.getString("location"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void clearLastLocation(String uuid) {
        String sql = "DELETE FROM player_locations WHERE uuid = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── HELPER METHODS ─────────────────────────────────────────────────────────

    private static ChestShop mapRowToShop(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        Location loc = deserializeLocation(rs.getString("location"));
        String b64 = rs.getString("item_bytes");
        ItemStack item = null;
        if (b64 != null) {
            try {
                byte[] bytes = Base64.getDecoder().decode(b64);
                item = deserializeItem(bytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        UUID owner = UUID.fromString(rs.getString("owner"));
        double price = rs.getDouble("price");
        boolean isBuyingShop = rs.getInt("is_buying_shop") == 1;
        boolean isAdmin = rs.getInt("is_admin") == 1;

        if (loc != null && item != null) {
            return new ChestShop(id, loc, item, owner, price, isAdmin, isBuyingShop);
        }
        return null;
    }

    public static String serializeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return loc.getWorld().getName() + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ() + ";" + loc.getYaw() + ";" + loc.getPitch();
    }

    public static Location deserializeLocation(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        try {
            String[] parts = str.split(";");
            if (parts.length < 4) return null;
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) return null;
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0.0f;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0.0f;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] serializeItem(ItemStack item) {
        try {
            java.io.ByteArrayOutputStream io = new java.io.ByteArrayOutputStream();
            org.bukkit.util.io.BukkitObjectOutputStream os = new org.bukkit.util.io.BukkitObjectOutputStream(io);
            os.writeObject(item);
            os.flush();
            return io.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    private static ItemStack deserializeItem(byte[] bytes) {
        try {
            java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(bytes);
            org.bukkit.util.io.BukkitObjectInputStream is = new org.bukkit.util.io.BukkitObjectInputStream(in);
            return (ItemStack) is.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void setMarketSpawn(Location spawn) {
        String sql = "INSERT OR REPLACE INTO global_metadata (key, value) VALUES ('market_spawn', ?)";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, serializeLocation(spawn));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Location getMarketSpawn() {
        String sql = "SELECT value FROM global_metadata WHERE key = 'market_spawn'";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return deserializeLocation(rs.getString("value"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void clearAllData() {
        String[] tables = {"plot_members", "shops", "plots", "players", "player_locations", "global_metadata"};
        try (java.sql.Statement stmt = getConnection().createStatement()) {
            for (String table : tables) {
                stmt.executeUpdate("DELETE FROM " + table);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
