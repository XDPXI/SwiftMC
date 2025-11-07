package dev.xdpxi.swiftmc.events;

import dev.xdpxi.swiftmc.utils.Log;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.instance.InstanceContainer;

public class PlayerDisconnectEvent {
    public static void addListener(GlobalEventHandler globalEventHandler, InstanceContainer instanceContainer) {
        globalEventHandler.addListener(net.minestom.server.event.player.PlayerDisconnectEvent.class, event -> {
            final Player player = event.getPlayer();

            // Log player leave
            Log.info(player.getName() + " left the game");
        });
    }
}
