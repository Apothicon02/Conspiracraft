package org.conspiracraft.game.blocks.types;

public class LightBlockType extends BlockType {
    public byte r;
    public byte g;
    public byte b;

    public LightBlockType(boolean transparent, boolean collidable, boolean fluidReplacable, boolean fluid, int red, int green, int blue) {
        super(transparent, collidable, fluidReplacable, fluid);
        r = (byte) red;
        g = (byte) green;
        b = (byte) blue;
    }
    public LightBlockType(boolean transparent, boolean collidable, boolean fluidReplacable, int red, int green, int blue) {
        super(transparent, collidable, fluidReplacable);
        r = (byte) red;
        g = (byte) green;
        b = (byte) blue;
    }
    public LightBlockType(boolean transparent, boolean collidable, int red, int green, int blue) {
        super(transparent, collidable);
        r = (byte) red;
        g = (byte) green;
        b = (byte) blue;
    }
    public LightBlockType(boolean transparent, int red, int green, int blue) {
        super(transparent);
        r = (byte) red;
        g = (byte) green;
        b = (byte) blue;
    }
    public LightBlockType(int red, int green, int blue) {
        super();
        r = (byte) red;
        g = (byte) green;
        b = (byte) blue;
    }
}
