package dev.xdpxi.swiftmc.utils;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class Config {
    private static final File CONFIG_FILE = new File("config.yml");
    public int seed;
    public int port = 25565;
    public int maxPlayers = 500;
    public boolean debugEnabled = false;

    public static Config loadOrCreate() throws Exception {
        LoaderOptions loaderOptions = new LoaderOptions();
        Yaml yaml = new Yaml(loaderOptions);

        Config config;
        if (CONFIG_FILE.exists()) {
            try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
                Map<String, Object> map = yaml.load(fis);
                config = new Config();

                Object seedObj = map.get("seed");
                if (seedObj != null) config.seed = ((Number) seedObj).intValue();

                Object portObj = map.get("port");
                if (portObj != null) config.port = ((Number) portObj).intValue();

                Object maxPlayersObj = map.get("maxPlayers");
                if (maxPlayersObj != null) config.maxPlayers = ((Number) maxPlayersObj).intValue();

                Object debugEnabledObj = map.get("debugEnabled");
                if (debugEnabledObj != null) config.debugEnabled = (Boolean) debugEnabledObj;
            }
        } else {
            config = new Config();
            config.seed = new Random().nextInt();

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("seed", config.seed);
            data.put("port", config.port);
            data.put("maxPlayers", config.maxPlayers);
            data.put("debugEnabled", config.debugEnabled);

            DumperOptions dumperOptions = new DumperOptions();
            dumperOptions.setPrettyFlow(true);
            dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

            Yaml yamlWriter = new Yaml(dumperOptions);
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                yamlWriter.dump(data, writer);
            }

            System.out.println("Created config.yml with random seed: " + config.seed);
        }

        return config;
    }
}
