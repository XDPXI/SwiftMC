package dev.xdpxi.swiftmc.commands;

import dev.xdpxi.swiftmc.utils.Log;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;

public class Kick extends Command {

    public Kick() {
        super("kick");

        var nameArg = ArgumentType.Word("player")
                .setSuggestionCallback((sender, context, suggestion) -> {
                    for (Player online : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                        suggestion.addEntry(new SuggestionEntry(online.getUsername()));
                    }
                });
        var reasonArg = ArgumentType.StringArray("reason");

        setCondition((sender, cmd) ->
                sender instanceof ConsoleSender || (sender instanceof Player p && p.getPermissionLevel() >= 2)
        );

        setDefaultExecutor((sender, context) ->
                sender.sendMessage(Component.text("Usage: /kick <player> [reason]", NamedTextColor.RED))
        );

        // /kick <player>
        addSyntax((sender, context) -> {
            String target = context.get(nameArg);
            kickPlayer(sender, target, null);
        }, nameArg);

        // /kick <player> <reason>
        addSyntax((sender, context) -> {
            String target = context.get(nameArg);
            String reason = String.join(" ", context.get(reasonArg));
            kickPlayer(sender, target, reason);
        }, nameArg, reasonArg);
    }

    private static void kickPlayer(net.minestom.server.command.CommandSender sender, String target, String reason) {
        for (Player online : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (online.getUsername().equalsIgnoreCase(target)) {
                String msg = reason != null && !reason.isBlank()
                        ? "Kicked by an operator: " + reason
                        : "Kicked by an operator.";
                online.kick(Component.text(msg, NamedTextColor.RED));
                sender.sendMessage(Component.text("Kicked " + online.getUsername() + ".", NamedTextColor.GREEN));
                Log.info((sender instanceof Player p ? p.getUsername() : "Console") + " kicked " + online.getUsername()
                        + (reason != null && !reason.isBlank() ? ": " + reason : ""));
                return;
            }
        }
        sender.sendMessage(Component.text("Player '" + target + "' is not online.", NamedTextColor.RED));
    }
}
