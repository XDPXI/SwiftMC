package dev.xdpxi.swiftmc.events;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.instance.block.Block;

public class EntityDamageEvent {
    public static void addListener(GlobalEventHandler globalEventHandler) {
        globalEventHandler.addListener(net.minestom.server.event.entity.EntityDamageEvent.class, event -> {
            if (!(event.getEntity() instanceof Player player)) return;

            if (event.getDamage().getType() == DamageType.FALL) {
                Pos pos = player.getPosition();

                // Cancel fall damage if in water
                if (isWater(player.getInstance().getBlock(pos.blockX(), pos.blockY(), pos.blockZ()))) {
                    event.setCancelled(true);
                }
            }
        });
    }

    private static boolean isWater(Block block) {
        return block.compare(Block.WATER);
    }
}
