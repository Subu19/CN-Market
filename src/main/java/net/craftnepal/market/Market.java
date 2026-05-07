package net.craftnepal.market;

import me.kodysimpson.simpapi.command.CommandManager;
import net.craftnepal.market.Listeners.*;
import net.craftnepal.market.files.PriceData;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.subcommands.*;
import net.craftnepal.market.subcommands.player.Back;
import net.craftnepal.market.subcommands.player.Shops;
import net.craftnepal.market.world.MarketGenerator;
import net.craftnepal.market.world.SchematicManager;
import net.craftnepal.market.utils.DisplayUtils;
import net.craftnepal.market.utils.EconomyUtils;
import net.craftnepal.market.utils.SendMessage;
import net.craftnepal.market.utils.TeleportUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;


public final class Market extends JavaPlugin {

    private static Market plugin;
    private static FileConfiguration config;
    private static File cfile;
    private DisplayUtils displayUtils;

    @Override
    public void onEnable() {
        plugin = this;

        // config
        config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();
        cfile = new File(getDataFolder(), "config.yml");

        // initialize data files
        RegionData.setup();
        PriceData.setup();
        net.craftnepal.market.files.LocationData.setup();
        net.craftnepal.market.managers.DynamicPriceManager.setup();

        // initialize economy
        if (!EconomyUtils.setupEconomy()) {
            Bukkit.getLogger().severe("Vault or an Economy plugin not found! Disabling Market.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Setup SimpAPI MenuManager
        me.kodysimpson.simpapi.menu.MenuManager.setup(getServer(), this);

        // event listeners
        getServer().getPluginManager().registerEvents(new RegionSelection(), this);
        getServer().getPluginManager().registerEvents(new MarketRegionProtection(), this);
        getServer().getPluginManager().registerEvents(new Movement(this), this);
        getServer().getPluginManager().registerEvents(new ShopInteraction(), this);
        getServer().getPluginManager().registerEvents(new MarketDisplayListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopStockListener(), this);
        getServer().getPluginManager().registerEvents(new MarketWorldListener(), this);
        getServer().getPluginManager().registerEvents(
                new net.craftnepal.market.Listeners.InternalCommandListener(), this);
        getServer().getPluginManager()
                .registerEvents(new net.craftnepal.market.Listeners.SearchListener(), this);
        getServer().getPluginManager()
                .registerEvents(new net.craftnepal.market.Listeners.CommandTabFilter(), this);
        getServer().getPluginManager().registerEvents(new ShopProtectionListener(), this);

        // initialize schematic manager
        SchematicManager.getInstance().init();

        // Attempt to load the market world if it already exists on disk
        initializeMarketWorld(false);

        // initialize display utils
        displayUtils = DisplayUtils.getInstance();

        // Spawn displays after world is loaded
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (getMarketWorld() != null) {
                displayUtils.spawnMarketDisplays();
            }
        }, 20L);



        // command register
        try {
            CommandManager.createCoreCommand(this, "market", "Market Commands", "/market",
                    (sender, subCommandList) -> {
                        if (sender instanceof org.bukkit.entity.Player) {
                            org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
                            if (!p.hasPermission("market.use")) {
                                SendMessage.sendPlayerMessage(p,
                                        "§cYou do not have permission to use the market.");
                                return;
                            }

                            org.bukkit.World marketWorld = Market.getPlugin().getMarketWorld();
                            if (marketWorld != null) {
                                org.bukkit.Location location =
                                        net.craftnepal.market.files.RegionData.get()
                                                .getLocation("market.spawn");
                                if (location == null) {
                                    location = marketWorld.getSpawnLocation();
                                }

                                org.bukkit.Location origin = p.getLocation();
                                SendMessage.sendPlayerMessage(p, "&eTeleporting to the market in 5 seconds... don't move!");
                                
                                TeleportUtils.scheduleTeleport(p, location, () -> {
                                    if (!net.craftnepal.market.utils.MarketUtils.isInMarketArea(origin)) {
                                        net.craftnepal.market.utils.PlayerUtils.saveLastLocation(p, origin);
                                    }
                                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                                    // Enable flight on arrival if configured
                                    net.craftnepal.market.Listeners.Movement.checkAndToggle(p, true);
                                });
                            } else {
                                SendMessage.sendPlayerMessage(p,
                                        "§cThe market world has not been set up yet.");
                                if (p.hasPermission("market.admin")) {
                                    SendMessage.sendPlayerMessage(p,
                                            "§eAdmin: §7Use §f/market admin setup <world> <plotSize> <pathwayWidth> §7to initialize the market.");
                                }
                            }
                        }
                    }, PlotCommand.class, AdminCommand.class, Back.class, Shops.class,
                    HelpCommand.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        Bukkit.getLogger().info("Market was loaded successfully!");
    }

    /**
     * Initializes the market world.
     * 
     * @param forceCreate If true, the world will be created even if the folder doesn't exist (used
     *        for initial setup).
     */
    public void initializeMarketWorld(boolean forceCreate) {
        if (!config.contains("market-world.name") && !forceCreate) {
            return;
        }

        String worldName = config.getString("market-world.name", "market");
        World marketWorld = Bukkit.getWorld(worldName);

        if (marketWorld == null) {
            Bukkit.getLogger().info((forceCreate ? "Creating new" : "Loading existing")
                    + " Market world: " + worldName);
            WorldCreator creator = new WorldCreator(worldName);
            creator.generator(new MarketGenerator());
            marketWorld = creator.createWorld();
        }

        if (marketWorld != null) {
            // Ensure spawn is set correctly from saved data if available
            org.bukkit.Location savedSpawn = RegionData.get().getLocation("market.spawn");
            if (savedSpawn != null) {
                marketWorld.setSpawnLocation(savedSpawn.getBlockX(), savedSpawn.getBlockY(), savedSpawn.getBlockZ());
            } else {
                marketWorld.setSpawnLocation(0, 65, 0);
            }
        }
    }

    public World getMarketWorld() {
        return Bukkit.getWorld(config.getString("market-world.name", "market"));
    }

    @Override
    public void onDisable() {
        if (displayUtils != null) {
            displayUtils.clearAllDisplays();
        }
        Bukkit.getLogger().info("Market is shuttingggggggggggg down!");
    }

    public static Market getPlugin() {
        return plugin;
    }

    public static FileConfiguration getMainConfig() {
        return config;
    }

    public static void reloadMainConfig() {
        config = YamlConfiguration.loadConfiguration(cfile);
    }
}
