package dev.xdpxi.swiftmc.commands;

import dev.xdpxi.swiftmc.Main;
import dev.xdpxi.swiftmc.utils.Log;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class Stop extends Command {

    public Stop() {
        super("stop");
        setDefaultExecutor((sender, context) -> {
            Log.info((sender instanceof Player p ? p.getUsername() : "Console") + " issued stop command.");
            sender.sendMessage(Component.text("Stopping server...", NamedTextColor.RED));
            Main.shutdown();
            System.exit(0);
        });
    }
}
