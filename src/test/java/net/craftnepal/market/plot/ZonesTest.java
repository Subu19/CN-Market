package net.craftnepal.market.plot;

import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/** The actor and boundary rules used by every protection handler. */
class ZonesTest {

    private static final String W = "market";
    private static final String OWNER = "owner-uuid";
    private static final String MEMBER = "member-uuid";
    private static final String STRANGER = "stranger-uuid";
    private static final int MAX_HEIGHT = 255;

    private final PlotGrid grid = new PlotGrid(16, 3, 1);

    private static String loc(int x, int y, int z) {
        return W + ";" + x + ".0;" + y + ".0;" + z + ".0;0.0;0.0";
    }

    /** Plot plot_2_0 owned by OWNER with MEMBER, plus a manual plot overlapping nothing on the grid. */
    private PlotIndex sampleIndex() {
        PlotIndex index = new PlotIndex();
        Plot owned = new Plot("plot_2_0", OWNER, loc(grid.minBlock(2), 64, grid.minBlock(0)),
                loc(grid.maxBlock(2), MAX_HEIGHT, grid.maxBlock(0)), null);
        owned.members().add(MEMBER);
        index.put(owned);
        index.put(new Plot("plot_3_0", null, null, null, null)); // registered but unclaimed
        return index;
    }

    private int insideX() { return grid.minBlock(2) + 5; }
    private int insideZ() { return grid.minBlock(0) + 5; }

    @Test
    void actorRule() {
        PlotIndex index = sampleIndex();
        int x = insideX(), z = insideZ();
        assertTrue(Zones.isTrusted(index, grid, W, MAX_HEIGHT, OWNER, x, 70, z), "owner");
        assertTrue(Zones.isTrusted(index, grid, W, MAX_HEIGHT, MEMBER, x, 70, z), "member");
        assertFalse(Zones.isTrusted(index, grid, W, MAX_HEIGHT, STRANGER, x, 70, z), "stranger");
        assertFalse(Zones.isTrusted(index, grid, W, MAX_HEIGHT, OWNER, x, MAX_HEIGHT + 1, z), "above height limit");
        assertTrue(Zones.isTrusted(index, grid, W, MAX_HEIGHT, OWNER, x, 10, z), "below the stored floor is still the grid plot");

        int pathX = grid.minBlock(2) - 1;
        assertNull(grid.plotIdAt(pathX, z));
        assertFalse(Zones.isTrusted(index, grid, W, MAX_HEIGHT, OWNER, pathX, 70, z), "pathway");

        int unclaimedX = grid.minBlock(3) + 2;
        assertFalse(Zones.isTrusted(index, grid, W, MAX_HEIGHT, OWNER, unclaimedX, 70, z), "registered, unclaimed");
        int unregisteredX = grid.minBlock(5) + 2;
        assertFalse(Zones.isTrusted(index, grid, W, MAX_HEIGHT, OWNER, unregisteredX, 70, z), "never registered");
        assertFalse(Zones.isTrusted(index, grid, W, MAX_HEIGHT, OWNER, 0 + 5, 70, 5), "spawn plot");
    }

    @Test
    void manualPlotTakesPrecedenceOverGrid() {
        PlotIndex index = sampleIndex();
        int x = insideX(), z = insideZ();
        Plot manual = new Plot("manual_1", STRANGER, loc(x, 60, z), loc(x + 2, 80, z + 2), null);
        index.put(manual);
        assertEquals("manual_1", Zones.plotIdAt(index, grid, W, true, x + 1, 70, z + 1));
        assertEquals("plot_2_0", Zones.plotIdAt(index, grid, W, true, x + 1, 90, z + 1), "above the manual plot");
        assertTrue(Zones.isTrusted(index, grid, W, MAX_HEIGHT, STRANGER, x + 1, 70, z + 1));
        assertFalse(Zones.isTrusted(index, grid, W, MAX_HEIGHT, OWNER, x + 1, 70, z + 1));
    }

    @Test
    void outsideTheMarketWorldOnlyBoundedPlotsExist() {
        PlotIndex index = sampleIndex();
        assertNull(Zones.plotIdAt(index, grid, "world", false, insideX(), 70, insideZ()));
        assertTrue(Zones.sameZone(index, grid, "world", false, 0, 70, 0, 500, 70, 500));
    }

    /** sameZone must give exactly the answer of comparing plot ids, which the old code did. */
    @Test
    void boundaryRuleMatchesComparingPlotIds() {
        PlotIndex index = sampleIndex();
        index.put(new Plot("manual_1", null, loc(40, 60, 40), loc(44, 80, 44), null));
        for (int x = -30; x <= 60; x++) {
            for (int z = -30; z <= 60; z++) {
                for (int[] d : new int[][] {{1, 0, 0}, {0, 0, 1}, {0, 1, 0}, {5, 0, 3}}) {
                    String a = Zones.plotIdAt(index, grid, W, true, x, 70, z);
                    String b = Zones.plotIdAt(index, grid, W, true, x + d[0], 70 + d[1], z + d[2]);
                    assertEquals(Objects.equals(a, b),
                            Zones.sameZone(index, grid, W, true, x, 70, z, x + d[0], 70 + d[1], z + d[2]),
                            "at " + x + "," + z + " delta " + d[0] + "," + d[2]);
                }
            }
        }
    }

    /** The redstone fast path (skip interior blocks) must never skip a block a neighbour check would stop. */
    @Test
    void redstoneFastPathAgreesWithNeighbourCheck() {
        PlotIndex index = new PlotIndex();
        int[][] neighbours = {{0, 0, -1}, {0, 0, 1}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}};
        for (int x = -60; x <= 60; x++) {
            for (int z = -60; z <= 60; z++) {
                boolean crosses = false;
                for (int[] d : neighbours) {
                    crosses |= !Zones.sameZone(index, grid, W, true, x, 70, z, x + d[0], 70 + d[1], z + d[2]);
                }
                if (grid.isInterior(x, z)) {
                    assertFalse(crosses, "interior block crosses a border at " + x + "," + z);
                }
            }
        }
    }
}
