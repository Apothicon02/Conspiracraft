package org.conspiracraft.items.types;

import org.conspiracraft.blocks.BlockTags;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.effects.Lightning;
import org.conspiracraft.items.Item;
import org.conspiracraft.physics.DDAResult;
import org.conspiracraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;

import static org.conspiracraft.Main.player;

public class LightningItemType extends ItemType {
    public LightningItemType(String name) {
        super(name);
    }
    @Override
    public int use(DDAResult dda, Item item) {
        if (player.inputHandler.leftButtonPressed && World.inBounds(player.selectedBlock)) {
            Vector3f lightningPos = new Vector3f(player.selectedBlock).max(new Vector3f(0, World.height, 0));
            for (int i = 1; i < World.height; i++) {
                lightningPos.sub(0, 1, 0);
                Vector2i block = World.getBlock((int)lightningPos.x(), (int)lightningPos.y(), (int)lightningPos.z());
                if (BlockTypes.blockTypes[block.x()].blocksLight(block) || BlockTags.leaves.tagged.contains(block.x())) {
                    World.effects.add(new Lightning(new Matrix4f().translate(lightningPos.x(), lightningPos.y()+1, lightningPos.z()).scale(1, i, 1)));
                    if (!player.creative) {item.amount--;}
                    break;
                }
            }
            return 500;
        } else {
            return 0;
        }
    }
}
