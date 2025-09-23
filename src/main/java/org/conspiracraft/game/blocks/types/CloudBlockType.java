package org.conspiracraft.game.blocks.types;

import org.conspiracraft.Main;
import org.conspiracraft.game.ScheduledTicker;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4i;

import static org.conspiracraft.game.world.World.*;

public class CloudBlockType extends BlockType {

    public CloudBlockType(BlockProperties blockProperties) {
        super(blockProperties);
    }

    @Override
    public void tick(Vector4i pos) {
        if (inBounds(pos.x, pos.y, pos.z)) {
            if (pos.w == 1) {
                ScheduledTicker.scheduleTick(Main.currentTick+1200, pos.xyz(new Vector3i()), 1);
                setBlock(pos.x, pos.y - 1, pos.z, 1, 15, false, false, 1, false);
            }
            fluidTick(pos.xyz(new Vector3i()));
            updateSupport(new Vector3i(pos.x, pos.y, pos.z));
        }
    }

    @Override
    public void onPlace(Vector3i pos, Vector2i block, boolean isSilent) {
        ScheduledTicker.scheduleTick(Main.currentTick+200+(int)(Math.random()*1000), pos, 1);

        if (!isSilent) {
            blockProperties.blockSFX.placed(new Vector3f(pos.x, pos.y, pos.z));
        }

        if (!blockProperties.isSolid) {
            Vector3i abovePos = new Vector3i(pos.x, pos.y + 1, pos.z);
            Vector2i aboveBlock = getBlock(abovePos);
            if (aboveBlock != null) {
                int aboveBlockId = aboveBlock.x();
                if (BlockTypes.blockTypeMap.get(aboveBlockId).needsSupport(aboveBlock)) {
                    lostSupport(abovePos, aboveBlock, false);
                }
            }
        }
        if (needsSupport(block)) {
            Vector2i belowBlock = getBlock(new Vector3i(pos.x, pos.y - 1, pos.z));
            if (belowBlock != null) {
                int belowBlockId = belowBlock.x();
                if (!BlockTypes.blockTypeMap.get(belowBlockId).blockProperties.isSolid) {
                    lostSupport(pos, block, false);
                }
            }
        }
    }
}
