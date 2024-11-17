package org.conspiracraft.game.blocks.properties;

public class Emission {
    private final byte redEmission;
    private final byte greenEmission;
    private final byte blueEmission;

    public Emission(byte r, byte g, byte b) {
        redEmission = r;
        greenEmission = g;
        blueEmission = b;
    }

    public byte r() {
        return redEmission;
    }
    public byte g() {
        return greenEmission;
    }
    public byte b() {
        return blueEmission;
    }
}