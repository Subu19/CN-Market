package net.craftnepal.market;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import me.kodysimpson.simpapi.command.CommandManager;
import me.kodysimpson.simpapi.menu.MenuManager;
import net.craftnepal.market.Listeners.*;
import net.craftnepal.market.commands.*;
import net.craftnepal.market.files.LocationData;
import net.craftnepal.market.files.PriceData;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.subcommands.*;
import net.craftnepal.market.utils.DisplayUtils;
import net.craftnepal.market.utils.MarketUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public final class Market extends JavaPlugin implements Listener {
    private static ProtocolManager protocolManager;
    private static Market plugin;
    private static FileConfiguration config;
    private static File cfile;
    private DisplayUtils displayUtils;

    @Override
    public void onEnable() {
        // turn on manager
        protocolManager = ProtocolLibrary.getProtocolManager();
        plugin = this;

        // load config
        getConfig().options().copyDefaults();
        saveDefaultConfig();
        config = getConfig();
        cfile = new File(getDataFolder(), "config.yml");

        // initialize menu
        MenuManager.setup(getServer(), this);

        // load custom config
        RegionData.setup();
        RegionData.get().addDefault("test", "spawn");
        RegionData.get().options().copyDefaults(true);
        RegionData.save();

        LocationData.setup();
        LocationData.get().addDefault("players", "");
        LocationData.get().options().copyDefaults(true);
        LocationData.save();

        PriceData.setup();

        // initialize display system
        displayUtils = DisplayUtils.getInstance();
        getServer().getScheduler().runTask(this, () -> {
            if (MarketUtils.getMarketSpawn() != null) {
                displayUtils.spawnMarketDisplays();
            }
        });

        // event listeners
        getServer().getPluginManager().registerEvents(new joinEvent(this), this);
        getServer().getPluginManager().registerEvents(new RegionSelection(), this);
        getServer().getPluginManager().registerEvents(new MarketRegionProtection(), this);
        getServer().getPluginManager().registerEvents(new Movement(this), this);
        getServer().getPluginManager().registerEvents(new ShopInteraction(), this);
        getServer().getPluginManager().registerEvents(new MarketDisplayListener(this), this);

        // command register
        try {
            CommandManager.createCoreCommand(this, "amarket", "Admin commands for market configuration", "/amarket",
                    null,
                    SelectionMode.class,
                    ToggleBorder.class,
                    SetSpawn.class,
                    ListPlots.class,
                    DeletePlot.class,
                    Bypass.class,
                    Reload.class);

            CommandManager.createCoreCommand(this, "market", "Market Commands for Players", "/market", null,
                    Claim.class,
                    PlotTeleport.class,
                    Spawn.class,
                    Back.class,
                    Shops.class,
                    SetPlotSpawn.class,
                    PlotSpawn.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        Bukkit.getLogger().info("Market was loaded successfully!");
    }

    @Override
    public void onDisable() {
        // Clean up displays when plugin disables
        if (displayUtils != null) {
            displayUtils.clearAllDisplays();
        }
        Bukkit.getLogger().info("Market is shutting down!");
    }

    // get plugins and configs
    public static Market getPlugin() {
        return plugin;
    }

    public static FileConfiguration getMainConfig() {
        return config;
    }

    public static void reloadMainConfig() {
        config = YamlConfiguration.loadConfiguration(cfile);
    }

    public static ProtocolManager getProtocolManager() {
        return protocolManager;
    }
}
