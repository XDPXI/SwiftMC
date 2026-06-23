package dev.xdpxi.swiftmc.plugin;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PluginManagerTest {

    @TempDir
    Path tempDir;

    private String originalUserDir;

    private static void writeJarWithPluginYml(File jar, @NonNull String yamlContent) throws Exception {
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar))) {
            jos.putNextEntry(new JarEntry("plugin.yml"));
            jos.write(yamlContent.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
    }

    @BeforeEach
    void redirectWorkingDir() {
        originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toAbsolutePath().toString());
        // PluginManager constructor creates "plugins" dir relative to user.dir
        tempDir.resolve("plugins").toFile().mkdirs();
    }

    @AfterEach
    void restoreWorkingDir() {
        System.setProperty("user.dir", originalUserDir);
    }

    @Test
    void emptyPluginsDir_loadsNothing() {
        PluginManager manager = new PluginManager();
        assertDoesNotThrow(manager::loadPlugins);
        assertTrue(manager.getPlugins().isEmpty());
    }

    @Test
    void nonJarFile_isIgnored() throws Exception {
        File txt = tempDir.resolve("plugins/notaplugin.txt").toFile();
        txt.createNewFile();

        PluginManager manager = new PluginManager();
        assertDoesNotThrow(manager::loadPlugins);
        assertTrue(manager.getPlugins().isEmpty());
    }

    @Test
    void jarMissingPluginYml_isSkipped() throws Exception {
        File jar = tempDir.resolve("plugins/empty.jar").toFile();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar))) {
            jos.putNextEntry(new JarEntry("com/example/Placeholder.class"));
            jos.write(new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE});
            jos.closeEntry();
        }

        PluginManager manager = new PluginManager();
        assertDoesNotThrow(manager::loadPlugins);
        assertTrue(manager.getPlugins().isEmpty());
    }

    @Test
    void isPluginLoaded_returnsFalse_whenNotLoaded() {
        PluginManager manager = new PluginManager();
        assertFalse(manager.isPluginLoaded("NonExistentPlugin"));
    }
}
