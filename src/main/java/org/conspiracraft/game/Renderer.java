package org.conspiracraft.game;

import org.conspiracraft.Main;
import org.joml.*;
import org.conspiracraft.engine.*;
import org.conspiracraft.engine.Window;

import java.io.IOException;

import static org.lwjgl.opengl.GL46.*;

public class Renderer {
    public static ShaderProgram scene;
    public static int sceneVaoId;

    public static int voxelsSSBOId;

    public static boolean showUI = true;

    public static boolean resized = false;
    public static int gigabyte = 1000000000;

    public static void createGLDebugger() {
        glEnable(GL_DEBUG_OUTPUT);
        glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
        glDebugMessageCallback((source, type, id, severity, length, message, userParam) -> {
            String msg = GLDebug.decode(source, type, id, severity, length, message, userParam);
            if (msg != null) {
                System.out.println(msg);
            }
        }, 0);
    }

    public static void generateVao() {
        sceneVaoId = glGenVertexArrays();
        glBindVertexArray(sceneVaoId);
        glBindBuffer(GL_ARRAY_BUFFER, glGenBuffers());
        glBufferData(GL_ARRAY_BUFFER, new float[]{
                -1, -1, 0,
                3, -1, 0,
                -1, 3, 0
        }, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
    }
    public static void createBuffers() {
        voxelsSSBOId = glCreateBuffers();
    }

    public static void init(Window window) throws Exception {
        createGLDebugger();
        scene = new ShaderProgram("scene.vert", new String[]{"scene.frag"},
                new String[]{"cam", "selected", "ui", "res"});
        generateVao();
        createBuffers();
    }

    public static void  updateUniforms(ShaderProgram program) {
        Matrix4f camMatrix = Main.player.getCameraMatrix();
        glUniformMatrix4fv(program.uniforms.get("cam"), true, new float[]{
                camMatrix.m00(), camMatrix.m10(), camMatrix.m20(), camMatrix.m30(),
                camMatrix.m01(), camMatrix.m11(), camMatrix.m21(), camMatrix.m31(),
                camMatrix.m02(), camMatrix.m12(), camMatrix.m22(), camMatrix.m32(),
                camMatrix.m03(), camMatrix.m13(), camMatrix.m23(), camMatrix.m33()});
        Vector3f selected = Main.raycast(new Matrix4f(Main.player.getCameraMatrix()), true, Main.reach);
        if (selected == null) {
            selected = new Vector3f(-1000, -1000, -1000);
        }
        glUniform3i(program.uniforms.get("selected"), (int) selected.x, (int) selected.y, (int) selected.z);
        glUniform1i(program.uniforms.get("ui"), showUI ? 1 : 0);
    }

    public static void updateVoxelBuffer() {
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, voxelsSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, voxelsSSBOId);
        glBufferData(GL_SHADER_STORAGE_BUFFER, World.voxels, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }

    public static void draw() {
        glBindVertexArray(sceneVaoId);
        glEnableVertexAttribArray(0);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glDisableVertexAttribArray(0);
    }
    public static void render(Window window) throws IOException {
        if (!Main.isClosing) {
            glClearColor(0, 0, 0, 1);
            glClear(GL_COLOR_BUFFER_BIT);

            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            scene.bind();
            updateUniforms(scene);
            updateVoxelBuffer();
            glUniform2i(scene.uniforms.get("res"), window.getWidth(), window.getHeight());
            draw();
        }
    }
}