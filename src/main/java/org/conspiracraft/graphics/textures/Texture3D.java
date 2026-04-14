package org.conspiracraft.graphics.textures;

public class Texture3D extends Texture {
    public int depth;

    public Texture3D(int width, int height, int depth, int channels, int format, int usage, boolean attachment) {
        super(width, height, channels, format, usage, attachment);
        this.depth = depth;
    }
}
