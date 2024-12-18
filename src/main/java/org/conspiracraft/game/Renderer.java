package org.conspiracraft.game;

import org.conspiracraft.Main;
import org.conspiracraft.game.blocks.Block;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.blocks.types.LightBlockType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4i;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.conspiracraft.engine.*;
import org.conspiracraft.engine.Window;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL43.*;
import static org.conspiracraft.game.world.World.*;

public class Renderer {
    public static ShaderProgram scene;
    public static int sceneVaoId;
    public static int atlasSSBOId;
    public static int region1SSBOId;
    public static int region1LightingSSBOId;
    public static int region1CornersSSBOId;
    public static int coherentNoiseId;
    public static int whiteNoiseId;
    public static int resUniform;
    public static int camUniform;
    public static int renderDistanceUniform;
    public static int timeOfDayUniform;
    public static int timeUniform;
    public static int selectedUniform;
    public static int uiUniform;
    public static int sunUniform;
    public static int renderDistanceMul = 4;
    public static float timeOfDay = 0.5f;
    public static double time = 0.5d;
    public static boolean atlasChanged = true;
    public static boolean worldChanged = false;
    public static boolean[] collisionData = new boolean[9984*9984+9984];
    public static boolean showUI = true;

    public static void init() throws Exception {
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
        whiteNoiseId = glGenTextures();
        atlasSSBOId = glGenBuffers();
        region1SSBOId = glGenBuffers();
        region1LightingSSBOId = glGenBuffers();
        region1CornersSSBOId = glGenBuffers();

        glBindTexture(GL_TEXTURE_2D, coherentNoiseId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 2048, 2048, 0, GL_RGBA, GL_UNSIGNED_BYTE, Utils.imageToBuffer(Noise.COHERERENT_NOISE));

        glBindTexture(GL_TEXTURE_2D, whiteNoiseId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1024, 1024, 0, GL_RGBA, GL_UNSIGNED_BYTE, Utils.imageToBuffer(Noise.WHITE_NOISE));

        resUniform = glGetUniformLocation(scene.programId, "res");
        camUniform = glGetUniformLocation(scene.programId, "cam");
        renderDistanceUniform = glGetUniformLocation(scene.programId, "renderDistance");
        timeOfDayUniform = glGetUniformLocation(scene.programId, "timeOfDay");
        timeUniform = glGetUniformLocation(scene.programId, "time");
        selectedUniform = glGetUniformLocation(scene.programId, "selected");
        uiUniform = glGetUniformLocation(scene.programId, "ui");
        sunUniform = glGetUniformLocation(scene.programId, "sun");

        glBindVertexArray(0);
    }

    public static void render(Window window) throws IOException {
        glClearColor(0, 0, 0, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        scene.bind();

        glUniform2f(resUniform, window.getWidth(), window.getHeight());
        Matrix4f camMatrix = Main.player.getCameraMatrix();
        glUniformMatrix4fv(camUniform, true, new float[]{
                camMatrix.m00(), camMatrix.m10(), camMatrix.m20(), camMatrix.m30(),
                camMatrix.m01(), camMatrix.m11(), camMatrix.m21(), camMatrix.m31(),
                camMatrix.m02(), camMatrix.m12(), camMatrix.m22(), camMatrix.m32(),
                camMatrix.m03(), camMatrix.m13(), camMatrix.m23(), camMatrix.m33()});
        glUniform1i(renderDistanceUniform, 150+(40*renderDistanceMul));
        glUniform1f(timeOfDayUniform, timeOfDay);
        glUniform1d(timeUniform, time);
        Vector3f selected = Main.raycast(new Matrix4f(Main.player.getCameraMatrix()), true, 100);
        if (selected == null) {
            selected = new Vector3f(-1000, -1000, -1000);
        }
        glUniform3i(selectedUniform, (int) selected.x, (int) selected.y, (int) selected.z);
        glUniform1i(uiUniform, showUI ? 1 : 0);
        float halfSize = size/2f;
        Vector3f sunPos = new Vector3f(size/4f, 0, size/4f);
        sunPos.rotateY((float) time);
        glUniform3f(sunUniform, sunPos.x+halfSize, height, sunPos.z+halfSize);

        glBindVertexArray(sceneVaoId);
        glEnableVertexAttribArray(0);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, coherentNoiseId);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, whiteNoiseId);
        if (atlasChanged) {
            atlasChanged = false;
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, atlasSSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, atlasSSBOId);

            BufferedImage atlasImage = ImageIO.read(Renderer.class.getClassLoader().getResourceAsStream("assets/base/textures/atlas.png"));
            int size = 9984*9984+9984;
            int[] atlasData = new int[size];
            for (int x = 0; x < 128; x++) {
                for (int y = 0; y < 256; y++) {
                    atlasData[(9984*x)+y] = Utils.colorToInt(new Color(atlasImage.getRGB(x, y), true));
                    collisionData[(9984*x)+y] = new Color(atlasImage.getRGB(x, y), true).getAlpha() != 0;
                }
            }

