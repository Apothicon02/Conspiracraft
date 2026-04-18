package org.conspiracraft.items;

import org.joml.Vector2i;
import org.conspiracraft.audio.SFX;
import org.conspiracraft.audio.Sounds;

import java.util.List;

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
}
