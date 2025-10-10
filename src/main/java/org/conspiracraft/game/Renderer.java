package org.conspiracraft.game;

import org.conspiracraft.Main;
import org.conspiracraft.game.blocks.Tags;
import org.conspiracraft.game.noise.Noises;
import org.conspiracraft.game.world.Chunk;
import org.conspiracraft.game.world.World;
import org.joml.*;
import org.lwjgl.BufferUtils;
import org.conspiracraft.engine.*;
import org.conspiracraft.engine.Window;
import org.lwjgl.PointerBuffer;
import org.lwjgl.util.vma.VmaVirtualAllocationCreateInfo;
import org.lwjgl.util.vma.VmaVirtualBlockCreateInfo;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.LongBuffer;
import java.util.LinkedList;

import static org.conspiracraft.engine.Utils.*;
import static org.conspiracraft.game.world.World.*;
import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public class Renderer {
    public static ShaderProgram scene;
    public static int sceneVaoId;
    public static int sceneHalfVaoId;
    public static int sceneOtherHalfVaoId;
    public static ShaderProgram blurScene;
    public static ShaderProgram unscaledScene;
    public static ShaderProgram reflectionScene;
    public static ShaderProgram finalScene;

    public static int atlasWidth = 536;
    public static int atlasHeight = 1024;
    public static int atlasDepth = atlasHeight/64;
    public static int atlasImageId;
    public static int coherentNoiseId;
    public static int whiteNoiseId;
    public static int cloudNoiseId;
    public static int sceneUnscaledImageId;

    public static int chunkBlocksSSBOId;
    public static int blocksSSBOId;
    public static int chunkCornersSSBOId;
    public static int cornersSSBOId;
    public static int chunkLightsSSBOId;
    public static int lightsSSBOId;
    public static int chunkEmptySSBOId;
    public static int playerSSBOId;

    public static int renderDistanceMul = 8; //3
    public static int aoQuality = 2;
    public static float timeOfDay = 0.5f;
    public static double time = 0.5d;
    public static boolean atlasChanged = true;
    public static boolean worldChanged = false;
    public static boolean[] collisionData = new boolean[(1024*1024)+1024];
    public static boolean showUI = true;
    public static boolean shadowsEnabled = true;
    public static boolean reflectionShadows = false;
    public static boolean reflectionsEnabled = true;

    public static boolean resized = false;
    public static int gigabyte = 1000000000;
    public static int defaultSSBOSize = gigabyte;
    public static int chunkArrSize = ((((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks);
    public static PointerBuffer blocks = BufferUtils.createPointerBuffer(1);
    public static int[] chunkBlockPointers = new int[chunkArrSize*5];
    public static long[] chunkBlockAllocs = new long[chunkArrSize];
    public static PointerBuffer corners = BufferUtils.createPointerBuffer(1);
    public static int[] chunkCornerPointers = new int[chunkArrSize*4];
    public static long[] chunkCornerAllocs = new long[chunkArrSize];
    public static PointerBuffer lights = BufferUtils.createPointerBuffer(1);
    public static int[] chunkLightPointers = new int[chunkArrSize*4];
    public static long[] chunkLightAllocs = new long[chunkArrSize];

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

    public static void generateTextures(Window window) throws IOException {
        atlasImageId = glGenTextures();
        BufferedImage atlasImage = ImageIO.read(Renderer.class.getClassLoader().getResourceAsStream("assets/base/textures/atlas.png"));
        glBindTexture(GL_TEXTURE_3D, atlasImageId);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA32F, atlasWidth, 64, atlasDepth, 0, GL_RGBA, GL_UNSIGNED_BYTE, Utils.imageToBuffer(atlasImage));

        coherentNoiseId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, coherentNoiseId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, 2048, 2048, 0, GL_RED, GL_UNSIGNED_BYTE, Noises.COHERERENT_NOISE.byteData());

        whiteNoiseId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, whiteNoiseId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, 1024, 1024, 0, GL_RED, GL_UNSIGNED_BYTE, Noises.WHITE_NOISE.byteData());

        cloudNoiseId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, cloudNoiseId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, 2048, 2048, 0, GL_RED, GL_UNSIGNED_BYTE, Noises.CLOUD_NOISE.byteData());

        sceneUnscaledImageId = glGenTextures();
        glBindTexture(GL_TEXTURE_3D, sceneUnscaledImageId);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexStorage3D(GL_TEXTURE_3D, 1, GL_RGBA32F, window.getWidth(), window.getHeight(), 4);
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

        sceneHalfVaoId = glGenVertexArrays();
        glBindVertexArray(sceneHalfVaoId);
        glBindBuffer(GL_ARRAY_BUFFER, glGenBuffers());
        glBufferData(GL_ARRAY_BUFFER, new float[]{
                0, -3, 0,
                -3, 0, 0,
                0, 3, 0
        }, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

        sceneOtherHalfVaoId = glGenVertexArrays();
        glBindVertexArray(sceneOtherHalfVaoId);
        glBindBuffer(GL_ARRAY_BUFFER, glGenBuffers());
        glBufferData(GL_ARRAY_BUFFER, new float[]{
                0, 3, 0,
                3, 0, 0,
                0, -3, 0
        }, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
    }
    public static void createBuffers() {
        chunkBlocksSSBOId = glCreateBuffers();
        blocksSSBOId = glCreateBuffers();
        chunkCornersSSBOId = glCreateBuffers();
        cornersSSBOId = glCreateBuffers();
        chunkLightsSSBOId = glCreateBuffers();
        lightsSSBOId = glCreateBuffers();
        chunkEmptySSBOId = glCreateBuffers();
        playerSSBOId = glCreateBuffers();
    }
    public static void createVMA() {
        VmaVirtualBlockCreateInfo blockCreateInfo = VmaVirtualBlockCreateInfo.create();
        blockCreateInfo.size(defaultSSBOSize);
        vmaCreateVirtualBlock(blockCreateInfo, blocks);
        vmaCreateVirtualBlock(blockCreateInfo, corners);
        vmaCreateVirtualBlock(blockCreateInfo, lights);
    }

    public static void fillBuffers() {
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, blocksSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, blocksSSBOId);
        glBufferData(GL_SHADER_STORAGE_BUFFER, defaultSSBOSize, GL_DYNAMIC_DRAW);
        chunkBlockPointers = new int[((((sizeChunks * sizeChunks) + sizeChunks) * heightChunks) + heightChunks) * 5];
        chunkBlockAllocs = new long[((((sizeChunks * sizeChunks) + sizeChunks) * heightChunks) + heightChunks)];

        outerLoop:
        for (int chunkX = 0; chunkX < sizeChunks; chunkX++) {
            for (int chunkZ = 0; chunkZ < sizeChunks; chunkZ++) {
                for (int chunkY = 0; chunkY < heightChunks; chunkY++) {
                    VmaVirtualAllocationCreateInfo allocCreateInfo = VmaVirtualAllocationCreateInfo.create();
                    int condensedChunkPos = condenseChunkPos(chunkX, chunkY, chunkZ);
                    int paletteSize = chunks[condensedChunkPos].getBlockPaletteSize();
                    int bitsPerValue = chunks[condensedChunkPos].bitsPerBlock();
                    int valueMask = chunks[condensedChunkPos].blockValueMask();
                    int[] compressedBlocks = chunks[condensedChunkPos].getBlockData();
                    if (compressedBlocks == null) {
                        allocCreateInfo.size((paletteSize) * 4L);
                    } else {
                        allocCreateInfo.size((paletteSize + compressedBlocks.length) * 4L);
                    }

                    PointerBuffer alloc = BufferUtils.createPointerBuffer(1);
                    LongBuffer offset = BufferUtils.createLongBuffer(1);
                    long res = vmaVirtualAllocate(blocks.get(0), allocCreateInfo, alloc, offset);
                    if (res == VK_SUCCESS) {
                        chunkBlockAllocs[condensedChunkPos] = alloc.get();
                        int pointer = (int) offset.get(0);
                        chunkBlockPointers[condensedChunkPos * 5] = pointer / 4;
                        chunkBlockPointers[(condensedChunkPos * 5) + 1] = paletteSize;
                        chunkBlockPointers[(condensedChunkPos * 5) + 2] = bitsPerValue;
                        chunkBlockPointers[(condensedChunkPos * 5) + 3] = valueMask;
                        chunkBlockPointers[(condensedChunkPos * 5) + 4] = chunks[condensedChunkPos].getSubChunks()[0];
                        glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer, chunks[condensedChunkPos].getBlockPalette());
                        if (compressedBlocks != null) {
                            glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer + (paletteSize * 4L), compressedBlocks);
                        }
                    } else {
                        System.out.print("blocksSSBO ran out of space! \n");
                        Main.isClosing = true;
                        break outerLoop;
                    }
                }
            }
        }
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkBlocksSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, chunkBlocksSSBOId);
        glBufferData(GL_SHADER_STORAGE_BUFFER, chunkBlockPointers, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkEmptySSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 6, chunkEmptySSBOId);
        glBufferData(GL_SHADER_STORAGE_BUFFER, chunkEmptiness, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, lightsSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 5, lightsSSBOId);
        glBufferData(GL_SHADER_STORAGE_BUFFER, defaultSSBOSize, GL_DYNAMIC_DRAW);
        chunkLightPointers = new int[((((sizeChunks * sizeChunks) + sizeChunks) * heightChunks) + heightChunks) * 4];
        chunkLightAllocs = new long[((((sizeChunks * sizeChunks) + sizeChunks) * heightChunks) + heightChunks)];

        outerLoop:
        for (int chunkX = 0; chunkX < sizeChunks; chunkX++) {
            for (int chunkZ = 0; chunkZ < sizeChunks; chunkZ++) {
                for (int chunkY = 0; chunkY < heightChunks; chunkY++) {
                    VmaVirtualAllocationCreateInfo allocCreateInfo = VmaVirtualAllocationCreateInfo.create();
                    int condensedChunkPos = condenseChunkPos(chunkX, chunkY, chunkZ);
                    int paletteSize = chunks[condensedChunkPos].getLightPaletteSize();
                    int bitsPerValue = chunks[condensedChunkPos].bitsPerLight();
                    int valueMask = chunks[condensedChunkPos].lightValueMask();
                    int[] compressedLights = chunks[condensedChunkPos].getLightData();
                    if (compressedLights == null) {
                        allocCreateInfo.size((paletteSize) * 4L);
                    } else {
                        allocCreateInfo.size((paletteSize + compressedLights.length) * 4L);
                    }

                    PointerBuffer alloc = BufferUtils.createPointerBuffer(1);
                    LongBuffer offset = BufferUtils.createLongBuffer(1);
                    long res = vmaVirtualAllocate(lights.get(0), allocCreateInfo, alloc, offset);
                    if (res == VK_SUCCESS) {
                        chunkLightAllocs[condensedChunkPos] = alloc.get();
                        int pointer = (int) offset.get(0);
                        chunkLightPointers[condensedChunkPos * 4] = pointer / 4;
                        chunkLightPointers[(condensedChunkPos * 4) + 1] = paletteSize;
                        chunkLightPointers[(condensedChunkPos * 4) + 2] = bitsPerValue;
                        chunkLightPointers[(condensedChunkPos * 4) + 3] = valueMask;
                        glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer, chunks[condensedChunkPos].getLightPalette());
                        if (compressedLights != null) {
                            glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer + (paletteSize * 4L), compressedLights);
                        }
                    } else {
                        System.out.print("lightsSSBO ran out of space! \n");
                        Main.isClosing = true;
                        break outerLoop;
                    }
                }
            }
        }
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkLightsSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, chunkLightsSSBOId);
        glBufferData(GL_SHADER_STORAGE_BUFFER, chunkLightPointers, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, cornersSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, cornersSSBOId);
        glBufferData(GL_SHADER_STORAGE_BUFFER, gigabyte/10, GL_DYNAMIC_DRAW); //0.1gb
        chunkCornerPointers = new int[((((sizeChunks * sizeChunks) + sizeChunks) * heightChunks) + heightChunks) * 4];
        chunkCornerAllocs = new long[((((sizeChunks * sizeChunks) + sizeChunks) * heightChunks) + heightChunks)];

        outerLoop:
        for (int chunkX = 0; chunkX < sizeChunks; chunkX++) {
            for (int chunkZ = 0; chunkZ < sizeChunks; chunkZ++) {
                for (int chunkY = 0; chunkY < heightChunks; chunkY++) {
                    VmaVirtualAllocationCreateInfo allocCreateInfo = VmaVirtualAllocationCreateInfo.create();
                    int condensedChunkPos = condenseChunkPos(chunkX, chunkY, chunkZ);
                    int paletteSize = chunks[condensedChunkPos].getCornerPaletteSize();
                    int bitsPerValue = chunks[condensedChunkPos].bitsPerCorner();
                    int valueMask = chunks[condensedChunkPos].cornerValueMask();
                    int[] compressedCorners = chunks[condensedChunkPos].getCornerData();
                    if (compressedCorners == null) {
                        allocCreateInfo.size((paletteSize) * 4L);
                    } else {
                        allocCreateInfo.size((paletteSize + compressedCorners.length) * 4L);
                    }

                    PointerBuffer alloc = BufferUtils.createPointerBuffer(1);
                    LongBuffer offset = BufferUtils.createLongBuffer(1);
                    long res = vmaVirtualAllocate(corners.get(0), allocCreateInfo, alloc, offset);
                    if (res == VK_SUCCESS) {
                        chunkCornerAllocs[condensedChunkPos] = alloc.get();
                        int pointer = (int) offset.get(0);
                        chunkCornerPointers[condensedChunkPos * 4] = pointer / 4;
                        chunkCornerPointers[(condensedChunkPos * 4) + 1] = paletteSize;
                        chunkCornerPointers[(condensedChunkPos * 4) + 2] = bitsPerValue;
                        chunkCornerPointers[(condensedChunkPos * 4) + 3] = valueMask;
                        glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer, chunks[condensedChunkPos].getCornerPalette());
                        if (compressedCorners != null) {
                            glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer + (paletteSize * 4L), compressedCorners);
                        }
                    } else {
                        System.out.print("cornersSSBO ran out of space! \n");
                        Main.isClosing = true;
                        break outerLoop;
                    }
                }
            }
        }
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkCornersSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, chunkCornersSSBOId);
        glBufferData(GL_SHADER_STORAGE_BUFFER, chunkCornerPointers, GL_DYNAMIC_DRAW);
    }

    public static void init(Window window) throws Exception {
        createGLDebugger();

        scene = new ShaderProgram("scene.vert", new String[]{"math.glsl", "world_reader.glsl", "scene.frag"},
                new String[]{"cam", "renderDistance", "timeOfDay", "time", "selected", "shadowsEnabled", "reflectionShadows", "sun", "ui", "res"});
        generateVao();
        generateTextures(window);
        createBuffers();
        createVMA();
        fillBuffers();
        blurScene = new ShaderProgram("scene.vert", new String[]{"math.glsl", "blur_scene.frag"},
                new String[]{"dir", "res"});
        unscaledScene = new ShaderProgram("scene.vert", new String[]{"math.glsl", "world_reader.glsl", "unscaled_scene.frag"},
                new String[]{"cam", "renderDistance", "aoQuality", "timeOfDay", "time", "selected", "shadowsEnabled", "reflectionShadows", "sun", "ui", "res"});
        reflectionScene = new ShaderProgram("scene.vert", new String[]{"math.glsl", "world_reader.glsl", "reflect_scene.frag"},
                new String[]{"cam", "renderDistance", "aoQuality",  "timeOfDay", "time", "selected", "shadowsEnabled", "reflectionShadows", "sun", "ui", "res"});
        finalScene = new ShaderProgram("scene.vert", new String[]{"math.glsl", "final_scene.frag"},
                new String[]{"cam", "renderDistance", "aoQuality",  "timeOfDay", "time", "selected", "shadowsEnabled", "reflectionShadows", "sun", "ui", "res"});
    }

    public static void  updateUniforms(ShaderProgram program) {
        Matrix4f camMatrix = Main.player.getCameraMatrix();
        glUniformMatrix4fv(program.uniforms.get("cam"), true, new float[]{
                camMatrix.m00(), camMatrix.m10(), camMatrix.m20(), camMatrix.m30(),
                camMatrix.m01(), camMatrix.m11(), camMatrix.m21(), camMatrix.m31(),
                camMatrix.m02(), camMatrix.m12(), camMatrix.m22(), camMatrix.m32(),
                camMatrix.m03(), camMatrix.m13(), camMatrix.m23(), camMatrix.m33()});
        glUniform1i(program.uniforms.get("renderDistance"), 200 + (100 * renderDistanceMul));
        glUniform1i(program.uniforms.get("aoQuality"), aoQuality);
        glUniform1f(program.uniforms.get("timeOfDay"), timeOfDay);
        glUniform1d(program.uniforms.get("time"), time);
        Vector3f selected = World.raycast(new Matrix4f(Main.player.getCameraMatrix()), true, Main.player.reach, false, Main.player.reachAccuracy);
        if (selected == null) {
            selected = new Vector3f(-1000, -1000, -1000);
        }
        glUniform3i(program.uniforms.get("selected"), (int) selected.x, (int) selected.y, (int) selected.z);
        glUniform1i(program.uniforms.get("shadowsEnabled"), shadowsEnabled ? 1 : 0);
        glUniform1i(program.uniforms.get("reflectionShadows"), reflectionShadows ? 1 : 0);
        float halfSize = size / 2f;
        Vector3f sunPos = new Vector3f(halfSize, 0, halfSize);
        sunPos.rotateY((float) time);
        sunPos = new Vector3f(sunPos.x + halfSize, height + 64, sunPos.z + halfSize);
        glUniform3f(program.uniforms.get("sun"), sunPos.x, sunPos.y, sunPos.z);
        glUniform1i(program.uniforms.get("ui"), showUI ? 1 : 0);
    }

    public static LinkedList<Integer> chunkBlockPointerChanges = new LinkedList<>();
    public static LinkedList<Integer> chunkCornerPointerChanges = new LinkedList<>();
    public static LinkedList<Integer> chunkLightPointerChanges = new LinkedList<>();
    public static void updateAtlasTexture() throws IOException {
        if (atlasChanged) {
            atlasChanged = false;

            BufferedImage atlasImage = ImageIO.read(Renderer.class.getClassLoader().getResourceAsStream("assets/base/textures/atlas.png"));
            for (int x = 0; x < atlasWidth; x++) {
                for (int y = 0; y < atlasHeight; y++) {
                    Color color = new Color(atlasImage.getRGB(x, y), true);
                    collisionData[(x*atlasHeight) + y] = color.getAlpha() != 0;
                }
            }
            glBindTexture(GL_TEXTURE_3D, atlasImageId);
            glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA32F, atlasWidth, 64, atlasDepth, 0, GL_RGBA, GL_UNSIGNED_BYTE, Utils.imageToBuffer(atlasImage));
        }
    }
    public static void updateCornerBuffers() {
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, cornersSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, cornersSSBOId);
        while (!chunkCornerQueue.isEmpty()) {
            int condensedChunkPos = chunkCornerQueue.getFirst();
            chunkCornerQueue.removeFirst();

            vmaVirtualFree(corners.get(0), chunkCornerAllocs[condensedChunkPos]);
            VmaVirtualAllocationCreateInfo allocCreateInfo = VmaVirtualAllocationCreateInfo.create();
            int paletteSize = chunks[condensedChunkPos].getCornerPaletteSize();
            int bitsPerValue = chunks[condensedChunkPos].bitsPerCorner();
            int valueMask = chunks[condensedChunkPos].cornerValueMask();
            int[] compressedCorners = chunks[condensedChunkPos].getCornerData();
            if (compressedCorners == null) {
                allocCreateInfo.size((paletteSize) * 4L);
            } else {
                allocCreateInfo.size((paletteSize + compressedCorners.length) * 4L);
            }

            PointerBuffer alloc = BufferUtils.createPointerBuffer(1);
            LongBuffer offset = BufferUtils.createLongBuffer(1);
            long res = vmaVirtualAllocate(corners.get(0), allocCreateInfo, alloc, offset);
            if (res == VK_SUCCESS) {
                chunkCornerAllocs[condensedChunkPos] = alloc.get();
                int pointer = (int) offset.get(0);
                chunkCornerPointers[condensedChunkPos * 4] = pointer / 4;
                chunkCornerPointers[(condensedChunkPos * 4) + 1] = paletteSize;
                chunkCornerPointers[(condensedChunkPos * 4) + 2] = bitsPerValue;
                chunkCornerPointers[(condensedChunkPos * 4) + 3] = valueMask;
                chunkCornerPointerChanges.addLast(condensedChunkPos * 4);
                glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer, chunks[condensedChunkPos].getCornerPalette());
                if (compressedCorners != null) {
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer + (paletteSize * 4L), compressedCorners);
                }
                chunkBlockPointers[(condensedChunkPos * 5) + 4] = chunks[condensedChunkPos].getSubChunks()[0];
                chunkBlockPointerChanges.addLast(condensedChunkPos * 5);
            } else {
                System.out.print("cornersSSBO ran out of space! \n");
                Main.isClosing = true;
                break;
            }
        }
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkCornersSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, chunkCornersSSBOId);
        for (int pos : chunkCornerPointerChanges) {
            glBufferSubData(GL_SHADER_STORAGE_BUFFER, pos * 4L, new int[]{chunkCornerPointers[pos], chunkCornerPointers[pos + 1], chunkCornerPointers[pos + 2], chunkCornerPointers[pos + 3]});
        }
        chunkCornerPointerChanges.clear();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }
    public static void updateLightBuffers() {
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, lightsSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 5, lightsSSBOId);
//            long startTime = System.currentTimeMillis();
//            boolean wasEmpty = lightQueue.isEmpty();
        while (!chunkLightQueue.isEmpty()) {
            int condensedChunkPos = chunkLightQueue.getFirst();
            chunkLightQueue.removeFirst();

            vmaVirtualFree(lights.get(0), chunkLightAllocs[condensedChunkPos]);
            VmaVirtualAllocationCreateInfo allocCreateInfo = VmaVirtualAllocationCreateInfo.create();
            int paletteSize = chunks[condensedChunkPos].getLightPaletteSize();
            int bitsPerValue = chunks[condensedChunkPos].bitsPerLight();
            int valueMask = chunks[condensedChunkPos].lightValueMask();
            int[] compressedLights = chunks[condensedChunkPos].getLightData();
            if (compressedLights == null) {
                allocCreateInfo.size((paletteSize) * 4L);
            } else {
                allocCreateInfo.size((paletteSize + compressedLights.length) * 4L);
            }

            PointerBuffer alloc = BufferUtils.createPointerBuffer(1);
            LongBuffer offset = BufferUtils.createLongBuffer(1);
            long res = vmaVirtualAllocate(lights.get(0), allocCreateInfo, alloc, offset);
            if (res == VK_SUCCESS) {
                chunkLightAllocs[condensedChunkPos] = alloc.get();
                int pointer = (int) offset.get(0);
                chunkLightPointers[condensedChunkPos * 4] = pointer / 4;
                chunkLightPointers[(condensedChunkPos * 4) + 1] = paletteSize;
                chunkLightPointers[(condensedChunkPos * 4) + 2] = bitsPerValue;
                chunkLightPointers[(condensedChunkPos * 4) + 3] = valueMask;
                chunkLightPointerChanges.addLast(condensedChunkPos * 4);
                Chunk chunk = chunks[condensedChunkPos];
                glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer, chunk.getLightPalette());
                if (compressedLights != null) {
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer + (paletteSize * 4L), compressedLights);
                }
                chunkBlockPointers[(condensedChunkPos * 5) + 4] = chunks[condensedChunkPos].getSubChunks()[0];
                chunkBlockPointerChanges.addLast(condensedChunkPos * 5);
            } else {
                System.out.print("lightsSSBO ran out of space! \n");
                Main.isClosing = true;
                break;
            }
        }
