package dev.xdpxi.swiftmc.plugin;

import dev.xdpxi.swiftmc.Main;
import dev.xdpxi.swiftmc.utils.Log;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

/**
 * Base class for all SwiftMC plugins.
 * Provides a Bukkit-style API while using Minestom underneath.
 */
public abstract class Plugin {
    private final List<Command> registeredCommands = new ArrayList<>();
    private final List<Task> scheduledTasks = new ArrayList<>();
    private PluginDescriptor descriptor;
    private File dataFolder;
    private PluginClassLoader classLoader;
    private boolean enabled = false;
    private EventNode<@NotNull Event> eventNode;
    private Map<String, Object> config;

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
        if (!enabled) {
            cleanup();
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
     * Loads the config.yml file.
     */
    public void loadConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            config = new HashMap<>();
            return;
        }

        try (FileInputStream fis = new FileInputStream(configFile)) {
            LoaderOptions loaderOptions = new LoaderOptions();
            Yaml yaml = new Yaml(loaderOptions);
            config = yaml.load(fis);
            if (config == null) {
                config = new HashMap<>();
            }
        } catch (Exception e) {
            logError("Failed to load config: " + e.getMessage());
            config = new HashMap<>();
        }
    }

    /**
     * Saves the current config to config.yml.
     */
    public void saveConfig() {
        if (config == null) {
            config = new HashMap<>();
        }

        File configFile = new File(getDataFolder(), "config.yml");
        try (FileWriter writer = new FileWriter(configFile)) {
            DumperOptions options = new DumperOptions();
            options.setPrettyFlow(true);
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);
            yaml.dump(config, writer);
        } catch (Exception e) {
            logError("Failed to save config: " + e.getMessage());
        }
    }

    /**
     * Gets a config value.
     */
    public Object getConfig(String key) {
        if (config == null) return null;
        return config.get(key);
    }

    /**
     * Gets a config value with a default.
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfig(String key, T defaultValue) {
        if (config == null) return defaultValue;
        Object value = config.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * Sets a config value.
     */
    public void setConfig(String key, Object value) {
        if (config == null) {
            config = new HashMap<>();
        }
        config.put(key, value);
    }

    /**
     * Registers a command with the server.
     */
    public void registerCommand(Command command) {
        MinecraftServer.getCommandManager().register(command);
        registeredCommands.add(command);
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
     * Schedules a repeating task.
     */
    public Task scheduleRepeatingTask(Runnable task, long delayTicks, long periodTicks) {
        Task scheduledTask = MinecraftServer.getSchedulerManager().scheduleTask(
                () -> {
                    task.run();
                    return TaskSchedule.tick((int) periodTicks);
                },
                TaskSchedule.tick((int) delayTicks)
        );
        scheduledTasks.add(scheduledTask);
        return scheduledTask;
    }

    /**
     * Schedules a delayed task.
     */
    public Task scheduleDelayedTask(Runnable task, long delayTicks) {
        Task scheduledTask = MinecraftServer.getSchedulerManager().scheduleTask(
                () -> {
                    task.run();
                    return TaskSchedule.stop();
                },
                TaskSchedule.tick((int) delayTicks)
        );
        scheduledTasks.add(scheduledTask);
        return scheduledTask;
    }

    /**
     * Gets the main server instance.
     */
    public Instance getServerInstance() {
        return Main.getInstanceContainer();
    }

    /**
     * Gets all online players.
     */
    public Collection<Player> getOnlinePlayers() {
        return MinecraftServer.getConnectionManager().getOnlinePlayers();
    }

    /**
     * Gets a player by name.
     */
    public Player getPlayer(String name) {
        return MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
                .filter(p -> p.getUsername().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets a player by UUID.
     */
    public Player getPlayer(UUID uuid) {
        return MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
                .filter(p -> p.getUuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    /**
     * Broadcasts a message to all players.
     */
    public void broadcastMessage(net.kyori.adventure.text.Component message) {
        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(p -> p.sendMessage(message));
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

    /**
     * Cleanup resources when plugin is disabled.
     */
    private void cleanup() {
        // Cancel all scheduled tasks
        scheduledTasks.forEach(Task::cancel);
        scheduledTasks.clear();

        // Unregister commands
        registeredCommands.forEach(cmd -> MinecraftServer.getCommandManager().unregister(cmd));
        registeredCommands.clear();

        // Remove event node
        if (eventNode != null) {
            MinecraftServer.getGlobalEventHandler().removeChild(eventNode);
            eventNode = null;
        }
    }

    // Internal methods used by PluginManager

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