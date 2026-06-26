package dev.xdpxi.swiftmc.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileWriter;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    @TempDir
    Path tempDir;

    private String originalUserDir;

    @BeforeEach
    void redirectWorkingDir() {
        originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toAbsolutePath().toString());
    }

    @AfterEach
    void restoreWorkingDir() {
        System.setProperty("user.dir", originalUserDir);
    }

    @Test
    void loadsExistingConfig() throws Exception {
        String yaml = "seed: 999\nport: 19132\nmaxPlayers: 100\nonline: false\ndebug: true\nvelocity: true\nvelocitySecret: mysecret\nsimpleVoiceChat: false\n";
        try (FileWriter fw = new FileWriter(tempDir.resolve("config.yml").toFile())) {
            fw.write(yaml);
        }

        Config config = Config.loadOrCreate();

        assertEquals(999, config.seed);
        assertEquals(19132, config.port);
        assertEquals(100, config.maxPlayers);
        assertFalse(config.online);
        assertTrue(config.debug);
        assertTrue(config.velocity);
        assertEquals("mysecret", config.velocitySecret);
        assertFalse(config.simpleVoiceChat);
    }

    @Test
    void createsDefaultConfigWhenMissing() throws Exception {
        Config config = Config.loadOrCreate();

        assertTrue(tempDir.resolve("config.yml").toFile().exists(), "config.yml should be created");
        assertEquals(25565, config.port);
        assertEquals(500, config.maxPlayers);
        assertTrue(config.online);
        assertFalse(config.debug);
        assertFalse(config.velocity);
        assertEquals("ENTER-YOUR-SECRET-HERE", config.velocitySecret);
        assertTrue(config.simpleVoiceChat);
    }

    @Test
    void missingFieldsUseDefaults() throws Exception {
        String yaml = "port: 12345\n";
        try (FileWriter fw = new FileWriter(tempDir.resolve("config.yml").toFile())) {
            fw.write(yaml);
        }

        Config config = Config.loadOrCreate();

        assertEquals(12345, config.port);
        assertEquals(500, config.maxPlayers);
        assertTrue(config.online);
    }

    @Test
    void unknownFieldsAreIgnored() throws Exception {
        String yaml = "seed: 1\nport: 25565\nunknownKey: someValue\n";
        try (FileWriter fw = new FileWriter(tempDir.resolve("config.yml").toFile())) {
            fw.write(yaml);
        }

        assertDoesNotThrow(() -> Config.loadOrCreate());
    }

    @Test
    void loadsChunkGenerationThreadsFromConfig() throws Exception {
        String yaml = "seed: 1\nport: 25565\nchunkGenerationThreads: 4\n";
        try (FileWriter fw = new FileWriter(tempDir.resolve("config.yml").toFile())) {
            fw.write(yaml);
        }

        Config config = Config.loadOrCreate();

        assertEquals(4, config.chunkGenerationThreads);
    }

    @Test
    void defaultChunkGenerationThreadsIsAtLeastOne() throws Exception {
        Config config = Config.loadOrCreate();

        assertTrue(config.chunkGenerationThreads >= 1);
    }

    @Test
    void missingChunkGenerationThreadsUsesDefault() throws Exception {
        String yaml = "seed: 1\nport: 25565\n";
        try (FileWriter fw = new FileWriter(tempDir.resolve("config.yml").toFile())) {
            fw.write(yaml);
        }

        Config config = Config.loadOrCreate();

        assertEquals(Math.max(1, Runtime.getRuntime().availableProcessors() - 1), config.chunkGenerationThreads);
    }
}
