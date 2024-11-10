package org.terraflat.game;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLUtil;
import org.terraflat.engine.*;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL43.*;

public class Renderer {
    public static ShaderProgram scene;
    public static int sceneVaoId;
    public static int region1SSBOId;
    public static int lod1SSBOId;
    public static IntBuffer region1Buffer;
    public static IntBuffer lod1Buffer;
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

        region1SSBOId = glGenBuffers();
        lod1SSBOId = glGenBuffers();

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

        if (worldChanged) {
            worldChanged = false;
            int seaLevel = 12;
            int size = 808;
            int fullSize = (size+1)*(size+1)*(size+1);
            int[] terrain = new int[fullSize];
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
                                terrain[pos] = 2;
                                terrain[x + (y+1) * size + z * size * size] = 4 + (Math.random() > 0.98f ? 1 : 0);
                                upmost = false;
                            } else {
                                terrain[pos] = 3;
                            }
                        } else {
                            if (y <= seaLevel) {
                                terrain[pos] = 1;
                            } else {
                                terrain[pos] = 0;
                            }
                        }
                    }
                }
            }
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, region1SSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, region1SSBOId);
            region1Buffer = BufferUtils.createIntBuffer(fullSize).put(terrain).flip();
            glBufferData(GL_SHADER_STORAGE_BUFFER, region1Buffer, GL_STATIC_DRAW);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

            int lodSize = size/4;
            int lodFullSize = (lodSize+1)*(lodSize+1)*(lodSize+1);
            int[] lod1 = new int[lodFullSize];
            for (int x = 0; x < lodSize; x++) {
                for (int z = 0; z < lodSize; z++) {
                    for (int y = 0; y < lodSize; y++) {
                        int lodPos = x + y * lodSize + z * lodSize * lodSize;
                        for (int i = 0; i < 4; i++) {
                            if (terrain[((x*4)+i) + ((y*4)+i) * size + ((z*4)+i) * size * size] != 0f) {
                                lod1[lodPos] = 1;
                            }
                        }
                    }
                }
            }
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, lod1SSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, lod1SSBOId);
            lod1Buffer = BufferUtils.createIntBuffer(lodFullSize).put(lod1).flip();
            glBufferData(GL_SHADER_STORAGE_BUFFER, lod1Buffer, GL_STATIC_DRAW);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        } else {
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, region1SSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, region1SSBOId);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, lod1SSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, lod1SSBOId);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        }

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