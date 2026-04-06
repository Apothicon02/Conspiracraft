package org.conspiracraft.renderer;

import org.conspiracraft.renderer.assets.Texture;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL30.*;

public class Renderer {
    public static Framebuffer displayFB;
    public static Framebuffer rasterFB;
    public static void init() {
        displayFB = new Framebuffer();
        rasterFB = new Framebuffer(1, true);
    }

    public static void render() {
        rasterFB.bind();
        glClearColor(0, 1, 0, 0);
        glClear(GL_COLOR_BUFFER_BIT);
        displayFB.bind();
    }

    public static void drawFullscreen() {

    }
    public static void drawCube(Matrix4f matrix, Vector4f color) {}
}
