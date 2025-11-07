package dev.xdpxi.swiftmc.events;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.ping.Status;

import static dev.xdpxi.swiftmc.Main.config;

public class ServerListPingEvent {
    public static void addListener(GlobalEventHandler globalEventHandler) {
        globalEventHandler.addListener(net.minestom.server.event.server.ServerListPingEvent.class, event -> {
            int onlinePlayers = MinecraftServer.getConnectionManager().getOnlinePlayerCount();

            event.setStatus(Status.builder()
                    .description(Component.text("Welcome to my Minecraft server!", NamedTextColor.GOLD))
                    .playerInfo(Status.PlayerInfo.builder()
                            .onlinePlayers(onlinePlayers)
                            .maxPlayers(config.maxPlayers)
                            .build())
                    .playerInfo(onlinePlayers, config.maxPlayers)
                    .versionInfo(new Status.VersionInfo("SwiftMC 1.21.10", 773))
                    .build());
        });
    }
}
