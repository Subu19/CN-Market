package net.craftnepal.market.shop;

import net.craftnepal.market.Entities.ChestShop;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every shop in memory, found by id, by block and by chunk. Writes happen on the server thread.
 * The by-id map is concurrent so async price tasks can iterate it safely.
 */
public final class ShopIndex {

    private final Map<String, ChestShop> byId = new ConcurrentHashMap<>();
    private final Map<String, Map<Long, ChestShop>> byBlock = new HashMap<>();
    private final Map<String, Map<Long, List<ChestShop>>> byChunk = new HashMap<>();

    /** x and z in 26 bits each, y in 12 bits: covers ±33M horizontally and -2048..2047 vertically. */
    static long blockKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    private static String world(Location l) {
        return l.getWorld() == null ? "" : l.getWorld().getName();
    }

    public void put(ChestShop shop) {
        remove(shop.getId());
        byId.put(shop.getId(), shop);
        Location l = shop.getLocation();
        if (l == null) return;
        byBlock.computeIfAbsent(world(l), w -> new HashMap<>())
                .put(blockKey(l.getBlockX(), l.getBlockY(), l.getBlockZ()), shop);
        byChunk.computeIfAbsent(world(l), w -> new HashMap<>())
                .computeIfAbsent(chunkKey(l.getBlockX() >> 4, l.getBlockZ() >> 4), k -> new ArrayList<>(2))
                .add(shop);
    }

    public ChestShop remove(String id) {
        ChestShop shop = byId.remove(id);
        if (shop == null || shop.getLocation() == null) return shop;
        Location l = shop.getLocation();
        Map<Long, ChestShop> blocks = byBlock.get(world(l));
        if (blocks != null) blocks.remove(blockKey(l.getBlockX(), l.getBlockY(), l.getBlockZ()), shop);
        Map<Long, List<ChestShop>> chunks = byChunk.get(world(l));
        if (chunks != null) {
            long key = chunkKey(l.getBlockX() >> 4, l.getBlockZ() >> 4);
            List<ChestShop> list = chunks.get(key);
            if (list != null && list.remove(shop) && list.isEmpty()) chunks.remove(key);
        }
        return shop;
    }

    public ChestShop get(String id) {
        return id == null ? null : byId.get(id);
    }

    public ChestShop at(String world, int x, int y, int z) {
        Map<Long, ChestShop> blocks = byBlock.get(world);
        return blocks == null ? null : blocks.get(blockKey(x, y, z));
    }

    public ChestShop at(Location l) {
        return l == null ? null : at(world(l), l.getBlockX(), l.getBlockY(), l.getBlockZ());
    }

    public List<ChestShop> inChunk(String world, int chunkX, int chunkZ) {
        Map<Long, List<ChestShop>> chunks = byChunk.get(world);
        List<ChestShop> list = chunks == null ? null : chunks.get(chunkKey(chunkX, chunkZ));
        return list == null ? List.of() : List.copyOf(list);
    }

    // ponytail: plot and owner lookups scan all shops; a few hundred shops, called from commands
    // and menus only. Add maps here if a server ever has tens of thousands of shops.
    public List<ChestShop> byPlot(String plotId) {
        List<ChestShop> list = new ArrayList<>();
        if (plotId == null) return list;
        for (ChestShop shop : byId.values()) {
            if (plotId.equals(shop.getPlotId())) list.add(shop);
        }
        return list;
    }

    public List<ChestShop> byOwner(String ownerUuid) {
        List<ChestShop> list = new ArrayList<>();
        if (ownerUuid == null) return list;
        for (ChestShop shop : byId.values()) {
            if (shop.getOwner() != null && ownerUuid.equals(shop.getOwner().toString())) list.add(shop);
        }
        return list;
    }

    /** Live, read-only view of every shop. Safe to iterate while shops are added or removed. */
    public Collection<ChestShop> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    /** Live, read-only id → shop view. */
    public Map<String, ChestShop> asMap() {
        return Collections.unmodifiableMap(byId);
    }

    public int size() {
        return byId.size();
    }

    public void clear() {
        byId.clear();
        byBlock.clear();
        byChunk.clear();
    }
}
