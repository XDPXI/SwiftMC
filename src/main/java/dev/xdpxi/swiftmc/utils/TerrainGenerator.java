package dev.xdpxi.swiftmc.utils;

import dev.xdpxi.swiftmc.Main;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        if (cinematic) {
            placeOreVeins(unit, baseX, baseZ, startY, endY, smoothedHeightMap);
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

    private void placeOreVeins(GenerationUnit unit, int baseX, int baseZ, int startY, int endY, int[][] smoothedHeightMap) {
        for (Ore ore : Ore.values()) {
            for (int i = 0; i < ore.attemptsPerChunk; i++) {
                if (Math.random() >= ore.chancePerAttempt) continue;

                int x = (int) (Math.random() * 16);
                int z = (int) (Math.random() * 16);
                int worldX = baseX + x;
                int worldZ = baseZ + z;
                int dirtDepth = cinematic ? dirtDepth(worldX, worldZ) : 2;
                int maxStoneY = Math.min(endY - 1, smoothedHeightMap[x][z] - 2 - dirtDepth);
                if (maxStoneY < startY) continue;

                int y = startY + (int) (Math.random() * (maxStoneY - startY + 1));
                int goal = 1 + (int) (Math.random() * ore.maxVeinSize);
                growOreVein(unit, baseX, baseZ, startY, endY, smoothedHeightMap, x, y, z, goal, ore);
            }
        }
    }

    private void growOreVein(GenerationUnit unit, int baseX, int baseZ, int startY, int endY,
                              int[][] smoothedHeightMap, int startX, int startVeinY, int startZ,
                              int goal, Ore ore) {
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

        Set<Long> visited = new HashSet<>();
        List<int[]> frontier = new ArrayList<>();
        int[] seed = {startX, startVeinY, startZ};
        visited.add(veinKey(seed[0], seed[1], seed[2]));
        frontier.add(seed);
        placeOreBlock(unit, baseX, baseZ, seed[0], seed[1], seed[2], ore);
        int placed = 1;

        int maxAttempts = goal * 20;
        while (placed < goal && !frontier.isEmpty() && maxAttempts-- > 0) {
            int[] cur = frontier.get((int) (Math.random() * frontier.size()));
            int[] dir = dirs[(int) (Math.random() * dirs.length)];
            int nx = cur[0] + dir[0];
            int ny = cur[1] + dir[1];
            int nz = cur[2] + dir[2];

            if (nx < 0 || nx > 15 || nz < 0 || nz > 15 || ny < startY || ny >= endY) continue;

            long key = veinKey(nx, ny, nz);
            if (visited.contains(key)) continue;
            visited.add(key);

            int worldX = baseX + nx;
            int worldZ = baseZ + nz;
            int dirtDepth = cinematic ? dirtDepth(worldX, worldZ) : 2;
            int maxStoneY = smoothedHeightMap[nx][nz] - 2 - dirtDepth;
            if (ny > maxStoneY) continue;

            placeOreBlock(unit, baseX, baseZ, nx, ny, nz, ore);
            frontier.add(new int[]{nx, ny, nz});
            placed++;
        }
    }

    private void placeOreBlock(GenerationUnit unit, int baseX, int baseZ, int x, int y, int z, Ore ore) {
        Block block = y < 0 ? ore.deepslateBlock : ore.stoneBlock;
        unit.modifier().setBlock(baseX + x, y, baseZ + z, block);
    }

    private static long veinKey(int x, int y, int z) {
        return ((long) x << 48) ^ ((long) z << 32) ^ (y & 0xFFFFFFFFL);
    }

    private enum Ore {
        COAL(17, 14, 1.0, Block.COAL_ORE, Block.DEEPSLATE_COAL_ORE),
        COPPER(15, 12, 1.0, Block.COPPER_ORE, Block.DEEPSLATE_COPPER_ORE),
        IRON(4, 12, 1.0, Block.IRON_ORE, Block.DEEPSLATE_IRON_ORE),
        REDSTONE(8, 8, 1.0, Block.REDSTONE_ORE, Block.DEEPSLATE_REDSTONE_ORE),
        LAPIS(7, 6, 0.85, Block.LAPIS_ORE, Block.DEEPSLATE_LAPIS_ORE),
        GOLD(9, 5, 0.65, Block.GOLD_ORE, Block.DEEPSLATE_GOLD_ORE),
        DIAMOND(12, 3, 0.5, Block.DIAMOND_ORE, Block.DEEPSLATE_DIAMOND_ORE),
        EMERALD(3, 2, 0.3, Block.EMERALD_ORE, Block.DEEPSLATE_EMERALD_ORE);

        final int maxVeinSize;
        final int attemptsPerChunk;
        final double chancePerAttempt;
        final Block stoneBlock;
        final Block deepslateBlock;

        Ore(int maxVeinSize, int attemptsPerChunk, double chancePerAttempt, Block stoneBlock, Block deepslateBlock) {
            this.maxVeinSize = maxVeinSize;
            this.attemptsPerChunk = attemptsPerChunk;
            this.chancePerAttempt = chancePerAttempt;
            this.stoneBlock = stoneBlock;
            this.deepslateBlock = deepslateBlock;
        }
    }

    private record TreePos(int x, int y, int z) {
    }
}
