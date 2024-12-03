package org.conspiracraft.game.blocks.types;

public class BlockType {
    public boolean isTransparent;
    public boolean isCollidable;

    public BlockType(boolean transparent, boolean collidable) {
        isTransparent = transparent;
        isCollidable = collidable;
    }
    public BlockType(boolean transparent) {
        isTransparent = transparent;
        isCollidable = true;
    }
}
