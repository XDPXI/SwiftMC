package dev.xdpxi.swiftmc.commands;

import dev.xdpxi.swiftmc.player.PlayerDataManager;
import dev.xdpxi.swiftmc.utils.Log;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

public class Deop extends Command {

    public Deop() {
        super("deop");

        var nameArg = ArgumentType.Word("player");

        setCondition((sender, cmd) ->
                sender instanceof ConsoleSender || (sender instanceof Player p && p.getPermissionLevel() >= 4)
        );

        setDefaultExecutor((sender, context) ->
                sender.sendMessage(Component.text("Usage: /deop <player>", NamedTextColor.RED))
        );

        addSyntax((sender, context) -> {
            String target = context.get(nameArg);
            PlayerDataManager.setOp(target, false).thenAccept(found -> {
                if (found) {
                    sender.sendMessage(Component.text("Deopped " + target + ".", NamedTextColor.GREEN));
                    Log.info((sender instanceof Player p ? p.getUsername() : "Console") + " deopped " + target);
                } else {
                    sender.sendMessage(Component.text("Player '" + target + "' has never joined the server.", NamedTextColor.RED));
                }
            });
        }, nameArg);
    }
}
