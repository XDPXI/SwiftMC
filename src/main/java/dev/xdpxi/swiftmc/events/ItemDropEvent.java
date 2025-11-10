package dev.xdpxi.swiftmc.events;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;

import java.time.Duration;

public class ItemDropEvent {
    public static void addListener(GlobalEventHandler globalEventHandler) {
        globalEventHandler.addListener(net.minestom.server.event.item.ItemDropEvent.class, event -> {
            Player player = event.getPlayer();

            // Get player position and looking direction
            Pos playerPos = player.getPosition();
            Vec direction = playerPos.direction();

            // Spawn position
            Vec spawnPos = playerPos.asVec()
                    .add(0, player.getEyeHeight(), 0)
                    .add(direction.mul(0.3));

            // Throw velocity
            Vec velocity = direction.mul(4);

            // Create and spawn
            ItemEntity itemEntity = new ItemEntity(event.getItemStack());
            itemEntity.setInstance(player.getInstance(), Pos.fromPoint(spawnPos));
            itemEntity.setVelocity(velocity);
            itemEntity.setPickupDelay(Duration.ofMillis(500));
        });
    }
}