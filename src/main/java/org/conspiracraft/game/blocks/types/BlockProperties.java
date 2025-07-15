package org.conspiracraft.game.blocks.types;

import org.conspiracraft.game.audio.BlockSFX;

public class BlockProperties implements Cloneable {
    public boolean isSolid = true;
    public BlockProperties isSolid(boolean isSolid) {
        this.isSolid = isSolid;
        return this;
    }
    public boolean blocksLight = true;
    public BlockProperties blocksLight(boolean blocksLight) {
        this.blocksLight = blocksLight;
        return this;
    }
    public boolean isCollidable = true;
    public BlockProperties isCollidable(boolean isCollidable) {
        this.isCollidable = isCollidable;
        return this;
    }
    public boolean isFluidReplaceable = false;
    public BlockProperties isFluidReplaceable(boolean isFluidReplaceable) {
        this.isFluidReplaceable = isFluidReplaceable;
        return this;
    }
    public boolean isFluid = false;
    public BlockProperties isFluid(boolean isFluid) {
        this.isFluid = isFluid;
        return this;
    }
    public boolean obstructsHeightmap = true;
    public BlockProperties obstructsHeightmap(boolean obstructsHeightmap) {
        this.obstructsHeightmap = obstructsHeightmap;
        return this;
    }
    public boolean needsSupport = false;
    public BlockProperties needsSupport(boolean needsSupport) {
        this.needsSupport = needsSupport;
        return this;
    }
    public BlockSFX blockSFX = new BlockSFX(new int[]{2, 3}, 1f, 1f);
    public BlockProperties blockSFX(int[] placeIds, float gain, float pitch) {
        this.blockSFX = new BlockSFX(placeIds, gain, pitch);
        return this;
    }

    public BlockProperties copy() {
        try {
            return (BlockProperties) this.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public BlockProperties() {}
}
