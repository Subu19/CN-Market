package net.craftnepal.market.managers;

import net.craftnepal.market.Entities.ChestShop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.List;
import java.util.UUID;

/** One-time import of the legacy RegionData.yml / LocationData.yml files into SQLite. */
final class YamlMigration {

    private YamlMigration() {}

    static void migrateFromYaml() {
        File dataFolder = Bukkit.getServer().getPluginManager().getPlugin("Market").getDataFolder();
        File regionFile = new File(dataFolder, "RegionData.yml");
        File locationFile = new File(dataFolder, "LocationData.yml");

        if (!regionFile.exists() && !locationFile.exists()) {
            return; // No files to migrate
        }

        // Legacy data predates the setup command: the world is "market" unless configured.
        MarketWorldManager.loadWorld(true);

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

                DatabaseManager.savePlot(plotId, owner, posMin, posMax, spawn);
                plotCount++;

                List<String> members = regionConfig.getStringList(path + ".members");
                if (members != null) {
                    for (String member : members) {
                        DatabaseManager.addPlotMember(plotId, member);
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
                            DatabaseManager.saveShop(shop, plotId, stock);
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
                    DatabaseManager.setOfflineEarnings(uuidStr, earnings);
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
                    DatabaseManager.saveLastLocation(uuidStr, lastLoc);
                    locationCount++;
                }
            }
            Bukkit.getLogger().info("[Market] Migrated " + locationCount + " player back locations.");
        }

        // 4. Migrate Global Spawn
        if (regionConfig != null && regionConfig.contains("market.spawn")) {
            Location spawn = regionConfig.getLocation("market.spawn");
            if (spawn != null) {
                DatabaseManager.setMarketSpawn(spawn);
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
                    itemStack = DatabaseManager.deserializeItem(bytes);
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[Market] Could not read item of legacy shop " + shopId + ": " + e.getMessage());
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
}
