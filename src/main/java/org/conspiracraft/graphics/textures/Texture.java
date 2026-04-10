package org.conspiracraft.graphics.textures;

public class Texture {
    public int width;
    public int height;
    public long image = -1;
    public long memory = -1;
    public long imageView = -1;
    public long sampler = -1;

    public Texture(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
