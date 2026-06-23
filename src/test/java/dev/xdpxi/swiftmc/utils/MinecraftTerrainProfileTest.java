package dev.xdpxi.swiftmc.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MinecraftTerrainProfileTest {

    @Test
    void getHeight_outputsInExpectedRange() {
        MinecraftTerrainProfile profile = new MinecraftTerrainProfile(12345L);
        boolean foundHigh = false;
        boolean foundLow = false;
        for (int x = -200; x <= 200; x += 10) {
            for (int z = -200; z <= 200; z += 10) {
                int h = profile.getHeight(x, z);
                assertTrue(h >= 44 && h <= 145,
                    "Height out of range at (" + x + "," + z + "): " + h);
                if (h > 75) foundHigh = true;
                if (h < 68) foundLow = true;
            }
        }
        assertTrue(foundHigh, "Expected some heights above 75");
        assertTrue(foundLow, "Expected some heights below 68");
    }

    @Test
    void getHeight_isDeterministic() {
        MinecraftTerrainProfile a = new MinecraftTerrainProfile(42L);
        MinecraftTerrainProfile b = new MinecraftTerrainProfile(42L);
        assertEquals(a.getHeight(100, 200), b.getHeight(100, 200));
    }
}
