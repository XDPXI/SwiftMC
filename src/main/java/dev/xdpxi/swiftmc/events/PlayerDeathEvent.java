package dev.xdpxi.swiftmc.events;

import dev.xdpxi.swiftmc.utils.Log;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;

public class PlayerDeathEvent {
    public static void addListener(GlobalEventHandler globalEventHandler) {
        globalEventHandler.addListener(net.minestom.server.event.player.PlayerDeathEvent.class, event -> {
            Player player = event.getPlayer();

            Log.info(player.getUsername() + " died");

            try {
                // Heal the player
                player.setHealth(20);
                player.setFood(20);
                player.clearEffects();

                // Teleport to spawn
                Pos respawnPoint = player.getRespawnPoint();
                player.teleport(respawnPoint);

                Log.debug(player.getUsername() + " respawned successfully");
            } catch (Exception e) {
                Log.error("Error respawning player " + player.getUsername() + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}