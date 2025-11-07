package dev.xdpxi.swiftmc.events;

import dev.xdpxi.swiftmc.utils.Log;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.instance.InstanceContainer;

public class PlayerSpawnEvent {
    public static void addListener(GlobalEventHandler globalEventHandler, InstanceContainer instanceContainer) {
        globalEventHandler.addListener(net.minestom.server.event.player.PlayerSpawnEvent.class, event -> {
            final Player player = event.getPlayer();

            // Set game mode to survival so inventory works properly
            player.setGameMode(GameMode.SURVIVAL);

            // Enable item pickup
            player.setCanPickupItem(true);

            // Set player skin
            player.setSkin(PlayerSkin.fromUsername(player.getUsername()));

            // Log player join
            Log.info(player.getUsername() + " joined the game");
        });
    }
}
