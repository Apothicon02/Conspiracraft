package org.conspiracraft.renderer;

import org.conspiracraft.renderer.assets.Models;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.Arrays;

import static org.lwjgl.opengl.GL45.*;

public class Renderer {
    public static Framebuffer displayFB;
    public static Framebuffer ddaFB;

    public static ShaderProgram copy;
    public static ShaderProgram dda;

    public static void init() throws Exception {
        displayFB = new Framebuffer();
        ddaFB = new Framebuffer(1, true);

        dda = new ShaderProgram("minimal.vert", new String[]{"dda.frag"},
                new String[]{});
        copy = new ShaderProgram("minimal.vert", new String[]{"copy.frag"},
                new String[]{});
    }

    public static void render() {
        ddaFB.bind();
        dda.bind();
        drawFullscreen();

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glUseProgram(copy.programId);
        glBindTextureUnit(0, ddaFB.textures.get(GL_COLOR_ATTACHMENT0).id);
        drawFullscreen();


//        displayFB.bind();
//        copy.bind();
//        glBindTextureUnit(0, ddaFB.textures.get(GL_COLOR_ATTACHMENT0).id);
//        drawFullscreen();
    }

    public static void drawFullscreen() {
        glBindVertexArray(Models.SCREEN_TRIANGLE.vaoId);
        glEnableVertexAttribArray(0);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glDisableVertexAttribArray(0);
    }
    public static void drawCube(Matrix4f matrix, Vector4f color) {}
}
