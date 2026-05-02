package net.craftnepal.market.utils;

import net.craftnepal.market.Entities.ChestShop;
import net.craftnepal.market.Entities.DisplayPair;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisplayUtils {
    private static volatile DisplayUtils instance;

    // plotId -> shopId -> DisplayPair
    private final Map<String, Map<String, DisplayPair>> marketDisplays = new HashMap<>();

    private DisplayUtils() {}

    public static DisplayUtils getInstance() {
        if (instance == null) {
            synchronized (DisplayUtils.class) {
                if (instance == null)
                    instance = new DisplayUtils();
            }
        }
        return instance;
    }

    // ── Spawn / Remove ────────────────────────────────────────────────

    public DisplayPair spawnDisplayPair(ChestShop shop) {
        if (!RegionUtils.isChunkLoaded(shop.getLocation()))
            return null;

        Location shopLoc = shop.getLocation();
        String plotId = PlotUtils.getPlotIdByLocation(shopLoc);
        if (plotId == null)
            return null;

        // Guard: remove stale display before spawning a new one
        removeDisplayPair(plotId, shop.getId());

        Location itemLoc = shopLoc.clone().add(0.5, 1.3, 0.5);
        Location textLoc = itemLoc.clone().add(0, 0.3, 0);

        ItemStack itemStack = buildDisplayItem(shop);
        String text = buildDisplayText(shop);

        Transformation itemTransform =
                new Transformation(new Vector3f(0, 0, 0), new AxisAngle4f(0, 0, 0, 0),
                        new Vector3f(0.5f, 0.5f, 0.5f), new AxisAngle4f(0, 0, 0, 0));

        ItemDisplay itemDisplay = shopLoc.getWorld().spawn(itemLoc, ItemDisplay.class, d -> {
            d.setItemStack(itemStack);
            d.setTransformation(itemTransform);
            d.setBillboard(TextDisplay.Billboard.CENTER);
            d.setPersistent(false);
        });

        TextDisplay textDisplay = shopLoc.getWorld().spawn(textLoc, TextDisplay.class, d -> {
            d.setText(text);
            d.setAlignment(TextDisplay.TextAlignment.CENTER);
            d.setBillboard(TextDisplay.Billboard.CENTER);
            Transformation t = d.getTransformation();
            t.getScale().set(0.5f);
            d.setTransformation(t);
            d.setPersistent(false);
        });

        DisplayPair pair = new DisplayPair(itemDisplay, textDisplay, shopLoc);
        marketDisplays.computeIfAbsent(plotId, k -> new HashMap<>()).put(shop.getId(), pair);
        Bukkit.getLogger().info("Created display for " + shop.getId());
        return pair;
    }

    public void removeDisplayPair(String plotId, String shopId) {
        Map<String, DisplayPair> plotDisplays = marketDisplays.get(plotId);
        if (plotDisplays == null)
            return;

        DisplayPair pair = plotDisplays.remove(shopId);
        if (pair != null)
            pair.remove();

        if (plotDisplays.isEmpty())
            marketDisplays.remove(plotId);
    }

    // ── Bulk Operations ───────────────────────────────────────────────

    public void spawnMarketDisplays() {
        clearAllDisplays();
        for (ChestShop shop : ShopUtils.getAllShops().values()) {
            if (RegionUtils.isChunkLoaded(shop.getLocation())) {
                spawnDisplayPair(shop);
            }
        }
    }

    public void clearAllDisplays() {
        marketDisplays.values().forEach(plot -> plot.values().forEach(DisplayPair::remove));
        marketDisplays.clear();
    }

    public void updateAllDisplays() {
        for (ChestShop shop : ShopUtils.getAllShops().values()) {
            if (RegionUtils.isChunkLoaded(shop.getLocation())) {
                updateDisplay(shop);
            }
        }
    }

    public void updateDisplay(ChestShop shop) {
        String plotId = PlotUtils.getPlotIdByLocation(shop.getLocation());
        if (plotId == null)
            return;

        Map<String, DisplayPair> plotDisplays = marketDisplays.get(plotId);
        if (plotDisplays == null)
            return;

        DisplayPair pair = plotDisplays.get(shop.getId());
        if (pair == null)
            return;

        pair.update(buildDisplayItem(shop), buildDisplayText(shop));
    }

    // ── Chunk Events ──────────────────────────────────────────────────

    public void handleChunkLoad(Chunk chunk) {
        if (!MarketUtils.isChunkInMarketRegion(chunk))
            return;

        for (ChestShop shop : ShopUtils.getAllShops().values()) {
            if (RegionUtils.isLocationInChunk(shop.getLocation(), chunk)) {
                spawnDisplayPair(shop);
            }
        }
    }

    public void handleChunkUnload(Chunk chunk) {
        if (!MarketUtils.isChunkInMarketRegion(chunk))
            return;

        // Collect (plotId, shopId) pairs to remove — avoids mutating while iterating
        List<String[]> toRemove = new ArrayList<>();
        for (Map.Entry<String, Map<String, DisplayPair>> plotEntry : marketDisplays.entrySet()) {
            for (Map.Entry<String, DisplayPair> shopEntry : plotEntry.getValue().entrySet()) {
                if (RegionUtils.isLocationInChunk(shopEntry.getValue().getLocation(), chunk)) {
                    toRemove.add(new String[] {plotEntry.getKey(), shopEntry.getKey()});
                }
            }
        }

        for (String[] ids : toRemove) {
            removeDisplayPair(ids[0], ids[1]);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /** Builds the display text without String.format for speed. */
    private String buildDisplayText(ChestShop shop) {
        StringBuilder sb = new StringBuilder();

        // 1. Admin Icon/Prefix
        if (shop.isAdmin()) {
            sb.append("§d§l⭐ "); // Star icon for admin shops
        }

        // 2. Name and Color based on type
        if (shop.isBuyingShop()) {
            sb.append("§b"); // Aqua for Buying shops
        } else {
            int stock = ShopUtils.getShopStock(shop);
            sb.append(stock > 0 ? "§a" : "§c"); // Green/Red for Selling shops
        }
        sb.append(getDisplayName(shop)).append("\n");

        // 3. Action Label and Price
        if (shop.isBuyingShop()) {
            sb.append("§bBuying at: §f");
        } else {
            sb.append("§6Selling at: §f");
        }

        String itemKey = ShopUtils.getItemKey(shop);
        String trend = net.craftnepal.market.managers.DynamicPriceManager.getTrendString(itemKey);
        
        sb.append(EconomyUtils.format(shop.getPrice())).append(" ").append(trend);

        return sb.toString();
    }

    private ItemStack buildDisplayItem(ChestShop shop) {
        return new ItemStack(shop.getItem());
    }

    private String getDisplayName(ChestShop shop) {
        return ShopUtils.getShopDisplayName(shop);
    }

    // ── Accessors ─────────────────────────────────────────────────────

    public Map<String, Map<String, DisplayPair>> getMarketDisplays() {
        return marketDisplays;
    }

    public DisplayPair getDisplayPair(String plotId, String shopId) {
        Map<String, DisplayPair> plotDisplays = marketDisplays.get(plotId);
        return plotDisplays != null ? plotDisplays.get(shopId) : null;
    }
}
