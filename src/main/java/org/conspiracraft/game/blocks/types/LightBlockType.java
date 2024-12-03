package org.conspiracraft.game.blocks.types;

import org.conspiracraft.game.blocks.Light;

public class LightBlockType extends BlockType {
    public Light emission;

    public LightBlockType(boolean transparent, boolean collidable, int r, int g, int b) {
        super(transparent, collidable);
        emission = new Light(r, g, b, 0);
    }
    public LightBlockType(boolean transparent, int r, int g, int b) {
        super(transparent, true);
        emission = new Light(r, g, b, 0);
    }
}
