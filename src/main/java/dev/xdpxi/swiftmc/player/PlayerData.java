package dev.xdpxi.swiftmc.player;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;

public class PlayerData {
    public double x, y, z;
    public float yaw, pitch;
    public GameMode gameMode;

    public PlayerData() {
    }

    public PlayerData(Player player) {
        Pos pos = player.getPosition();
        this.x = pos.x();
        this.y = pos.y();
        this.z = pos.z();
        this.yaw = pos.yaw();
        this.pitch = pos.pitch();
        this.gameMode = player.getGameMode();
    }

    public Pos toPos() {
        return new Pos(x, y, z, yaw, pitch);
    }
}
