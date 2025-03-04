package org.conspiracraft.game.blocks.types;

import org.joml.Vector2i;

public class BlockType {
    public boolean isSolid;
    public boolean blocksLight;
    public boolean isCollidable;
    public boolean isFluidReplaceable;
    public boolean isFluid;
    boolean obstructsHeightmap;

    public boolean obstructingHeightmap(Vector2i block) {
        return obstructsHeightmap;
    }

    public BlockType(boolean solid, boolean canBlockLight, boolean collidable, boolean fluidReplacable, boolean fluid, boolean obstructHeightmap) {
        isSolid = solid;
        blocksLight = canBlockLight;
        isCollidable = collidable;
        isFluidReplaceable = fluidReplacable;
        isFluid = fluid;
        obstructsHeightmap = obstructHeightmap;
    }
    public BlockType(boolean solid, boolean canBlockLight, boolean collidable, boolean fluidReplacable, boolean fluid) {
        isSolid = solid;
        blocksLight = canBlockLight;
        isCollidable = collidable;
        isFluidReplaceable = fluidReplacable;
        isFluid = fluid;
        obstructsHeightmap = true;
    }
    public BlockType(boolean solid, boolean canBlockLight, boolean collidable, boolean fluidReplacable) {
        isSolid = solid;
        blocksLight = canBlockLight;
        isCollidable = collidable;
        isFluidReplaceable = fluidReplacable;
        isFluid = false;
        obstructsHeightmap = true;
    }
    public BlockType(boolean solid, boolean canBlockLight, boolean collidable) {
        isSolid = solid;
        blocksLight = canBlockLight;
        isCollidable = collidable;
        isFluidReplaceable = false;
        isFluid = false;
        obstructsHeightmap = true;
    }
    public BlockType(boolean solid, boolean canBlockLight) {
        isSolid = solid;
        blocksLight = canBlockLight;
        isCollidable = true;
        isFluidReplaceable = false;
        isFluid = false;
        obstructsHeightmap = true;
    }
    public BlockType(boolean solid) {
        isSolid = solid;
        blocksLight = true;
        isCollidable = true;
        isFluidReplaceable = false;
        isFluid = false;
        obstructsHeightmap = true;
    }
    public BlockType() {
        isSolid = true;
        blocksLight = true;
        isCollidable = true;
        isFluidReplaceable = false;
        isFluid = false;
        obstructsHeightmap = true;
    }
}
