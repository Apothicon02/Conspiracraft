package org.conspiracraft.game.blocks.types;

import org.conspiracraft.game.blocks.Light;

public class LightBlockType extends BlockType {
    public Light emission;

    public LightBlockType(boolean transparent, int r, int g, int b) {
        super(transparent);
        emission = new Light(r, g, b, 0);}
}
