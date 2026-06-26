package dev.xdpxi.swiftmc.terrain;

import dev.xdpxi.swiftmc.Main;
import dev.xdpxi.swiftmc.utils.FastNoise;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class TerrainGenerator implements net.minestom.server.instance.generator.Generator {

    private static final int WATER_LEVEL = 52;
    private static final int BEDROCK_Y = -64;
    private static final int BEDROCK_TRANSITION_HEIGHT = 5;
    private static final int DEEPSLATE_Y = -5;
    private static final int DEEPSLATE_TRANSITION_HEIGHT = 5;

    private final TerrainProfile profile;
    private final long seed;
    private final FastNoise bedrockNoise;
    private final FastNoise deepslateNoise;

    public TerrainGenerator() {
        this.seed = Main.config.seed;
        long seed = this.seed;
        this.profile = new TerrainProfile(seed);
        this.bedrockNoise = new FastNoise(seed + 6);
        this.deepslateNoise = new FastNoise(seed + 7);
    }

    private static boolean tooCloseToTree(@NonNull List<TreePos> trees, int x, int z, int minDist) {
        for (TreePos t : trees) {
            if (Math.abs(t.x - x) <= minDist && Math.abs(t.z - z) <= minDist) return true;
        }
        return false;
    }

    private static long veinKey(int x, int y, int z) {
        return ((long) x << 48) ^ ((long) z << 32) ^ (y & 0xFFFFFFFFL);
    }

    @Override
    public void generate(@NonNull GenerationUnit unit) {
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

        // Precompute per-column values to avoid redundant noise/hash calls inside loops
        int[][] dirtDepths = new int[16][16];
        double[][] bedrockNCache = new double[16][16];
        double[][] deepslateNCache = new double[16][16];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = baseX + x;
                int wz = baseZ + z;
                dirtDepths[x][z] = dirtDepth(wx, wz);
                bedrockNCache[x][z] = (bedrockNoise.get(wx * 0.4, wz * 0.4) + 1.0) / 2.0;
                deepslateNCache[x][z] = (deepslateNoise.get(wx * 0.4, wz * 0.4) + 1.0) / 2.0;
            }
        }

        List<TreePos> trees = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = baseX + x;
                int worldZ = baseZ + z;
                int height = smoothedHeightMap[x][z];

                generateColumn(unit, x, z, worldX, worldZ, height, dirtDepths[x][z], startY, endY,
                        smoothedHeightMap, bedrockNCache[x][z], deepslateNCache[x][z]);

                if (height > WATER_LEVEL && random.nextDouble() < profile.getTreeProbability(worldX, worldZ)
                        && x >= 2 && x <= 13 && z >= 2 && z <= 13
                        && !tooCloseToTree(trees, worldX, worldZ, 5)) {
                    trees.add(new TreePos(worldX, height, worldZ));
                }

                if (height < WATER_LEVEL) {
                    placeOceanFloorVegetation(unit, worldX, worldZ, height);
                }
            }
        }

        for (TreePos tree : trees) {
            placeTree(unit, tree.x, tree.y, tree.z);
        }

        placeOreVeins(unit, baseX, baseZ, startY, endY, smoothedHeightMap, dirtDepths);
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
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double roll = random.nextDouble();
        if (roll < 0.2) {
            unit.modifier().setBlock(worldX, height, worldZ, Block.SEAGRASS);
        } else if (roll < 0.25 && (WATER_LEVEL - 1) - height >= 1) {
            unit.modifier().setBlock(worldX, height, worldZ, Block.TALL_SEAGRASS.withProperty("half", "lower"));
            unit.modifier().setBlock(worldX, height + 1, worldZ, Block.TALL_SEAGRASS.withProperty("half", "upper"));
        } else if (roll < 0.35) {
            int available = (WATER_LEVEL - 1) - height;
            if (available <= 0) return;
            int gap = Math.min(random.nextInt(6), available - 1);
            int kelpHeight = available - gap;
            for (int y = height; y < height + kelpHeight - 1; y++) {
                unit.modifier().setBlock(worldX, y, worldZ, Block.KELP_PLANT);
            }
            unit.modifier().setBlock(worldX, height + kelpHeight - 1, worldZ, Block.KELP);
        }
    }

    private int dirtDepth(int worldX, int worldZ) {
        long h = seed ^ (worldX * 374761393L) ^ (worldZ * 668265263L);
        h = (h ^ (h >>> 13)) * 1274126177L;
        h = h ^ (h >>> 16);
        return 2 + (int) (Math.abs(h) % 4);
    }

    private void generateColumn(@NonNull GenerationUnit unit, int x, int z, int worldX, int worldZ, int height,
                                int dirtDepth, int startY, int endY, int[][] heightMap,
                                double bedrockN, double deepslateN) {
        var modifier = unit.modifier();

        int bedrockTop = BEDROCK_Y + BEDROCK_TRANSITION_HEIGHT;
        int bedrockLoopEnd = Math.min(endY, bedrockTop + 1);
        for (int y = Math.max(startY, BEDROCK_Y); y < bedrockLoopEnd; y++) {
            Block block;
            if (y == BEDROCK_Y) {
                block = Block.BEDROCK;
            } else {
                double t = (double) (y - BEDROCK_Y) / (BEDROCK_TRANSITION_HEIGHT + 1);
                block = bedrockN > t ? Block.BEDROCK : Block.DEEPSLATE;
            }
            modifier.setBlock(worldX, y, worldZ, block);
        }

        int dirtStart = height - 1 - dirtDepth;
        int stoneStart = Math.max(startY, bedrockTop + 1);
        int stoneEnd = Math.min(endY, dirtStart);

        if (stoneStart < stoneEnd) {
            int deepslateTransitionStart = DEEPSLATE_Y + 1;
            int deepslateTransitionEnd = DEEPSLATE_Y + DEEPSLATE_TRANSITION_HEIGHT + 1;

            int pureDeepslateEnd = Math.min(stoneEnd, deepslateTransitionStart);
            for (int y = stoneStart; y < pureDeepslateEnd; y++) {
                modifier.setBlock(worldX, y, worldZ, Block.DEEPSLATE);
            }

            int transitionStart = Math.max(stoneStart, deepslateTransitionStart);
            int transitionEnd = Math.min(stoneEnd, deepslateTransitionEnd);
            for (int y = transitionStart; y < transitionEnd; y++) {
                double t = (double) (y - DEEPSLATE_Y) / (DEEPSLATE_TRANSITION_HEIGHT + 1);
                modifier.setBlock(worldX, y, worldZ, deepslateN > t ? Block.DEEPSLATE : Block.STONE);
            }

            int pureStoneStart = Math.max(stoneStart, deepslateTransitionEnd);
            for (int y = pureStoneStart; y < stoneEnd; y++) {
                modifier.setBlock(worldX, y, worldZ, Block.STONE);
            }
        }

        int dirtFillStart = Math.max(startY, dirtStart);
        int dirtFillEnd = Math.min(endY, height - 1);
        for (int y = dirtFillStart; y < dirtFillEnd; y++) {
            modifier.setBlock(worldX, y, worldZ, Block.DIRT);
        }

        int topY = height - 1;
        if (topY >= startY && topY < endY) {
            Block top = isNearWater(x, z, heightMap) ? Block.SAND : Block.GRASS_BLOCK;
            modifier.setBlock(worldX, topY, worldZ, top);
        }

        int waterStart = Math.max(startY, height);
        int waterEnd = Math.min(endY, WATER_LEVEL);
        for (int y = waterStart; y < waterEnd; y++) {
            modifier.setBlock(worldX, y, worldZ, Block.WATER);
        }
    }

    private boolean isNearWater(int x, int z, int[][] heightMap) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int nx = x + dx;
                int nz = z + dz;
                if (nx < 0 || nx >= 16 || nz < 0 || nz >= 16) continue;
                if (heightMap[nx][nz] < WATER_LEVEL) return true;
            }
        }
        return false;
    }

    private void placeOreVeins(GenerationUnit unit, int baseX, int baseZ, int startY, int endY,
                               int[][] smoothedHeightMap, int[][] dirtDepths) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (Ore ore : Ore.values()) {
            for (int i = 0; i < ore.attemptsPerChunk; i++) {
                if (random.nextDouble() >= ore.chancePerAttempt) continue;

                int x = random.nextInt(16);
                int z = random.nextInt(16);
                int maxStoneY = Math.min(endY - 1, smoothedHeightMap[x][z] - 2 - dirtDepths[x][z]);
                if (maxStoneY < startY) continue;

                int y = startY + random.nextInt(maxStoneY - startY + 1);
                int goal = 1 + random.nextInt(ore.maxVeinSize);
                growOreVein(unit, baseX, baseZ, startY, endY, smoothedHeightMap, dirtDepths, x, y, z, goal, ore);
            }
        }
    }

    private void growOreVein(GenerationUnit unit, int baseX, int baseZ, int startY, int endY,
                             int[][] smoothedHeightMap, int[][] dirtDepths, int startX, int startVeinY, int startZ,
                             int goal, Ore ore) {
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

        ThreadLocalRandom random = ThreadLocalRandom.current();
        Set<Long> visited = new HashSet<>();
        List<int[]> frontier = new ArrayList<>();
        int[] seed = {startX, startVeinY, startZ};
        visited.add(veinKey(seed[0], seed[1], seed[2]));
        frontier.add(seed);
        placeOreBlock(unit, baseX, baseZ, seed[0], seed[1], seed[2], ore);
        int placed = 1;

        int maxAttempts = goal * 20;
        while (placed < goal && !frontier.isEmpty() && maxAttempts-- > 0) {
            int[] cur = frontier.get(random.nextInt(frontier.size()));
            int[] dir = dirs[random.nextInt(dirs.length)];
            int nx = cur[0] + dir[0];
            int ny = cur[1] + dir[1];
            int nz = cur[2] + dir[2];

            if (nx < 0 || nx > 15 || nz < 0 || nz > 15 || ny < startY || ny >= endY) continue;

            long key = veinKey(nx, ny, nz);
            if (visited.contains(key)) continue;
            visited.add(key);

            int maxStoneY = smoothedHeightMap[nx][nz] - 2 - dirtDepths[nx][nz];
            if (ny > maxStoneY) continue;

            placeOreBlock(unit, baseX, baseZ, nx, ny, nz, ore);
            frontier.add(new int[]{nx, ny, nz});
            placed++;
        }
    }

    private void placeOreBlock(@NonNull GenerationUnit unit, int baseX, int baseZ, int x, int y, int z, Ore ore) {
        Block block = y < 0 ? ore.deepslateBlock : ore.stoneBlock;
        unit.modifier().setBlock(baseX + x, y, baseZ + z, block);
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
