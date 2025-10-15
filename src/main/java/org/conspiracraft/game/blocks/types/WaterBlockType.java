package org.conspiracraft.game.blocks.types;

import org.conspiracraft.game.blocks.Fluids;
import org.conspiracraft.game.blocks.Tags;
import org.conspiracraft.game.gameplay.StackManager;
import org.conspiracraft.game.world.World;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

import static org.conspiracraft.Main.player;
import static org.conspiracraft.game.world.World.inBounds;

public class WaterBlockType extends BlockType {

    public void moisturize(Vector3i pos) {
        Vector2i fluid = World.getBlock(pos);
        Vector2i ogFluid = new Vector2i(fluid);
        if (fluid.y > 0) {
            Vector2i belowBlock = World.getBlock(pos.x, pos.y - 1, pos.z);
            if (belowBlock.x == BlockTypes.getId(BlockTypes.DIRT)) {
                fluid.x = fluid.y <= 1 ? 0 : fluid.x;
                fluid.y--;
                World.setBlock(pos.x, pos.y - 1, pos.z, BlockTypes.getId(BlockTypes.MUD), 0, true, false, 3, false);
            } else if (Tags.crystals.tagged.contains(belowBlock.x)) {
                fluid.x = belowBlock.x;
                fluid.y = belowBlock.y;
            } else if (Tags.soakers.tagged.contains(belowBlock.x)) {
                fluid.x = 0;
                fluid.y = 0;
            } else if (fluid.y == 1 && belowBlock.x == BlockTypes.getId(BlockTypes.GRASS)) {
                int blockType = 4;
                if (Math.random() < 0.33f) { //33% chance to generate a flower
                    blockType = Tags.shortFlowers.tagged.get((int) (Math.random() * Tags.shortFlowers.tagged.size()));
                }
                fluid.x = -1;
                World.setBlock(pos.x, pos.y, pos.z, blockType, (int) (Math.random() * 4), true, false, 3, false);
            }
            if (BlockTypes.blockTypeMap.get(fluid.x).blockProperties.isFluid && fluid.y >= 1) {
                for (Vector3i nPos : new Vector3i[]{new Vector3i(pos.x, pos.y - 1, pos.z), new Vector3i(pos.x - 1, pos.y, pos.z), new Vector3i(pos.x + 1, pos.y, pos.z),
                        new Vector3i(pos.x, pos.y, pos.z - 1), new Vector3i(pos.x, pos.y, pos.z + 1)}) {
                    Vector2i nBlock = World.getBlock(nPos.x, nPos.y, nPos.z);
                    if (nBlock.x == BlockTypes.getId(BlockTypes.MAGMA)) {
                        World.setBlock(nPos.x, nPos.y, nPos.z, BlockTypes.getId(BlockTypes.OBSIDIAN), 0, true, false, 3, false);
                        fluid.x = Fluids.liquidGasMap.get(fluid.x);
                        break;
                    }
                }
            }
            if (fluid.x != ogFluid.x || fluid.y != ogFluid.y) { //only set block if it changed
                World.setBlock(pos.x, pos.y, pos.z, fluid.x, fluid.y, true, false, 3, false);
            }
        }
    }

    @Override
    public void tick(Vector4i pos) {
        if (inBounds(pos.x, pos.y, pos.z)) {
            Vector3i justPos = new Vector3i(pos.x, pos.y, pos.z);
            fluidTick(justPos);
            updateSupport(justPos);
            moisturize(justPos);
        }
    }

    @Override
    public boolean whilePlayerBreaking(Vector3i pos, Vector2i blockBreaking, Vector2i hand) {
        if (BlockTypes.blockTypeMap.get(hand.x) == BlockTypes.BUCKET) {
            player.stack[0] = Fluids.fluidBucketMap.get(blockBreaking.x);
            player.stack[1] = blockBreaking.y;
            World.setBlock(pos.x, pos.y, pos.z, 0, 0, true, false, 1, false);
        } else if (Fluids.fluidBucketMap.get(blockBreaking.x) == hand.x) {
            int room = 15-hand.y;
            int flow = Math.min(room, blockBreaking.y);
            player.stack[1] += flow;
            blockBreaking.y -= flow;
            if (blockBreaking.y < 1) {
                World.setBlock(pos.x, pos.y, pos.z, 0, 0, true, false, 1, false);
            } else {
                World.setBlock(pos.x, pos.y, pos.z, blockBreaking.x, blockBreaking.y, true, false, 1, false);
            }
            return false;
        } else {
            return false;
        }
        return true;
    }

    public WaterBlockType(BlockProperties blockProperties) {
        super(blockProperties);
    }
}
