package org.conspiracraft.game.blocks;

public class LightBlockType extends BlockType {
    public Light emission;

    public LightBlockType(int r, int g, int b) {emission = new Light((byte) r, (byte) g, (byte) b, (byte) 0);}
}
