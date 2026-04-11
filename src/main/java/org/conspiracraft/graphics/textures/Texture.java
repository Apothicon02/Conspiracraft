package org.conspiracraft.graphics.textures;

public class Texture {
    public int width;
    public int height;
    public int channels;
    public long image = -1;
    public long memory = -1;
    public long imageView = -1;
    public long sampler = -1;

    public Texture(int width, int height, int channels) {
        this.width = width;
        this.height = height;
        this.channels = channels;
    }
}
