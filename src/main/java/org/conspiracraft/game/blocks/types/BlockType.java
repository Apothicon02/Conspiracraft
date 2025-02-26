package org.conspiracraft.game.blocks.types;

public class BlockType {
    public boolean isSolid;
    public boolean blocksLight;
    public boolean isCollidable;
    public boolean isFluidReplaceable;
    public boolean isFluid;

    public BlockType(boolean solid, boolean canBlockLight, boolean collidable, boolean fluidReplacable, boolean fluid) {
        isSolid = solid;
        blocksLight = canBlockLight;
        isCollidable = collidable;
        isFluidReplaceable = fluidReplacable;
        isFluid = fluid;
    }public BlockType(boolean solid, boolean canBlockLight, boolean collidable, boolean fluidReplacable) {
        isSolid = solid;
        blocksLight = canBlockLight;
        isCollidable = collidable;
        isFluidReplaceable = fluidReplacable;
        isFluid = false;
    }
    public BlockType(boolean solid, boolean canBlockLight, boolean collidable) {
        isSolid = solid;
        blocksLight = canBlockLight;
        isCollidable = collidable;
        isFluidReplaceable = false;
        isFluid = false;
    }
    public BlockType(boolean solid, boolean canBlockLight) {
        isSolid = solid;
        blocksLight = canBlockLight;
        isCollidable = true;
        isFluidReplaceable = false;
        isFluid = false;
    }
    public BlockType(boolean solid) {
        isSolid = solid;
        blocksLight = true;
        isCollidable = true;
        isFluidReplaceable = false;
        isFluid = false;
    }
    public BlockType() {
        isSolid = true;
        blocksLight = true;
        isCollidable = true;
        isFluidReplaceable = false;
        isFluid = false;
    }
}
