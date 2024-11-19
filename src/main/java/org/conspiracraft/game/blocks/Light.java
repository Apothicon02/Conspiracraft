package org.conspiracraft.game.blocks;

public class Light {
    private byte r;
    private byte g;
    private byte b;
    private byte s;

    public Light(int red, int green, int blue, int sun) {
        r = (byte) red;
        g = (byte) green;
        b = (byte) blue;
        s = (byte) sun;
    }

    public byte r() {
        return r;
    }
    public void r(int red) {
        r = (byte) red;
    }
    public byte g() {
        return g;
    }
    public void g(int green) {
        g = (byte) green;
    }
    public byte b() {
        return b;
    }
    public void b(int blue) {
        b = (byte) blue;
    }
    public byte s() {
        return s;
    }
    public void s(int blue) {
        s = (byte) blue;
    }
}