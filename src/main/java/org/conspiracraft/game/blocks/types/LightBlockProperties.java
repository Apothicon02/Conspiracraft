package org.conspiracraft.game.blocks.types;

public class LightBlockProperties extends BlockProperties {
    public byte r;
    public LightBlockProperties r(int r) {
        this.r = (byte)r;
        return this;
    }
    public byte g;
    public LightBlockProperties g(int g) {
        this.g = (byte)g;
        return this;
    }
    public byte b;
    public LightBlockProperties b(int b) {
        this.b = (byte)b;
        return this;
    }

    public LightBlockProperties copy() {
        try {
            return (LightBlockProperties) this.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public LightBlockProperties() {}
}
