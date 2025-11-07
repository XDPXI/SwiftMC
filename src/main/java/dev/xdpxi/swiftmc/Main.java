package dev.xdpxi.swiftmc;

import dev.xdpxi.swiftmc.events.*;
import dev.xdpxi.swiftmc.utils.Config;
import dev.xdpxi.swiftmc.utils.ServerLogger;
import dev.xdpxi.swiftmc.utils.TerrainGenerator;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;

public class Main {
    public static Config config;

    static void main(String[] args) throws Exception {
        // Setup config
        config = Config.loadOrCreate();

        // Setup logging
        ServerLogger.setup(args);

        // Init server
        MinecraftServer minecraftServer = MinecraftServer.init();

        // Instances
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instanceContainer = instanceManager.createInstanceContainer();

        // Terrain Generator
        instanceContainer.setGenerator(new TerrainGenerator());

        // Events
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        AsyncPlayerConfigurationEvent.addListener(globalEventHandler, instanceContainer);
        PickupItemEvent.addListener(globalEventHandler, instanceContainer);
        PlayerBlockBreakEvent.addListener(globalEventHandler, instanceContainer);
        PlayerSpawnEvent.addListener(globalEventHandler, instanceContainer);
        ServerListPingEvent.addListener(globalEventHandler, instanceContainer);

        // Start server
        minecraftServer.start("0.0.0.0", config.port);
    }
}