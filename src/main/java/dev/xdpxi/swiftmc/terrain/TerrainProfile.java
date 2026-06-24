package dev.xdpxi.swiftmc.terrain;

import dev.xdpxi.swiftmc.utils.FastNoise;

public class TerrainProfile {

    private final FastNoise continental;
    private final FastNoise erosion;
    private final FastNoise ridge;
    private final FastNoise warpA;
    private final FastNoise warpB;
    private final FastNoise treeDensity;

    public TerrainProfile(long seed) {
        this.continental = new FastNoise(seed);
        this.erosion = new FastNoise(seed + 1);
        this.ridge = new FastNoise(seed + 2);
        this.warpA = new FastNoise(seed + 3);
        this.warpB = new FastNoise(seed + 4);
        this.treeDensity = new FastNoise(seed + 5);
    }

    public int getHeight(int worldX, int worldZ) {
        double wx1 = warpA.get(worldX * 0.003, worldZ * 0.003) * 120.0;
        double wz1 = warpA.get(worldX * 0.003 + 43.7, worldZ * 0.003 + 43.7) * 120.0;
        double cx = worldX + wx1;
        double cz = worldZ + wz1;

        double wx2 = warpB.get(cx * 0.007, cz * 0.007) * 45.0;
        double wz2 = warpB.get(cx * 0.007 + 87.3, cz * 0.007 + 87.3) * 45.0;
        double sx = cx + wx2;
        double sz = cz + wz2;

        double cont = continental.get(sx * 0.002 + 47.3, sz * 0.002 + 47.3);

        double ero = Math.max(0, Math.min(1,
                (erosion.get(sx * 0.005, sz * 0.005) + 0.7) / 1.4));

        double pv = ridgeFBM(sx, sz);

        double c = cont / 0.65;

        double height;
        if (c < -0.6) {
            double t = (c + 1.0) / 0.4;
            height = 28 + t * 14;
        } else if (c < -0.1) {
            double t = (c + 0.6) / 0.5;
            height = 42 + t * 8;
        } else if (c < 0.1) {
            double t = (c + 0.1) / 0.2;
            height = 50 + t * 18;
        } else {
            double t = Math.min((c - 0.1) / 0.9, 1.0);
            double base = 68 + t * 35;

            double mf = Math.pow(Math.max(0.0, 1.0 - ero), 1.5);

            double peaks = Math.pow(pv, 1.2) * mf * 170.0;

            double hills = pv * (1.0 - mf) * 28.0;

            height = base + peaks + hills;
        }

        return (int) Math.max(28, Math.min(244, height));
    }

    public double getTreeProbability(int worldX, int worldZ) {
        double raw = treeDensity.get(worldX * 0.006, worldZ * 0.006);
        double density = (raw + 0.7) / 1.4;
        return 0.0002 + density * density * 0.04;
    }

    private double ridgeFBM(double x, double z) {
        double freq = 0.008;
        double amp = 1.0;
        double total = 0;
        double maxVal = 0;
        double weight = 1.0;

        for (int i = 0; i < 4; i++) {
            double raw = ridge.get(x * freq, z * freq);
            double n = 1.0 - Math.abs(raw);
            n = n * n;
            total += n * amp * weight;
            maxVal += amp * weight;
            weight = Math.max(0.05, n);
            amp *= 0.45;
            freq *= 2.2;
        }
        return total / maxVal;
    }
}
