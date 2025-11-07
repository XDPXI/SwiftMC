package dev.xdpxi.swiftmc.nether;

import dev.xdpxi.swiftmc.Main;
import dev.xdpxi.swiftmc.utils.FastNoise;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;

public class NetherGenerator implements Generator {
    private static final int BEDROCK_FLOOR = 0;
    private static final int BEDROCK_CEILING = 127;
    private static final int LAVA_LEVEL = 31;
    private final FastNoise noise;
    private final FastNoise caveNoise;
    private final FastNoise detailNoise;

    public NetherGenerator() {
        long seed = Main.config.seed;
        this.noise = new FastNoise(seed + 1000);
        this.caveNoise = new FastNoise(seed + 2000);
        this.detailNoise = new FastNoise(seed + 3000);
    }

    @Override
    public void generate(GenerationUnit unit) {
        int baseX = unit.absoluteStart().blockX();
        int baseZ = unit.absoluteStart().blockZ();
        int startY = unit.absoluteStart().blockY();
        int endY = unit.absoluteEnd().blockY();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = baseX + x;
                int worldZ = baseZ + z;

                for (int y = startY; y < endY; y++) {
                    Block block = getBlockAt(worldX, y, worldZ);
                    if (block != null) {
                        unit.modifier().setBlock(worldX, y, worldZ, block);
                    }
                }
            }
        }
    }

    private Block getBlockAt(int x, int y, int z) {
        // Bedrock layers
        if (y == BEDROCK_FLOOR || y == BEDROCK_CEILING) {
            return Block.BEDROCK;
        }
        if (y < BEDROCK_FLOOR + 5) {
            if (Math.random() > (y - BEDROCK_FLOOR) / 5.0) {
                return Block.BEDROCK;
            }
        }
        if (y > BEDROCK_CEILING - 5) {
            if (Math.random() > (BEDROCK_CEILING - y) / 5.0) {
                return Block.BEDROCK;
            }
        }

        // Generate cave structure using 3D noise
        double caveValue = caveNoise.get(x * 0.03, z * 0.03) * 30 + 64;
        double caveThickness = Math.abs(detailNoise.get(x * 0.02, z * 0.02)) * 25 + 15;

        boolean isCave = Math.abs(y - caveValue) < caveThickness;

        if (isCave) {
            // Fill low areas with lava
            if (y <= LAVA_LEVEL) {
                return Block.LAVA;
            }
            return null; // Air
        }

        // Solid netherrack
        double density = noise.get(x * 0.08, z * 0.08);

        if (density > -0.2) {
            // Add some variety
            if (Math.random() < 0.01) {
                return Block.SOUL_SAND;
            }

            // Rare nether quartz ore
            if (Math.random() < 0.008) {
                return Block.NETHER_QUARTZ_ORE;
            }

            // Rare ancient debris (only in specific Y levels)
            if (y >= 8 && y <= 22 && Math.random() < 0.0002) {
                return Block.ANCIENT_DEBRIS;
            }

            // Rare nether gold ore
            if (y >= 10 && y <= 117 && Math.random() < 0.005) {
                return Block.NETHER_GOLD_ORE;
            }

            return Block.NETHERRACK;
        }

        // More air pockets
        return null;
    }
}