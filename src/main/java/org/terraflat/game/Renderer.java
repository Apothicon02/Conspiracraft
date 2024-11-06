package org.terraflat.game;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLUtil;
import org.terraflat.engine.*;

import javax.imageio.ImageIO;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL43.*;

public class Renderer {
    public static ShaderProgram scene;
    public static int sceneVaoId;
    public static int regionVoxelsSSBOId;
    public static FloatBuffer voxelRegionBuffer;
    public static int resUniform;
    public static int camPosUniform;

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

        List<Float> terrain = new ArrayList<>();
        FastNoiseLite noise = new FastNoiseLite();
        noise.SetNoiseType(FastNoiseLite.NoiseType.Cellular);
        for (int x = 0; x <= 5; x++) {
            for (int z = 0; z <= 5; z++) {
                float baseCellularNoise = noise.GetNoise(x, z);
                boolean upmost = true;
                for (int y = 5; y >= 0; y--) {
                    double baseGradient = TerraflatMath.gradient(y, 5, 0, 2, -1);
                    if (baseCellularNoise+baseGradient > 0) {
                        if (upmost) {
                            terrain.add((float) x);
                            terrain.add((float) y);
                            terrain.add((float) z);
                            terrain.add(0000.2000f);
                            upmost = false;
                        }
                    }
                }
            }
        }
        float[] voxels = new float[terrain.size()];
        for (int i = 0; i < terrain.size(); i++) {
            voxels[i] = terrain.get(i);
        }
        voxelRegionBuffer = BufferUtils.createFloatBuffer(voxels.length);
        voxelRegionBuffer.put(new float[]{
                0f, 2f, 0f, 0005.0000f,

                0f, 2f, 1f, 0004.0000f,
                1f, 1f, 0f, 0004.0000f,
                1f, 1f, 1f, 0004.0000f,
                0f, 2f, 2f, 0004.0000f,
                2f, 1f, 1f, 0004.0000f,
                1f, 2f, 2f, 0004.0000f,
                2f, 1f, 2f, 0004.0000f,

                0f, 0f, 0f, 0003.0000f,
                0f, 0f, 1f, 0003.0000f,
                0f, 0f, 2f, 0003.0000f,
                1f, 0f, 2f, 0003.0000f,

                0f, 1f, 0f, 0002.0000f,
                0f, 1f, 1f, 0002.0000f,
                1f, 0f, 0f, 0002.0000f,
                1f, 0f, 1f, 0002.0000f,
                0f, 1f, 2f, 0002.0000f,
                2f, 0f, 1f, 0002.0000f,
                1f, 1f, 2f, 0002.0000f,
                2f, 0f, 2f, 0002.0000f,

                2f, 0f, 0f, 0001.0000f,
                3f, 0f, 0f, 0001.0001f
        }).flip();
        regionVoxelsSSBOId = glGenBuffers();

        glBindTexture(GL_TEXTURE_2D, glGenTextures());
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 9984, 9984, 0, GL_RGBA, GL_UNSIGNED_BYTE, Utils.imageToBuffer(ImageIO.read(Renderer.class.getClassLoader().getResourceAsStream("assets/base/textures/atlas.png"))));

        resUniform = glGetUniformLocation(scene.programId, "res");
        camPosUniform = glGetUniformLocation(scene.programId, "camPos");

        glBindVertexArray(0);
    }

    public static void render(Window window) {
        glClearColor(0, 0, 0, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        scene.bind();

        glUniform2f(resUniform, window.getWidth(), window.getHeight());
        Matrix4f camMatrix = Main.camera.getViewMatrix();
        glUniform3f(camPosUniform, camMatrix.m30(), camMatrix.m31(), camMatrix.m32());

        glBindVertexArray(sceneVaoId);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, regionVoxelsSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, regionVoxelsSSBOId);
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