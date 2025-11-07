package dev.xdpxi.swiftmc.player;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PlayerData {
    public double x, y, z;
    public float yaw, pitch;
    public GameMode gameMode;
    public List<ItemSerialization> inventory = new ArrayList<>();

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

        // Save inventory
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getItemStack(i);
            if (stack != null && !stack.isAir()) {
                inventory.add(new ItemSerialization(stack));
            } else {
                inventory.add(null);
            }
        }
    }

    public Pos toPos() {
        return new Pos(x, y, z, yaw, pitch);
    }

    public void applyInventory(Player player) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemSerialization itemSerialization = inventory.get(i);
            if (itemSerialization != null) {
                inv.setItemStack(i, itemSerialization.toItemStack());
            } else {
                inv.setItemStack(i, ItemStack.AIR);
            }
        }
    }
}
