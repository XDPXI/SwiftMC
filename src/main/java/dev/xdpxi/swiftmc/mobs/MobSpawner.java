package dev.xdpxi.swiftmc.mobs;

import dev.xdpxi.swiftmc.Main;
import dev.xdpxi.swiftmc.utils.Log;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeInstance;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public record MobSpawner(Instance instance) {
    private static final int SPAWN_RADIUS_CHUNKS = 4; // spawn within 4 chunks around player
    private static final int MAX_MOBS_PER_TYPE = 30;
    private static final int SPAWN_ATTEMPTS_PER_TICK = 5;
    private static final int SPAWN_INTERVAL_TICKS = 100; // every 5 seconds
    private static final int GROUP_MIN = 2;
    private static final int GROUP_MAX = 4;
    private static final int MIN_SPAWN_DISTANCE = 24; // blocks away from player
    private static final int MAX_SPAWN_DISTANCE = 64; // blocks away from player
    private static final int DESPAWN_DELAY_SECONDS = 60 * 3; // naturally spawned mobs despawn after 3 minutes
    private static final List<EntityCreature> spawnedMobs = Collections.synchronizedList(new ArrayList<>());

    @Contract(value = " -> new", pure = true)
    static @NonNull List<EntityCreature> getSpawnedMobs() {
        synchronized (spawnedMobs) {
            return new ArrayList<>(spawnedMobs);
        }
    }

    public static void spawnMob(EntityType type, Pos pos) {
        spawnMob(type, pos, false);
    }

    private static void spawnMob(EntityType type, Pos pos, boolean natural) {
        EntityCreature mob = new EntityCreature(type);

        // Add AI
        mob.addAIGroup(List.of(new RandomStrollGoal(mob, 20)), List.of());

        // Set attributes
        AttributeInstance speedAttr = mob.getAttribute(Attribute.MOVEMENT_SPEED);
        speedAttr.setBaseValue((double) Mobs.getMobSpeed(type) / 100);

        AttributeInstance healthAttr = mob.getAttribute(Attribute.MAX_HEALTH);
        healthAttr.setBaseValue(Mobs.getMobHealth(type));
        mob.setHealth((float) healthAttr.getBaseValue());

        // Spawn in instance
        mob
                .setInstance(Main.getInstanceContainer(), pos)
                .thenRun(() -> {
                    spawnedMobs.add(mob);
                    Log.debug(
                            "Spawned " +
                                    type.name() +
                                    " at " +
                                    pos.blockX() +
                                    ", " +
                                    pos.blockY() +
                                    ", " +
                                    pos.blockZ()
                    );

                    if (natural) {
                        MinecraftServer.getSchedulerManager().buildTask(() -> {
                            if (!mob.isRemoved()) {
                                mob.remove();
                                spawnedMobs.remove(mob);
                                Log.debug(
                                        "Despawned " +
                                                type.name() +
                                                " after " +
                                                DESPAWN_DELAY_SECONDS +
                                                " seconds"
                                );
                            }
                        }).delay(TaskSchedule.seconds(DESPAWN_DELAY_SECONDS)).schedule();
                    }
                });
    }

    public void start() {
        MinecraftServer.getSchedulerManager().submitTask(() -> {
            trySpawnMobs();
            return TaskSchedule.tick(SPAWN_INTERVAL_TICKS);
        });
        Log.info("Mob spawner started.");
    }

    private void trySpawnMobs() {
        long chickenCount;
        long cowCount;
        long pigCount;
        synchronized (spawnedMobs) {
            // Clean up removed/inactive mobs
            spawnedMobs.removeIf(mob -> mob.isRemoved() || !mob.isActive());

            // Count mobs by type
            chickenCount = spawnedMobs
                    .stream()
                    .filter(mob -> mob.getEntityType() == EntityType.CHICKEN)
                    .count();
            cowCount = spawnedMobs
                    .stream()
                    .filter(mob -> mob.getEntityType() == EntityType.COW)
                    .count();
            pigCount = spawnedMobs
                    .stream()
                    .filter(mob -> mob.getEntityType() == EntityType.PIG)
                    .count();
        }

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

            Player player = players
                    .stream()
                    .skip(ThreadLocalRandom.current().nextInt(players.size()))
                    .findFirst()
                    .orElse(null);
            if (player == null) continue;

            // Find a random spawn location near the player
            Pos groupCenter = getRandomSpawnLocationNearPlayer(player);
            if (groupCenter == null) continue;

            // Check distance from player
            double distance = player.getPosition().distance(groupCenter);
            if (
                    distance < MIN_SPAWN_DISTANCE || distance > MAX_SPAWN_DISTANCE
            ) continue;

            // Spawn a group of mobs around that point
            int groupSize = ThreadLocalRandom.current().nextInt(
                    GROUP_MIN,
                    GROUP_MAX + 1
            );
            for (int g = 0; g < groupSize; g++) {
                double offsetX = ThreadLocalRandom.current().nextDouble(-3, 3);
                double offsetZ = ThreadLocalRandom.current().nextDouble(-3, 3);
                Pos spawnPos = groupCenter.add(offsetX, 0, offsetZ);
                if (isValidSpawnLocation(spawnPos)) {
                    spawnMob(mobType, spawnPos, true);
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

    private @Nullable Pos getRandomSpawnLocationNearPlayer(@NonNull Player player) {
        if (player.getChunk() == null) return null;

        int playerChunkX = player.getChunk().getChunkX();
        int playerChunkZ = player.getChunk().getChunkZ();

        // Pick a random chunk within radius
        int chunkX =
                playerChunkX +
                        ThreadLocalRandom.current().nextInt(
                                -SPAWN_RADIUS_CHUNKS,
                                SPAWN_RADIUS_CHUNKS + 1
                        );
        int chunkZ =
                playerChunkZ +
                        ThreadLocalRandom.current().nextInt(
                                -SPAWN_RADIUS_CHUNKS,
                                SPAWN_RADIUS_CHUNKS + 1
                        );

        if (!instance.isChunkLoaded(chunkX, chunkZ)) return null;

        int x = chunkX * 16 + ThreadLocalRandom.current().nextInt(16);
        int z = chunkZ * 16 + ThreadLocalRandom.current().nextInt(16);

        // Search for ground level
        for (int y = 120; y > 40; y--) {
            Block block = instance.getBlock(x, y, z);
            Block above = instance.getBlock(x, y + 1, z);
            Block above2 = instance.getBlock(x, y + 2, z);

            if (
                    !block.isAir() &&
                            !block.compare(Block.WATER) &&
                            above.isAir() &&
                            above2.isAir()
            ) {
                return new Pos(x + 0.5, y + 1, z + 0.5);
            }
        }

        return null;
    }

    private boolean isValidSpawnLocation(@NonNull Pos pos) {
        Block below = instance.getBlock(pos.sub(0, 1, 0));
        if (below.isAir() || below.compare(Block.WATER)) {
            return false;
        }

        Block atPos = instance.getBlock(pos);
        Block above = instance.getBlock(pos.add(0, 1, 0));

        if (!atPos.isAir() || !above.isAir()) return false;
        if (
                atPos.compare(Block.WATER) || above.compare(Block.WATER)
        ) return false;

        return pos.y() >= 50;
    }
}
