package org.conspiracraft.game.blocks.types;

public class BlockType {
    public boolean isTransparent;
    public boolean isCollidable;
    public boolean isFluidReplaceable;
    public boolean isFluid;

    public BlockType(boolean transparent, boolean collidable, boolean fluidReplacable, boolean fluid) {
        isTransparent = transparent;
        isCollidable = collidable;
        isFluidReplaceable = fluidReplacable;
        isFluid = fluid;
    }public BlockType(boolean transparent, boolean collidable, boolean fluidReplacable) {
        isTransparent = transparent;
        isCollidable = collidable;
        isFluidReplaceable = fluidReplacable;
        isFluid = false;
    }
    public BlockType(boolean transparent, boolean collidable) {
        isTransparent = transparent;
        isCollidable = collidable;
        isFluidReplaceable = false;
        isFluid = false;
    }
    public BlockType(boolean transparent) {
        isTransparent = transparent;
        isCollidable = true;
        isFluidReplaceable = false;
        isFluid = false;
    }
    public BlockType() {
        isTransparent = false;
        isCollidable = true;
        isFluidReplaceable = false;
        isFluid = false;
    }
}
