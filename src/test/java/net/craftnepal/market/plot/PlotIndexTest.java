package net.craftnepal.market.plot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlotIndexTest {

    private static Plot manual(String id, String world, int x1, int y1, int z1, int x2, int y2, int z2) {
        return new Plot(id, null, world + ";" + x1 + ".0;" + y1 + ".0;" + z1 + ".0;0.0;0.0",
                world + ";" + x2 + ".0;" + y2 + ".0;" + z2 + ".0;0.0;0.0", null);
    }

    @Test
    void boundedPlotFoundInsideAndNotOutside() {
        PlotIndex index = new PlotIndex();
        index.put(manual("manual_1", "market", 10, 64, 10, 20, 100, 20));
        assertTrue(index.hasBoundedPlots());

        assertEquals("manual_1", index.boundedAt("market", 10, 64, 10).id());
        assertEquals("manual_1", index.boundedAt("market", 20, 100, 20).id());
        assertNull(index.boundedAt("market", 21, 64, 10));
        assertNull(index.boundedAt("market", 15, 63, 15), "below the plot");
        assertNull(index.boundedAt("market", 15, 101, 15), "above the plot");
        assertNull(index.boundedAt("other", 15, 70, 15), "other world");
    }

    @Test
    void cornersGivenInAnyOrder() {
        PlotIndex index = new PlotIndex();
        index.put(manual("manual_1", "market", 20, 100, 20, 10, 64, 10));
        assertEquals("manual_1", index.boundedAt("market", 15, 70, 15).id());
    }

    @Test
    void plotSpanningManyChunksIncludingNegative() {
        PlotIndex index = new PlotIndex();
        index.put(manual("manual_big", "market", -40, 0, -40, 40, 255, 40));
        for (int x = -40; x <= 40; x += 7) {
            for (int z = -40; z <= 40; z += 7) {
                assertNotNull(index.boundedAt("market", x, 10, z), x + "," + z);
            }
        }
        assertNull(index.boundedAt("market", -41, 10, 0));
        assertNull(index.boundedAt("market", 41, 10, 0));
    }

    @Test
    void gridPlotsAreNotBoundsIndexed() {
        PlotIndex index = new PlotIndex();
        index.put(manual("plot_0_0", "market", 0, 64, 0, 15, 255, 15));
        assertFalse(index.hasBoundedPlots());
        assertNull(index.boundedAt("market", 5, 70, 5));
        assertNotNull(index.get("plot_0_0"));
    }

    @Test
    void removeClearsEveryChunk() {
        PlotIndex index = new PlotIndex();
        index.put(manual("manual_1", "market", 0, 0, 0, 100, 10, 100));
        assertNotNull(index.remove("manual_1"));
        assertNull(index.get("manual_1"));
        assertFalse(index.hasBoundedPlots());
        assertNull(index.boundedAt("market", 50, 5, 50));
    }

    @Test
    void replaceReindexesAndMovesToEnd() {
        PlotIndex index = new PlotIndex();
        index.put(manual("manual_a", "market", 0, 0, 0, 10, 10, 10));
        index.put(manual("manual_b", "market", 100, 0, 100, 110, 10, 110));
        index.put(manual("manual_a", "market", 200, 0, 200, 210, 10, 210));
        assertEquals(java.util.List.of("manual_b", "manual_a"), index.ids());
        assertNull(index.boundedAt("market", 5, 5, 5));
        assertEquals("manual_a", index.boundedAt("market", 205, 5, 205).id());
    }

    @Test
    void plotWithoutBoundsIsKeptButNotLocatable() {
        PlotIndex index = new PlotIndex();
        index.put(new Plot("manual_x", "owner", null, null, null));
        assertNotNull(index.get("manual_x"));
        assertFalse(index.hasBoundedPlots());
    }

    @Test
    void ownerAndMembers() {
        Plot plot = manual("manual_1", "market", 0, 0, 0, 1, 1, 1);
        assertFalse(plot.isOwned());
        plot.setOwner("");
        assertFalse(plot.isOwned());
        plot.setOwner("abc");
        assertTrue(plot.isOwned());
        plot.members().add("m1");
        plot.members().add("m1");
        assertEquals(1, plot.members().size());
    }

    @Test
    void firstOverlapIncludesGridPlots() {
        PlotIndex index = new PlotIndex();
        index.put(manual("plot_0_0", "market", 0, 64, 0, 15, 255, 15));
        assertEquals("plot_0_0", index.firstOverlap("market", 10, 70, 10, 30, 80, 30).id());
        assertNull(index.firstOverlap("market", 16, 70, 16, 30, 80, 30));
        assertNull(index.firstOverlap("other", 10, 70, 10, 30, 80, 30));
    }
}
