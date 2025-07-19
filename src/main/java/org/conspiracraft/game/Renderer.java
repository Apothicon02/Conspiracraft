package org.conspiracraft.game;

import org.conspiracraft.Main;
import org.conspiracraft.game.noise.Noises;
import org.conspiracraft.game.world.Chunk;
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
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.LinkedList;

import static org.conspiracraft.engine.Utils.*;
import static org.conspiracraft.game.Player.selectedBlock;
import static org.conspiracraft.game.world.World.*;
import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public class Renderer {
    public static ShaderProgram scene;
    public static int sceneVaoId;
    public static int sceneFboId;
    public static ShaderProgram finalScene;

    public static int sceneImageId;
    public static int coherentNoiseId;
    public static int whiteNoiseId;
    public static int cloudNoiseId;

    public static int atlasSSBOId;
    public static int chunkBlocksSSBOId;
    public static int blocksSSBOId;
    public static int chunkCornersSSBOId;
    public static int cornersSSBOId;
    public static int chunkLightsSSBOId;
    public static int lightsSSBOId;
    public static int chunkEmptySSBOId;

    public static int camUniform;
    public static int renderDistanceUniform;
    public static int timeOfDayUniform;
    public static int timeUniform;
    public static int selectedUniform;
    public static int uiUniform;
    public static int resUniform;
    public static int shadowsEnabledUniform;
    public static int raytracedCausticsUniform;
    public static int snowingUniform;
    public static int sunUniform;
    public static int cloudsEnabledUniform;
    public static int handUniform;

    public static int resFinalUniform;

    public static int renderDistanceMul = 3; //4
    public static float timeOfDay = 0.5f;
    public static double time = 0.5d;
    public static boolean atlasChanged = true;
    public static boolean worldChanged = false;
    public static boolean[] collisionData = new boolean[(1024*1024)+1024];
    public static boolean showUI = true;
    public static boolean shadowsEnabled = true;
    public static boolean raytracedCaustics = true;
    public static boolean cloudsEnabled = true;
    public static boolean snowing = false;
    public static Vector2i lowRes = new Vector2i(1280, 720);

    public static int defaultSSBOSize = (int) (0.2f*1000000000)*((size/2048)^2);
    public static PointerBuffer blocks = BufferUtils.createPointerBuffer(1);
    public static int[] chunkBlockPointers = new int[((((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks)*5];
    public static long[] chunkBlockAllocs = new long[((((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks)];
    public static PointerBuffer corners = BufferUtils.createPointerBuffer(1);
    public static int[] chunkCornerPointers = new int[((((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks)*4];
    public static long[] chunkCornerAllocs = new long[((((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks)];
    public static PointerBuffer lights = BufferUtils.createPointerBuffer(1);
    public static int[] chunkLightPointers = new int[((((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks)*4];
    public static long[] chunkLightAllocs = new long[((((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks)];

    public static void init(Window window) throws Exception {
        glEnable(GL_DEBUG_OUTPUT);
        glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
        glDebugMessageCallback((source, type, id, severity, length, message, userParam) -> {
            String msg = GLDebug.decode(source, type, id, severity, length, message, userParam);
            if (msg != null) {
                System.out.println(msg);
            }
        }, 0);

        sceneFboId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, sceneFboId);

        scene = new ShaderProgram();
        scene.createVertexShader(readFile("assets/base/shaders/scene.vert"));
        scene.createFragmentShader(readFile("assets/base/shaders/scene.frag"));
        scene.link();

        sceneVaoId = glGenVertexArrays();
        glBindVertexArray(sceneVaoId);
        FloatBuffer verticesBuffer = BufferUtils.createFloatBuffer(117);
        verticesBuffer.put(new float[]{
                -1, -1, 0, 3, -1, 0, -1, 3, 0
        }).flip();
        glBindBuffer(GL_ARRAY_BUFFER, glGenBuffers());
        glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

        sceneImageId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, sceneImageId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA32F, lowRes.x, lowRes.y);
        glBindImageTexture(0, sceneImageId, 0, false, 0, GL_READ_WRITE, GL_RGBA32F);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, sceneImageId, 0);

        coherentNoiseId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, coherentNoiseId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, 2048, 2048, 0, GL_RED, GL_UNSIGNED_BYTE, imageToGrayscaleBuffer(Noises.COHERERENT_NOISE.image));

        whiteNoiseId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, whiteNoiseId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, 1024, 1024, 0, GL_RED, GL_UNSIGNED_BYTE, imageToGrayscaleBuffer(Noises.WHITE_NOISE.image));

        cloudNoiseId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, cloudNoiseId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, 2048, 2048, 0, GL_RED, GL_UNSIGNED_BYTE, imageToGrayscaleBuffer(Noises.CLOUD_NOISE.image));

        Noises.clearImages();

        atlasSSBOId = glCreateBuffers();
        chunkBlocksSSBOId = glCreateBuffers();
        blocksSSBOId = glCreateBuffers();
        chunkCornersSSBOId = glCreateBuffers();
        cornersSSBOId = glCreateBuffers();
        chunkLightsSSBOId = glCreateBuffers();
        lightsSSBOId = glCreateBuffers();
        chunkEmptySSBOId = glCreateBuffers();

        camUniform = glGetUniformLocation(scene.programId, "cam");
        renderDistanceUniform = glGetUniformLocation(scene.programId, "renderDistance");
        timeOfDayUniform = glGetUniformLocation(scene.programId, "timeOfDay");
        timeUniform = glGetUniformLocation(scene.programId, "time");
        selectedUniform = glGetUniformLocation(scene.programId, "selected");
        shadowsEnabledUniform = glGetUniformLocation(scene.programId, "shadowsEnabled");
        raytracedCausticsUniform = glGetUniformLocation(scene.programId, "raytracedCaustics");
        snowingUniform = glGetUniformLocation(scene.programId, "snowing");
        sunUniform = glGetUniformLocation(scene.programId, "sun");
        cloudsEnabledUniform = glGetUniformLocation(scene.programId, "cloudsEnabled");
        handUniform = glGetUniformLocation(scene.programId, "hand");
        uiUniform = glGetUniformLocation(scene.programId, "ui");
        resUniform = glGetUniformLocation(scene.programId, "res");

        VmaVirtualBlockCreateInfo blockCreateInfo = VmaVirtualBlockCreateInfo.create();
        blockCreateInfo.size(defaultSSBOSize);
        long result = vmaCreateVirtualBlock(blockCreateInfo, blocks);
        if (result != VK_SUCCESS) {
            int nothing = 0;
        }
        result = vmaCreateVirtualBlock(blockCreateInfo, corners);
        if (result != VK_SUCCESS) {
            int nothing = 0;
        }
        result = vmaCreateVirtualBlock(blockCreateInfo, lights);
        if (result != VK_SUCCESS) {
            int nothing = 0;
        }

        finalScene = new ShaderProgram();
        finalScene.createVertexShader(readFile("assets/base/shaders/scene.vert"));
        finalScene.createFragmentShader(readFile("assets/base/shaders/final_scene.frag"));
        finalScene.link();

        resFinalUniform = glGetUniformLocation(finalScene.programId, "res");
    }

    public static void render(Window window) throws IOException {
        if (!Main.isClosing) {
            glBindFramebuffer(GL_FRAMEBUFFER, sceneFboId);
            glClearColor(0, 0, 0, 1);
            glClear(GL_COLOR_BUFFER_BIT);

            scene.bind();

            Matrix4f camMatrix = Main.player.getCameraMatrix();
            glUniformMatrix4fv(camUniform, true, new float[]{
                    camMatrix.m00(), camMatrix.m10(), camMatrix.m20(), camMatrix.m30(),
                    camMatrix.m01(), camMatrix.m11(), camMatrix.m21(), camMatrix.m31(),
                    camMatrix.m02(), camMatrix.m12(), camMatrix.m22(), camMatrix.m32(),
                    camMatrix.m03(), camMatrix.m13(), camMatrix.m23(), camMatrix.m33()});
            glUniform1i(renderDistanceUniform, 200 + (100 * renderDistanceMul));
            glUniform1f(timeOfDayUniform, timeOfDay);
            glUniform1d(timeUniform, time);
            Vector3f selected = Main.raycast(new Matrix4f(Main.player.getCameraMatrix()), true, Main.reach, true, Main.reachAccuracy);
            if (selected == null) {
                selected = new Vector3f(-1000, -1000, -1000);
            }
            glUniform3i(selectedUniform, (int) selected.x, (int) selected.y, (int) selected.z);
            glUniform1i(shadowsEnabledUniform, shadowsEnabled ? 1 : 0);
            glUniform1i(raytracedCausticsUniform, raytracedCaustics ? 1 : 0);
            glUniform1i(snowingUniform, snowing ? 1 : 0);
            float halfSize = size / 2f;
            Vector3f sunPos = new Vector3f(size / 4f, 0, size / 4f);
            sunPos.rotateY((float) time);
            sunPos = new Vector3f(sunPos.x + halfSize, height + 64, sunPos.z + halfSize);
            glUniform3f(sunUniform, sunPos.x, sunPos.y, sunPos.z);
            glUniform1i(cloudsEnabledUniform, cloudsEnabled ? 1 : 0);
            glUniform3i(handUniform, selectedBlock.x, selectedBlock.y, selectedBlock.z);
            glUniform1i(uiUniform, showUI ? 1 : 0);
            glUniform2i(resUniform, lowRes.x, lowRes.y);

            if (atlasChanged) {
                atlasChanged = false;
                glBindBuffer(GL_SHADER_STORAGE_BUFFER, atlasSSBOId);
                glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, atlasSSBOId);

                BufferedImage atlasImage = ImageIO.read(Renderer.class.getClassLoader().getResourceAsStream("assets/base/textures/atlas.png"));
                int size = (1024*1024)+1024;
                int[] atlasData = new int[size];
                for (int x = 0; x < 184; x++) {
                    for (int y = 0; y < 1024; y++) {
                        atlasData[(x*1024) + y] = colorToInt(new Color(atlasImage.getRGB(x, y), true));
                        collisionData[(x*1024) + y] = new Color(atlasImage.getRGB(x, y), true).getAlpha() != 0;
                    }
                }

                IntBuffer atlasBuffer = BufferUtils.createIntBuffer(size).put(atlasData).flip();
                glBufferData(GL_SHADER_STORAGE_BUFFER, atlasBuffer, GL_STATIC_DRAW);
                glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
            }

            LinkedList<Integer> chunkBlockPointerChanges = new LinkedList<>();

            //Corners start
            LinkedList<Integer> chunkCornerPointerChanges = new LinkedList<>();
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, cornersSSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, cornersSSBOId);
            if (worldChanged) {
                glBufferData(GL_SHADER_STORAGE_BUFFER, defaultSSBOSize, GL_DYNAMIC_DRAW);
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
                                chunkCornerAllocs[condensedChunkPos] = res;
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

            } else {
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
                        chunkCornerAllocs[condensedChunkPos] = res;
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
            }
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

            glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkCornersSSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, chunkCornersSSBOId);
            if (worldChanged) {
                glBufferData(GL_SHADER_STORAGE_BUFFER, chunkCornerPointers, GL_DYNAMIC_DRAW);
            } else {
                for (int pos : chunkCornerPointerChanges) {
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, pos * 4L, new int[]{chunkCornerPointers[pos], chunkCornerPointers[pos + 1], chunkCornerPointers[pos + 2], chunkCornerPointers[pos + 3]});
                }
                chunkCornerPointerChanges.clear();
            }
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
            //Corners end

            //Lights start
            LinkedList<Integer> chunkLightPointerChanges = new LinkedList<>();
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, lightsSSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 6, lightsSSBOId);
            if (worldChanged) {
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
                                chunkLightAllocs[condensedChunkPos] = res;
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

            } else {
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
                        chunkLightAllocs[condensedChunkPos] = res;
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
            }
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

            glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkLightsSSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 5, chunkLightsSSBOId);
            if (worldChanged) {
                glBufferData(GL_SHADER_STORAGE_BUFFER, chunkLightPointers, GL_DYNAMIC_DRAW);
            } else {
                for (int pos : chunkLightPointerChanges) {
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, pos * 4L, new int[]{chunkLightPointers[pos], chunkLightPointers[pos + 1], chunkLightPointers[pos + 2], chunkLightPointers[pos + 3]});
                }
                chunkLightPointerChanges.clear();
            }
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
            //Lights end

            //Blocks start
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, blocksSSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, blocksSSBOId);
            if (worldChanged) {
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
                                chunkBlockAllocs[condensedChunkPos] = res;
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

            } else {
                while (!chunkBlockQueue.isEmpty()) {
                    int condensedChunkPos = chunkBlockQueue.getFirst();
                    chunkBlockQueue.removeFirst();
                    updateBlock(condensedChunkPos, chunkBlockPointerChanges);
                }
            }
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

            glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkBlocksSSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, chunkBlocksSSBOId);
            if (worldChanged) {
                glBufferData(GL_SHADER_STORAGE_BUFFER, chunkBlockPointers, GL_DYNAMIC_DRAW);
            } else {
                for (int pos : chunkBlockPointerChanges) {
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, pos * 4L, new int[]{chunkBlockPointers[pos], chunkBlockPointers[pos + 1], chunkBlockPointers[pos + 2], chunkBlockPointers[pos + 3], chunkBlockPointers[pos + 4], chunkBlockPointers[pos + 5], chunkBlockPointers[pos + 6]});
                }
                chunkBlockPointerChanges.clear();
            }
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

            glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkEmptySSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 7, chunkEmptySSBOId);
            if (worldChanged) {
                glBufferData(GL_SHADER_STORAGE_BUFFER, chunkEmptiness, GL_DYNAMIC_DRAW);
            } else {
                if (chunkEmptinessChanged) {
                    chunkEmptinessChanged = false;
                    glBufferData(GL_SHADER_STORAGE_BUFFER, chunkEmptiness, GL_DYNAMIC_DRAW);
                }
            }
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
            //Blocks end

            glBindImageTexture(0, sceneImageId, 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F);
            glBindTextureUnit(1, coherentNoiseId);
            glBindTextureUnit(2, whiteNoiseId);
            glBindTextureUnit(3, cloudNoiseId);

            glBindVertexArray(sceneVaoId);
            glEnableVertexAttribArray(0);
            glDrawArrays(GL_TRIANGLES, 0, 3);
            glDisableVertexAttribArray(0);
            scene.unbind();

            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            finalScene.bind();
            glUniform2i(resFinalUniform, window.getWidth(), window.getHeight());
            glBindTextureUnit(0, sceneImageId);

            glBindVertexArray(sceneVaoId);
            glEnableVertexAttribArray(0);
            glDrawArrays(GL_TRIANGLES, 0, 3);
            glDisableVertexAttribArray(0);
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
            chunkBlockAllocs[condensedChunkPos] = res;
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

    public void cleanup() {
        glDeleteVertexArrays(sceneVaoId);
        glDeleteFramebuffers(sceneFboId);
        scene.cleanup();
        finalScene.cleanup();
    }
}