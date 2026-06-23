package dev.xdpxi.swiftmc.player;

import net.minestom.server.coordinate.Pos;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerDataTest {

    @Test
    void defaultConstructor_hasEmptyState() {
        PlayerData data = new PlayerData();

        assertNotNull(data.inventory, "inventory list should not be null");
        assertTrue(data.inventory.isEmpty(), "inventory should be empty");
        assertEquals(0.0, data.x);
        assertEquals(0.0, data.y);
        assertEquals(0.0, data.z);
        assertEquals(0.0f, data.yaw);
        assertEquals(0.0f, data.pitch);
        assertNull(data.gameMode);
    }

    @Test
    void toPos_roundTrips() {
        PlayerData data = new PlayerData();
        data.x = 100.5;
        data.y = 64.0;
        data.z = -200.25;
        data.yaw = 45.0f;
        data.pitch = -30.0f;

        Pos pos = data.toPos();

        assertEquals(100.5, pos.x(), 1e-9);
        assertEquals(64.0, pos.y(), 1e-9);
        assertEquals(-200.25, pos.z(), 1e-9);
        assertEquals(45.0f, pos.yaw(), 1e-4f);
        assertEquals(-30.0f, pos.pitch(), 1e-4f);
    }
}
