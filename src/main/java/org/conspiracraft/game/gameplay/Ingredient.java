package org.conspiracraft.game.gameplay;

import org.conspiracraft.game.blocks.Tag;
import org.joml.Vector2i;

public class Ingredient {
    private Integer blockType = null;
    private Vector2i block = null;
    private Tag blockTag = null;
    public Ingredient(int blockType) {
        this.blockType = blockType;
    }
    public Ingredient(Vector2i block) {
        this.block = block;
    }
    public Ingredient(Tag blockTag) {
        this.blockTag = blockTag;
    }

    public boolean matchesIngredient(Vector2i input) {
        if (blockTag != null) {
            return blockTag.tagged.contains(input.x);
        } else if (block != null) {
            return block.x == input.x && block.y == input.y;
        } if (blockType != null) {
            return blockType == input.x;
        }
        return true;
    }
}
