package org.conspiracraft.items.types;

import org.conspiracraft.blocks.types.BlockType;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.elements.Element;
import org.conspiracraft.items.IceItem;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.ItemUseResult;
import org.conspiracraft.physics.DDAResult;
import org.conspiracraft.world.World;
import org.joml.Vector2i;

import static org.conspiracraft.Main.player;

public class IceItemType extends ItemType {
    public Element element = null;
    public IceItemType(String name) {
        super(name);
    }

    public IceItemType element(Element element) {
        this.element = element;
        element.iceItemType = this;
        return this;
    }

    @Override
    public IceItem createItem() {
        return (IceItem) new IceItem().type(this);
    }
    @Override
    public ItemUseResult use(DDAResult dda, Item item) {
        if (blockToPlace >> 16 > 0 && player.inputHandler.rightButtonPressed && dda.hitAnything && item.amount >= 1000) {
            int block = World.getBlock(dda.prevHit);
            BlockType blockType = BlockTypes.blockTypes[block >> 16];
            if (blockType.blockProperties.isFluidReplaceable) {
                World.setBlock(dda.prevHit.x(), dda.prevHit.y(), dda.prevHit.z(), blockToPlace >> 16, blockToPlace & 0xFFFF);
                if (!player.creative) {
                    item.amount -= 1000;
                }
                return new ItemUseResult(200, item, 1.f);
            }
        }
        return new ItemUseResult(0, item, 0.f);
    }
}
