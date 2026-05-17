package net.craftnepal.market;

import me.kodysimpson.simpapi.command.CommandManager;
import net.craftnepal.market.Listeners.*;
import net.craftnepal.market.files.LocationData;
import net.craftnepal.market.files.PriceData;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.handlers.MarketCommandHandler;
import net.craftnepal.market.managers.DynamicPriceManager;
import net.craftnepal.market.managers.MarketWorldManager;
import net.craftnepal.market.managers.StockNotificationManager;
import net.craftnepal.market.subcommands.*;
import net.craftnepal.market.subcommands.player.Back;
import net.craftnepal.market.subcommands.player.Shops;
import net.craftnepal.market.utils.DisplayUtils;
import net.craftnepal.market.utils.EconomyUtils;
import net.craftnepal.market.utils.TransactionLogUtils;
import net.craftnepal.market.world.SchematicManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
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

        setupConfig();
        setupDataFiles();

        if (!EconomyUtils.setupEconomy()) {
            Bukkit.getLogger().severe("Vault or an Economy plugin not found! Disabling Market.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        me.kodysimpson.simpapi.menu.MenuManager.setup(getServer(), this);

        registerListeners();
        registerCommands();

        SchematicManager.getInstance().init();
        MarketWorldManager.initialize(false);

        displayUtils = DisplayUtils.getInstance();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (getMarketWorld() != null) displayUtils.spawnMarketDisplays();
        }, 20L);

        StockNotificationManager.startPeriodicChecks();

        Bukkit.getLogger().info("Market was loaded successfully!");
    }

    @Override
    public void onDisable() {
        if (displayUtils != null) displayUtils.clearAllDisplays();
        Bukkit.getLogger().info("Market is shutting down!");
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void setupConfig() {
        config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();
        cfile = new File(getDataFolder(), "config.yml");
    }

    private void setupDataFiles() {
        RegionData.setup();
        PriceData.setup();
        LocationData.setup();
        DynamicPriceManager.setup();
        TransactionLogUtils.setup();
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new RegionSelection(), this);
        pm.registerEvents(new MarketRegionProtection(), this);
        pm.registerEvents(new Movement(this), this);
        pm.registerEvents(new ShopInteraction(), this);
        pm.registerEvents(new MarketDisplayListener(this), this);
        pm.registerEvents(new ShopStockListener(), this);
        pm.registerEvents(new MarketWorldListener(), this);
        pm.registerEvents(new InternalCommandListener(), this);
        pm.registerEvents(new SearchListener(), this);
        pm.registerEvents(new CommandTabFilter(), this);
        pm.registerEvents(new ShopProtectionListener(), this);
        pm.registerEvents(new MenuCleanupListener(), this);
        pm.registerEvents(new PlayerJoinListener(), this);
    }

    private void registerCommands() {
        try {
            CommandManager.createCoreCommand(this, "market", "Market Commands", "/market",
                    (sender, subCommandList) -> MarketCommandHandler.handle(sender),
                    PlotCommand.class, AdminCommand.class, Back.class, Shops.class,
                    HelpCommand.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Loads or creates the market world. Delegates to MarketWorldManager.
     * Called externally by the admin setup flow.
     */
    public void initializeMarketWorld(boolean forceCreate) {
        MarketWorldManager.initialize(forceCreate);
    }

    public World getMarketWorld() {
        return Bukkit.getWorld(config.getString("market-world.name", "market"));
    }

    public static Market getPlugin() { return plugin; }

    public static FileConfiguration getMainConfig() { return config; }

    public static void reloadMainConfig() {
        config = YamlConfiguration.loadConfiguration(cfile);
    }
}
