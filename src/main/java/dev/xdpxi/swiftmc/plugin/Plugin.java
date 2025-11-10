package dev.xdpxi.swiftmc.plugin;

import dev.xdpxi.swiftmc.utils.Log;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Base class for all SwiftMC plugins.
 * Provides a Bukkit-style API while using Minestom underneath.
 */
public abstract class Plugin {
    private PluginDescriptor descriptor;
    private File dataFolder;
    private PluginClassLoader classLoader;
    private boolean enabled = false;
    private EventNode<@NotNull Event> eventNode;

    /**
     * Called when the plugin is loaded.
     */
    public abstract void onLoad();

    /**
     * Called when the plugin is enabled.
     */
    public abstract void onEnable();

    /**
     * Called when the plugin is disabled.
     */
    public abstract void onDisable();

    /**
     * Gets the plugin's data folder.
     * Creates it if it doesn't exist.
     */
    public File getDataFolder() {
        if (dataFolder == null) {
            dataFolder = new File("plugins/" + getName());
        }
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        return dataFolder;
    }

    void setDataFolder(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    /**
     * Gets the plugin's name.
     */
    public String getName() {
        return descriptor.name();
    }

    /**
     * Gets the plugin's version.
     */
    public String getVersion() {
        return descriptor.version();
    }

    /**
     * Gets the plugin's description.
     */
    public String getDescription() {
        return descriptor.description();
    }

    /**
     * Gets the plugin's author.
     */
    public String getAuthor() {
        return descriptor.author();
    }

    /**
     * Checks if the plugin is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled && eventNode != null) {
            MinecraftServer.getGlobalEventHandler().removeChild(eventNode);
            eventNode = null;
        }
    }

    /**
     * Gets a resource from the plugin JAR.
     */
    public InputStream getResource(String filename) {
        if (classLoader == null) {
            throw new IllegalStateException("Plugin not properly initialized");
        }
        return classLoader.getResourceAsStream(filename);
    }

    /**
     * Saves a resource from the plugin JAR to the data folder.
     */
    public void saveResource(String resourcePath, boolean replace) {
        if (resourcePath == null || resourcePath.isEmpty()) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = getResource(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found");
        }

        File outFile = new File(getDataFolder(), resourcePath);
        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(getDataFolder(), resourcePath.substring(0, Math.max(lastIndex, 0)));

        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try {
            if (!outFile.exists() || replace) {
                Files.copy(in, outFile.toPath());
            }
        } catch (Exception e) {
            Log.error("Could not save " + outFile.getName() + " to " + outFile);
            e.printStackTrace();
        }
    }

    /**
     * Saves the default config.yml from the plugin JAR.
     */
    public void saveDefaultConfig() {
        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveResource("config.yml", false);
        }
    }

    /**
     * Registers a command with the server.
     */
    public void registerCommand(Command command) {
        MinecraftServer.getCommandManager().register(command);
        Log.info(getName() + " registered command: " + command.getName());
    }

    /**
     * Registers an event listener.
     */
    public <T extends Event> void registerEvent(Class<T> eventType, EventListener<@NotNull T> listener) {
        if (eventNode == null) {
            eventNode = EventNode.all(getName() + "-events");
            MinecraftServer.getGlobalEventHandler().addChild(eventNode);
        }
        eventNode.addListener(eventType, listener::run);
    }

    /**
     * Logs an info message.
     */
    public void logInfo(String message) {
        Log.info("[" + getName() + "] " + message);
    }

    /**
     * Logs a warning message.
     */
    public void logWarning(String message) {
        Log.warn("[" + getName() + "] " + message);
    }

    // Internal methods used by PluginManager

    /**
     * Logs an error message.
     */
    public void logError(String message) {
        Log.error("[" + getName() + "] " + message);
    }

    /**
     * Logs a debug message.
     */
    public void logDebug(String message) {
        Log.debug("[" + getName() + "] " + message);
    }

    void setClassLoader(PluginClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    PluginDescriptor getDescriptor() {
        return descriptor;
    }

    void setDescriptor(PluginDescriptor descriptor) {
        this.descriptor = descriptor;
    }
}