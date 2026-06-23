package dev.xdpxi.swiftmc.utils;

import dev.xdpxi.swiftmc.Main;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;

import java.util.ArrayList;
import java.util.List;

public class TerrainGenerator implements net.minestom.server.instance.generator.Generator {

    private static final int WATER_LEVEL = 52;
    private static final int BEDROCK_Y = -64;
    private static final int BEDROCK_TRANSITION_HEIGHT = 5;
    private static final int DEEPSLATE_Y = -5;
    private static final int DEEPSLATE_TRANSITION_HEIGHT = 5;

    private final TerrainProfile profile;
    private final long seed;
    private final boolean cinematic;
    private final FastNoise bedrockNoise;
    private final FastNoise deepslateNoise;

    public TerrainGenerator() {
        this.seed = Main.config.seed;
        long seed = this.seed;
        String style = Main.config.terrainStyle;
        if ("cinematic".equalsIgnoreCase(style)) {
            this.profile = new CinematicTerrainProfile(seed);
            this.cinematic = true;
        } else {
            this.cinematic = false;
            if (!"minecraft".equalsIgnoreCase(style)) {
                Log.warn("Unknown terrainStyle '" + style + "', falling back to 'minecraft'");
            }
            this.profile = new MinecraftTerrainProfile(seed);
        }
        this.bedrockNoise = cinematic ? new FastNoise(seed + 6) : null;
        this.deepslateNoise = new FastNoise(seed + 7);
    }

    private static boolean tooCloseToTree(List<TreePos> trees, int x, int z, int minDist) {
        for (TreePos t : trees) {
            if (Math.abs(t.x - x) <= minDist && Math.abs(t.z - z) <= minDist) return true;
        }
        return false;
    }

