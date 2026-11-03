package org.conspiracraft.items.types;

import org.conspiracraft.blocks.types.BlockType;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.ItemSFX;
import org.conspiracraft.items.ItemTag;
import org.conspiracraft.items.ItemUseResult;
import org.conspiracraft.physics.DDAResult;
import org.conspiracraft.world.Chunk;
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
    public static float DEFAULT_USE_SPEED = 50.f;
    public float useSpeed = DEFAULT_USE_SPEED;
    public Vector2i atlasOffset = null;
    public int blockToPlace = 0;
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
    public ItemType useSpeed(float speed) {
        useSpeed = speed;
        return this;
    }
    public ItemType atlasOffset(int x, int y) {
        atlasOffset = new Vector2i(x, y);
        return this;
    }
    public ItemType blockToPlace(int x, int y) {
        blockToPlace = Chunk.packInts(x, y);
        return this;
    }
    public ItemType sfx(ItemSFX sfx) {
        sound = sfx;
        return this;
    }
    public int maxDurability() {return 0;}
    public ItemUseResult use(DDAResult dda, Item item) {
        if (blockToPlace >> 16 > 0 && player.inputHandler.rightButtonPressed && dda.hitAnything) {
            int block = World.getBlock(dda.prevHit);
            BlockType blockType = BlockTypes.blockTypes[block >> 16];
            if (blockType.blockProperties.isFluidReplaceable) {
                World.setBlock(dda.prevHit.x(), dda.prevHit.y(), dda.prevHit.z(), blockToPlace >> 16, blockToPlace & 0xFFFF);
                if (!player.creative) {item.amount--;}
                return new ItemUseResult(1, item, 1.f);
            }
        }
        return new ItemUseResult(0, item, 0.f);
    }
}
