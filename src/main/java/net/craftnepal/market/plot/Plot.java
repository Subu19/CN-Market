package net.craftnepal.market.plot;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * In-memory copy of one row of the {@code plots} table plus its members.
 *
 * <p>Locations are kept in their stored text form ({@code world;x;y;z;yaw;pitch}) so the index can
 * be built before worlds are loaded and without calling Bukkit. Bounds are parsed once here.
 * Main-thread only.
 */
public final class Plot {

    private final String id;
    private final boolean grid;
    private final String posMin;
    private final String posMax;
    private String owner;
    private String spawn;
    private final Set<String> members = new LinkedHashSet<>();

    // Parsed bounds, inclusive block coordinates. Valid only when hasBounds is true.
    private final boolean hasBounds;
    private final String world;
    private final int minX, minY, minZ, maxX, maxY, maxZ;

    public Plot(String id, String owner, String posMin, String posMax, String spawn) {
        this.id = id;
        this.grid = PlotGrid.parseId(id) != null;
        this.owner = owner;
        this.posMin = posMin;
        this.posMax = posMax;
        this.spawn = spawn;

        double[] a = parse(posMin);
        double[] b = parse(posMax);
        String worldA = worldOf(posMin);
        if (a == null || b == null || worldA == null || !worldA.equals(worldOf(posMax))) {
            hasBounds = false;
            world = null;
            minX = minY = minZ = maxX = maxY = maxZ = 0;
        } else {
            hasBounds = true;
            world = worldA;
            minX = (int) Math.floor(Math.min(a[0], b[0]));
            minY = (int) Math.floor(Math.min(a[1], b[1]));
            minZ = (int) Math.floor(Math.min(a[2], b[2]));
            maxX = (int) Math.floor(Math.max(a[0], b[0]));
            maxY = (int) Math.floor(Math.max(a[1], b[1]));
            maxZ = (int) Math.floor(Math.max(a[2], b[2]));
        }
    }

    public String id() { return id; }
    /** True for automatic {@code plot_x_z} plots, whose area comes from {@link PlotGrid}. */
    public boolean isGrid() { return grid; }
    public String owner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public boolean isOwned() { return owner != null && !owner.isEmpty(); }
    public String posMinText() { return posMin; }
    public String posMaxText() { return posMax; }
    public String spawnText() { return spawn; }
    public void setSpawnText(String spawn) { this.spawn = spawn; }
    /** Live, mutable member set (player UUID strings) in insertion order. */
    public Set<String> members() { return members; }

    public boolean hasBounds() { return hasBounds; }
    public String world() { return world; }
    public int minX() { return minX; }
    public int minY() { return minY; }
    public int minZ() { return minZ; }
    public int maxX() { return maxX; }
    public int maxY() { return maxY; }
    public int maxZ() { return maxZ; }

    public boolean contains(String worldName, int x, int y, int z) {
        return hasBounds && x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ
                && world.equals(worldName);
    }

    public boolean intersects(String worldName, int x1, int y1, int z1, int x2, int y2, int z2) {
        return hasBounds && world.equals(worldName)
                && minX <= Math.max(x1, x2) && maxX >= Math.min(x1, x2)
                && minY <= Math.max(y1, y2) && maxY >= Math.min(y1, y2)
                && minZ <= Math.max(z1, z2) && maxZ >= Math.min(z1, z2);
    }

    /** Parses x, y, z out of {@code world;x;y;z[;yaw;pitch]}. */
    static double[] parse(String text) {
        if (text == null) return null;
        String[] parts = text.split(";");
        if (parts.length < 4) return null;
        try {
            return new double[] {Double.parseDouble(parts[1]), Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3])};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String worldOf(String text) {
        if (text == null) return null;
        int i = text.indexOf(';');
        return i <= 0 ? null : text.substring(0, i);
    }
}
