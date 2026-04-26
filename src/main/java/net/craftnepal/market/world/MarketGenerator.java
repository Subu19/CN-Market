package net.craftnepal.market.world;

import net.craftnepal.market.Market;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class MarketGenerator extends ChunkGenerator {

    private final int plotSize;
    private final int pathwayWidth;
    private final Material floorMaterial;
    private final Material pathwayMaterial;
    private final Material borderMaterial;
    private final int totalSize;

    public MarketGenerator() {
        FileConfiguration config = Market.getMainConfig();
        this.plotSize = config.getInt("market-world.plot-size", 16);
        this.pathwayWidth = config.getInt("market-world.pathway-width", 3);
        this.floorMaterial = Material.valueOf(config.getString("market-world.floor-material", "GRASS_BLOCK"));
        this.pathwayMaterial = Material.valueOf(config.getString("market-world.pathway-material", "STONE_BRICKS"));
        this.borderMaterial = Material.valueOf(config.getString("market-world.border-material", "SMOOTH_STONE_SLAB"));
        this.totalSize = plotSize + pathwayWidth;
    }

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        int startX = chunkX << 4;
        int startZ = chunkZ << 4;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;

                // Simple flat world at y=64
                for (int y = 0; y < 64; y++) {
                    chunkData.setBlock(x, y, z, Material.DIRT);
                }
                chunkData.setBlock(x, 0, z, Material.BEDROCK);

                // Grid calculation
                // We want the pathways to be centered at 0,0 if possible, or at least have a junction at 0,0.
                // Let's adjust so that 0,0 is the center of a pathway junction.
                
                int adjustedX = worldX + (pathwayWidth / 2);
                int adjustedZ = worldZ + (pathwayWidth / 2);

                // Use modulo to determine if we are in a pathway or plot
                int modX = Math.abs(adjustedX) % totalSize;
                int modZ = Math.abs(adjustedZ) % totalSize;

                // Adjust for negative coordinates to maintain the grid
                if (adjustedX < 0 && modX != 0) modX = totalSize - modX;
                if (adjustedZ < 0 && modZ != 0) modZ = totalSize - modZ;

                boolean isPathwayX = modX < pathwayWidth;
                boolean isPathwayZ = modZ < pathwayWidth;

                if (isPathwayX || isPathwayZ) {
                    chunkData.setBlock(x, 64, z, pathwayMaterial);
                    
                    // Add a border if it's the edge of a pathway
                    if ((modX == 0 || modX == pathwayWidth - 1) && !isPathwayZ) {
                         chunkData.setBlock(x, 65, z, borderMaterial);
                    } else if ((modZ == 0 || modZ == pathwayWidth - 1) && !isPathwayX) {
                         chunkData.setBlock(x, 65, z, borderMaterial);
                    }
                    
                } else {
                    chunkData.setBlock(x, 64, z, floorMaterial);
                }
            }
        }
    }

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateBedrock() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }
}
