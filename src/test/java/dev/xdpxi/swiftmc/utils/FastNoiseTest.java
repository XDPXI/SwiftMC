package dev.xdpxi.swiftmc.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FastNoiseTest {

    @Test
    void getRidge_outputsInZeroToOneRange() {
        FastNoise noise = new FastNoise(42L);
        for (int x = -50; x <= 50; x++) {
            for (int z = -50; z <= 50; z++) {
                double ridge = noise.getRidge(x * 0.05, z * 0.05);
                assertTrue(ridge >= 0.0 && ridge <= 1.0,
                        "getRidge out of range at (" + x + "," + z + "): " + ridge);
            }
        }
    }

    @Test
    void getRidge_isDeterministic() {
        FastNoise a = new FastNoise(99L);
        FastNoise b = new FastNoise(99L);
        assertEquals(a.getRidge(1.5, 2.5), b.getRidge(1.5, 2.5));
    }
}
