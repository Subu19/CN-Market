package net.craftnepal.market.plot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All plots, held in memory so lookups never touch the database. Main-thread only.
 *
 * <p>Grid plots are found by arithmetic ({@link PlotGrid}); only plots with their own bounds
 * (manual selections, legacy imports) are indexed by chunk, the same trick GriefPrevention uses:
 * each such plot is listed under every chunk it overlaps.
 */
public final class PlotIndex {

    private final Map<String, Plot> byId = new LinkedHashMap<>();
    private final Map<Long, List<Plot>> boundedByChunk = new HashMap<>();

    public Plot get(String id) {
        return id == null ? null : byId.get(id);
    }

    public Collection<Plot> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public List<String> ids() {
        return new ArrayList<>(byId.keySet());
    }

    /** True when at least one plot is located by its own bounds instead of the grid. */
    public boolean hasBoundedPlots() {
        return !boundedByChunk.isEmpty();
    }

    /** Adds or replaces a plot. A replaced plot moves to the end of the iteration order. */
    public void put(Plot plot) {
        remove(plot.id());
        byId.put(plot.id(), plot);
        if (!plot.isGrid() && plot.hasBounds()) {
            for (int cx = plot.minX() >> 4; cx <= plot.maxX() >> 4; cx++) {
                for (int cz = plot.minZ() >> 4; cz <= plot.maxZ() >> 4; cz++) {
                    boundedByChunk.computeIfAbsent(PlotGrid.cellKey(cx, cz), k -> new ArrayList<>(1)).add(plot);
                }
            }
        }
    }

    public Plot remove(String id) {
        Plot plot = byId.remove(id);
        if (plot != null && !plot.isGrid() && plot.hasBounds()) {
            for (int cx = plot.minX() >> 4; cx <= plot.maxX() >> 4; cx++) {
                for (int cz = plot.minZ() >> 4; cz <= plot.maxZ() >> 4; cz++) {
                    long key = PlotGrid.cellKey(cx, cz);
                    List<Plot> list = boundedByChunk.get(key);
                    if (list != null) {
                        list.remove(plot);
                        if (list.isEmpty()) boundedByChunk.remove(key);
                    }
                }
            }
        }
        return plot;
    }

    public void clear() {
        byId.clear();
        boundedByChunk.clear();
    }

    /** The bounded (non-grid) plot containing the block, or null. */
    public Plot boundedAt(String world, int x, int y, int z) {
        List<Plot> list = boundedByChunk.get(PlotGrid.cellKey(x >> 4, z >> 4));
        if (list == null) return null;
        for (int i = 0, n = list.size(); i < n; i++) {
            Plot plot = list.get(i);
            if (plot.contains(world, x, y, z)) return plot;
        }
        return null;
    }

    /** First stored plot whose saved bounds overlap the box (grid plots included), or null. */
    public Plot firstOverlap(String world, int x1, int y1, int z1, int x2, int y2, int z2) {
        for (Plot plot : byId.values()) {
            if (plot.intersects(world, x1, y1, z1, x2, y2, z2)) return plot;
        }
        return null;
    }
}
