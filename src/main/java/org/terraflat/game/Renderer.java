package org.terraflat.game;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLUtil;
import org.terraflat.engine.ShaderProgram;
import org.terraflat.engine.Utils;
import org.terraflat.engine.Window;

import javax.imageio.ImageIO;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL43.*;


public class Renderer {
    public static ShaderProgram scene;
    public static int sceneVaoId;
    public static int subChunkSSBOId;
    public static IntBuffer voxelRegionBuffer;

    public void init() throws Exception {
        GL.createCapabilities();
        GLUtil.setupDebugMessageCallback();
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

        subChunkSSBOId = glGenBuffers();
        voxelRegionBuffer = BufferUtils.createIntBuffer(3);
        voxelRegionBuffer.put(new int[]{0, 0, 2}).flip();

        glBindTexture(GL_TEXTURE_2D, glGenTextures());
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 32000, 16000, 0, GL_RGBA, GL_UNSIGNED_BYTE, Utils.imageToBuffer(ImageIO.read(Renderer.class.getClassLoader().getResourceAsStream("assets/base/textures/atlas.png"))));

        glBindVertexArray(0);
    }

    public static void render(Window window) {
        ShaderProgram scene = Renderer.scene;
        glClearColor(0, 0, 0, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        scene.bind();

        glBindVertexArray(sceneVaoId);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, subChunkSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, subChunkSSBOId);
        glBufferData(GL_SHADER_STORAGE_BUFFER, voxelRegionBuffer, GL_STATIC_DRAW);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

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