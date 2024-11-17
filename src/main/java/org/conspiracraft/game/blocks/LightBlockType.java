package org.conspiracraft.game.blocks;

import org.conspiracraft.game.blocks.properties.Emission;

public class LightBlockType extends BlockType {
    public Emission emission;

    public LightBlockType(int r, int g, int b) {
        emission = new Emission((byte) r, (byte) g, (byte) b);
    }
}
