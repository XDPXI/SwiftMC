package dev.xdpxi.swiftmc;

import dev.xdpxi.swiftmc.events.*;
import dev.xdpxi.swiftmc.player.PlayerDataManager;
import dev.xdpxi.swiftmc.utils.Config;
import dev.xdpxi.swiftmc.utils.Log;
import dev.xdpxi.swiftmc.utils.TerrainGenerator;
import io.github.togar2.pvp.MinestomPvP;
import io.github.togar2.pvp.feature.CombatFeatureSet;
import io.github.togar2.pvp.feature.CombatFeatures;
import io.github.togar2.pvp.utils.CombatVersion;
import net.hollowcube.polar.PolarLoader;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;

import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static Config config;

    static void main(String[] args) throws Exception {
        Log.info("Starting server setup...");

        // Setup config
        try {
            config = Config.loadOrCreate();
            Log.info("Configuration loaded successfully.");
        } catch (Exception e) {
            Log.error("Failed to load configuration: " + e.getMessage());
            throw e;
        }

        // Init server
        MinecraftServer minecraftServer = MinecraftServer.init();
        Log.info("MinecraftServer initialized.");

        // Init Minestom PVP
        try {
            MinestomPvP.init();
            Log.info("MinestomPvP initialized successfully.");
        } catch (Exception e) {
            Log.warn("MinestomPvP initialization failed: " + e.getMessage());
        }

        // Instances
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instanceContainer = instanceManager.createInstanceContainer();
        Log.debug("InstanceContainer created.");

        // Polar world loader
        Path worldFolder = Path.of("worlds");
        Files.createDirectories(worldFolder);
        Path polarFile = worldFolder.resolve("overworld.polar");
        instanceContainer.setChunkLoader(new PolarLoader(polarFile));
        Log.info("Polar world loader set for instance.");

        // Terrain Generator
        instanceContainer.setGenerator(new TerrainGenerator());
        Log.info("Custom terrain generator applied.");

        // Events
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        Log.debug("GlobalEventHandler obtained.");

        AsyncPlayerConfigurationEvent.addListener(globalEventHandler, instanceContainer);
        PickupItemEvent.addListener(globalEventHandler, instanceContainer);
        PlayerBlockBreakEvent.addListener(globalEventHandler, instanceContainer);
        PlayerCommandEvent.addListener(globalEventHandler, instanceContainer);
        PlayerDisconnectEvent.addListener(globalEventHandler, instanceContainer);
        PlayerSpawnEvent.addListener(globalEventHandler, instanceContainer);
        ServerListPingEvent.addListener(globalEventHandler, instanceContainer);
        Log.info("Event listeners registered.");

        // MineStom PVP Events
        CombatFeatureSet featureSet = CombatFeatures.empty()
                .version(CombatVersion.MODERN)
                .remove(CombatFeatures.VANILLA_TRIDENT.featureType())
                .build();
        globalEventHandler.addChild(featureSet.createNode());
        Log.info("Combat features enabled.");

        // Save world when closing server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Log.info("Shutdown initiated. Saving world...");

            MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player -> {
                PlayerDataManager.savePlayer(player);
                Log.info(player.getUsername() + " data saved on shutdown.");
            });

            instanceContainer.saveChunksToStorage();

            MinecraftServer.stopCleanly();

            Log.close();

            Log.info("Server stopped cleanly.");
        }));

        // Start server
        try {
            minecraftServer.start("0.0.0.0", config.port);
            Log.info("Server started on port " + config.port);
        } catch (Exception e) {
            Log.error("Failed to start server: " + e.getMessage());
            throw e;
        }
    }
}
