package dev.xdpxi.swiftmc.plugin;

import dev.xdpxi.swiftmc.utils.Log;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Manages plugin loading, enabling, and disabling.
 */
public class PluginManager {
    private final Map<String, Plugin> plugins = new LinkedHashMap<>();
    private final Map<String, PluginClassLoader> classLoaders = new HashMap<>();
    private final File pluginsFolder;

    public PluginManager() {
        this.pluginsFolder = new File("plugins");
        if (!pluginsFolder.exists()) {
            pluginsFolder.mkdirs();
        }
    }

    /**
     * Loads all plugins from the plugins folder.
     */
    public void loadPlugins() {
        Log.info("Loading plugins from " + pluginsFolder.getAbsolutePath());

        File[] files = pluginsFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null || files.length == 0) {
            Log.info("No plugins found.");
            return;
        }

        // First pass: Load all plugin descriptors
        Map<File, PluginDescriptor> descriptors = new HashMap<>();
        for (File file : files) {
            try {
                PluginDescriptor descriptor = loadDescriptor(file);
                descriptors.put(file, descriptor);
                Log.info("Found plugin: " + descriptor.name() + " v" + descriptor.version());
            } catch (Exception e) {
                Log.error("Failed to load plugin descriptor from " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Second pass: Load plugins in dependency order
        Set<String> loaded = new HashSet<>();
        while (loaded.size() < descriptors.size()) {
            boolean progress = false;

            for (Map.Entry<File, PluginDescriptor> entry : descriptors.entrySet()) {
                File file = entry.getKey();
                PluginDescriptor descriptor = entry.getValue();

                if (loaded.contains(descriptor.name())) {
                    continue;
                }

                // Check if all hard dependencies are loaded
                boolean canLoad = true;
                for (String depend : descriptor.depend()) {
                    if (!loaded.contains(depend)) {
                        canLoad = false;
                        break;
                    }
                }

                if (canLoad) {
                    try {
                        Plugin plugin = loadPlugin(file, descriptor);
                        plugins.put(descriptor.name(), plugin);
                        loaded.add(descriptor.name());
                        progress = true;
                        Log.info("Loaded plugin: " + descriptor.name() + " v" + descriptor.version() +
                                " by " + descriptor.author());
                    } catch (Exception e) {
                        Log.error("Failed to load plugin " + descriptor.name() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }

            // If no progress was made, we have circular dependencies or missing dependencies
            if (!progress) {
                for (Map.Entry<File, PluginDescriptor> entry : descriptors.entrySet()) {
                    PluginDescriptor descriptor = entry.getValue();
                    if (!loaded.contains(descriptor.name())) {
                        Log.error("Could not load plugin " + descriptor.name() +
                                " due to missing dependencies: " + descriptor.depend());
                    }
                }
                break;
            }
        }
    }

    /**
     * Enables all loaded plugins.
     */
    public void enablePlugins() {
        Log.info("Enabling plugins...");
        for (Plugin plugin : plugins.values()) {
            enablePlugin(plugin);
        }
        Log.info("Enabled " + plugins.size() + " plugin(s).");
    }

    /**
     * Disables all plugins.
     */
    public void disablePlugins() {
        Log.info("Disabling plugins...");
        // Disable in reverse order
        List<Plugin> pluginList = new ArrayList<>(plugins.values());
        Collections.reverse(pluginList);

        for (Plugin plugin : pluginList) {
            disablePlugin(plugin);
        }
    }

    /**
     * Gets a plugin by name.
     */
    public Plugin getPlugin(String name) {
        return plugins.get(name);
    }

    /**
     * Gets all loaded plugins.
     */
    public Collection<Plugin> getPlugins() {
        return Collections.unmodifiableCollection(plugins.values());
    }

    /**
     * Checks if a plugin is loaded.
     */
    public boolean isPluginLoaded(String name) {
        return plugins.containsKey(name);
    }

    // Internal methods

    private PluginDescriptor loadDescriptor(File file) throws Exception {
        try (JarFile jar = new JarFile(file)) {
            JarEntry entry = jar.getJarEntry("plugin.yml");
            if (entry == null) {
                throw new IllegalArgumentException("Plugin JAR must contain plugin.yml");
            }

            try (InputStream stream = jar.getInputStream(entry)) {
                return PluginDescriptor.load(stream);
            }
        }
    }

    private Plugin loadPlugin(File file, PluginDescriptor descriptor) throws Exception {
        // Create class loader
        PluginClassLoader classLoader = new PluginClassLoader(
                this,
                descriptor,
                file,
                getClass().getClassLoader()
        );
        classLoaders.put(descriptor.name(), classLoader);

        // Load main class
        Class<?> mainClass = classLoader.loadClass(descriptor.main());
        if (!Plugin.class.isAssignableFrom(mainClass)) {
            throw new IllegalArgumentException("Main class must extend Plugin");
        }

        // Create plugin instance
        Plugin plugin = (Plugin) mainClass.getDeclaredConstructor().newInstance();
        plugin.setDescriptor(descriptor);
        plugin.setDataFolder(new File(pluginsFolder, descriptor.name()));
        plugin.setClassLoader(classLoader);

        // Call onLoad
        try {
            plugin.onLoad();
        } catch (Exception e) {
            Log.error("Error during onLoad for " + descriptor.name() + ": " + e.getMessage());
            e.printStackTrace();
        }

        return plugin;
    }

    private void enablePlugin(Plugin plugin) {
        if (plugin.isEnabled()) {
            return;
        }

        try {
            plugin.onEnable();
            plugin.setEnabled(true);
            Log.info("Enabled plugin: " + plugin.getName());
        } catch (Exception e) {
            Log.error("Error enabling plugin " + plugin.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void disablePlugin(Plugin plugin) {
        if (!plugin.isEnabled()) {
            return;
        }

        try {
            plugin.onDisable();
            plugin.setEnabled(false);
            Log.info("Disabled plugin: " + plugin.getName());
        } catch (Exception e) {
            Log.error("Error disabling plugin " + plugin.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets a class from other plugins' class loaders.
     * Used for inter-plugin dependencies.
     */
    Class<?> getClassFromPlugins(String name, PluginClassLoader requestingLoader) {
        for (PluginClassLoader loader : classLoaders.values()) {
            if (loader == requestingLoader) {
                continue;
            }

            try {
                return loader.findClass(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }
}