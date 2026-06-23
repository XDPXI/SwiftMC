package dev.xdpxi.swiftmc.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CinematicTerrainProfileTest {

    @Test
    void getHeight_outputsInExpectedRange() {
        CinematicTerrainProfile profile = new CinematicTerrainProfile(12345L);
        boolean foundHigh = false;
        boolean foundLow = false;
        for (int x = -2000; x <= 2000; x += 100) {
            for (int z = -2000; z <= 2000; z += 100) {
                int h = profile.getHeight(x, z);
                assertTrue(h >= 25 && h <= 245,
                    "Height out of range at (" + x + "," + z + "): " + h);
                if (h > 150) foundHigh = true;
                if (h < 55) foundLow = true;
            }
        }
        assertTrue(foundHigh, "Expected some peaks above y=150");
        assertTrue(foundLow, "Expected some valleys below y=55");
    }

    @Test
    void getHeight_isDeterministic() {
        CinematicTerrainProfile a = new CinematicTerrainProfile(77L);
        CinematicTerrainProfile b = new CinematicTerrainProfile(77L);
        assertEquals(a.getHeight(500, -300), b.getHeight(500, -300));
    }
}
