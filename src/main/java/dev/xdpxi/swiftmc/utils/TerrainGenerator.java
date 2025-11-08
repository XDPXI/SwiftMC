package dev.xdpxi.swiftmc.utils;

import dev.xdpxi.swiftmc.Main;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;

import java.util.ArrayList;
import java.util.List;

public class TerrainGenerator implements net.minestom.server.instance.generator.Generator {
    private static final int BASE_HEIGHT = 64;
    private static final int WATER_LEVEL = 52;
    private static final double[] NOISE_FREQUENCIES = {0.02, 0.05, 0.1};
    private static final double[] NOISE_AMPLITUDES = {20, 10, 5};
    private static final double TREE_PROBABILITY = 0.001;
    private final FastNoise noise = new FastNoise(Main.config.seed);

    @Override
    public void generate(GenerationUnit unit) {
        int baseX = unit.absoluteStart().blockX();
        int baseZ = unit.absoluteStart().blockZ();
        int startY = unit.absoluteStart().blockY();
        int endY = unit.absoluteEnd().blockY();

        // Generate height map with extended area for proper smoothing
        int[][] heightMap = new int[18][18]; // Extended to 18x18 to include neighboring chunk data

        for (int x = -1; x < 17; x++) {
            for (int z = -1; z < 17; z++) {
                double height = BASE_HEIGHT;
                int worldX = baseX + x;
                int worldZ = baseZ + z;
                for (int i = 0; i < NOISE_FREQUENCIES.length; i++) {
                    height += noise.get(worldX * NOISE_FREQUENCIES[i], worldZ * NOISE_FREQUENCIES[i]) * NOISE_AMPLITUDES[i];
                }
                height = Math.max(startY, Math.min(endY - 1, height));
                heightMap[x + 1][z + 1] = (int) height;
            }
        }

        // Smooth height map (now includes border data)
        int[][] smoothedHeightMap = new int[16][16];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int sum = 0;
                int count = 0;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        // Access from the 18x18 grid (offset by 1)
                        int nx = x + dx + 1;
                        int nz = z + dz + 1;
                        sum += heightMap[nx][nz];
                        count++;
                    }
                }
                smoothedHeightMap[x][z] = sum / count;
            }
        }

        List<TreePos> trees = new ArrayList<>();

        // Fill terrain columns and record trees
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = baseX + x;
                int worldZ = baseZ + z;
                int height = smoothedHeightMap[x][z];

                // Fill column
                for (int y = startY; y < endY; y++) {
                    Block block = getBlockAt(y, height, x, z, smoothedHeightMap);
                    if (block != null) unit.modifier().setBlock(worldX, y, worldZ, block);
                }

                // Record trees - keep trunks 2 blocks away from chunk borders
                if (height > WATER_LEVEL && Math.random() < TREE_PROBABILITY && x >= 2 && x <= 13 && z >= 2 && z <= 13) {
                    trees.add(new TreePos(worldX, height, worldZ));
                }
            }
        }

        // Generate trees
        for (TreePos tree : trees) {
            placeTree(unit, tree.x, tree.y, tree.z);
        }
    }

    private void placeTree(GenerationUnit unit, int worldX, int worldY, int worldZ) {
        int trunkHeight = 4;
        int baseY = worldY + trunkHeight;

        // Leaves
        placeLeafLayer(unit, worldX, baseY - 2, worldZ, 2, true);
        placeLeafLayer(unit, worldX, baseY - 1, worldZ, 2, true);
        placeLeafLayer(unit, worldX, baseY, worldZ, 1, false);
        placeLeafLayer(unit, worldX, baseY + 1, worldZ, 1, true);

        // Trunk
        for (int y = worldY; y <= worldY + trunkHeight; y++) {
            unit.modifier().setBlock(worldX, y, worldZ, Block.OAK_LOG);
        }

        // Dirt
        unit.modifier().setBlock(worldX, worldY - 1, worldZ, Block.DIRT);
    }

    private void placeLeafLayer(GenerationUnit unit, int centerX, int y, int centerZ, int radius, boolean removeCorners) {
        // Place leaves in a square pattern with given radius
        // radius=1 gives 3x3, radius=2 gives 5x5
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // Skip corners if requested
                if (removeCorners && Math.abs(dx) == radius && Math.abs(dz) == radius) {
                    continue;
                }

                int leafX = centerX + dx;
                int leafZ = centerZ + dz;

                // Don't replace the trunk block
                if (dx == 0 && dz == 0 && radius > 1) continue;

                // Place leaf without chunk boundary restrictions
                unit.modifier().setBlock(leafX, y, leafZ, Block.OAK_LEAVES);
            }
        }
    }

    private Block getBlockAt(int y, int height, int x, int z, int[][] heightMap) {
        if (y < height - 3) return Block.STONE;
        if (y < height - 1) return Block.DIRT;

        if (y == height - 1) {
            boolean nearWater = false;
            for (int dx = -1; dx <= 1 && !nearWater; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int nx = x + dx;
                    int nz = z + dz;
                    if (nx < 0 || nx >= 16 || nz < 0 || nz >= 16) continue;
                    if (heightMap[nx][nz] < WATER_LEVEL) {
                        nearWater = true;
                        break;
                    }
                }
            }
            return nearWater ? Block.SAND : Block.GRASS_BLOCK;
        }

        if (y < WATER_LEVEL) {
            if (height > WATER_LEVEL - 1) {
                return Block.SAND;
            }
            return Block.WATER;
        }

        return null;
    }

    private record TreePos(int x, int y, int z) {
    }
}