//            if (!wasEmpty) {
//                System.out.print(System.currentTimeMillis()-startTime + "ms \n");
//            }
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkLightsSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, chunkLightsSSBOId);
        for (int pos : chunkLightPointerChanges) {
            glBufferSubData(GL_SHADER_STORAGE_BUFFER, pos * 4L, new int[]{chunkLightPointers[pos], chunkLightPointers[pos + 1], chunkLightPointers[pos + 2], chunkLightPointers[pos + 3]});
        }
        chunkLightPointerChanges.clear();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        //Lights end
    }
    public static void updateBlockBuffers(long startTime) {
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, blocksSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, blocksSSBOId);
        while (!chunkBlockQueue.isEmpty()) {
            int condensedChunkPos = chunkBlockQueue.getFirst();
            chunkBlockQueue.removeFirst();
            updateBlock(condensedChunkPos, chunkBlockPointerChanges);
        }
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkBlocksSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, chunkBlocksSSBOId);
        for (int pos : chunkBlockPointerChanges) {
            glBufferSubData(GL_SHADER_STORAGE_BUFFER, pos * 4L, new int[]{chunkBlockPointers[pos], chunkBlockPointers[pos + 1], chunkBlockPointers[pos + 2], chunkBlockPointers[pos + 3], chunkBlockPointers[pos + 4], chunkBlockPointers[pos + 5], chunkBlockPointers[pos + 6]});
        }
        chunkBlockPointerChanges.clear();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkEmptySSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 6, chunkEmptySSBOId);
        if (chunkEmptinessChanged) {
            chunkEmptinessChanged = false;
            glBufferData(GL_SHADER_STORAGE_BUFFER, chunkEmptiness, GL_DYNAMIC_DRAW);
        }
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }

    public static void updatePlayerBuffer() {
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, playerSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 7, playerSSBOId);
        glBufferData(GL_SHADER_STORAGE_BUFFER, Main.player.stack, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }

    public static void updateBuffers() {
        long startTime = System.currentTimeMillis();
        updateCornerBuffers();
        updateLightBuffers();
        updateBlockBuffers(startTime);
        updatePlayerBuffer();
    }

    public static void bindTextures() throws IOException {
        glBindImageTexture(0, atlasImageId, 0, false, 0, GL_READ_ONLY, GL_RGBA32F);
        updateAtlasTexture();
        glBindTextureUnit(1, coherentNoiseId);
        glBindTextureUnit(2, whiteNoiseId);
        glBindTextureUnit(3, cloudNoiseId);
    }

    public static void draw() {
        glBindVertexArray(sceneVaoId);
        glEnableVertexAttribArray(0);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glDisableVertexAttribArray(0);
    }
    public static void drawHalf() {
        glBindVertexArray(sceneHalfVaoId);
        glEnableVertexAttribArray(0);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glDisableVertexAttribArray(0);
    }
    public static void drawOtherHalf() {
        glBindVertexArray(sceneOtherHalfVaoId);
        glEnableVertexAttribArray(0);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glDisableVertexAttribArray(0);
    }
    public static boolean everyOtherFrame = true;
    public static void render(Window window) throws IOException {
        everyOtherFrame = !everyOtherFrame;
        if (!Main.isClosing) {
            if (resized) {
                generateTextures(window);
                resized = false;
            }
            glClearColor(0, 0, 0, 1);
            glClear(GL_COLOR_BUFFER_BIT);
            updateBuffers();

            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            bindTextures();
            if (everyOtherFrame) {
                unscaledScene.bind();
                updateUniforms(unscaledScene);
                glUniform2i(unscaledScene.uniforms.get("res"), window.getWidth(), window.getHeight());
                glBindImageTexture(4, sceneUnscaledImageId, 0, false, 0, GL_READ_WRITE, GL_RGBA32F);

                drawHalf();
                unscaledScene.unbind();

                if (reflectionsEnabled) {
                    reflectionScene.bind();
                    updateUniforms(reflectionScene);
                    glUniform2i(reflectionScene.uniforms.get("res"), window.getWidth(), window.getHeight());
                    drawHalf();
                    reflectionScene.unbind();
                    blurScene.bind();
                    glUniform2f(blurScene.uniforms.get("dir"), 1f, 0f);
                    glUniform2i(blurScene.uniforms.get("res"), window.getWidth(), window.getHeight());
                    drawHalf();
                    glUniform2f(blurScene.uniforms.get("dir"), 0f, 1f);
                    drawHalf();
                    blurScene.unbind();
                }
            }

            finalScene.bind();
            updateUniforms(finalScene);
            glUniform2i(finalScene.uniforms.get("res"), window.getWidth(), window.getHeight());
            draw();
            finalScene.unbind();

            worldChanged = false;
        }
    }

    public static void updateBlock(int condensedChunkPos, LinkedList<Integer> chunkBlockPointerChanges) {
        vmaVirtualFree(blocks.get(0), chunkBlockAllocs[condensedChunkPos]);
        VmaVirtualAllocationCreateInfo allocCreateInfo = VmaVirtualAllocationCreateInfo.create();
        int paletteSize = chunks[condensedChunkPos].getBlockPaletteSize();
        int bitsPerValue = chunks[condensedChunkPos].bitsPerBlock();
        int valueMask = chunks[condensedChunkPos].blockValueMask();
        int[] compressedBlocks = chunks[condensedChunkPos].getBlockData();
        if (compressedBlocks == null) {
            allocCreateInfo.size((paletteSize) * 4L);
        } else {
            allocCreateInfo.size((paletteSize + compressedBlocks.length) * 4L);
        }

        PointerBuffer alloc = BufferUtils.createPointerBuffer(1);
        LongBuffer offset = BufferUtils.createLongBuffer(1);
        long res = vmaVirtualAllocate(blocks.get(0), allocCreateInfo, alloc, offset);
        if (res == VK_SUCCESS) {
            chunkBlockAllocs[condensedChunkPos] = alloc.get();
            int pointer = (int) offset.get(0);
            chunkBlockPointers[condensedChunkPos*5] = pointer / 4;
            chunkBlockPointers[(condensedChunkPos*5) + 1] = paletteSize;
            chunkBlockPointers[(condensedChunkPos*5) + 2] = bitsPerValue;
            chunkBlockPointers[(condensedChunkPos*5) + 3] = valueMask;
            chunkBlockPointers[(condensedChunkPos*5) + 4] = chunks[condensedChunkPos].getSubChunks()[0];
            chunkBlockPointerChanges.addLast(condensedChunkPos*5);
            glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer, chunks[condensedChunkPos].getBlockPalette());
            if (compressedBlocks != null) {
                glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer + (paletteSize * 4L), compressedBlocks);
            }
        } else {
            System.out.print("blocksSSBO ran out of space! \n");
            Main.isClosing = true;
        }
    }
}