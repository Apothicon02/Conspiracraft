package org.conspiracraft.game.rendering;

import org.conspiracraft.Main;
import org.conspiracraft.game.Noise;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.blocks.types.LightBlockType;
import org.conspiracraft.game.world.Chunk;
import org.conspiracraft.game.world.FluidHelper;
import org.joml.*;
import org.joml.Math;
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
import static org.conspiracraft.game.world.World.*;
import static org.lwjgl.opengl.GL43C.*;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public class Renderer {
    public static ShaderProgram scene;
    public static int sceneVaoId;

    public static int coherentNoiseId;
    public static int whiteNoiseId;

    public static int atlasSSBOId;
    public static int chunkBlocksSSBOId;
    public static int blocksSSBOId;
    public static int chunkCornersSSBOId;
    public static int cornersSSBOId;
    public static int chunkLightsSSBOId;
    public static int lightsSSBOId;

    public static int resUniform;
    public static int camUniform;
    public static int renderDistanceUniform;
    public static int timeOfDayUniform;
    public static int timeUniform;
    public static int selectedUniform;
    public static int uiUniform;
    public static int shadowsEnabledUniform;
    public static int snowingUniform;
    public static int sunUniform;

    public static int renderDistanceMul = 8; //4
    public static float timeOfDay = 0.5f;
    public static double time = 0.5d;
    public static boolean atlasChanged = true;
    public static boolean worldChanged = false;
    public static boolean[] collisionData = new boolean[9984*9984+9984];
    public static boolean showUI = true;
    public static boolean shadowsEnabled = false;
    public static boolean snowing = false;

    public static PointerBuffer blocks = BufferUtils.createPointerBuffer(1);
    public static int[] chunkBlockPointers = new int[((((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks)*4];
    public static long[] chunkBlockAllocs = new long[((((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks)];
    public static PointerBuffer corners = BufferUtils.createPointerBuffer(1);
    public static int[] chunkCornerPointers = new int[((((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks)*4];
    public static long[] chunkCornerAllocs = new long[((((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks)];
    public static PointerBuffer lights = BufferUtils.createPointerBuffer(1);
    public static int[] chunkLightPointers = new int[((((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks)*4];
    public static long[] chunkLightAllocs = new long[((((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks)];

    public static void init(Window window) throws Exception {
        sceneVaoId = glGenVertexArrays();
        glBindVertexArray(sceneVaoId);
        FloatBuffer verticesBuffer = BufferUtils.createFloatBuffer(117);
        verticesBuffer.put(new float[]{
                -1, -1, 0, 3, -1, 0, -1, 3, 0
        }).flip();
        glBindBuffer(GL_ARRAY_BUFFER, glGenBuffers());
        glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

        scene = new ShaderProgram();
        scene.createVertexShader(Utils.readFile("assets/base/shaders/scene.vert"));
        scene.createFragmentShader(Utils.readFile("assets/base/shaders/scene.frag"));
        scene.link();

        coherentNoiseId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, coherentNoiseId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 2048, 2048, 0, GL_RGBA, GL_UNSIGNED_BYTE, Utils.imageToBuffer(Noise.COHERERENT_NOISE));

        whiteNoiseId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, whiteNoiseId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1024, 1024, 0, GL_RGBA, GL_UNSIGNED_BYTE, Utils.imageToBuffer(Noise.WHITE_NOISE));

        atlasSSBOId = glGenBuffers();
        chunkBlocksSSBOId = glGenBuffers();
        blocksSSBOId = glGenBuffers();
        chunkCornersSSBOId = glGenBuffers();
        cornersSSBOId = glGenBuffers();
        chunkLightsSSBOId = glGenBuffers();
        lightsSSBOId = glGenBuffers();

        resUniform = glGetUniformLocation(scene.programId, "res");
        camUniform = glGetUniformLocation(scene.programId, "cam");
        renderDistanceUniform = glGetUniformLocation(scene.programId, "renderDistance");
        timeOfDayUniform = glGetUniformLocation(scene.programId, "timeOfDay");
        timeUniform = glGetUniformLocation(scene.programId, "time");
        selectedUniform = glGetUniformLocation(scene.programId, "selected");
        uiUniform = glGetUniformLocation(scene.programId, "ui");
        shadowsEnabledUniform = glGetUniformLocation(scene.programId, "shadowsEnabled");
        snowingUniform = glGetUniformLocation(scene.programId, "snowing");
        sunUniform = glGetUniformLocation(scene.programId, "sun");

        VmaVirtualBlockCreateInfo blockCreateInfo = VmaVirtualBlockCreateInfo.create();
        blockCreateInfo.size(Integer.MAX_VALUE);
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
        glUniform1i(renderDistanceUniform, 200+(100*renderDistanceMul));
        glUniform1f(timeOfDayUniform, timeOfDay);
        glUniform1d(timeUniform, time);
        Vector3f selected = Main.raycast(new Matrix4f(Main.player.getCameraMatrix()), true, 100, true);
        if (selected == null) {
            selected = new Vector3f(-1000, -1000, -1000);
        }
        glUniform3i(selectedUniform, (int) selected.x, (int) selected.y, (int) selected.z);
        glUniform1i(uiUniform, showUI ? 1 : 0);
        glUniform1i(shadowsEnabledUniform, shadowsEnabled ? 1 : 0);
        glUniform1i(snowingUniform, snowing ? 1 : 0);
        float halfSize = size/2f;
        Vector3f sunPos = new Vector3f(size/4f, 0, size/4f);
        sunPos.rotateY((float) time);
        sunPos = new Vector3f(sunPos.x+halfSize, height, sunPos.z+halfSize);
        glUniform3f(sunUniform, sunPos.x, sunPos.y, sunPos.z);

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
            for (int x = 0; x < 152; x++) {
                for (int y = 0; y < 1088; y++) {
                    atlasData[(9984*x)+y] = Utils.colorToInt(new Color(atlasImage.getRGB(x, y), true));
                    collisionData[(9984*x)+y] = new Color(atlasImage.getRGB(x, y), true).getAlpha() != 0;
                }
            }

            IntBuffer atlasBuffer = BufferUtils.createIntBuffer(size).put(atlasData).flip();
            glBufferData(GL_SHADER_STORAGE_BUFFER, atlasBuffer, GL_STATIC_DRAW);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        }

        //Blocks start
        LinkedList<Integer> chunkBlockPointerChanges = new LinkedList<>();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, blocksSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, blocksSSBOId);
        if (worldChanged) {
            glBufferData(GL_SHADER_STORAGE_BUFFER, Integer.MAX_VALUE, GL_DYNAMIC_DRAW);
            chunkBlockPointers = new int[((((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks)*4];
            chunkBlockAllocs = new long[((((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks)];

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
                            chunkBlockPointers[condensedChunkPos*4] = pointer/4;
                            chunkBlockPointers[(condensedChunkPos*4)+1] = paletteSize;
                            chunkBlockPointers[(condensedChunkPos*4)+2] = bitsPerValue;
                            chunkBlockPointers[(condensedChunkPos*4)+3] = valueMask;
                            glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer, chunks[condensedChunkPos].getBlockPalette());
                            if (compressedBlocks != null) {
                                glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer + (paletteSize * 4L), compressedBlocks);
                            }
                        } else {
                            // Allocation failed - no space for it could be found. Handle this error!
                        }
                    }
                }
            }

        } else {
            while (!blockQueue.isEmpty()) {
                Vector4i blockData = blockQueue.getFirst();
                blockQueue.removeFirst();
                updateBlock(blockData, chunkBlockPointerChanges);
            }
            for (int i = 0; i < Math.min(150, liquidQueue.size()); i++) {
                Vector4i blockData = liquidQueue.getFirst();
                liquidQueue.removeFirst();
                updateBlock(blockData, chunkBlockPointerChanges);
            }
        }
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkBlocksSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, chunkBlocksSSBOId);
        if (worldChanged) {
            glBufferData(GL_SHADER_STORAGE_BUFFER, chunkBlockPointers, GL_DYNAMIC_DRAW);
        } else {
            for (int pos : chunkBlockPointerChanges) {
                glBufferSubData(GL_SHADER_STORAGE_BUFFER, pos*4L, new int[]{chunkBlockPointers[pos], chunkBlockPointers[pos+1], chunkBlockPointers[pos+2], chunkBlockPointers[pos+3]});
            }
            chunkBlockPointerChanges.clear();
        }
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        //Blocks end

        //Corners start
        LinkedList<Integer> chunkCornerPointerChanges = new LinkedList<>();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, cornersSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, cornersSSBOId);
        if (worldChanged) {
            glBufferData(GL_SHADER_STORAGE_BUFFER, Integer.MAX_VALUE, GL_DYNAMIC_DRAW);
            chunkCornerPointers = new int[((((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks)*4];
            chunkCornerAllocs = new long[((((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks)];

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
                            chunkCornerPointers[condensedChunkPos*4] = pointer/4;
                            chunkCornerPointers[(condensedChunkPos*4)+1] = paletteSize;
                            chunkCornerPointers[(condensedChunkPos*4)+2] = bitsPerValue;
                            chunkCornerPointers[(condensedChunkPos*4)+3] = valueMask;
                            glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer, chunks[condensedChunkPos].getCornerPalette());
                            if (compressedCorners != null) {
                                glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer + (paletteSize * 4L), compressedCorners);
                            }
                        } else {
                            // Allocation failed - no space for it could be found. Handle this error!
                        }
                    }
                }
            }

        } else {
            while (!cornerQueue.isEmpty()) {
                Vector4i cornerData = cornerQueue.getFirst();
                cornerQueue.removeFirst();
                Vector3i pos = new Vector3i(cornerData.x, cornerData.y, cornerData.z);
                Vector4i oldLight = getLight(pos);
                byte r = 0;
                byte g = 0;
                byte b = 0;
                if (BlockTypes.blockTypeMap.get(getBlock(pos).x) instanceof LightBlockType lType) {
                    r = lType.r;
                    g = lType.g;
                    b = lType.b;
                    updateLight(pos);
                }
                Vector3i chunkPos = new Vector3i(pos.x / chunkSize, pos.y / chunkSize, pos.z / chunkSize);
                int condensedChunkPos = condenseChunkPos(chunkPos);
                int localPos = condenseLocalPos(pos.x - (chunkPos.x * chunkSize), pos.y - (chunkPos.y * chunkSize), pos.z - (chunkPos.z * chunkSize));
                Chunk chunk = chunks[condensedChunkPos];
                chunk.setCorner(localPos, cornerData.w, pos);
                chunk.setLight(localPos, r, g, b, 0, pos);
                updateHeightmap(pos.x, pos.z, true);
                recalculateLight(pos, oldLight.x, oldLight.y, oldLight.z, oldLight.w);

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
                    chunkCornerPointerChanges.addLast(condensedChunkPos*4);
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer, chunks[condensedChunkPos].getCornerPalette());
                    if (compressedCorners != null) {
                        glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer + (paletteSize * 4L), compressedCorners);
                    }
                } else {
                    // Allocation failed - no space for it could be found. Handle this error!
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
                glBufferSubData(GL_SHADER_STORAGE_BUFFER, pos*4L, new int[]{chunkCornerPointers[pos], chunkCornerPointers[pos+1], chunkCornerPointers[pos+2], chunkCornerPointers[pos+3]});
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
            glBufferData(GL_SHADER_STORAGE_BUFFER, Integer.MAX_VALUE, GL_DYNAMIC_DRAW);
            chunkLightPointers = new int[((((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks)*4];
            chunkLightAllocs = new long[((((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks)];

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
                            chunkLightPointers[condensedChunkPos*4] = pointer/4;
                            chunkLightPointers[(condensedChunkPos*4)+1] = paletteSize;
                            chunkLightPointers[(condensedChunkPos*4)+2] = bitsPerValue;
                            chunkLightPointers[(condensedChunkPos*4)+3] = valueMask;
                            glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer, chunks[condensedChunkPos].getLightPalette());
                            if (compressedLights != null) {
                                glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer + (paletteSize * 4L), compressedLights);
                            }
                        } else {
                            // Allocation failed - no space for it could be found. Handle this error!
                        }
                    }
                }
            }

        } else {
            while (!lightQueue.isEmpty()) {
                Vector3i lightData = lightQueue.getFirst();
                lightQueue.removeFirst();
                Vector3i chunkPos = new Vector3i(lightData.x / chunkSize, lightData.y / chunkSize, lightData.z / chunkSize);
                int condensedChunkPos = condenseChunkPos(chunkPos);
                updateLight(lightData);

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
                    chunkLightPointerChanges.addLast(condensedChunkPos*4);
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer, chunks[condensedChunkPos].getLightPalette());
                    if (compressedLights != null) {
                        glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer + (paletteSize * 4L), compressedLights);
                    }
                } else {
                    // Allocation failed - no space for it could be found. Handle this error!
                }
            }
        }
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkLightsSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 5, chunkLightsSSBOId);
        glBufferData(GL_SHADER_STORAGE_BUFFER, chunkLightPointers, GL_DYNAMIC_DRAW);
        if (worldChanged) {
            glBufferData(GL_SHADER_STORAGE_BUFFER, chunkLightPointers, GL_DYNAMIC_DRAW);
        } else {
            for (int pos : chunkLightPointerChanges) {
                glBufferSubData(GL_SHADER_STORAGE_BUFFER, pos*4L, new int[]{chunkLightPointers[pos], chunkLightPointers[pos+1], chunkLightPointers[pos+2], chunkLightPointers[pos+3]});
            }
            chunkLightPointerChanges.clear();
        }
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        //Lights end

        glBindVertexArray(sceneVaoId);
        glEnableVertexAttribArray(0);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glDisableVertexAttribArray(0);

        scene.unbind();
        worldChanged = false;
    }

    public static void updateBlock(Vector4i blockData, LinkedList<Integer> chunkBlockPointerChanges) {
        Vector3i pos = new Vector3i(blockData.x, blockData.y, blockData.z);
        Vector4i oldLight = getLight(pos);
        byte r = 0;
        byte g = 0;
        byte b = 0;
        Vector2i blockId = Utils.unpackInt(blockData.w());
        if (BlockTypes.blockTypeMap.get(blockId.x) instanceof LightBlockType lType) {
            r = lType.r;
            g = lType.g;
            b = lType.b;
            updateLight(pos);
        }
        Vector3i chunkPos = new Vector3i(pos.x / chunkSize, pos.y / chunkSize, pos.z / chunkSize);
        int condensedChunkPos = condenseChunkPos(chunkPos);
        int localPos = condenseLocalPos(pos.x - (chunkPos.x * chunkSize), pos.y - (chunkPos.y * chunkSize), pos.z - (chunkPos.z * chunkSize));
        Chunk chunk = chunks[condensedChunkPos];
        Vector2i newBlockId = FluidHelper.updateFluid(pos, blockId);
        chunk.setBlock(localPos, newBlockId, pos);
        chunk.setLight(localPos, r, g, b, 0, pos);
        updateHeightmap(blockData.x, blockData.z, true);
        recalculateLight(pos, oldLight.x, oldLight.y, oldLight.z, oldLight.w);

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
            chunkBlockPointers[condensedChunkPos * 4] = pointer / 4;
            chunkBlockPointers[(condensedChunkPos * 4) + 1] = paletteSize;
            chunkBlockPointers[(condensedChunkPos * 4) + 2] = bitsPerValue;
            chunkBlockPointers[(condensedChunkPos * 4) + 3] = valueMask;
            chunkBlockPointerChanges.addLast(condensedChunkPos*4);
            glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer, chunks[condensedChunkPos].getBlockPalette());
            if (compressedBlocks != null) {
                glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer + (paletteSize * 4L), compressedBlocks);
            }
        } else {
            // Allocation failed - no space for it could be found. Handle this error!
        }
    }

    public void cleanup() {
        glDeleteVertexArrays(sceneVaoId);
        scene.cleanup();
    }
}