package dev.xdpxi.swiftmc.player;

import net.minestom.server.entity.GameMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class PlayerDataManagerTest {

    private final UUID testUuid = UUID.randomUUID();

    @AfterEach
    void cleanup() throws Exception {
        Files.deleteIfExists(Path.of("players", testUuid + ".json"));
    }

    @Test
    void saveDataAsync_writesFileOffCallingThread() throws ExecutionException, InterruptedException, TimeoutException {
        PlayerData data = new PlayerData();
        data.x = 12.5;
        data.y = 70.0;
        data.z = -3.0;
        data.gameMode = GameMode.SURVIVAL;

        PlayerDataManager.saveDataAsync(testUuid, data).get(5, TimeUnit.SECONDS);

        Path file = Path.of("players", testUuid + ".json");
        assertTrue(Files.exists(file), "expected save to have written the player file");
    }

    @Test
    void loadDataAsync_roundTripsSavedData() throws ExecutionException, InterruptedException, TimeoutException {
        PlayerData original = new PlayerData();
        original.x = 1.0;
        original.y = 2.0;
        original.z = 3.0;
        original.yaw = 45f;
        original.pitch = -10f;
        original.gameMode = GameMode.CREATIVE;

        PlayerDataManager.saveDataAsync(testUuid, original).get(5, TimeUnit.SECONDS);
        PlayerData loaded = PlayerDataManager.loadDataAsync(testUuid).get(5, TimeUnit.SECONDS);

        assertNotNull(loaded);
        assertEquals(1.0, loaded.x, 1e-9);
        assertEquals(2.0, loaded.y, 1e-9);
        assertEquals(3.0, loaded.z, 1e-9);
        assertEquals(45f, loaded.yaw, 1e-4f);
        assertEquals(-10f, loaded.pitch, 1e-4f);
        assertEquals(GameMode.CREATIVE, loaded.gameMode);
    }

    @Test
    void loadDataAsync_returnsNull_whenFileMissing() throws ExecutionException, InterruptedException, TimeoutException {
        PlayerData loaded = PlayerDataManager.loadDataAsync(UUID.randomUUID()).get(5, TimeUnit.SECONDS);
        assertNull(loaded);
    }
}
