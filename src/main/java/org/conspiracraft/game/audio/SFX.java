package org.conspiracraft.game.audio;

public class SFX {
    public int id;
    public long length;
    public SFX(int id, float length) {
        this.id = id;
        this.length = (long)(length*1000);
    }
}