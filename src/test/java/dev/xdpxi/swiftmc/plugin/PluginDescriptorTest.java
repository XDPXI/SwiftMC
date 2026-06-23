package dev.xdpxi.swiftmc.plugin;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PluginDescriptorTest {

    @Contract("_ -> new")
    private static @NonNull InputStream yaml(@NonNull String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void loadsFullDescriptor() {
        String content = "name: TestPlugin\nversion: \"1.0\"\nmain: dev.test.Main\ndescription: A test plugin\nauthor: Tester\ndepend:\n  - OtherPlugin\nsoftdepend:\n  - OptionalPlugin\n";

        PluginDescriptor desc = PluginDescriptor.load(yaml(content));

        assertEquals("TestPlugin", desc.name());
        assertEquals("1.0", desc.version());
        assertEquals("dev.test.Main", desc.main());
        assertEquals("A test plugin", desc.description());
        assertEquals("Tester", desc.author());
        assertEquals(1, desc.depend().size());
        assertEquals("OtherPlugin", desc.depend().get(0));
        assertEquals(1, desc.softDepend().size());
        assertEquals("OptionalPlugin", desc.softDepend().get(0));
    }

    @Test
    void missingRequiredField_throws() {
        String content = "version: \"1.0\"\nmain: dev.test.Main\n"; // missing name

        assertThrows(IllegalArgumentException.class,
            () -> PluginDescriptor.load(yaml(content)));
    }

    @Test
    void missingOptionalFields_useDefaults() {
        String content = "name: Minimal\nversion: \"0.1\"\nmain: dev.test.Minimal\n";

        PluginDescriptor desc = PluginDescriptor.load(yaml(content));

        assertEquals("", desc.description());
        assertEquals("Unknown", desc.author());
        assertNotNull(desc.depend());
        assertTrue(desc.depend().isEmpty());
        assertNotNull(desc.softDepend());
        assertTrue(desc.softDepend().isEmpty());
    }

    @Test
    void nullDependLists_returnEmptyLists() {
        String content = "name: NoDeps\nversion: \"1.0\"\nmain: dev.test.NoDeps\ndepend:\nsoftdepend:\n";

        PluginDescriptor desc = PluginDescriptor.load(yaml(content));

        assertNotNull(desc.depend());
        assertNotNull(desc.softDepend());
        assertTrue(desc.depend().isEmpty());
        assertTrue(desc.softDepend().isEmpty());
    }
}
