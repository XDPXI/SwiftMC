package dev.xdpxi.swiftmc.player;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.inventory.PlayerInventory;

public class PlayerData {
    public double x, y, z;
    public float yaw, pitch;

    public PlayerData() {
    }

    public PlayerData(Pos pos, PlayerInventory inv) {
        this.x = pos.x();
        this.y = pos.y();
        this.z = pos.z();
        this.yaw = pos.yaw();
        this.pitch = pos.pitch();
    }

    public Pos toPos() {
        return new Pos(x, y, z, yaw, pitch);
    }
}
