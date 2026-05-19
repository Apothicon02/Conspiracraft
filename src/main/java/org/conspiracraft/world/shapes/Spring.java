package org.conspiracraft.world.shapes;

import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.world.World;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.conspiracraft.world.World.getBlock;

public class Spring {
    public static void generate(int x, int y, int z, int blockType, int blockSubType, int radius) {
        ArrayList<Vector3i> blocks = new ArrayList<>();
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                if (World.inBounds(lX, y, lZ)) {
                    int xDist = lX - x;
                    int zDist = lZ - z;
                    int dist = xDist * xDist + zDist * zDist;
                    if (dist <= radius * 2) {
                        Vector2i belowBlock = getBlock(lX, y-1, lZ);
                        Vector2i block = getBlock(lX, y, lZ);
                        if ((belowBlock.x() == BlockTypes.GRASS.id || belowBlock.x() == BlockTypes.DIRT.id || belowBlock.x() == BlockTypes.SNOW.id) && BlockTypes.blockTypes[block.x()].blockProperties.isFluidReplaceable) {
                            blocks.add(new Vector3i(lX, y, lZ));
                        }
                    }
                }
            }
        }

        blocks.forEach((pos) -> {
            World.replaceBlock(pos.x(), pos.y()+1, pos.z(), 0, 0);
            World.setBlock(pos.x(), pos.y(), pos.z(), 1, 14);
            World.setBlock(pos.x(), pos.y()-1, pos.z(), blockType, blockSubType);
            if (!blocks.contains(new Vector3i(pos).add(1, 0, 0))) {
                World.replaceBlock(pos.x()+1, pos.y()+1, pos.z(), 0, 0);
                World.setBlock(pos.x()+1, pos.y(), pos.z(), blockType, blockSubType);
                World.setBlock(pos.x()+1, pos.y()-1, pos.z(), blockType, blockSubType);
            }
            if (!blocks.contains(new Vector3i(pos).add(-1, 0, 0))) {
                World.replaceBlock(pos.x()-1, pos.y()+1, pos.z(), 0, 0);
                World.setBlock(pos.x()-1, pos.y(), pos.z(), blockType, blockSubType);
                World.setBlock(pos.x()-1, pos.y()-1, pos.z(), blockType, blockSubType);
            }
            if (!blocks.contains(new Vector3i(pos).add(0, 0, 1))) {
                World.replaceBlock(pos.x(), pos.y()+1, pos.z()+1, 0, 0);
                World.setBlock(pos.x(), pos.y(), pos.z()+1, blockType, blockSubType);
                World.setBlock(pos.x(), pos.y()-1, pos.z()+1, blockType, blockSubType);
            }
            if (!blocks.contains(new Vector3i(pos).add(0, 0, -1))) {
                World.replaceBlock(pos.x(), pos.y()+1, pos.z()-1, 0, 0);
                World.setBlock(pos.x(), pos.y(), pos.z()-1, blockType, blockSubType);
                World.setBlock(pos.x(), pos.y()-1, pos.z()-1, blockType, blockSubType);
            }
        });
    }
}
