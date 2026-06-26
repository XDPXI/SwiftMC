package dev.xdpxi.swiftmc.commands;

import dev.xdpxi.swiftmc.utils.Log;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentEnum;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;

public class Gamemode extends Command {

    public Gamemode() {
        super("gamemode", "gm");

        var modeArg = ArgumentType.Enum("mode", GameModeArg.class)
                .setFormat(ArgumentEnum.Format.LOWER_CASED);

        setDefaultExecutor((sender, context) ->
                sender.sendMessage(Component.text("Usage: /gamemode <adventure|survival|creative|spectator>", NamedTextColor.RED))
        );

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
                return;
            }

            GameModeArg arg = context.get(modeArg);
            GameMode gameMode = switch (arg) {
                case ADVENTURE -> GameMode.ADVENTURE;
                case SURVIVAL -> GameMode.SURVIVAL;
                case CREATIVE -> GameMode.CREATIVE;
                case SPECTATOR -> GameMode.SPECTATOR;
            };

            player.setGameMode(gameMode);
            String name = arg.name().charAt(0) + arg.name().substring(1).toLowerCase();
            player.sendMessage(Component.text("Gamemode set to " + name, NamedTextColor.GREEN));
            Log.info(player.getUsername() + " changed gamemode to " + name);
        }, modeArg);
    }

    public enum GameModeArg {
        ADVENTURE, SURVIVAL, CREATIVE, SPECTATOR
    }
}
