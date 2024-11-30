package org.conspiracraft.game;

import org.conspiracraft.game.blocks.Block;
import org.conspiracraft.game.blocks.Light;
import org.joml.Matrix4f;
import org.joml.Vector3i;
import org.joml.Vector4i;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLUtil;
import org.conspiracraft.engine.*;
import org.conspiracraft.engine.Window;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL43.*;
import static org.conspiracraft.game.World.*;

public class Renderer {
    public static ShaderProgram scene;
    public static int sceneVaoId;
    public static int atlasSSBOId;
    public static int region1SSBOId;
    public static int region1LightingSSBOId;
    public static int coherentNoiseId;
    public static int region1LightingId;
    public static int resUniform;
    public static int camUniform;
    public static int renderDistanceUniform;
    public static int timeOfDayUniform;
    public static int renderDistanceMul = 4;
    public static float timeOfDay = 0.5f;
    public static boolean atlasChanged = true;
    public static boolean worldChanged = false;
    public static int[] atlasData = null;

    public static void init() throws Exception {
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

        coherentNoiseId = glGenTextures();
        region1LightingId = glGenTextures();
        atlasSSBOId = glGenBuffers();
        region1SSBOId = glGenBuffers();
        region1LightingSSBOId = glGenBuffers();

        resUniform = glGetUniformLocation(scene.programId, "res");
        camUniform = glGetUniformLocation(scene.programId, "cam");
        renderDistanceUniform = glGetUniformLocation(scene.programId, "renderDistance");
        timeOfDayUniform = glGetUniformLocation(scene.programId, "timeOfDay");

        glBindVertexArray(0);
    }

    public static void render(Window window) throws IOException {
        glClearColor(0, 0, 0, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        scene.bind();

        glUniform2f(resUniform, window.getWidth(), window.getHeight());
        Matrix4f camMatrix = Main.camera.viewMatrix;
        glUniformMatrix4fv(camUniform, true, new float[]{
                camMatrix.m00(), camMatrix.m10(), camMatrix.m20(), camMatrix.m30(),
                camMatrix.m01(), camMatrix.m11(), camMatrix.m21(), camMatrix.m31(),
                camMatrix.m02(), camMatrix.m12(), camMatrix.m22(), camMatrix.m32(),
                camMatrix.m03(), camMatrix.m13(), camMatrix.m23(), camMatrix.m33()});
        glUniform1i(renderDistanceUniform, 150+(40*renderDistanceMul));
        glUniform1f(timeOfDayUniform, timeOfDay);

        glBindVertexArray(sceneVaoId);
        glEnableVertexAttribArray(0);

        if (atlasChanged) {
            atlasChanged = false;
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, atlasSSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, atlasSSBOId);

            BufferedImage atlasImage = ImageIO.read(Renderer.class.getClassLoader().getResourceAsStream("assets/base/textures/atlas.png"));
            int size = 9984*9984+9984;
            atlasData = new int[size];
            for (int x = 0; x < 9984; x++) {
                for (int y = 0; y < 9984; y++) {
                    atlasData[(9984*x)+y] = Utils.colorToInt(new Color(atlasImage.getRGB(x, y), true));
                }
            }

            IntBuffer atlasBuffer = BufferUtils.createIntBuffer(size).put(atlasData).flip();
            glBufferData(GL_SHADER_STORAGE_BUFFER, atlasBuffer, GL_STATIC_DRAW);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

            glBindTexture(GL_TEXTURE_2D, coherentNoiseId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 2048, 2048, 0, GL_RGBA, GL_UNSIGNED_BYTE, Utils.imageToBuffer(Noise.COHERERENT_NOISE));
        }
        if (worldChanged) {
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, region1SSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, region1SSBOId);
            int[] blocks = new int[size*size*height];
            for (int i = 0; i < region1Blocks.length; i++) {
                Block block = region1Blocks[i];
                if (block != null) {
                    blocks[i] = block.id();
                }
            }
            for (Vector4i blockData : blockQueue) {
                blocks[World.condensePos(blockData.x, blockData.y, blockData.z)] = blockData.w;
            }
            blockQueue = new ArrayList<>(List.of());
            glBufferData(GL_SHADER_STORAGE_BUFFER, blocks, GL_DYNAMIC_DRAW);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        } else {
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, region1SSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, region1SSBOId);
            for (int i = 0; i < Math.min(Math.min(Engine.fps, 100), blockQueue.size()); i++) {
                Vector4i blockData = blockQueue.getFirst();
                blockQueue.removeFirst();
                if (blockData != null) {
                    Vector3i pos = new Vector3i(blockData.x, blockData.y, blockData.z);
                    int condensedPos = World.condensePos(pos);
                    Block oldBlock = region1Blocks[condensedPos];
                    Block block = new Block(blockData.w);
                    region1Blocks[condensedPos] = block;
                    updateHeightmap(blockData.x, blockData.z, true);
                    Light oldLight = oldBlock.light;
                    if (oldLight == null) {
                        block.updateLight(pos);
                        oldLight = block.light;
                        if (oldLight == null) {
                            oldLight = new Light(0, 0, 0, 0);
                        }
                    }
                    recalculateLight(pos, oldLight);
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, condensedPos*4L, new int[]{blockData.w});
                }
            }
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        }
        if (worldChanged) {
            worldChanged = false;
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, region1LightingSSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, region1LightingSSBOId);
            int[] lights = new int[size*size*height];
            for (int i = 0; i < region1Blocks.length; i++) {
                Block block = region1Blocks[i];
                if (block != null && block.light != null) {
                    lights[i] = Utils.lightToInt(block.light);
                }
            }
            glBufferData(GL_SHADER_STORAGE_BUFFER, lights, GL_DYNAMIC_DRAW);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        } else {
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, region1LightingSSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, region1LightingSSBOId);
            for (int i = 0; i < 400; i++) {
                if (!lightQueue.isEmpty()) {
                    Vector3i lightPos = lightQueue.getFirst();
                    lightQueue.removeFirst();
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, condensePos(lightPos) * 4L, new int[]{updateLight(lightPos)});
                } else {
                    break;
                }
            }
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