            IntBuffer atlasBuffer = BufferUtils.createIntBuffer(size).put(atlasData).flip();
            glBufferData(GL_SHADER_STORAGE_BUFFER, atlasBuffer, GL_STATIC_DRAW);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        }
        if (worldChanged) {
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, region1SSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, region1SSBOId);
            glBufferData(GL_SHADER_STORAGE_BUFFER, (size*size*height)*4, GL_DYNAMIC_DRAW);

            int[] blocks = new int[height];
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    for (int y = 0; y < height; y++) {
                        blocks[y] = getBlock(x, y, z).id();
                    }
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, (((x*size)+z)*height)*4, blocks);
                }
            }

            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        } else {
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, region1SSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, region1SSBOId);
            for (int i = 0; i < Math.min(Math.min(Engine.fps, 100), blockQueue.size()); i++) {
                Vector4i blockData = new Vector4i(blockQueue.getFirst(), blockQueue.get(1), blockQueue.get(2), Utils.packInts(blockQueue.get(3), blockQueue.get(4)));
                blockQueue.removeFirst();
                blockQueue.removeFirst();
                blockQueue.removeFirst();
                blockQueue.removeFirst();
                blockQueue.removeFirst();
                Vector3i pos = new Vector3i(blockData.x, blockData.y, blockData.z);
                Block oldBlock = getBlock(pos);
                byte r = 0;
                byte g = 0;
                byte b = 0;
                if (BlockTypes.blockTypeMap.get(Utils.unpackInt(blockData.w()).x) instanceof LightBlockType lType) {
                    r = lType.r;
                    g = lType.g;
                    b = lType.b;
                    queueLightUpdate(pos);
                }
                Vector3i chunkPos = new Vector3i(pos.x/chunkSize, pos.y/chunkSize, pos.z/chunkSize);
                region1Chunks[condenseChunkPos(chunkPos.x, chunkPos.y, chunkPos.z)].setBlock(condenseLocalPos(pos.x-(chunkPos.x*chunkSize), pos.y-(chunkPos.y*chunkSize), pos.z-(chunkPos.z*chunkSize)), new Block(blockData.w, r, g, b, (byte) 0), true);
                updateHeightmap(blockData.x, blockData.z, true);
                recalculateLight(pos, oldBlock.r(), oldBlock.g(), oldBlock.b(), oldBlock.s());
                glBufferSubData(GL_SHADER_STORAGE_BUFFER, condensePos(pos)*4L, new int[]{blockData.w});
            }
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        }
        if (worldChanged) {
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, region1LightingSSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, region1LightingSSBOId);
            glBufferData(GL_SHADER_STORAGE_BUFFER, (size*size*height)*4, GL_DYNAMIC_DRAW);

            int[] lights = new int[height];
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    for (int y = 0; y < height; y++) {
                        Block block = getBlock(x, y, z);
                        lights[y] = Utils.vector4iToInt(new Vector4i(block.r(), block.g(), block.b(), block.s()));
                    }
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, (((x*size)+z)*height)*4, lights);
                }
            }

            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        } else {
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, region1LightingSSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, region1LightingSSBOId);
            for (int i = 0; i < 400; i++) {
                if (!lightQueue.isEmpty()) {
                    Vector3i lightPos = new Vector3i(lightQueue.getFirst(), lightQueue.get(1), lightQueue.get(2));
                    lightQueue.removeFirst();
                    lightQueue.removeFirst();
                    lightQueue.removeFirst();
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, condensePos(lightPos)*4L, new int[]{updateLight(lightPos)});
                } else {
                    break;
                }
            }
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        }
        if (worldChanged) {
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, region1CornersSSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, region1CornersSSBOId);
            glBufferData(GL_SHADER_STORAGE_BUFFER, size*size*height, GL_DYNAMIC_DRAW);

            ByteBuffer corners = ByteBuffer.allocate(height);
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    for (int y = 0; y < height; y++) {
                        corners.put(y, getCorners(x, y, z));
                    }
                    corners.flip();
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, ((x*size)+z)*height, corners);
                    corners.flip();
                    corners.clear();
                }
            }

            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        } else {
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, region1CornersSSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, region1CornersSSBOId);
            for (int i = 0; i < 400; i++) {
                if (!cornerQueue.isEmpty()) {
                    Vector3i pos = new Vector3i(cornerQueue.getFirst(), cornerQueue.get(1), cornerQueue.get(2));
                    cornerQueue.removeFirst();
                    cornerQueue.removeFirst();
                    cornerQueue.removeFirst();
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, condensePos(pos), ByteBuffer.wrap(new byte[]{getCorners(pos.x, pos.y, pos.z)}).flip());
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
        worldChanged = false;
    }

    public void cleanup() {
        glDeleteVertexArrays(sceneVaoId);
        scene.cleanup();
    }
}