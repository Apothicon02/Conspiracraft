package org.conspiracraft.game.blocks.types;

public class LightBlockType extends BlockType {
    public byte r;
    public byte g;
    public byte b;

    public LightBlockType(boolean solid, boolean canBlockLight, boolean collidable, boolean fluidReplacable, boolean fluid, int red, int green, int blue) {
        super(solid, canBlockLight, collidable, fluidReplacable, fluid);
        r = (byte) red;
        g = (byte) green;
        b = (byte) blue;
    }
    public LightBlockType(boolean solid, boolean canBlockLight, boolean collidable, boolean fluidReplacable, int red, int green, int blue) {
        super(solid, canBlockLight, collidable, fluidReplacable);
        r = (byte) red;
        g = (byte) green;
        b = (byte) blue;
    }
    public LightBlockType(boolean solid, boolean canBlockLight, boolean collidable, int red, int green, int blue) {
        super(solid, canBlockLight, collidable);
        r = (byte) red;
        g = (byte) green;
        b = (byte) blue;
    }
    public LightBlockType(boolean solid, boolean canBlockLight, int red, int green, int blue) {
        super(solid, canBlockLight);
        r = (byte) red;
        g = (byte) green;
        b = (byte) blue;
    }
    public LightBlockType(boolean solid, int red, int green, int blue) {
        super(solid);
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
