package net.craftnepal.market.plot;

/**
 * Pure plot lookups over a {@link PlotIndex} and a {@link PlotGrid}, so the protection rules can be
 * tested without a server. {@code inMarket} says whether the world is the market world, which is
 * the only world the grid covers.
 */
public final class Zones {

    private Zones() {}

    /** Plot id at a block: a plot with its own bounds first, then the grid. Null outside plots. */
    public static String plotIdAt(PlotIndex index, PlotGrid grid, String world, boolean inMarket, int x, int y, int z) {
        if (index.hasBoundedPlots()) {
            Plot bounded = index.boundedAt(world, x, y, z);
            if (bounded != null) return bounded.id();
        }
        return inMarket ? grid.plotIdAt(x, z) : null;
    }

    /** Same answer as comparing two {@link #plotIdAt} results, without building strings. */
    public static boolean sameZone(PlotIndex index, PlotGrid grid, String world, boolean inMarket,
                                   int x1, int y1, int z1, int x2, int y2, int z2) {
        if (index.hasBoundedPlots()) {
            Plot a = index.boundedAt(world, x1, y1, z1);
            Plot b = index.boundedAt(world, x2, y2, z2);
            if (a != null || b != null) return a == b;
        }
        if (!inMarket) return true;
        boolean pathA = grid.isPathway(x1, z1);
        boolean pathB = grid.isPathway(x2, z2);
        if (pathA || pathB) return pathA == pathB;
        return grid.cell(x1) == grid.cell(x2) && grid.cell(z1) == grid.cell(z2);
    }

    /**
     * Actor rule for a player inside the market world: owner or member of the plot at the block, and
     * not above the height limit.
     */
    public static boolean isTrusted(PlotIndex index, PlotGrid grid, String world, int maxHeight,
                                    String playerUuid, int x, int y, int z) {
        if (y > maxHeight) return false;
        Plot plot = index.get(plotIdAt(index, grid, world, true, x, y, z));
        if (plot == null) return false; // pathway, spawn or unclaimed grid plot
        return playerUuid.equals(plot.owner()) || plot.members().contains(playerUuid);
    }
}
