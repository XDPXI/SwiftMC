package dev.xdpxi.swiftmc.utils;

public class MinecraftTerrainProfile implements TerrainProfile {

    private static final int BASE_HEIGHT = 64;
    private static final double WARP_STRENGTH = 30.0;
    private static final double WARP_FREQ = 0.004;
    private static final double[] FREQS = {0.008, 0.02, 0.05, 0.1};
    private static final double[] AMPS = {18.0, 10.0, 6.0, 3.0};

    private final FastNoise noise;
    private final FastNoise warpNoise;

    public MinecraftTerrainProfile(long seed) {
        this.noise = new FastNoise(seed);
        this.warpNoise = new FastNoise(seed + 1);
    }

    @Override
    public int getHeight(int worldX, int worldZ) {
        double wx = warpNoise.get(worldX * WARP_FREQ, worldZ * WARP_FREQ) * WARP_STRENGTH;
        double wz = warpNoise.get(worldX * WARP_FREQ + 100, worldZ * WARP_FREQ + 100) * WARP_STRENGTH;

        double px = worldX + wx;
        double pz = worldZ + wz;

        double height = BASE_HEIGHT;
        for (int i = 0; i < FREQS.length; i++) {
            height += noise.get(px * FREQS[i], pz * FREQS[i]) * AMPS[i];
        }

        return (int) Math.max(55, Math.min(145, height));
    }
}
