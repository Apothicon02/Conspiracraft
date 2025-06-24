package org.conspiracraft.game.blocks.types;

import org.joml.Vector2i;

public class BlockType {
    public boolean isSolid = true;
    public boolean blocksLight = true;
    public boolean isCollidable = true;
    public boolean isFluidReplaceable = false;
    public boolean isFluid = false;
    public boolean obstructsHeightmap = true;
    public boolean needsSupport = false;

    public boolean obstructingHeightmap(Vector2i block) {
        return obstructsHeightmap;
    }

    public BlockType(boolean solid, boolean canBlockLight, boolean collidable, boolean fluidReplacable, boolean fluid, boolean obstructHeightmap, boolean needSupport) {
        isSolid = solid;
        blocksLight = canBlockLight;
        isCollidable = collidable;
        isFluidReplaceable = fluidReplacable;
        isFluid = fluid;
        obstructsHeightmap = obstructHeightmap;
        needsSupport = needSupport;
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
    }
    public BlockType(boolean solid, boolean canBlockLight, boolean collidable, boolean fluidReplacable) {
        isSolid = solid;
        blocksLight = canBlockLight;
        isCollidable = collidable;
        isFluidReplaceable = fluidReplacable;
    }
    public BlockType(boolean solid, boolean canBlockLight, boolean collidable) {
        isSolid = solid;
        blocksLight = canBlockLight;
        isCollidable = collidable;
    }
    public BlockType(boolean solid, boolean canBlockLight) {
        isSolid = solid;
        blocksLight = canBlockLight;
    }
    public BlockType(boolean solid) {
        isSolid = solid;
    }
    public BlockType() {}
}
