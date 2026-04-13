package org.conspiracraft.graphics.textures;

public class Texture3D extends Texture {
    public int depth;

    public Texture3D(int width, int height, int depth, int channels, int format, boolean attachment) {
        super(width, height, channels, format, attachment);
        this.depth = depth;
    }
}
