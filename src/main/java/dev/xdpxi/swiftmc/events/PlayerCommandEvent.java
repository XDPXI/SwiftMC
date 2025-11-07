package dev.xdpxi.swiftmc.events;

import dev.xdpxi.swiftmc.nether.NetherManager;
import dev.xdpxi.swiftmc.utils.Log;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;

public class PlayerCommandEvent {
    public static void addListener(GlobalEventHandler globalEventHandler) {
        globalEventHandler.addListener(net.minestom.server.event.player.PlayerCommandEvent.class, event -> {
            String command = event.getCommand();
            Player player = event.getPlayer();

            switch (command) {
                case "survival" -> {
                    player.setGameMode(GameMode.SURVIVAL);
                    player.sendMessage(Component.text("Gamemode set to Survival", NamedTextColor.GREEN));
                    Log.info(player.getUsername() + " changed gamemode to SURVIVAL");
                    event.setCancelled(true);
                }
                case "creative" -> {
                    player.setGameMode(GameMode.CREATIVE);
                    player.sendMessage(Component.text("Gamemode set to Creative", NamedTextColor.GREEN));
                    Log.info(player.getUsername() + " changed gamemode to CREATIVE");
                    event.setCancelled(true);
                }
                case "spectator" -> {
                    player.setGameMode(GameMode.SPECTATOR);
                    player.sendMessage(Component.text("Gamemode set to Spectator", NamedTextColor.GREEN));
                    Log.info(player.getUsername() + " changed gamemode to SPECTATOR");
                    event.setCancelled(true);
                }
                case "nether" -> {
                    if (player.getInstance() == NetherManager.getOverworldInstance()) {
                        player.setInstance(NetherManager.getNetherInstance(), player.getPosition()).thenRun(() -> {
                            player.sendMessage(Component.text("Teleported to Nether!", NamedTextColor.RED));
                        });
                    }
                    event.setCancelled(true);
                }
                case "overworld" -> {
                    if (player.getInstance() == NetherManager.getNetherInstance()) {
                        player.setInstance(NetherManager.getOverworldInstance(), player.getPosition()).thenRun(() -> {
                            player.sendMessage(Component.text("Teleported to Overworld!", NamedTextColor.GREEN));
                        });
                    }
                    event.setCancelled(true);
                }
            }
        });
    }
}