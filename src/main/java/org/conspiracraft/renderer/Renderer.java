package org.conspiracraft.renderer;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL41.glClearDepthf;

public class Renderer {
    public static void init() {

    }

    public static void render() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glClearColor(1, 0, 0, 0);
        glClearDepthf(0.f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public static void drawCube(Matrix4f matrix, Vector4f color) {}
}
