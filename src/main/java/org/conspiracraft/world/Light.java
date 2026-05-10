package org.conspiracraft.world;

public record Light(byte r, byte g, byte b, byte s) {
    public Light(int r, int g, int b, int s) {
        this((byte) r, (byte) g, (byte) b, (byte) s);
    }
}
