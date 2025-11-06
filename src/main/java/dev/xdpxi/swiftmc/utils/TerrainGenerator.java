package dev.xdpxi.swiftmc.utils;

import dev.xdpxi.swiftmc.Main;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;

public class TerrainGenerator implements net.minestom.server.instance.generator.Generator {
    private static final int BASE_HEIGHT = 64;
    private static final int WATER_LEVEL = 60;
    private static final double[] NOISE_FREQUENCIES = {0.02, 0.05, 0.1};
    private static final double[] NOISE_AMPLITUDES = {20, 10, 5};
    private static final double TREE_PROBABILITY = 0.1; // 3% chance per block
    private final FastNoise noise = new FastNoise(Main.config.seed);

    @Override
    public void generate(GenerationUnit unit) {
        int baseX = unit.absoluteStart().blockX();
        int baseZ = unit.absoluteStart().blockZ();
        int startY = unit.absoluteStart().blockY();
        int endY = unit.absoluteEnd().blockY();

        int[][] heightMap = new int[16][16];

        // Generate height map
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                double height = BASE_HEIGHT;
                int worldX = baseX + x;
                int worldZ = baseZ + z;
                for (int i = 0; i < NOISE_FREQUENCIES.length; i++) {
                    height += noise.get(worldX * NOISE_FREQUENCIES[i], worldZ * NOISE_FREQUENCIES[i]) * NOISE_AMPLITUDES[i];
                }
                height = Math.max(startY, Math.min(endY - 1, height));
                heightMap[x][z] = (int) height;
            }
        }

        // Fill blocks & place trees
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = baseX + x;
                int worldZ = baseZ + z;
                int height = heightMap[x][z];

                // Fill column
                for (int y = startY; y < endY; y++) {
                    Block block = getBlockAt(y, height, x, z, heightMap);
                    if (block != null) unit.modifier().setBlock(worldX, y, worldZ, block);
                }

                // Tree generation
                if (height > WATER_LEVEL && noise.get(worldX, worldZ) * 0.5 + 0.5 < TREE_PROBABILITY) {
                    placeTree(unit, worldX, height, worldZ, startY, endY);
                }
            }
        }
    }

    private Block getBlockAt(int y, int height, int x, int z, int[][] heightMap) {
        if (y < height - 3) return Block.STONE;
        if (y < height - 1) return Block.DIRT;

        // Check for sand near water
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

        if (y < WATER_LEVEL) return Block.WATER;
        return null;
    }

    private void placeTree(GenerationUnit unit, int worldX, int worldY, int worldZ, int minY, int maxY) {
        int trunkHeight = 4 + (int) (Math.random() * 3); // 4-6 blocks
        // Trunk
        for (int y = worldY; y < worldY + trunkHeight && y < maxY; y++) {
            unit.modifier().setBlock(worldX, y, worldZ, Block.OAK_LOG);
        }

        // Leaves
        int leafRadius = 2;
        int leafCenter = worldY + trunkHeight - 1;
        for (int dx = -leafRadius; dx <= leafRadius; dx++) {
            for (int dz = -leafRadius; dz <= leafRadius; dz++) {
                for (int dy = -leafRadius; dy <= leafRadius; dy++) {
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (dist <= leafRadius && leafCenter + dy < maxY && leafCenter + dy >= minY) {
                        unit.modifier().setBlock(worldX + dx, leafCenter + dy, worldZ + dz, Block.OAK_LEAVES);
                    }
                }
            }
        }
    }
}
