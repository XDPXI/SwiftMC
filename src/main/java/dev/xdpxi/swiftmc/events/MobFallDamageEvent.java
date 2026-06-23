package dev.xdpxi.swiftmc.events;

import dev.xdpxi.swiftmc.mobs.Mobs;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.entity.EntityDespawnEvent;
import net.minestom.server.event.entity.EntityTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MobFallDamageEvent {

    private static final Map<UUID, Double> fallDistance = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> prevY = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> wasOnGround = new ConcurrentHashMap<>();

    public static void addListener(GlobalEventHandler globalEventHandler) {
        globalEventHandler.addListener(EntityTickEvent.class, event -> {
            if (!(event.getEntity() instanceof EntityCreature mob)) return;
            if (Mobs.isFlyingMob(mob.getEntityType())) return;

            UUID id = mob.getUuid();
            boolean onGround = mob.isOnGround();
            double currentY = mob.getPosition().y();
            Double lastY = prevY.get(id);
            boolean prevGrounded = wasOnGround.getOrDefault(id, true);

            if (lastY != null && !onGround && currentY < lastY) {
                fallDistance.merge(id, lastY - currentY, Double::sum);
            }

            if (onGround && !prevGrounded) {
                double dist = fallDistance.getOrDefault(id, 0.0);
                float damage = (float) Math.max(0.0, dist - 3.0);
                if (damage > 0) {
                    mob.damage(DamageType.FALL, damage);
                }
                fallDistance.remove(id);
            }

            if (onGround) {
                fallDistance.remove(id);
            }

            prevY.put(id, currentY);
            wasOnGround.put(id, onGround);
        });

        globalEventHandler.addListener(EntityDespawnEvent.class, event -> {
            if (!(event.getEntity() instanceof EntityCreature)) return;
            UUID id = event.getEntity().getUuid();
            fallDistance.remove(id);
            prevY.remove(id);
            wasOnGround.remove(id);
        });
    }
}
