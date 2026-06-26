package dev.xdpxi.swiftmc.commands;

import dev.xdpxi.swiftmc.utils.Log;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.relative.ArgumentRelativeVec3;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;

public class Teleport extends Command {

    public Teleport() {
        super("tp", "teleport");

        var posArg = new ArgumentRelativeVec3("position");

        setDefaultExecutor((sender, context) ->
                sender.sendMessage(Component.text("Usage: /tp <x> <y> <z>", NamedTextColor.RED))
        );

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
                return;
            }

            Vec target = context.get(posArg).from(player);
            Pos current = player.getPosition();
            Pos dest = new Pos(target.x(), target.y(), target.z(), current.yaw(), current.pitch());

            player.teleport(dest).thenRun(() -> {
                player.sendMessage(Component.text(
                        String.format("Teleported to %.1f, %.1f, %.1f", dest.x(), dest.y(), dest.z()),
                        NamedTextColor.GREEN
                ));
                Log.info(player.getUsername() + " teleported to " + dest.x() + ", " + dest.y() + ", " + dest.z());
            });
        }, posArg);
    }
}
