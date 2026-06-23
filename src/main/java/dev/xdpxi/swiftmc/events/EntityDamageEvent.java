package dev.xdpxi.swiftmc.events;

import dev.xdpxi.swiftmc.mobs.Mobs;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.instance.block.Block;
import net.minestom.server.timer.TaskSchedule;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EntityDamageEvent {

    private static final Set<UUID> panicBoostedMobs = ConcurrentHashMap.newKeySet();

    public static void addListener(GlobalEventHandler globalEventHandler) {
        globalEventHandler.addListener(
                net.minestom.server.event.entity.EntityDamageEvent.class,
                event -> {
                    if (event.getEntity() instanceof Player player) {
                        if (event.getDamage().getType() == DamageType.FALL) {
                            Pos pos = player.getPosition();

                            // Cancel fall damage if in water
                            if (
                                    isWater(
                                            player
                                                    .getInstance()
                                                    .getBlock(
                                                            pos.blockX(),
                                                            pos.blockY(),
                                                            pos.blockZ()
                                                    )
                                    )
                            ) {
                                event.setCancelled(true);
                            }
                        }
                        return;
                    }

                    if (!(event.getEntity() instanceof EntityCreature mob)) return;

                    // Flying mobs no fall
                    if (event.getDamage().getType() == DamageType.FALL && Mobs.isFlyingMob(mob.getEntityType())) {
                        event.setCancelled(true);
                        return;
                    }

                    // Passive mobs panic
                    if (!Mobs.isPassiveMob(mob.getEntityType())) return;
                    if (panicBoostedMobs.contains(mob.getUuid())) return;

                    var speedAttr = mob.getAttribute(Attribute.MOVEMENT_SPEED);
                    double baseSpeed = speedAttr.getBaseValue();
                    speedAttr.setBaseValue(baseSpeed * 2.0);
                    panicBoostedMobs.add(mob.getUuid());

                    MinecraftServer.getSchedulerManager().buildTask(() -> {
                        if (!mob.isRemoved()) {
                            speedAttr.setBaseValue(baseSpeed);
                        }
                        panicBoostedMobs.remove(mob.getUuid());
                    }).delay(TaskSchedule.seconds(3)).schedule();
                }
        );
    }

    private static boolean isWater(Block block) {
        return block.compare(Block.WATER);
    }
}
