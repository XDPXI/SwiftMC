package dev.xdpxi.swiftmc.utils;

public class FastNoise {
    private static final double[] FADE_LUT = new double[256];

    static {
        for (int i = 0; i < 256; i++) {
            double t = i / 255.0;
            FADE_LUT[i] = t * t * t * (t * (t * 6 - 15) + 10);
        }
    }

    private final byte[] perm = new byte[512];

    public FastNoise(long seed) {
        for (int i = 0; i < 256; i++) perm[i] = (byte) i;

        long state = seed;
        for (int i = 255; i > 0; i--) {
            state = state * 6364136223846793005L + 1442695040888963407L;
            int j = (int) ((state >>> 32) % (i + 1));
            byte temp = perm[i];
            perm[i] = perm[j];
            perm[j] = temp;
        }

        System.arraycopy(perm, 0, perm, 256, 256);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double y) {
        int h = hash & 7;
        double u = h < 4 ? x : y;
        double v = h < 4 ? y : x;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    public double get(double x, double y) {
        int X = ((int) Math.floor(x)) & 255;
        int Y = ((int) Math.floor(y)) & 255;

        x -= Math.floor(x);
        y -= Math.floor(y);

        double u = FADE_LUT[(int) (x * 255)];
        double v = FADE_LUT[(int) (y * 255)];

        int a = (perm[X] & 0xFF) + Y;
        int b = (perm[X + 1] & 0xFF) + Y;

        return lerp(v,
                lerp(u, grad(perm[a] & 0xFF, x, y), grad(perm[b] & 0xFF, x - 1, y)),
                lerp(u, grad(perm[a + 1] & 0xFF, x, y - 1), grad(perm[b + 1] & 0xFF, x - 1, y - 1))
        );
    }
}
