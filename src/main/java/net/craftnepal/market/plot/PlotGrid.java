package net.craftnepal.market.plot;

/**
 * Pure arithmetic for the automatic plot grid of the market world. No Bukkit types, so it can be
 * unit tested and called from any hot path without allocation.
 *
 * <p>The world is tiled with cells of {@code plotSize + pathwayWidth} blocks. Each cell starts with
 * a pathway strip of {@code pathwayWidth} blocks, shifted by {@code pathwayWidth / 2} so that the
 * pathway intersection sits on the world origin.
 */
public final class PlotGrid {

    public static final String GRID_PREFIX = "plot_";

    private final int plotSize;
    private final int pathwayWidth;
    private final int spawnRadius;
    private final int totalSize;
    private final int halfPath;

    public PlotGrid(int plotSize, int pathwayWidth, int spawnRadius) {
        if (plotSize <= 0 || pathwayWidth < 0) {
            throw new IllegalArgumentException("plotSize must be > 0 and pathwayWidth >= 0");
        }
        this.plotSize = plotSize;
        this.pathwayWidth = pathwayWidth;
        this.spawnRadius = spawnRadius;
        this.totalSize = plotSize + pathwayWidth;
        this.halfPath = pathwayWidth / 2;
    }

    public int plotSize() { return plotSize; }
    public int pathwayWidth() { return pathwayWidth; }
    public int spawnRadius() { return spawnRadius; }
    public int totalSize() { return totalSize; }

    /** Position of a world coordinate inside its cell, 0 .. totalSize-1. */
    public int mod(int worldCoord) {
        return Math.floorMod(worldCoord + halfPath, totalSize);
    }

    /** Cell index of a world coordinate. */
    public int cell(int worldCoord) {
        return Math.floorDiv(worldCoord + halfPath, totalSize);
    }

    public boolean isPathway(int x, int z) {
        return mod(x) < pathwayWidth || mod(z) < pathwayWidth;
    }

    /**
     * True when the block is inside a plot and none of its four horizontal neighbours leaves that
     * plot, i.e. it is not on the plot's outer ring.
     */
    public boolean isInterior(int x, int z) {
        int mx = mod(x);
        int mz = mod(z);
        return mx > pathwayWidth && mx < totalSize - 1 && mz > pathwayWidth && mz < totalSize - 1;
    }

    /** Plot id at a world column, or null for pathway. */
    public String plotIdAt(int x, int z) {
        if (isPathway(x, z)) return null;
        return id(cell(x), cell(z));
    }

    public static String id(int cellX, int cellZ) {
        return GRID_PREFIX + cellX + "_" + cellZ;
    }

    /** Packs a cell coordinate into one long, collision free for all int pairs. */
    public static long cellKey(int cellX, int cellZ) {
        return ((long) cellX << 32) | (cellZ & 0xFFFFFFFFL);
    }

    /**
     * Parses {@code plot_<x>_<z>}. Returns null when the id is not a grid id.
     */
    public static int[] parseId(String id) {
        if (id == null || !id.startsWith(GRID_PREFIX)) return null;
        String[] parts = id.split("_");
        if (parts.length != 3) return null;
        try {
            return new int[] {Integer.parseInt(parts[1]), Integer.parseInt(parts[2])};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Lowest world X (or Z) of a plot in the given cell. */
    public int minBlock(int cellIndex) {
        return cellIndex * totalSize - halfPath + pathwayWidth;
    }

    /** Highest world X (or Z) of a plot in the given cell. */
    public int maxBlock(int cellIndex) {
        return minBlock(cellIndex) + plotSize - 1;
    }

    /** Plots in the square around the origin reserved for the server. */
    public boolean isSpawnCell(int cellX, int cellZ) {
        return cellX >= -spawnRadius && cellX < spawnRadius && cellZ >= -spawnRadius && cellZ < spawnRadius;
    }

    public boolean isSpawnPlot(String plotId) {
        int[] c = parseId(plotId);
        return c != null && isSpawnCell(c[0], c[1]);
    }

    /** Half-width of the square spawn area centred on 0,0, including its internal pathways. */
    public int spawnAreaRadius() {
        return spawnRadius * totalSize - (pathwayWidth - halfPath);
    }

    public boolean isSpawnArea(int x, int z) {
        int r = spawnAreaRadius();
        return Math.abs(x) <= r && Math.abs(z) <= r;
    }
}
