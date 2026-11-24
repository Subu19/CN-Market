package net.craftnepal.market;

import net.craftnepal.market.plot.PlotGrid;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Immutable snapshot of the config values read on hot paths. Rebuilt on enable, reload, setup and
 * reset, so event handlers never call {@code config.get*}.
 */
public final class MarketSettings {

    private final String worldName;
    private final boolean worldConfigured;
    private final PlotGrid grid;
    private final int maxHeight;
    private final int maxPlotsPerPlayer;
    private final Set<Material> allowedInteractBlocks;
    private final Set<Material> shopBlacklist;
    private final boolean allowFlight;
    private final float displayViewRange;
    private final String prefix;

    private MarketSettings(FileConfiguration c, Logger log) {
        worldConfigured = c.contains("market-world.name");
        worldName = c.getString("market-world.name", "market");
        int plotSize = c.getInt("market-world.plot-size", 16);
        int pathwayWidth = c.getInt("market-world.pathway-width", 3);
        if (plotSize <= 0 || pathwayWidth < 0) {
            log.warning("Invalid market-world.plot-size/pathway-width (" + plotSize + "/" + pathwayWidth
                    + "), using 16/3.");
            plotSize = 16;
            pathwayWidth = 3;
        }
        grid = new PlotGrid(plotSize, pathwayWidth, c.getInt("market-world.spawn-radius", 1));
        maxHeight = c.getInt("market-world.max-height", 255);
        maxPlotsPerPlayer = c.getInt("market-world.max-plots-per-player", 1);
        allowedInteractBlocks = materials(c.getStringList("market-world.allowed-interact-blocks"));
        shopBlacklist = materials(c.getStringList("player-shop-blacklist"));
        allowFlight = c.getBoolean("allow-flight", false);
        displayViewRange = (float) c.getDouble("display-view-range", 1.0);
        String p = c.getString("prefix");
        prefix = p == null ? "" : p;
    }

    public static MarketSettings from(FileConfiguration config, Logger log) {
        return new MarketSettings(config, log);
    }

    private static Set<Material> materials(List<String> names) {
        Set<Material> set = EnumSet.noneOf(Material.class);
        for (String name : names) {
            Material m = Material.matchMaterial(name);
            if (m != null) set.add(m);
        }
        return Collections.unmodifiableSet(set);
    }

    public String worldName() { return worldName; }
    /** False on a fresh install before {@code /market admin setup} has run. */
    public boolean worldConfigured() { return worldConfigured; }
    public PlotGrid grid() { return grid; }
    public int maxHeight() { return maxHeight; }
    public int maxPlotsPerPlayer() { return maxPlotsPerPlayer; }
    public Set<Material> allowedInteractBlocks() { return allowedInteractBlocks; }
    public Set<Material> shopBlacklist() { return shopBlacklist; }
    public boolean allowFlight() { return allowFlight; }
    public float displayViewRange() { return displayViewRange; }
    public String prefix() { return prefix; }
}
