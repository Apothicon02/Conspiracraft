package org.conspiracraft.graphics.textures;

public class Texture3D extends Texture {
    public int depth;

    public Texture3D(int width, int height, int depth, int channels) {
        super(width, height, channels);
        this.depth = depth;
    }
}
