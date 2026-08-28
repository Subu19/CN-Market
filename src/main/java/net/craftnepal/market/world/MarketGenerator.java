package net.craftnepal.market.world;

import net.craftnepal.market.Market;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * Flat chunk generator for the market world.
 *
 * Grid layout (repeating tile of size {@code totalSize = plotSize + pathwayWidth}):
 * <pre>
 *   ┌──────────────┬───┐
 *   │              │   │  ← pathwayWidth columns (X pathway)
 *   │   PLOT       │ P │
 *   │              │ A │
 *   ├──────────────┤ T │
 *   │  PATHWAY ROW │ H │  ← pathwayWidth rows (Z pathway)
 *   └──────────────┴───┘
 * </pre>
 *
 * When a pathway schematic is configured (and WorldEdit is present), the generator
 * places only the base terrain for pathway cells (same floor material as plots).
 * The {@link SchematicManager} then pastes the schematic on top via ChunkLoadEvent.
 *
 * When no schematic is configured, pathway cells receive {@code pathwayMaterial}
 * with optional {@code borderMaterial} rails along the edges.
 */
public class MarketGenerator extends ChunkGenerator {

    private final int plotSize;
    private final int pathwayWidth;
    private final Material floorMaterial;
    private final Material pathwayMaterial;
    private final Material borderMaterial;
    private final int totalSize;
    private final int spawnRadius;

    public MarketGenerator() {
        FileConfiguration config = Market.getMainConfig();
        this.plotSize       = config.getInt("market-world.plot-size", 16);
        this.pathwayWidth   = config.getInt("market-world.pathway-width", 3);
        this.floorMaterial  = parseMaterial(config.getString("market-world.floor-material"),   Material.GRASS_BLOCK);
        this.pathwayMaterial = parseMaterial(config.getString("market-world.pathway-material"), Material.STONE_BRICKS);
        this.borderMaterial  = parseMaterial(config.getString("market-world.border-material"),  Material.SMOOTH_STONE_SLAB);
        this.totalSize = plotSize + pathwayWidth;
        this.spawnRadius = config.getInt("market-world.spawn-radius", 1);
    }

    private static Material parseMaterial(String name, Material fallback) {
        if (name == null || name.isBlank()) return fallback;
        try { return Material.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return fallback; }
    }

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random,
                              int chunkX, int chunkZ, @NotNull ChunkData chunkData) {

        // Is a pathway schematic going to be pasted on top of pathways?
        boolean useSchematic = SchematicManager.getInstance().isAvailable();

        int startX = chunkX << 4;
        int startZ = chunkZ << 4;
        int halfPath = pathwayWidth / 2;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;

                // ── Base column: bedrock + dirt fill ──────────────────────
                chunkData.setBlock(x, 0, z, Material.BEDROCK);
                for (int y = 1; y < 64; y++) {
                    chunkData.setBlock(x, y, z, Material.DIRT);
                }

                // ── Grid calculation (same logic as PlotUtils) ────────────
                int adjustedX = worldX + halfPath;
                int adjustedZ = worldZ + halfPath;

                int modX = Math.abs(adjustedX) % totalSize;
                int modZ = Math.abs(adjustedZ) % totalSize;

                if (adjustedX < 0 && modX != 0) modX = totalSize - modX;
                if (adjustedZ < 0 && modZ != 0) modZ = totalSize - modZ;

                boolean isPathwayX = modX < pathwayWidth;
                boolean isPathwayZ = modZ < pathwayWidth;
                boolean isPathway  = isPathwayX || isPathwayZ;

                // A perfectly symmetric spawn area centered at (0,0) pathway intersection
                // It covers all plots inside the radius and their internal pathways,
                // but EXCLUDES the outer perimeter pathways so they can be generated normally.
                int symmetricRadius = spawnRadius * totalSize - (pathwayWidth - halfPath);
                boolean isSpawn = Math.abs(worldX) <= symmetricRadius && Math.abs(worldZ) <= symmetricRadius;

                // ── Surface block ─────────────────────────────────────────
                if (isSpawn) {
                    // Central plots reserved for server admins
                    chunkData.setBlock(x, 64, z, Material.STONE);
                } else if (isPathway) {
                    if (useSchematic) {
                        // Leave pathway surface as floor material so it looks neutral
                        // before the schematic is pasted by SchematicManager on ChunkLoad.
                        chunkData.setBlock(x, 64, z, floorMaterial);
                    } else {
                        // Material-based pathway generation
                        chunkData.setBlock(x, 64, z, pathwayMaterial);

                        // Border rails on the inner edges of pathway strips
                        if ((modX == 0 || modX == pathwayWidth - 1) && !isPathwayZ) {
                            chunkData.setBlock(x, 65, z, borderMaterial);
                        } else if ((modZ == 0 || modZ == pathwayWidth - 1) && !isPathwayX) {
                            chunkData.setBlock(x, 65, z, borderMaterial);
                        }
                    }
                } else {
                    // Plot floor
                    chunkData.setBlock(x, 64, z, floorMaterial);
                }
            }
        }
    }

    @Override public boolean shouldGenerateNoise()       { return false; }
    @Override public boolean shouldGenerateSurface()     { return false; }
    @Override public boolean shouldGenerateBedrock()     { return false; }
    @Override public boolean shouldGenerateCaves()       { return false; }
    @Override public boolean shouldGenerateDecorations() { return false; }
    @Override public boolean shouldGenerateMobs()        { return false; }
    @Override public boolean shouldGenerateStructures()  { return false; }
}
