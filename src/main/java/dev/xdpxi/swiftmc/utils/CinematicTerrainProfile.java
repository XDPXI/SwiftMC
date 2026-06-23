package dev.xdpxi.swiftmc.utils;

public class CinematicTerrainProfile implements TerrainProfile {

    private static final double CONTINENTAL_FREQ = 0.0008;
    private static final double EROSION_FREQ = 0.006;
    private static final double RIDGE_FREQ = 0.012;
    private static final double WARP_STRENGTH = 80.0;
    private static final double WARP_FREQ_A = 0.003;
    private static final double WARP_FREQ_B = 0.007;

    // Spline breakpoints: {raw noise value, target height}
    private static final double[][] SPLINE = {
            {-1.0, 30},
            {-0.3, 52},
            {0.0, 64},
            {0.4, 85},
            {0.7, 130},
            {1.5, 240}
    };

    private final FastNoise continentalNoise;
    private final FastNoise erosionNoise;
    private final FastNoise ridgeNoise;
    private final FastNoise warpNoiseA;
    private final FastNoise warpNoiseB;

    public CinematicTerrainProfile(long seed) {
        this.continentalNoise = new FastNoise(seed);
        this.erosionNoise = new FastNoise(seed + 1);
        this.ridgeNoise = new FastNoise(seed + 2);
        this.warpNoiseA = new FastNoise(seed + 3);
        this.warpNoiseB = new FastNoise(seed + 4);
    }

    private static double spline(double t) {
        if (t <= SPLINE[0][0]) return SPLINE[0][1];
        for (int i = 1; i < SPLINE.length; i++) {
            if (t <= SPLINE[i][0]) {
                double frac = (t - SPLINE[i - 1][0]) / (SPLINE[i][0] - SPLINE[i - 1][0]);
                return SPLINE[i - 1][1] + frac * (SPLINE[i][1] - SPLINE[i - 1][1]);
            }
        }
        return SPLINE[SPLINE.length - 1][1];
    }

    @Override
    public int getHeight(int worldX, int worldZ) {
        double wxA = warpNoiseA.get(worldX * WARP_FREQ_A, worldZ * WARP_FREQ_A) * WARP_STRENGTH;
        double wzA = warpNoiseA.get(worldX * WARP_FREQ_A + 200, worldZ * WARP_FREQ_A + 200) * WARP_STRENGTH;
        double wxB = warpNoiseB.get(worldX * WARP_FREQ_B, worldZ * WARP_FREQ_B) * (WARP_STRENGTH * 0.4);
        double wzB = warpNoiseB.get(worldX * WARP_FREQ_B + 400, worldZ * WARP_FREQ_B + 400) * (WARP_STRENGTH * 0.4);

        double px = worldX + wxA + wxB;
        double pz = worldZ + wzA + wzB;

        double continental = continentalNoise.get(px * CONTINENTAL_FREQ, pz * CONTINENTAL_FREQ);
        double erosion = (erosionNoise.get(px * EROSION_FREQ, pz * EROSION_FREQ) + 1.0) * 0.5;
        double ridge = ridgeNoise.getRidge(px * RIDGE_FREQ, pz * RIDGE_FREQ);
        double ridgeContrib = ridge * (1.0 - Math.min(erosion / 0.3, 1.0)) * 130.0;

        double raw = continental + (ridgeContrib / 200.0);
        double baseHeight = spline(raw);

        double flatHeight = 64.0;
        double height = baseHeight + (flatHeight - baseHeight) * Math.max(0, erosion - 0.5) * 2.0;
        height += ridgeContrib * (1.0 - Math.max(0, erosion - 0.3));

        return (int) Math.max(25, Math.min(245, height));
    }
}
