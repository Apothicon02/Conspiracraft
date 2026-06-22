package org.conspiracraft.items.types;

import org.conspiracraft.Main;
import org.conspiracraft.blocks.types.BlockType;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.gui.GUI;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.ItemSFX;
import org.conspiracraft.items.ItemTag;
import org.conspiracraft.physics.DDAResult;
import org.conspiracraft.world.World;
import org.joml.Vector2i;
import org.conspiracraft.audio.SFX;
import org.conspiracraft.audio.Sounds;

import java.util.List;

import static org.conspiracraft.Main.player;

public class ItemType {
    public List<ItemTag> tags = List.of();
    public String name;
    public int maxStackSize = 1;
    public Vector2i atlasOffset = null;
    public Vector2i blockToPlace = new Vector2i(0);
    public ItemSFX sound = new ItemSFX(new SFX[]{Sounds.CLOUD}, 0.2f, 1);

    public ItemType(String name) {
        this.name = name;
    }

    public Item createItem() {
        return new Item().type(this);
    }
    public ItemType maxStackSize(int size) {
        maxStackSize = size;
        return this;
    }
    public ItemType atlasOffset(int x, int y) {
        atlasOffset = new Vector2i(x, y);
        return this;
    }
    public ItemType blockToPlace(int x, int y) {
        blockToPlace = new Vector2i(x, y);
        return this;
    }
    public ItemType sfx(ItemSFX sfx) {
        sound = sfx;
        return this;
    }
    public int use(DDAResult dda, Item item) {
        if (blockToPlace != null && blockToPlace.x() > 0 && player.inputHandler.rightButtonPressed && dda.hitAnything) {
            Vector2i block = World.getBlock(dda.prevHit);
            BlockType blockType = BlockTypes.blockTypes[block.x()];
            if (blockType.blockProperties.isFluidReplaceable) {
                World.setBlock(dda.prevHit.x(), dda.prevHit.y(), dda.prevHit.z(), blockToPlace.x(), blockToPlace.y());
                if (!player.creative) {item.amount--;}
                return 200;
            }
        }
        return 0;
    }
}
