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

    private static File getConfigFile() {
        String userDir = System.getProperty("user.dir");
        return new File(userDir, "config.yml");
    }

    public int seed;
    public int port = 25565;
    public int maxPlayers = 500;
    public boolean online = true;
    public boolean debug = false;
    public boolean velocity = false;
    public String velocitySecret = "ENTER-YOUR-SECRET-HERE";
    public boolean simpleVoiceChat = true;
    public String terrainStyle = "minecraft";

    public static Config loadOrCreate() throws Exception {
        LoaderOptions loaderOptions = new LoaderOptions();
        Yaml yaml = new Yaml(loaderOptions);

        Config config;
        if (getConfigFile().exists()) {
            try (FileInputStream fis = new FileInputStream(getConfigFile())) {
                Map<String, Object> map = yaml.load(fis);
                config = new Config();

                Object seedObj = map.get("seed");
                if (seedObj != null) config.seed = (Integer) seedObj;

                Object portObj = map.get("port");
                if (portObj != null) config.port = (Integer) portObj;

                Object maxPlayersObj = map.get("maxPlayers");
                if (maxPlayersObj != null) config.maxPlayers =
                        (Integer) maxPlayersObj;

                Object onlineObj = map.get("online");
                if (onlineObj != null) config.online =
                        (Boolean) onlineObj;

                Object debugObj = map.get("debug");
                if (debugObj != null) config.debug =
                        (Boolean) debugObj;

                Object velocityObj = map.get("velocity");
                if (velocityObj != null) config.velocity =
                        (Boolean) velocityObj;

                Object velocitySecretObj = map.get("velocitySecret");
                if (velocitySecretObj != null) config.velocitySecret =
                        (String) velocitySecretObj;

                Object simpleVoiceChatObj = map.get("simpleVoiceChat");
                if (simpleVoiceChatObj != null) config.simpleVoiceChat =
                        (Boolean) simpleVoiceChatObj;

                Object terrainStyleObj = map.get("terrainStyle");
                if (terrainStyleObj != null) config.terrainStyle =
                        (String) terrainStyleObj;
            }
        } else {
            Log.warn("Config not found! Generating with default values.");
            config = new Config();
            config.seed = new Random().nextInt();

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("seed", config.seed);
            data.put("port", config.port);
            data.put("maxPlayers", config.maxPlayers);
            data.put("online", config.online);
            data.put("debug", config.debug);
            data.put("velocity", config.velocity);
            data.put("velocitySecret", config.velocitySecret);
            data.put("simpleVoiceChat", config.simpleVoiceChat);
            data.put("terrainStyle", config.terrainStyle);

            DumperOptions dumperOptions = new DumperOptions();
            dumperOptions.setPrettyFlow(true);
            dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

            Yaml yamlWriter = new Yaml(dumperOptions);
            try (FileWriter writer = new FileWriter(getConfigFile())) {
                yamlWriter.dump(data, writer);
            }

            Log.info("Created config with random seed: " + config.seed);
        }

        return config;
    }
}
