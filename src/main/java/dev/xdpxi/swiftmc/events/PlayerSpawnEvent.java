package dev.xdpxi.swiftmc.events;

import dev.xdpxi.swiftmc.player.PlayerDataManager;
import dev.xdpxi.swiftmc.utils.Log;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.event.GlobalEventHandler;

public class PlayerSpawnEvent {
    public static void addListener(GlobalEventHandler globalEventHandler) {
        globalEventHandler.addListener(net.minestom.server.event.player.PlayerSpawnEvent.class, event -> {
            final Player player = event.getPlayer();

            // Load player data
            PlayerDataManager.loadPlayer(player);

            // Set game mode to survival
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
