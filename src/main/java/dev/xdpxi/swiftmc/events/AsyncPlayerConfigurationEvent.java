package dev.xdpxi.swiftmc.events;

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
                net.minestom.server.event.player
                        .AsyncPlayerConfigurationEvent.class,
                event -> {
                    final Player player = event.getPlayer();
                    event.setSpawningInstance(instanceContainer);
                    player.setRespawnPoint(new Pos(0, 64, 0));
                }
        );
    }
}
