package org.conspiracraft.blocks.entities;

import org.joml.Vector2i;
import org.joml.Vector3i;
import org.conspiracraft.world.World;

public class BlockEntity {
    public BlockEntity() {}

    public BlockEntity create() {
        return new BlockEntity();
    }

    public void remove(int xyz) {
        World.blockEntities.remove(xyz);
    }

    public void tick(Vector2i block, Vector3i pos) {}
}
