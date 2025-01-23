package org.conspiracraft.game.rendering;

import org.conspiracraft.Main;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.blocks.types.LightBlockType;
import org.conspiracraft.game.world.World;
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

    public static int atlasSSBOId;
    public static int chunkBlocksSSBOId;
    public static int blocksSSBOId;

    public static int resUniform;
    public static int camUniform;
    public static int renderDistanceUniform;

    public static int renderDistanceMul = 20; //4
    public static float timeOfDay = 0.5f;
    public static double time = 0.5d;
    public static boolean atlasChanged = true;
    public static boolean worldChanged = false;
    public static boolean[] collisionData = new boolean[9984*9984+9984];
    public static boolean showUI = true;

    public static PointerBuffer blocks = BufferUtils.createPointerBuffer(1);
    public static int[] chunkPointers = new int[((((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks)*2];
    public static long[] chunkAllocs = new long[((((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks)];

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

        atlasSSBOId = glGenBuffers();
        chunkBlocksSSBOId = glGenBuffers();
        blocksSSBOId = glGenBuffers();

        resUniform = glGetUniformLocation(scene.programId, "res");
        camUniform = glGetUniformLocation(scene.programId, "cam");
        renderDistanceUniform = glGetUniformLocation(scene.programId, "renderDistance");

        VmaVirtualBlockCreateInfo blockCreateInfo = VmaVirtualBlockCreateInfo.create();
        blockCreateInfo.size(Integer.MAX_VALUE);
        long result = vmaCreateVirtualBlock(blockCreateInfo, blocks);
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

        if (atlasChanged) {
            atlasChanged = false;
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, atlasSSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, atlasSSBOId);

            BufferedImage atlasImage = ImageIO.read(Renderer.class.getClassLoader().getResourceAsStream("assets/base/textures/atlas.png"));
            int size = 9984*9984+9984;
            int[] atlasData = new int[size];
            for (int x = 0; x < 144; x++) {
                for (int y = 0; y < 256; y++) {
                    atlasData[(9984*x)+y] = Utils.colorToInt(new Color(atlasImage.getRGB(x, y), true));
                    collisionData[(9984*x)+y] = new Color(atlasImage.getRGB(x, y), true).getAlpha() != 0;
                }
            }

            IntBuffer atlasBuffer = BufferUtils.createIntBuffer(size).put(atlasData).flip();
            glBufferData(GL_SHADER_STORAGE_BUFFER, atlasBuffer, GL_STATIC_DRAW);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        }

        LinkedList<Integer> chunkPointerChanges = new LinkedList<>();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, blocksSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, blocksSSBOId);
        if (worldChanged) {
            glBufferData(GL_SHADER_STORAGE_BUFFER, Integer.MAX_VALUE, GL_DYNAMIC_DRAW);
            chunkPointers = new int[((((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks)*2];
            chunkAllocs = new long[((((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks)];

            for (int chunkX = 0; chunkX < sizeChunks; chunkX++) {
                for (int chunkZ = 0; chunkZ < sizeChunks; chunkZ++) {
                    for (int chunkY = 0; chunkY < heightChunks; chunkY++) {
                        VmaVirtualAllocationCreateInfo allocCreateInfo = VmaVirtualAllocationCreateInfo.create();
                        int condensedChunkPos = condenseChunkPos(chunkX, chunkY, chunkZ);
                        int paletteSize = chunks[condensedChunkPos].getBlockPaletteSize();
                        int[] compressedBlocks = chunks[condensedChunkPos].getBlockData();
                        allocCreateInfo.size((paletteSize+compressedBlocks.length)*4L);

                        PointerBuffer alloc = BufferUtils.createPointerBuffer(1);
                        LongBuffer offset = BufferUtils.createLongBuffer(1);
                        long res = vmaVirtualAllocate(blocks.get(0), allocCreateInfo, alloc, offset);
                        if (res == VK_SUCCESS) {
                            chunkAllocs[condensedChunkPos] = res;
                            int pointer = (int) offset.get(0);
                            chunkPointers[condensedChunkPos*2] = pointer/4;
                            chunkPointers[(condensedChunkPos*2)+1] = paletteSize;
                            chunkPointerChanges.addLast(condensedChunkPos*2);
                            glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer, chunks[condensedChunkPos].getBlockPalette());
                            glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer+(paletteSize*4L), compressedBlocks);
                        } else {
                            // Allocation failed - no space for it could be found. Handle this error!
                        }
                    }
                }
            }

        } else {
            for (int i = 0; i < Math.min(Math.min(Engine.fps, 100), blockQueue.size()); i++) {
                Vector4i blockData = blockQueue.getFirst();
                blockQueue.removeFirst();
                Vector3i pos = new Vector3i(blockData.x, blockData.y, blockData.z);
//                Vector2i oldBlock = getBlock(pos);
//                byte r = 0;
//                byte g = 0;
//                byte b = 0;
//                if (BlockTypes.blockTypeMap.get(Utils.unpackInt(blockData.w()).x) instanceof LightBlockType lType) {
//                    r = lType.r;
//                    g = lType.g;
//                    b = lType.b;
//                    updateLight(pos);
//                }
                Vector3i chunkPos = new Vector3i(pos.x / chunkSize, pos.y / chunkSize, pos.z / chunkSize);
                int condensedChunkPos = condenseChunkPos(chunkPos);
                int localPos = condenseLocalPos(pos.x - (chunkPos.x * chunkSize), pos.y - (chunkPos.y * chunkSize), pos.z - (chunkPos.z * chunkSize));
                chunks[condensedChunkPos].setBlock(localPos, Utils.unpackInt(blockData.w), pos);
//                updateHeightmap(blockData.x, blockData.z, true);
//                recalculateLight(pos, oldBlock.r(), oldBlock.g(), oldBlock.b(), oldBlock.s());

                vmaVirtualFree(blocks.get(0), chunkAllocs[condensedChunkPos]);
                VmaVirtualAllocationCreateInfo allocCreateInfo = VmaVirtualAllocationCreateInfo.create();
                int paletteSize = chunks[condensedChunkPos].getBlockPaletteSize();
                int[] compressedBlocks = chunks[condensedChunkPos].getBlockData();
                allocCreateInfo.size((paletteSize + compressedBlocks.length) * 4L);

                PointerBuffer alloc = BufferUtils.createPointerBuffer(1);
                LongBuffer offset = BufferUtils.createLongBuffer(1);
                long res = vmaVirtualAllocate(blocks.get(0), allocCreateInfo, alloc, offset);
                if (res == VK_SUCCESS) {
                    chunkAllocs[condensedChunkPos] = res;
                    int pointer = (int) offset.get(0);
                    chunkPointers[condensedChunkPos * 2] = pointer / 4;
                    chunkPointers[(condensedChunkPos * 2) + 1] = paletteSize;
                    chunkPointerChanges.addLast(condensedChunkPos*2);
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer, chunks[condensedChunkPos].getBlockPalette());
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer + (paletteSize * 4L), compressedBlocks);
                } else {
                    // Allocation failed - no space for it could be found. Handle this error!
                }
            }
        }
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkBlocksSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, chunkBlocksSSBOId);
        if (worldChanged) {
            glBufferData(GL_SHADER_STORAGE_BUFFER, chunkPointers, GL_DYNAMIC_DRAW);
        } else {
            for (int i : chunkPointerChanges) {
                glBufferSubData(GL_SHADER_STORAGE_BUFFER, i, new int[]{chunkPointers[i], chunkPointers[i+1]});
            }
            chunkPointerChanges.clear();
        }
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        glBindVertexArray(sceneVaoId);
        glEnableVertexAttribArray(0);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glDisableVertexAttribArray(0);

        scene.unbind();
        worldChanged = false;
    }

    public void cleanup() {
        glDeleteVertexArrays(sceneVaoId);
        scene.cleanup();
    }
}