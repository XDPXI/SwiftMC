package dev.xdpxi.swiftmc.plugin;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents plugin metadata from plugin.yml
 */
public record PluginDescriptor(String name, String version, String main, String description, String author,
                               List<String> depend, List<String> softDepend) {
    public PluginDescriptor(String name, String version, String main, String description,
                            String author, List<String> depend, List<String> softDepend) {
        this.name = name;
        this.version = version;
        this.main = main;
        this.description = description;
        this.author = author;
        this.depend = depend != null ? depend : new ArrayList<>();
        this.softDepend = softDepend != null ? softDepend : new ArrayList<>();
    }

    /**
     * Loads a plugin descriptor from plugin.yml
     */
    public static PluginDescriptor load(InputStream stream) throws Exception {
        LoaderOptions loaderOptions = new LoaderOptions();
        Yaml yaml = new Yaml(loaderOptions);
        Map<String, Object> data = yaml.load(stream);

        String name = (String) data.get("name");
        String version = (String) data.get("version");
        String main = (String) data.get("main");
        String description = (String) data.getOrDefault("description", "");
        String author = (String) data.getOrDefault("author", "Unknown");

        @SuppressWarnings("unchecked")
        List<String> depend = (List<String>) data.get("depend");

        @SuppressWarnings("unchecked")
        List<String> softDepend = (List<String>) data.get("softdepend");

        if (name == null || version == null || main == null) {
            throw new IllegalArgumentException("plugin.yml must contain name, version, and main fields");
        }

        return new PluginDescriptor(name, version, main, description, author, depend, softDepend);
    }
}