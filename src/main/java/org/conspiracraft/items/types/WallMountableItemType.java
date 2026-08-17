package org.conspiracraft.items.types;

import org.conspiracraft.blocks.types.BlockType;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.ItemUseResult;
import org.conspiracraft.physics.DDAResult;
import org.conspiracraft.world.World;
import org.joml.Vector2i;
import org.joml.Vector3f;

import static org.conspiracraft.Main.player;

public class WallMountableItemType extends ItemType {

    public WallMountableItemType(String name) {super(name);}

    @Override
    public ItemUseResult use(DDAResult dda, Item item) {
        if (blockToPlace != null && blockToPlace.x() > 0 && player.inputHandler.rightButtonPressed && dda.hitAnything) {
            Vector2i block = World.getBlock(dda.prevHit);
            BlockType blockType = BlockTypes.blockTypes[block.x()];
            if (blockType.blockProperties.isFluidReplaceable) {
                int subType = blockToPlace.y();
                Vector3f normal = new Vector3f(dda.hit).sub(dda.prevHit.x(), dda.prevHit.y(), dda.prevHit.z()).negate();
                if (normal.y() == 0) {
                    if (normal.x() > 0.f) {
                        subType += 1;
                    } else if (normal.z() > 0.f) {
                        subType += 3;
                    } else if (normal.x() < 0.f){
                        subType += 2;
                    } else {
                        subType += 4;
                    }
                }
                World.setBlock(dda.prevHit.x(), dda.prevHit.y(), dda.prevHit.z(), blockToPlace.x(), subType);
                if (!player.creative) {item.amount--;}
                return new ItemUseResult(200, item);
            }
        }
        return new ItemUseResult(0, item);
    }
}
