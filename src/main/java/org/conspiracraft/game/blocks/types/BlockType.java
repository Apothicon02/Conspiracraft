package org.conspiracraft.game.blocks.types;

import org.conspiracraft.game.audio.AudioController;
import org.conspiracraft.game.audio.BlockSFX;
import org.conspiracraft.game.audio.Source;
import org.conspiracraft.game.world.FluidHelper;
import org.conspiracraft.game.world.World;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;

import static org.conspiracraft.game.world.World.getBlock;
import static org.conspiracraft.game.world.World.setBlock;

public class BlockType {
    public boolean isSolid = true;
    public boolean blocksLight = true;
    public boolean isCollidable = true;
    public boolean isFluidReplaceable = false;
    public boolean isFluid = false;
    public boolean obstructsHeightmap = true;
    public boolean needsSupport = false;
    public BlockSFX blockSFX = new BlockSFX(new int[]{2, 3}, 1f, 1f);
    public BlockType blockSFX(int[] placeIds, float gain, float pitch) {
        blockSFX = new BlockSFX(placeIds, gain, pitch);
        return this;
    }

    public boolean obstructingHeightmap(Vector2i block) {
        return obstructsHeightmap;
    }

    public void tick(Vector3i pos) {
        FluidHelper.updateFluid(pos, getBlock(pos));
    }

    public void onPlace(Vector3i pos, boolean isSilent) {
        if (!isSilent) {
            blockSFX.placed(new Vector3f(pos.x, pos.y, pos.z));
        }

        Vector2i aboveBlock = getBlock(new Vector3i(pos.x, pos.y+1, pos.z));
        if (aboveBlock != null) {
            int aboveBlockId = aboveBlock.x();
            if (BlockTypes.blockTypeMap.get(aboveBlockId).needsSupport) {
                setBlock(pos.x, pos.y+1, pos.z, 0, 0, true, true, 1, false);
            }
        }
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
