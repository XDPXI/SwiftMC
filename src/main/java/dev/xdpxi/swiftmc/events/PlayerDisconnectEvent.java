package dev.xdpxi.swiftmc.events;

import dev.xdpxi.swiftmc.player.PlayerDataManager;
import dev.xdpxi.swiftmc.utils.Log;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import org.jspecify.annotations.NonNull;

public class PlayerDisconnectEvent {

    public static void addListener(@NonNull GlobalEventHandler globalEventHandler) {
        globalEventHandler.addListener(
                net.minestom.server.event.player.PlayerDisconnectEvent.class,
                event -> {
                    final Player player = event.getPlayer();

                    // Save player data
                    PlayerDataManager.savePlayer(player);

                    // Log player leave
                    Log.info(player.getUsername() + " left the game");
                }
        );
    }
}