    @Override
    public void generate(GenerationUnit unit) {
        int baseX = unit.absoluteStart().blockX();
        int baseZ = unit.absoluteStart().blockZ();
        int startY = unit.absoluteStart().blockY();
        int endY = unit.absoluteEnd().blockY();

        int[][] heightMap = new int[18][18];
        for (int x = -1; x < 17; x++) {
            for (int z = -1; z < 17; z++) {
                int h = profile.getHeight(baseX + x, baseZ + z);
                heightMap[x + 1][z + 1] = Math.max(startY, Math.min(endY - 1, h));
            }
        }

        int[][] smoothedHeightMap = new int[16][16];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int sum = 0;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        sum += heightMap[x + dx + 1][z + dz + 1];
                    }
                }
                smoothedHeightMap[x][z] = sum / 9;
            }
        }

        List<TreePos> trees = new ArrayList<>();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = baseX + x;
                int worldZ = baseZ + z;
                int height = smoothedHeightMap[x][z];
                int dirtDepth = cinematic ? dirtDepth(worldX, worldZ) : 2;

                for (int y = startY; y < endY; y++) {
                    Block block = getBedrockOverride(y, worldX, worldZ);
                    if (block == null) block = getBlockAt(y, height, x, z, worldX, worldZ, smoothedHeightMap, dirtDepth);
                    if (block != null) unit.modifier().setBlock(worldX, y, worldZ, block);
                }

                if (height > WATER_LEVEL && Math.random() < profile.getTreeProbability(worldX, worldZ)
                        && x >= 2 && x <= 13 && z >= 2 && z <= 13
                        && !tooCloseToTree(trees, worldX, worldZ, 5)) {
                    trees.add(new TreePos(worldX, height, worldZ));
                }

                if (cinematic && height < WATER_LEVEL) {
                    placeOceanFloorVegetation(unit, worldX, worldZ, height);
                }
            }
        }

        for (TreePos tree : trees) {
            placeTree(unit, tree.x, tree.y, tree.z);
        }
    }

    private void placeTree(GenerationUnit unit, int worldX, int worldY, int worldZ) {
        int trunkHeight = 4;
        int baseY = worldY + trunkHeight;

        placeLeafLayer(unit, worldX, baseY - 2, worldZ, 2, true);
        placeLeafLayer(unit, worldX, baseY - 1, worldZ, 2, true);
        placeLeafLayer(unit, worldX, baseY, worldZ, 1, false);
        placeLeafLayer(unit, worldX, baseY + 1, worldZ, 1, true);

        for (int y = worldY; y <= worldY + trunkHeight; y++) {
            unit.modifier().setBlock(worldX, y, worldZ, Block.OAK_LOG);
        }

        unit.modifier().setBlock(worldX, worldY - 1, worldZ, Block.DIRT);
    }

    private void placeLeafLayer(GenerationUnit unit, int centerX, int y, int centerZ,
                                int radius, boolean removeCorners) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (removeCorners && Math.abs(dx) == radius && Math.abs(dz) == radius) continue;
                if (dx == 0 && dz == 0 && radius > 1) continue;
                unit.modifier().setBlock(centerX + dx, y, centerZ + dz, Block.OAK_LEAVES);
            }
        }
    }

    private void placeOceanFloorVegetation(GenerationUnit unit, int worldX, int worldZ, int height) {
        double roll = Math.random();
        if (roll < 0.2) {
            unit.modifier().setBlock(worldX, height, worldZ, Block.SEAGRASS);
        } else if (roll < 0.25 && (WATER_LEVEL - 1) - height >= 1) {
            unit.modifier().setBlock(worldX, height, worldZ, Block.TALL_SEAGRASS.withProperty("half", "lower"));
            unit.modifier().setBlock(worldX, height + 1, worldZ, Block.TALL_SEAGRASS.withProperty("half", "upper"));
        } else if (roll < 0.35) {
            int available = (WATER_LEVEL - 1) - height;
            if (available <= 0) return;
            int gap = Math.min((int) (Math.random() * 6), available - 1);
            int kelpHeight = available - gap;
            for (int y = height; y < height + kelpHeight - 1; y++) {
                unit.modifier().setBlock(worldX, y, worldZ, Block.KELP_PLANT);
            }
            unit.modifier().setBlock(worldX, height + kelpHeight - 1, worldZ, Block.KELP);
        }
    }

    private Block getBedrockOverride(int y, int worldX, int worldZ) {
        if (!cinematic) return null;
        if (y == BEDROCK_Y) return Block.BEDROCK;
        if (y > BEDROCK_Y && y <= BEDROCK_Y + BEDROCK_TRANSITION_HEIGHT) {
            double t = (double) (y - BEDROCK_Y) / (BEDROCK_TRANSITION_HEIGHT + 1);
            double n = (bedrockNoise.get(worldX * 0.4, worldZ * 0.4) + 1.0) / 2.0;
            if (n > t) return Block.BEDROCK;
        }
        return null;
    }

    private Block getStoneOrDeepslate(int y, int worldX, int worldZ) {
        if (y <= DEEPSLATE_Y) return Block.DEEPSLATE;
        if (y <= DEEPSLATE_Y + DEEPSLATE_TRANSITION_HEIGHT) {
            double t = (double) (y - DEEPSLATE_Y) / (DEEPSLATE_TRANSITION_HEIGHT + 1);
            double n = (deepslateNoise.get(worldX * 0.4, worldZ * 0.4) + 1.0) / 2.0;
            if (n > t) return Block.DEEPSLATE;
        }
        return Block.STONE;
    }

    private int dirtDepth(int worldX, int worldZ) {
        long h = seed ^ (worldX * 374761393L) ^ (worldZ * 668265263L);
        h = (h ^ (h >>> 13)) * 1274126177L;
        h = h ^ (h >>> 16);
        return 2 + (int) (Math.abs(h) % 4);
    }

    private Block getBlockAt(int y, int height, int x, int z, int worldX, int worldZ, int[][] heightMap, int dirtDepth) {
        if (y < height - 1 - dirtDepth) return getStoneOrDeepslate(y, worldX, worldZ);
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
            return Block.WATER;
        }

        return null;
    }

    private record TreePos(int x, int y, int z) {
    }
}
