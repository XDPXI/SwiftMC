package dev.xdpxi.swiftmc.events;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public class EntityDeathEvent {
    public static void addListener(GlobalEventHandler globalEventHandler) {
        globalEventHandler.addListener(net.minestom.server.event.entity.EntityDeathEvent.class, event -> {
            if (!(event.getEntity() instanceof EntityCreature creature)) return;

            Pos deathPos = creature.getPosition();
            EntityType type = creature.getEntityType();

            // Drop items based on mob type
            switch (creature.getEntityType().name()) {
                case "minecraft:chicken" -> {
                    dropItem(creature, Material.FEATHER, 0, 2, deathPos);
                    dropItem(creature, Material.CHICKEN, 1, 1, deathPos);
                }
                case "minecraft:cow" -> {
                    dropItem(creature, Material.LEATHER, 0, 2, deathPos);
                    dropItem(creature, Material.BEEF, 1, 3, deathPos);
                }
                case "minecraft:pig" -> {
                    dropItem(creature, Material.PORKCHOP, 1, 3, deathPos);
                }
            }
        });
    }

    private static void dropItem(EntityCreature creature, Material material, int min, int max, Pos pos) {
        int amount = ThreadLocalRandom.current().nextInt(min, max + 1);
        if (amount <= 0) return;

        ItemStack itemStack = ItemStack.of(material, amount);

        // Random velocity for item spread
        Vec velocity = new Vec(
                (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.3,
                0.2,
                (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.3
        );

        ItemEntity itemEntity = new ItemEntity(itemStack);
        itemEntity.setInstance(creature.getInstance(), pos);
        itemEntity.setVelocity(velocity);
        itemEntity.setPickupDelay(Duration.ofMillis(500));
    }
}