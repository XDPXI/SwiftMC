package dev.xdpxi.swiftmc;

import dev.xdpxi.swiftmc.utils.Config;
import dev.xdpxi.swiftmc.utils.ServerLogger;
import dev.xdpxi.swiftmc.utils.TerrainGenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.ping.Status;

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

        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();
            event.setSpawningInstance(instanceContainer);
            player.setRespawnPoint(new Pos(0, 64, 0));
        });

        globalEventHandler.addListener(ServerListPingEvent.class, event -> {
            int onlinePlayers = MinecraftServer.getConnectionManager().getOnlinePlayerCount();

            event.setStatus(Status.builder()
                    .description(Component.text("Welcome to my Minecraft server!", NamedTextColor.GOLD))
                    .playerInfo(Status.PlayerInfo.builder()
                            .onlinePlayers(onlinePlayers)
                            .maxPlayers(config.maxPlayers)
                            .build())
                    .playerInfo(onlinePlayers, config.maxPlayers)
                    .versionInfo(new Status.VersionInfo("1.21.10", 773))
                    .build());
        });

        // Start server
        minecraftServer.start("0.0.0.0", config.port);
    }
}
