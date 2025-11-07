package dev.xdpxi.swiftmc.events;

import dev.xdpxi.swiftmc.utils.Log;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.instance.InstanceContainer;

public class PlayerCommandEvent {
    public static void addListener(GlobalEventHandler globalEventHandler, InstanceContainer instanceContainer) {
        globalEventHandler.addListener(net.minestom.server.event.player.PlayerCommandEvent.class, event -> {
            String command = event.getCommand();
            Player player = event.getPlayer();

            if (command.equals("survival")) {
                player.setGameMode(GameMode.SURVIVAL);
                player.sendMessage(Component.text("Gamemode set to Survival", NamedTextColor.GREEN));
                Log.info(player.getName() + " changed gamemode to SURVIVAL");
                event.setCancelled(true);
            } else if (command.equals("creative")) {
                player.setGameMode(GameMode.CREATIVE);
                player.sendMessage(Component.text("Gamemode set to Creative", NamedTextColor.GREEN));
                Log.info(player.getName() + " changed gamemode to CREATIVE");
                event.setCancelled(true);
            } else if (command.equals("spectator")) {
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage(Component.text("Gamemode set to Spectator", NamedTextColor.GREEN));
                Log.info(player.getName() + " changed gamemode to SPECTATOR");
                event.setCancelled(true);
            }
        });
    }
}