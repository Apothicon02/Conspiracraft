package org.conspiracraft.game.blocks.types;

import org.conspiracraft.game.audio.BlockSFX;
import org.conspiracraft.game.audio.SFX;
import org.conspiracraft.game.audio.Sounds;

public class BlockProperties implements Cloneable {
    public int ttb = 500;
    public BlockProperties ttb(int ttb) {
        this.ttb = ttb;
        return this;
    }
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
    public boolean isGas = false;
    public BlockProperties isGas(boolean isGas) {
        this.isGas = isGas;
        return this;
    }
    protected boolean obstructsHeightmap = true;
    public BlockProperties obstructsHeightmap(boolean obstructsHeightmap) {
        this.obstructsHeightmap = obstructsHeightmap;
        return this;
    }
    protected boolean needsSupport = false;
    public BlockProperties needsSupport(boolean needsSupport) {
        this.needsSupport = needsSupport;
        return this;
    }
    public BlockSFX blockSFX = new BlockSFX(new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 1f, 0.5f, new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 1f, 0.5f);
    public BlockProperties blockSFX(SFX[] placeIds, float placeGain, float placePitch, SFX[] stepIds, float stepGain, float stepPitch) {
        this.blockSFX = new BlockSFX(placeIds, placeGain, placePitch, stepIds, stepGain, stepPitch);
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
