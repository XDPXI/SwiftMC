package dev.xdpxi.swiftmc.commands;

import dev.xdpxi.swiftmc.utils.Log;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;

public class Creative extends Command {
    public Creative() {
        super("creative");

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
                return;
            }

            player.setGameMode(GameMode.CREATIVE);
            player.sendMessage(Component.text("Gamemode set to Creative", NamedTextColor.GREEN));
            Log.info(player.getUsername() + " changed gamemode to Creative");
        });
    }
}
