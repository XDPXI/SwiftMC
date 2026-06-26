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

public class Ban extends Command {

    public Ban() {
        super("ban");

        var nameArg = ArgumentType.Word("player")
                .setSuggestionCallback((sender, context, suggestion) -> {
                    for (Player online : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                        suggestion.addEntry(new SuggestionEntry(online.getUsername()));
                    }
                });
        var reasonArg = ArgumentType.StringArray("reason");

        setCondition((sender, cmd) ->
                sender instanceof ConsoleSender || (sender instanceof Player p && p.getPermissionLevel() >= 4)
        );

        setDefaultExecutor((sender, context) ->
                sender.sendMessage(Component.text("Usage: /ban <player> [reason]", NamedTextColor.RED))
        );

        // /ban <player>
        addSyntax((sender, context) -> {
            String target = context.get(nameArg);
            executeBan(sender, target, null);
        }, nameArg);

        // /ban <player> <reason>
        addSyntax((sender, context) -> {
            String target = context.get(nameArg);
            String reason = String.join(" ", context.get(reasonArg));
            executeBan(sender, target, reason);
        }, nameArg, reasonArg);
    }

    private static void executeBan(net.minestom.server.command.CommandSender sender, String target, String reason) {
        PlayerDataManager.setBan(target, true, reason).thenAccept(found -> {
            if (found) {
                sender.sendMessage(Component.text("Banned " + target + ".", NamedTextColor.GREEN));
                Log.info((sender instanceof Player p ? p.getUsername() : "Console") + " banned " + target
                        + (reason != null && !reason.isBlank() ? ": " + reason : ""));
            } else {
                sender.sendMessage(Component.text("Player '" + target + "' has never joined the server.", NamedTextColor.RED));
            }
        });
    }
}
