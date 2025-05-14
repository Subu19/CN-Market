package net.craftnepal.market.utils;

import net.craftnepal.market.Entities.ChestShop;
import net.craftnepal.market.Entities.DisplayPair;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisplayUtils {
    private static DisplayUtils instance;
    private final Map<String, Map<String, DisplayPair>> marketDisplays = new HashMap<>();


    private DisplayUtils() {
    }

    public static DisplayUtils getInstance() {
        if (instance == null) {
            instance = new DisplayUtils();
        }
        return instance;
    }

    public void spawnMarketDisplays() {
        clearAllDisplays();
        Map<String, ChestShop> shops = ShopUtils.getAllShops();
        for (ChestShop shop : shops.values()) {
            if (RegionUtils.isChunkLoaded(shop.getLocation())) {
                spawnDisplayPair(shop);
            }
        }
    }

    public void clearAllDisplays() {
        for (Map<String, DisplayPair> plotDisplays : marketDisplays.values()) {
            for (DisplayPair displayPair : plotDisplays.values()) {
                displayPair.remove();
            }
        }
        marketDisplays.clear();
    }

    public void updateAllDisplays() {
        Map<String, ChestShop> shops = ShopUtils.getAllShops();
        for (ChestShop shop : shops.values()) {
            if (RegionUtils.isChunkLoaded(shop.getLocation())) {
                updateDisplay(shop);
            }
        }
    }

    public void updateDisplay(ChestShop shop) {
        String plotId = PlotUtils.getPlotIdByLocation(shop.getLocation());
        if (plotId != null && marketDisplays.containsKey(plotId)) {
            DisplayPair displayPair = marketDisplays.get(plotId).get(shop.getId());
            if (displayPair != null) {
                ItemStack displayItem = new ItemStack(shop.getItem());
                String displayText = String.format("§6Price: §f$%.2f", shop.getPrice());
                displayPair.update(displayItem, displayText);
            }
        }
    }

    public DisplayPair spawnDisplayPair(ChestShop shop) {
        Bukkit.getLogger().info("created display for "+shop.getId());
        if (!RegionUtils.isChunkLoaded(shop.getLocation())) {
            return null;
        }

        Location shopLoc = shop.getLocation();
        String plotId = PlotUtils.getPlotIdByLocation(shopLoc);

        if (plotId == null)
            return null;

        // Create display locations
        Location displayLoc = shopLoc.clone().add(0.5, 1.3, 0.5);
        Location textLoc = displayLoc.clone().add(0, 0.3, 0);

        ItemStack itemStack = new ItemStack(shop.getItem());
        ItemMeta meta = itemStack.getItemMeta();

        // Create displays
        ItemDisplay itemDisplay = shopLoc.getWorld().spawn(displayLoc, ItemDisplay.class, display -> {
            display.setItemStack(itemStack);
            Transformation transformation = new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 0),
                    new Vector3f(0.5f, 0.5f, 0.5f),
                    new AxisAngle4f(0, 0, 0, 0));
            display.setPersistent(false);
            display.setTransformation(transformation);
        });

        String displayName;
        if (meta != null && meta.hasDisplayName()) {
            displayName = meta.getDisplayName();
        } else {
            displayName = shop.getItem().toString().replace('_', ' ');
        }
        TextDisplay textDisplay = shopLoc.getWorld().spawn(textLoc, TextDisplay.class, display -> {
            display.setText(String.format("§a%s\n§6Price: §f$%.2f",displayName, shop.getPrice()));
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
//            display.setBillboard(TextDisplay.Billboard.CENTER);
            Transformation transformation = display.getTransformation();
            transformation.getScale().set(0.5D);
            display.setTransformation(transformation);
            display.setBillboard(TextDisplay.Billboard.CENTER);
            display.setPersistent(false);
        });

        DisplayPair displayPair = new DisplayPair(itemDisplay, textDisplay, shopLoc);

        // Store the display pair
        marketDisplays.computeIfAbsent(plotId, k -> new HashMap<>())
                .put(shop.getId(), displayPair);

        return displayPair;
    }

    public void removeDisplayPair(String plotId, String shopId) {
        if (marketDisplays.containsKey(plotId)) {
            DisplayPair displayPair = marketDisplays.get(plotId).get(shopId);
            if (displayPair != null) {
                displayPair.remove();
                marketDisplays.get(plotId).remove(shopId);
            }
            if (marketDisplays.get(plotId).isEmpty()) {
                marketDisplays.remove(plotId);
            }
        }
    }

    public void handleChunkLoad(Chunk chunk) {
        if (!MarketUtils.isChunkInMarketRegion(chunk))
            return;

        Map<String, ChestShop> shops = ShopUtils.getAllShops();
        for (ChestShop shop : shops.values()) {
            if (RegionUtils.isLocationInChunk(shop.getLocation(), chunk)) {
                spawnDisplayPair(shop);
            }
        }
    }

    public void handleChunkUnload(Chunk chunk) {
        if (!MarketUtils.isChunkInMarketRegion(chunk))
            return;

        List<DisplayPair> toRemove = new ArrayList<>();
        for (Map<String, DisplayPair> plotDisplays : marketDisplays.values()) {
            for (DisplayPair displayPair : plotDisplays.values()) {
                if (RegionUtils.isLocationInChunk(displayPair.getLocation(), chunk)) {
                    toRemove.add(displayPair);
                }
            }
        }

        for (DisplayPair displayPair : toRemove) {
            String plotId = PlotUtils.getPlotIdByLocation(displayPair.getLocation());
            if (plotId != null) {
                for (Map.Entry<String, DisplayPair> entry : marketDisplays.get(plotId).entrySet()) {
                    if (entry.getValue() == displayPair) {
                        removeDisplayPair(plotId, entry.getKey());
                        break;
                    }
                }
            }
        }
    }



    public Map<String, Map<String, DisplayPair>> getMarketDisplays() {
        return marketDisplays;
    }

    public DisplayPair getDisplayPair(String plotId, String shopId) {
        return marketDisplays.containsKey(plotId) ? marketDisplays.get(plotId).get(shopId) : null;
    }
}
