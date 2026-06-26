package dev.xdpxi.swiftmc.events;

import dev.xdpxi.swiftmc.player.PlayerData;
import dev.xdpxi.swiftmc.player.PlayerDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.instance.InstanceContainer;
import org.jspecify.annotations.NonNull;

public class AsyncPlayerConfigurationEvent {

    public static void addListener(
            @NonNull GlobalEventHandler globalEventHandler,
            InstanceContainer instanceContainer
    ) {
        globalEventHandler.addListener(
                net.minestom.server.event.player.AsyncPlayerConfigurationEvent.class,
                event -> {
                    final Player player = event.getPlayer();
                    player.setRespawnPoint(new Pos(0, 64, 0));

                    PlayerData data = PlayerDataManager.loadDataByUuid(player.getUuid()).join();
                    if (data != null && data.banned) {
                        String msg = data.banReason != null && !data.banReason.isBlank()
                                ? "You are banned: " + data.banReason
                                : "You are banned from this server.";
                        player.kick(Component.text(msg, NamedTextColor.RED));
                        return;
                    }

                    event.setSpawningInstance(instanceContainer);
                }
        );
    }
}
