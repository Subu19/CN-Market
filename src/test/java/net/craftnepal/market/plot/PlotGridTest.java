package net.craftnepal.market.plot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterization tests: {@link PlotGrid} must give exactly the answers of the arithmetic that
 * lived in PlotUtils before the refactor. The old code is kept verbatim below as the oracle.
 */
class PlotGridTest {

    private static final int[][] LAYOUTS = {{16, 3}, {16, 4}, {10, 5}, {32, 1}, {7, 2}, {16, 0}};
    private static final int RANGE = 200;

    // ── Oracle: copied from PlotUtils.getAutomaticPlotIdAt before the refactor ──
    private static String oldPlotId(int worldX, int worldZ, int plotSize, int pathwayWidth) {
        int totalSize = plotSize + pathwayWidth;
        int adjustedX = worldX + (pathwayWidth / 2);
        int adjustedZ = worldZ + (pathwayWidth / 2);
        int modX = Math.abs(adjustedX) % totalSize;
        int modZ = Math.abs(adjustedZ) % totalSize;
        if (adjustedX < 0 && modX != 0) modX = totalSize - modX;
        if (adjustedZ < 0 && modZ != 0) modZ = totalSize - modZ;
        if (modX < pathwayWidth || modZ < pathwayWidth) {
            return null;
        }
        int plotX = (int) Math.floor((double) adjustedX / totalSize);
        int plotZ = (int) Math.floor((double) adjustedZ / totalSize);
        return "plot_" + plotX + "_" + plotZ;
    }

    // ── Oracle: PlotUtils.registerAutomaticPlot bounds ──
    private static int oldStart(int plotIndex, int plotSize, int pathwayWidth) {
        int totalSize = plotSize + pathwayWidth;
        return (plotIndex * totalSize) - (pathwayWidth / 2) + pathwayWidth;
    }

    // ── Oracle: PlotUtils.isSpawnPlot ──
    private static boolean oldIsSpawnPlot(String plotId, int radius) {
        if (plotId == null || !plotId.startsWith("plot_")) return false;
        try {
            String[] parts = plotId.split("_");
            if (parts.length != 3) return false;
            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            return x >= -radius && x < radius && z >= -radius && z < radius;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Oracle: PlotUtils.isSpawnLocation ──
    private static boolean oldIsSpawnLocation(int x, int z, int plotSize, int pathwayWidth, int spawnRadius) {
        int totalSize = plotSize + pathwayWidth;
        int halfPath = pathwayWidth / 2;
        int symmetricRadius = spawnRadius * totalSize - (pathwayWidth - halfPath);
        return Math.abs(x) <= symmetricRadius && Math.abs(z) <= symmetricRadius;
    }

    @Test
    void plotIdMatchesOldArithmetic() {
        for (int[] layout : LAYOUTS) {
            PlotGrid grid = new PlotGrid(layout[0], layout[1], 1);
            for (int x = -RANGE; x <= RANGE; x++) {
                for (int z = -RANGE; z <= RANGE; z++) {
                    assertEquals(oldPlotId(x, z, layout[0], layout[1]), grid.plotIdAt(x, z),
                            () -> "layout " + layout[0] + "/" + layout[1]);
                }
            }
        }
    }

    @Test
    void boundsMatchOldArithmeticAndRoundTrip() {
        for (int[] layout : LAYOUTS) {
            int plotSize = layout[0];
            int pathwayWidth = layout[1];
            PlotGrid grid = new PlotGrid(plotSize, pathwayWidth, 1);
            for (int cx = -6; cx <= 6; cx++) {
                assertEquals(oldStart(cx, plotSize, pathwayWidth), grid.minBlock(cx));
                assertEquals(oldStart(cx, plotSize, pathwayWidth) + plotSize - 1, grid.maxBlock(cx));
                for (int cz = -6; cz <= 6; cz++) {
                    String id = PlotGrid.id(cx, cz);
                    for (int x = grid.minBlock(cx); x <= grid.maxBlock(cx); x++) {
                        for (int z = grid.minBlock(cz); z <= grid.maxBlock(cz); z++) {
                            assertEquals(id, grid.plotIdAt(x, z));
                        }
                    }
                    assertNotEquals(id, grid.plotIdAt(grid.minBlock(cx) - 1, grid.minBlock(cz)));
                    assertNotEquals(id, grid.plotIdAt(grid.maxBlock(cx) + 1, grid.minBlock(cz)));
                    assertNotEquals(id, grid.plotIdAt(grid.minBlock(cx), grid.maxBlock(cz) + 1));
                }
            }
        }
    }

    @Test
    void spawnChecksMatchOldArithmetic() {
        for (int[] layout : LAYOUTS) {
            for (int radius = 0; radius <= 3; radius++) {
                PlotGrid grid = new PlotGrid(layout[0], layout[1], radius);
                for (int cx = -5; cx <= 5; cx++) {
                    for (int cz = -5; cz <= 5; cz++) {
                        String id = PlotGrid.id(cx, cz);
                        assertEquals(oldIsSpawnPlot(id, radius), grid.isSpawnPlot(id));
                    }
                }
                for (int x = -RANGE; x <= RANGE; x += 3) {
                    for (int z = -RANGE; z <= RANGE; z += 3) {
                        assertEquals(oldIsSpawnLocation(x, z, layout[0], layout[1], radius), grid.isSpawnArea(x, z));
                    }
                }
            }
        }
        PlotGrid grid = new PlotGrid(16, 3, 1);
        assertFalse(grid.isSpawnPlot(null));
        assertFalse(grid.isSpawnPlot("manual_123"));
        assertFalse(grid.isSpawnPlot("plot_a_b"));
    }

    @Test
    void interiorMeansAllHorizontalNeighboursShareThePlot() {
        for (int[] layout : LAYOUTS) {
            PlotGrid grid = new PlotGrid(layout[0], layout[1], 1);
            for (int x = -RANGE; x <= RANGE; x++) {
                for (int z = -RANGE; z <= RANGE; z++) {
                    String id = grid.plotIdAt(x, z);
                    boolean allSame = id != null
                            && id.equals(grid.plotIdAt(x + 1, z)) && id.equals(grid.plotIdAt(x - 1, z))
                            && id.equals(grid.plotIdAt(x, z + 1)) && id.equals(grid.plotIdAt(x, z - 1));
                    if (grid.isInterior(x, z)) {
                        assertTrue(allSame, "interior block with a foreign neighbour at " + x + "," + z);
                    }
                    if (allSame) {
                        assertTrue(grid.isInterior(x, z), "missed interior block at " + x + "," + z);
                    }
                }
            }
        }
    }

    @Test
    void cellKeyHasNoCollisionsForNegativeCoordinates() {
        java.util.Set<Long> keys = new java.util.HashSet<>();
        for (int x = -50; x <= 50; x++) {
            for (int z = -50; z <= 50; z++) {
                assertTrue(keys.add(PlotGrid.cellKey(x, z)));
            }
        }
    }

    @Test
    void parseId() {
        assertArrayEquals(new int[] {-3, 7}, PlotGrid.parseId("plot_-3_7"));
        assertNull(PlotGrid.parseId("manual_1700000000"));
        assertNull(PlotGrid.parseId("plot_1"));
        assertNull(PlotGrid.parseId(null));
    }
}
