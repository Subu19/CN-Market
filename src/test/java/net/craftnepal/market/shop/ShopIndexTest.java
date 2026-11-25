package net.craftnepal.market.shop;

import net.craftnepal.market.Entities.ChestShop;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ShopIndexTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();

    private static ChestShop shop(String id, int x, int y, int z, UUID owner, String plot) {
        ChestShop s = new ChestShop(id, new Location(null, x, y, z), null, owner, 1.0);
        s.setPlotId(plot);
        return s;
    }

    @Test
    void findsShopsByBlockChunkPlotAndOwner() {
        ShopIndex index = new ShopIndex();
        index.put(shop("a", 10, 64, 10, ALICE, "plot_1_0"));
        index.put(shop("b", 12, 70, 3, ALICE, "plot_1_0"));
        index.put(shop("c", -1, -5, -1, BOB, "plot_0_0"));

        assertEquals("a", index.at("", 10, 64, 10).getId());
        assertNull(index.at("", 10, 65, 10));
        assertEquals("c", index.at("", -1, -5, -1).getId());
        assertNull(index.at("other_world", 10, 64, 10));

        assertEquals(2, index.inChunk("", 0, 0).size());
        assertEquals(1, index.inChunk("", -1, -1).size());
        assertEquals(2, index.byPlot("plot_1_0").size());
        assertEquals(1, index.byOwner(BOB.toString()).size());
        assertEquals(3, index.all().size());
    }

    @Test
    void removeAndReplaceLeaveNoStaleEntries() {
        ShopIndex index = new ShopIndex();
        index.put(shop("a", 10, 64, 10, ALICE, "plot_1_0"));
        index.put(shop("a", 40, 64, 40, ALICE, "plot_2_0")); // same id, moved

        assertNull(index.at("", 10, 64, 10));
        assertEquals("a", index.at("", 40, 64, 40).getId());
        assertTrue(index.inChunk("", 0, 0).isEmpty());
        assertTrue(index.byPlot("plot_1_0").isEmpty());

        index.remove("a");
        assertNull(index.at("", 40, 64, 40));
        assertTrue(index.inChunk("", 2, 2).isEmpty());
        assertEquals(0, index.size());
    }

    @Test
    void blockKeysAreUniqueNearOrigin() {
        Set<Long> keys = new HashSet<>();
        for (int x = -20; x <= 20; x++)
            for (int y = -64; y <= 320; y += 7)
                for (int z = -20; z <= 20; z++)
                    assertTrue(keys.add(ShopIndex.blockKey(x, y, z)), x + "," + y + "," + z);
    }
}
