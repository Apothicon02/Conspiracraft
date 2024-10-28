package org.terraflat.game;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.terraflat.engine.ShaderProgram;
import org.terraflat.engine.Utils;
import org.terraflat.engine.Window;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;


public class Renderer {
    public static ShaderProgram scene;
    public static int sceneVaoId;

    public void init() throws Exception {
        GL.createCapabilities();

        sceneVaoId = glGenVertexArrays();
        glBindVertexArray(sceneVaoId);

        scene = new ShaderProgram();
        scene.createVertexShader(Utils.readFile("assets/base/shaders/scene.vert"));
        scene.createFragmentShader(Utils.readFile("assets/base/shaders/scene.frag"));
        scene.link();

        int sceneVboId = glGenBuffers();
        ByteBuffer verticesBuffer = BufferUtils.createByteBuffer(6);
        verticesBuffer.put(new byte[]{-1, -1, 3, -1, -1, 3}).flip();
        glBindBuffer(GL_ARRAY_BUFFER, sceneVboId);
        glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 2, GL_BYTE, false, 0, 0);
        glEnableVertexAttribArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        int subChunkVboId = glGenBuffers();
        IntBuffer subChunkBuffer = BufferUtils.createIntBuffer(3);
        subChunkBuffer.put(new int[]{155, 0, 30}).flip();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, subChunkVboId);
        glBufferData(GL_SHADER_STORAGE_BUFFER, subChunkBuffer, GL_STATIC_DRAW);
        glBindBufferBase(subChunkVboId, 0, GL_SHADER_STORAGE_BUFFER);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        glBindVertexArray(0);
    }

    public static void render(Window window) {
        ShaderProgram scene = Renderer.scene;
        glClearColor(0, 0, 0, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        scene.bind();

        glBindVertexArray(sceneVaoId);
        glEnableVertexAttribArray(0);

        glDrawArrays(GL_TRIANGLES, 0, 3);

        glDisableVertexAttribArray(0);
        glBindVertexArray(0);

        scene.unbind();
    }

    public void cleanup() {
        glDeleteVertexArrays(sceneVaoId);
        scene.cleanup();
    }
}