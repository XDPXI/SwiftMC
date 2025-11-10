package dev.xdpxi.swiftmc.mobs;

import net.minestom.server.entity.EntityCreature;

import java.util.List;

public class Mobs {
    public static List<EntityCreature> getSpawnedMobs() {
        return MobSpawner.getSpawnedMobs();
    }
}
