package org.terraflat.game.blocks;

import org.terraflat.game.space.Voxel;

public class Block {
    private Voxel defaultVoxel = new Voxel((short) -32768);

    public Block() {
    }

    public void setDefaultVoxel(Voxel defaultVoxel) {
        this.defaultVoxel = defaultVoxel;
    }
    public Voxel getDefaultVoxel() {
        return defaultVoxel;
    }
}