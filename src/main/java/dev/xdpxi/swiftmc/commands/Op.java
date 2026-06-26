package dev.xdpxi.swiftmc.commands;

import dev.xdpxi.swiftmc.player.PlayerDataManager;
import dev.xdpxi.swiftmc.utils.Log;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;

public class Op extends Command {

    public Op() {
        super("op");

        var nameArg = ArgumentType.Word("player")
                .setSuggestionCallback((sender, context, suggestion) -> {
                    for (Player online : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                        if (online.getPermissionLevel() < 4) {
                            suggestion.addEntry(new SuggestionEntry(online.getUsername()));
                        }
                    }
                });

        setCondition((sender, cmd) ->
                sender instanceof ConsoleSender || (sender instanceof Player p && p.getPermissionLevel() >= 4)
        );

        setDefaultExecutor((sender, context) ->
                sender.sendMessage(Component.text("Usage: /op <player>", NamedTextColor.RED))
        );

        addSyntax((sender, context) -> {
            String target = context.get(nameArg);
            PlayerDataManager.setOp(target, true).thenAccept(found -> {
                if (found) {
                    sender.sendMessage(Component.text("Opped " + target + ".", NamedTextColor.GREEN));
                    Log.info((sender instanceof Player p ? p.getUsername() : "Console") + " opped " + target);
                } else {
                    sender.sendMessage(Component.text("Player '" + target + "' has never joined the server.", NamedTextColor.RED));
                }
            });
        }, nameArg);
    }
}
