package dev.xdpxi.swiftmc;

import dev.xdpxi.swiftmc.events.*;
import dev.xdpxi.swiftmc.utils.Config;
import dev.xdpxi.swiftmc.utils.ServerLogger;
import dev.xdpxi.swiftmc.utils.TerrainGenerator;
import io.github.togar2.pvp.MinestomPvP;
import io.github.togar2.pvp.feature.CombatFeatureSet;
import io.github.togar2.pvp.feature.CombatFeatures;
import net.hollowcube.polar.PolarLoader;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;

import java.nio.file.Path;

public class Main {
    public static Config config;

    static void main(String[] args) throws Exception {
        // Setup config
        config = Config.loadOrCreate();

        // Setup logging
        ServerLogger.setup(args);

        // Init server
        MinecraftServer minecraftServer = MinecraftServer.init();

        // Init Minestom PVP
        MinestomPvP.init();

        // Instances
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instanceContainer = instanceManager.createInstanceContainer();

        // Polar world loader
        instanceContainer.setChunkLoader(new PolarLoader(Path.of("world.polar")));

        // Terrain Generator
        instanceContainer.setGenerator(new TerrainGenerator());

        // Events
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        
        AsyncPlayerConfigurationEvent.addListener(globalEventHandler, instanceContainer);
        PickupItemEvent.addListener(globalEventHandler, instanceContainer);
        PlayerBlockBreakEvent.addListener(globalEventHandler, instanceContainer);
        PlayerDisconnectEvent.addListener(globalEventHandler, instanceContainer);
        PlayerSpawnEvent.addListener(globalEventHandler, instanceContainer);
        ServerListPingEvent.addListener(globalEventHandler, instanceContainer);

        // MineStom PVP Events
        CombatFeatureSet modernVanilla = CombatFeatures.modernVanilla();
        globalEventHandler.addChild(modernVanilla.createNode());

        // Save chunks to storage after stopping the server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            instanceContainer.saveChunksToStorage();
            MinecraftServer.stopCleanly();
        }));

        // Start server
        minecraftServer.start("0.0.0.0", config.port);
    }
}