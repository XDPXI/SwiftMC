package dev.xdpxi.swiftmc.events;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.instance.InstanceContainer;

public class PickupItemEvent {
    public static void addListener(GlobalEventHandler globalEventHandler, InstanceContainer instanceContainer) {
        globalEventHandler.addListener(net.minestom.server.event.item.PickupItemEvent.class, event -> {
            Player player = (Player) event.getLivingEntity();
            ItemEntity itemEntity = event.getItemEntity();

            // Add the item to player's inventory
            boolean added = player.getInventory().addItemStack(itemEntity.getItemStack());

            if (added) {
                // Play pickup sound
                player.playSound(Sound.sound(Key.key("entity.item.pickup"), Sound.Source.PLAYER, 0.2f, 1.0f));
            } else {
                // Inventory full, cancel the pickup
                event.setCancelled(true);
            }
        });
    }
}
