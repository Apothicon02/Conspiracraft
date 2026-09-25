package org.conspiracraft.items.types;

import org.conspiracraft.Main;
import org.conspiracraft.audio.*;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.items.GrappleItem;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.ItemUseResult;
import org.conspiracraft.physics.DDAResult;
import org.conspiracraft.player.Player;
import org.conspiracraft.world.World;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3f;

import static org.conspiracraft.player.HandManager.lmbDown;

public class GrappleItemType extends ItemType {
    public int strength;
    public int maxDurability;
    public GrappleItemType(String name, int strength, int maxDurability) {
        super(name);
        this.strength = strength;
        this.maxDurability = maxDurability;
    }
    @Override
    public Item createItem() {
        return new GrappleItem().durability(maxDurability).type(this);
    }
    @Override
    public int maxDurability() {return maxDurability;}
    @Override
    public ItemUseResult use(DDAResult dda, Item item) {
        if (item instanceof GrappleItem grappleItem) {
            if (lmbDown) {
                if (grappleItem.grapplePos.y() == -1) {
                    Vector2i block = World.getBlock(dda.hit.x(), dda.hit.y(), dda.hit.z());
                    if (BlockTypes.blockTypes[block.x()].blockProperties.resistance > 0.01f) {
                        item = grappleItem.damage(1);
                        grappleItem.grappleBlock.set(block);
                        grappleItem.grapplePos.set(dda.hitD);
                        grappleItem.maxDistance = new Vector3d(Main.player.pos).sub(grappleItem.grapplePos).length()-6;

                        Source source = new Source(new Vector3f(dda.hitD), 0.35f + (Player.playerRand.nextFloat()*0.05f), 0.9f + (Player.playerRand.nextFloat() / 3), 0.f, 0);
                        source.play(Sounds.HOOK, true);
                        AudioController.disposableSources.add(source);

                        return new ItemUseResult(1, item, 2.f);
                    }
                }
                return new ItemUseResult(1, item, 0.f);
            }
        }
        return new ItemUseResult(0, item, 0.f);
    }
}
