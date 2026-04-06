package org.conspiracraft.renderer.assets;

import static org.lwjgl.opengl.GL46.*;

public class Texture {
    public int[] parameters;
    public int width;
    public int height;
    public int id = -1;

    public Texture(int[] parameters, int width, int height) {
        this.parameters = parameters;
        this.width = width;
        this.height = height;
        this.id = glGenTextures();
    }
}
