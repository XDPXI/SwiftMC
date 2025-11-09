package dev.xdpxi.swiftmc.api.plugin;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * ClassLoader for loading plugin classes from JAR files.
 */
public class PluginClassLoader extends URLClassLoader {
    private final PluginManager pluginManager;
    private final PluginDescriptor descriptor;

    public PluginClassLoader(PluginManager pluginManager, PluginDescriptor descriptor,
                             File file, ClassLoader parent) throws Exception {
        super(new URL[]{file.toURI().toURL()}, parent);
        this.pluginManager = pluginManager;
        this.descriptor = descriptor;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // Try to load from this plugin first
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            // Try to load from other plugins
            Class<?> result = pluginManager.getClassFromPlugins(name, this);
            if (result != null) {
                return result;
            }
            throw e;
        }
    }

    public PluginDescriptor getDescriptor() {
        return descriptor;
    }
}