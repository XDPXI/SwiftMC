package dev.xdpxi.swiftmc.api.mobs;

import dev.xdpxi.swiftmc.utils.Log;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.ai.goal.RandomStrollGoal;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.timer.TaskSchedule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public record MobSpawner(Instance instance) {
    private static final int SPAWN_RADIUS_CHUNKS = 2; // spawn within 2 chunks around player
    private static final int MAX_MOBS_PER_TYPE = 20;
    private static final int SPAWN_ATTEMPTS_PER_TICK = 3;
    private static final int SPAWN_INTERVAL_TICKS = 100; // every 5 seconds
    private static final int GROUP_MIN = 3;
    private static final int GROUP_MAX = 5;
    private static final List<EntityCreature> spawnedMobs = new ArrayList<>();

    public void start() {
        MinecraftServer.getSchedulerManager().submitTask(() -> {
            trySpawnMobs();
            return TaskSchedule.tick(SPAWN_INTERVAL_TICKS);
        });
        Log.info("Mob spawner started.");
    }

    private void trySpawnMobs() {
        spawnedMobs.removeIf(mob -> mob.isRemoved() || !mob.isActive());

        long chickenCount = spawnedMobs.stream()
                .filter(mob -> mob.getEntityType() == EntityType.CHICKEN).count();
        long cowCount = spawnedMobs.stream()
                .filter(mob -> mob.getEntityType() == EntityType.COW).count();
        long pigCount = spawnedMobs.stream()
                .filter(mob -> mob.getEntityType() == EntityType.PIG).count();

        for (int i = 0; i < SPAWN_ATTEMPTS_PER_TICK; i++) {
            EntityType mobType = getRandomMobType();
            long currentCount = switch (mobType.name()) {
                case "minecraft:chicken" -> chickenCount;
                case "minecraft:cow" -> cowCount;
                case "minecraft:pig" -> pigCount;
                default -> 0;
            };

            if (currentCount >= MAX_MOBS_PER_TYPE) continue;

            // Pick a random player to spawn near
            var players = instance.getPlayers();
            if (players.isEmpty()) continue;
            Player player = players.stream()
                    .skip(ThreadLocalRandom.current().nextInt(players.size()))
                    .findFirst()
                    .orElse(null);
            if (player == null) continue;

            // Find a random spawn location near the player
            Pos groupCenter = getRandomSpawnLocationNearPlayer(player);
            if (groupCenter == null) continue;

            // Spawn a group of mobs around that point
            int groupSize = ThreadLocalRandom.current().nextInt(GROUP_MIN, GROUP_MAX + 1);
            for (int g = 0; g < groupSize; g++) {
                double offsetX = ThreadLocalRandom.current().nextDouble(-3, 3);
                double offsetZ = ThreadLocalRandom.current().nextDouble(-3, 3);
                Pos spawnPos = groupCenter.add(offsetX, 0, offsetZ);
                if (isValidSpawnLocation(spawnPos)) {
                    spawnMob(mobType, spawnPos);
                }
            }
        }
    }

    private EntityType getRandomMobType() {
        int rand = ThreadLocalRandom.current().nextInt(3);
        return switch (rand) {
            case 0 -> EntityType.CHICKEN;
            case 1 -> EntityType.COW;
            default -> EntityType.PIG;
        };
    }

    private Pos getRandomSpawnLocationNearPlayer(Player player) {
        assert player.getChunk() != null;
        int playerChunkX = player.getChunk().getChunkX();
        int playerChunkZ = player.getChunk().getChunkZ();

        // Pick a random chunk within radius
        int chunkX = playerChunkX + ThreadLocalRandom.current().nextInt(-SPAWN_RADIUS_CHUNKS, SPAWN_RADIUS_CHUNKS + 1);
        int chunkZ = playerChunkZ + ThreadLocalRandom.current().nextInt(-SPAWN_RADIUS_CHUNKS, SPAWN_RADIUS_CHUNKS + 1);

        if (!instance.isChunkLoaded(chunkX, chunkZ)) return null;

        int x = chunkX * 16 + ThreadLocalRandom.current().nextInt(16);
        int z = chunkZ * 16 + ThreadLocalRandom.current().nextInt(16);

        // Search for ground level
        for (int y = 100; y > 40; y--) {
            Block block = instance.getBlock(x, y, z);
            Block above = instance.getBlock(x, y + 1, z);
            Block above2 = instance.getBlock(x, y + 2, z);
            if (!block.isAir() && above.isAir() && above2.isAir()) {
                return new Pos(x + 0.5, y + 1, z + 0.5);
            }
        }

        return null;
    }

    private boolean isValidSpawnLocation(Pos pos) {
        Block below = instance.getBlock(pos.sub(0, 1, 0));
        if (below.isAir() || below.compare(Block.WATER) || below.compare(Block.SAND)) {
            return false;
        }
        Block atPos = instance.getBlock(pos);
        if (atPos.compare(Block.WATER)) return false;
        return pos.y() >= 60;
    }

    private void spawnMob(EntityType type, Pos pos) {
        EntityCreature mob = new EntityCreature(type);
        mob.addAIGroup(
                List.of(new RandomStrollGoal(mob, 20)),
                List.of()
        );
        mob.getAttribute(Attribute.MOVEMENT_SPEED)
                .setBaseValue(0.1f);
        mob.setInstance(instance, pos);
        spawnedMobs.add(mob);
        Log.debug("Spawned " + type.name() + " at " + pos.blockX() + ", " + pos.blockY() + ", " + pos.blockZ());
    }

    static List<EntityCreature> getSpawnedMobs() {
        return new ArrayList<>(spawnedMobs);
    }
}
