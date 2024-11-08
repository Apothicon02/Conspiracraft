package org.terraflat.game;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLUtil;
import org.terraflat.engine.*;

import javax.imageio.ImageIO;
import java.io.IOException;
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
    public static int camUniform;
    public static boolean worldChanged = true;
    public static boolean atlasChanged = true;

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

        regionVoxelsSSBOId = glGenBuffers();

        resUniform = glGetUniformLocation(scene.programId, "res");
        camUniform = glGetUniformLocation(scene.programId, "cam");

        glBindVertexArray(0);
    }

    public static void render(Window window) throws IOException {
        glClearColor(0, 0, 0, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        scene.bind();

        glUniform2f(resUniform, window.getWidth(), window.getHeight());
        Matrix4f camMatrix = Main.camera.getViewMatrix();
        glUniformMatrix4fv(camUniform, true, new float[]{
                camMatrix.m00(), camMatrix.m10(), camMatrix.m20(), camMatrix.m30(),
                camMatrix.m01(), camMatrix.m11(), camMatrix.m21(), camMatrix.m31(),
                camMatrix.m02(), camMatrix.m12(), camMatrix.m22(), camMatrix.m32(),
                camMatrix.m03(), camMatrix.m13(), camMatrix.m23(), camMatrix.m33()});

        glBindVertexArray(sceneVaoId);
        glEnableVertexAttribArray(0);

        if (atlasChanged) {
            atlasChanged = false;
            glBindTexture(GL_TEXTURE_2D, glGenTextures());
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 9984, 9984, 0, GL_RGBA, GL_UNSIGNED_BYTE, Utils.imageToBuffer(ImageIO.read(Renderer.class.getClassLoader().getResourceAsStream("assets/base/textures/atlas.png"))));
        }

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, regionVoxelsSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, regionVoxelsSSBOId);
        if (worldChanged) {
            worldChanged = false;
            int seaLevel = 12;
            int size = 811;
            int fullSize = (size+1)*(size+1)*(size+1);
            float[] terrain = new float[fullSize];
            FastNoiseLite noise = new FastNoiseLite((int) (Math.random()*9999));
            noise.SetNoiseType(FastNoiseLite.NoiseType.Cellular);
            for (int x = 1; x <= size; x++) {
                for (int z = 1; z <= size; z++) {
                    float baseCellularNoise = noise.GetNoise(x, z);
                    boolean upmost = true;
                    for (int y = 30; y >= 1; y--) {
                        int pos = x + y * size + z * size * size;
                        double baseGradient = TerraflatMath.gradient(y, 30, 1, 2, -1);
                        if (baseCellularNoise + baseGradient > 0) {
                            if (upmost && y >= seaLevel) {
                                terrain[pos] = 0002.000f;
                                terrain[x + (y+1) * size + z * size * size] = 0004.000f + (Math.random() > 0.98f ? 1f : 0f);
                                upmost = false;
                            } else {
                                terrain[pos] = 0003.000f;
                            }
                        } else {
                            if (y <= seaLevel) {
                                terrain[pos] = 0001.000f;
                            } else {
                                terrain[pos] = 0000.000f;
                            }
                        }
                    }
                }
            }
            voxelRegionBuffer = BufferUtils.createFloatBuffer(fullSize).put(terrain).flip();
            glBufferData(GL_SHADER_STORAGE_BUFFER, voxelRegionBuffer, GL_STATIC_DRAW);
        }
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