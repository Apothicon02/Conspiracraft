package org.conspiracraft.game.blocks;

public class Light {
    private byte r;
    private byte g;
    private byte b;
    private byte s;

    public Light(int red, int green, int blue, int sun) {
        r = (byte) Math.max(red, 0);
        g = (byte) Math.max(green, 0);;
        b = (byte) Math.max(blue, 0);;
        s = (byte) Math.max(sun, 0);;
    }

    public Light(int all) {
        r = (byte) Math.max(all, 0);
        g = (byte) Math.max(all, 0);;
        b = (byte) Math.max(all, 0);;
        s = (byte) Math.max(all, 0);;
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
    public void s(int sun) {
        s = (byte) sun;
    }
}
