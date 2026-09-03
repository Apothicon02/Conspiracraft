package org.conspiracraft.graphics;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import org.apache.commons.math3.random.HaltonSequenceGenerator;
import org.conspiracraft.Constants;
import org.conspiracraft.Settings;
import org.conspiracraft.blocks.Materials;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.effects.Effect;
import org.conspiracraft.effects.Lightning;
import org.conspiracraft.effects.Particle;
import org.conspiracraft.entities.Entity;
import org.conspiracraft.entities.EntityTypes;
import org.conspiracraft.graphics.buffers.Buffer;
import org.conspiracraft.gui.GUI;
import org.conspiracraft.Main;
import org.conspiracraft.graphics.buffers.ubos.PushUBO;
import org.conspiracraft.graphics.models.Index;
import org.conspiracraft.graphics.models.Models;
import org.conspiracraft.graphics.models.Vertex;
import org.conspiracraft.graphics.textures.Texture;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.types.ItemTypes;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.graphics.buffers.CmdBufferHelper;
import org.conspiracraft.graphics.textures.ImageHelper;
import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.world.Chunk;
import org.conspiracraft.world.LightHelper;
import org.conspiracraft.space.StarSystem;
import org.conspiracraft.world.World;
import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaVirtualAllocationCreateInfo;
import org.lwjgl.util.vma.VmaVirtualBlockCreateInfo;
import org.lwjgl.vulkan.*;

import java.io.IOException;
import java.lang.Math;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.conspiracraft.Main.player;
import static org.conspiracraft.graphics.Graphics.*;
import static org.conspiracraft.graphics.Graphics.indexBuf;
import static org.conspiracraft.graphics.Pipelines.*;
import static org.conspiracraft.graphics.buffers.CmdBuffer.cmdBuffers;
import static org.conspiracraft.graphics.Device.*;
import static org.conspiracraft.graphics.Swapchain.*;
import static org.conspiracraft.graphics.SyncObjects.*;
import static org.conspiracraft.world.Chunk.lodsPerChunk;
import static org.conspiracraft.world.World.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.KHRFragmentShadingRate.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRSynchronization2.VK_PIPELINE_STAGE_2_FRAGMENT_SHADING_RATE_ATTACHMENT_BIT_KHR;
import static org.lwjgl.vulkan.VK14.*;

public class Renderer {

    public static int imageIdx = 0;
    public static int frameIdx = 0;
    public static boolean firstImages = true;
    public static boolean initialized = false;
    public static boolean clearedDepth = false;
    public static VkCommandBuffer currentCmdBuffer;
    public static PushUBO pushUBO = new PushUBO();
    public static Pipeline currentPipeline;
    public static ComputePipeline currentComputePipeline;
    public static boolean reloadAtlas = false;
    public static boolean reloadTextures = true;
    public static int jitterFrame = 0;
    public static HaltonSequenceGenerator halton = new HaltonSequenceGenerator(2);
    public static float[] xOffsets = new float[8];
    public static float[] yOffsets = new float[8];
    public static final Vector3f viewPos = new Vector3f();
    public static final Vector3f modelOffset = new Vector3f();
    public static void render() throws Exception {
        if (!initialized && !LightHelper.lightQueue.isEmpty()) {return;}
        try (MemoryStack stack = MemoryStack.stackPush()) {
            boolean drawStuff = true;
            if (startCommandBuffers(stack)) {
                clearedDepth = false;
                if (!initialized) {
                    //System.out.println("atlas = " + Long.toHexString(Textures.atlas.image));System.out.println("noises = " + Long.toHexString(Textures.noises.image));System.out.println("colors1 = " + Long.toHexString(Textures.colors1.image));System.out.println("depth1 = " + Long.toHexString(Textures.depth1.image));System.out.println("norms1 = " + Long.toHexString(Textures.norms1.image));System.out.println("colors2 = " + Long.toHexString(Textures.colors2.image));System.out.println("depth2 = " + Long.toHexString(Textures.depth2.image));System.out.println("norms2 = " + Long.toHexString(Textures.norms2.image));System.out.println("gui = " + Long.toHexString(Textures.gui.image));System.out.println("items = " + Long.toHexString(Textures.items.image));System.out.println("entities = " + Long.toHexString(Textures.entities.image));System.out.println("blurred_horizontally = " + Long.toHexString(Textures.blurred_horizontally.image));System.out.println("blurred = " + Long.toHexString(Textures.blurred.image));System.out.println("blueNoise = " + Long.toHexString(Textures.blueNoise.image));System.out.println("vrs = " + Long.toHexString(Textures.vrs.image));System.out.println("colorsOld = " + Long.toHexString(Textures.colorsOld.image));
                    generating = false;
                    for (int i = 0; i < xOffsets.length; i++) {
                        double[] haltonVec = halton.nextVector();
                        xOffsets[i] = (float) (haltonVec[0]-0.5f)*0.25f;
                        yOffsets[i] = (float) (haltonVec[1]-0.5f)*0.25f;
                    }
                    fillSSBOs();
                    if (reloadTextures) {
                        reloadTextures(stack);
                    }
                    initialized = true;
                } else {
                    //long startTime = System.nanoTime();
                    //boolean wasEmpty = updateQueue.isEmpty();
                    long startTime = System.currentTimeMillis();
                    while (!updateQueue.isEmpty()) {
                        long chunkPos = updateQueue.pollFirst();
                        updateChunk(chunkPos);
                        synchronized (lock) {
                            updateSet.remove(chunkPos);
                        }
                    }
                    //if (!wasEmpty) {System.out.println("SSBO uploads took " + String.format("%.2f", (System.nanoTime() - startTime)/1000000.d) + "ms");}
                    ssboBarriers();
                    if (reloadTextures) {
                        reloadTextures(stack);
                        drawStuff = false;
                    } else if (reloadAtlas) {
                        reloadAtlas = false;
                        startTime = System.currentTimeMillis();
                        Materials.fillTexture(stack);
                        BlockTypes.fillTexture(stack);
                        //atlasBarriers();
                        System.out.println("Atlas reloading took "+(System.currentTimeMillis()-startTime)+"ms");
                    }
                }
                if (drawStuff) {
                    viewPos.set(Main.player.getCameraTranslationInterpolated());
                    globalUBO.update(stack);
                    globalUBO.push(stack);
                    vkCmdBindDescriptorSets(currentCmdBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, stack.longs(Descriptors.descriptorSet), null);
                    vkCmdBindDescriptorSets(currentCmdBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, pipelineLayout, 0, stack.longs(Descriptors.descriptorSet), null);

                    VkDebugUtilsLabelEXT labelInfo = VkDebugUtilsLabelEXT.calloc(stack);
                    labelInfo.sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_LABEL_EXT);
                    labelInfo.pLabelName(MemoryUtil.memUTF8("Rstr"));
                    EXTDebugUtils.vkCmdBeginDebugUtilsLabelEXT(currentCmdBuffer, labelInfo);
                    drawRaster(stack);
                    EXTDebugUtils.vkCmdEndDebugUtilsLabelEXT(currentCmdBuffer);
                    labelInfo = VkDebugUtilsLabelEXT.calloc(stack);
                    labelInfo.sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_LABEL_EXT);
                    labelInfo.pLabelName(MemoryUtil.memUTF8("DDA"));
                    EXTDebugUtils.vkCmdBeginDebugUtilsLabelEXT(currentCmdBuffer, labelInfo);
                    drawDDA(stack);
                    EXTDebugUtils.vkCmdEndDebugUtilsLabelEXT(currentCmdBuffer);
                    labelInfo = VkDebugUtilsLabelEXT.calloc(stack);
                    labelInfo.sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_LABEL_EXT);
                    labelInfo.pLabelName(MemoryUtil.memUTF8("AO"));
                    EXTDebugUtils.vkCmdBeginDebugUtilsLabelEXT(currentCmdBuffer, labelInfo);
                    drawSSAO(stack);
                    EXTDebugUtils.vkCmdEndDebugUtilsLabelEXT(currentCmdBuffer);
                    labelInfo = VkDebugUtilsLabelEXT.calloc(stack);
                    labelInfo.sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_LABEL_EXT);
                    labelInfo.pLabelName(MemoryUtil.memUTF8("AA"));
                    EXTDebugUtils.vkCmdBeginDebugUtilsLabelEXT(currentCmdBuffer, labelInfo);
                    drawAA(stack);
                    EXTDebugUtils.vkCmdEndDebugUtilsLabelEXT(currentCmdBuffer);
                    labelInfo = VkDebugUtilsLabelEXT.calloc(stack);
                    labelInfo.sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_LABEL_EXT);
                    labelInfo.pLabelName(MemoryUtil.memUTF8("Blur"));
                    EXTDebugUtils.vkCmdBeginDebugUtilsLabelEXT(currentCmdBuffer, labelInfo);
                    drawBlur(stack);
                    EXTDebugUtils.vkCmdEndDebugUtilsLabelEXT(currentCmdBuffer);
                    labelInfo = VkDebugUtilsLabelEXT.calloc(stack);
                    labelInfo.sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_LABEL_EXT);
                    labelInfo.pLabelName(MemoryUtil.memUTF8("GUI"));
                    EXTDebugUtils.vkCmdBeginDebugUtilsLabelEXT(currentCmdBuffer, labelInfo);
                    drawGUI(stack);
                    EXTDebugUtils.vkCmdEndDebugUtilsLabelEXT(currentCmdBuffer);

                    pushUBO.updateTex(Textures.colors1);
                    pushUBO.push();
                    bindPresentImage(stack);
                    vkCmdDraw(currentCmdBuffer, 3, 1, 0, 0);
                    unbindPresentImage(stack);
                } else {
                    bindPresentImage(stack);
                    unbindPresentImage(stack);
                }
                submitCommandBuffers(stack);
                if (Settings.taaEnabled) {
                    Main.window.jitterX=(xOffsets[jitterFrame]/Settings.width);
                    Main.window.jitterY=(yOffsets[jitterFrame]/Settings.height);
                    jitterFrame++;
                    if (jitterFrame >= xOffsets.length) {jitterFrame = 0;}
                }
            }
        }
    }

    public static void reloadTextures(MemoryStack stack) throws IOException {
        reloadTextures = false;
        long startTime = System.currentTimeMillis();
        Materials.fillTexture(stack);
        BlockTypes.fillTexture(stack);
        ByteBuffer noisesBuffer = Utils.imageToBuffer(Utils.loadImage("generic/texture/coherent_noise"));
        ImageHelper.fillImage(stack, Textures.noises, noisesBuffer);
        memFree(noisesBuffer);
        ByteBuffer blueNoiseBuffer = Utils.imageToBuffer(Utils.loadImage("generic/texture/blue_noise"));
        ImageHelper.fillImage(stack, Textures.blueNoise, blueNoiseBuffer);
        memFree(blueNoiseBuffer);
        GUI.fillTexture();
        EntityTypes.fillTexture(stack);
        System.out.println("Texture initialization took " + (System.currentTimeMillis() - startTime) + "ms");
    }
    public static void updateChunk(long packedChunkPos) {
        Chunk chunk = getChunk(packedChunkPos);
        Vector3i chunkPos = new Vector3i(chunk.cXI, chunk.cYI, chunk.cZI);
        long wrappedPackedChunkPos = ((((chunk.cX%sizeChunks)*sizeChunks)+(chunk.cZ%sizeChunks))*heightChunks)+(chunk.cY%heightChunks);
        updateChunkBlocks(wrappedPackedChunkPos, chunkPos, packedChunkPos, chunk);
        updateChunkLights(wrappedPackedChunkPos, chunkPos, packedChunkPos, chunk);
    }
    public static final int lodsByteSize = lodsPerChunk*8;
    public static void updateChunkBlocks(long wPackedChunkPos, Vector3i chunkPos, long packedChunkPos, Chunk chunk) {
        long chunkPtr = chunkSSBO.stagingBuffer.pointer.get(0);
        long voxelPtr = voxelSSBO.stagingBuffer.pointer.get(0);
        if (initialized) {
            vmaVirtualFree(blocks.get(0), chunkBlockAllocs.get(packedChunkPos));
        }
        VmaVirtualAllocationCreateInfo allocCreateInfo = VmaVirtualAllocationCreateInfo.create();
        allocCreateInfo.alignment(4);
        int paletteSize = chunk.getBlockPaletteSize();
        int bitsPerValue = chunk.bitsPerBlock();
        int valueMask = chunk.blockValueMask();
        int[] compressedBlocks = chunk.getBlockData();
        if (compressedBlocks == null) {
            allocCreateInfo.size((paletteSize) * 4L);
        } else {
            allocCreateInfo.size(((paletteSize + compressedBlocks.length) * 4L)+lodsByteSize);
        }

        PointerBuffer alloc = BufferUtils.createPointerBuffer(1);
        LongBuffer offset = BufferUtils.createLongBuffer(1);
        long res = vmaVirtualAllocate(blocks.get(0), allocCreateInfo, alloc, offset);
        if (res == VK_SUCCESS) {
            chunkBlockAllocs.put(packedChunkPos, alloc.get());
            int pointer = (int) offset.get(0);
            long chunkBufOffset = (long)wPackedChunkPos*chunkByteSize;
            MemoryUtil.memIntBuffer(chunkPtr+chunkBufOffset, 4)
                    .put(0, pointer/4).put(1, paletteSize).put(2, bitsPerValue).put(3, valueMask);
            MemoryUtil.memIntBuffer(voxelPtr+pointer, paletteSize)
                    .put(0, chunk.getBlockPalette());
            if (compressedBlocks != null) {
                MemoryUtil.memIntBuffer(voxelPtr+pointer+(paletteSize*4L), compressedBlocks.length)
                        .put(0, compressedBlocks);
                MemoryUtil.memLongBuffer(voxelPtr+pointer+((paletteSize+compressedBlocks.length)*4L), lodsPerChunk)
                        .put(0, chunk.getLodData());
            }
            if (initialized) {
                updateRegion(World.packRegionPos(new Vector3i(chunkPos).div(regionSizeChunks)));
                VkBufferCopy.Buffer chunkBufferCopy = VkBufferCopy.calloc(1).srcOffset(chunkBufOffset).dstOffset(chunkBufOffset).size(16L);
                vkCmdCopyBuffer(currentCmdBuffer, chunkSSBO.stagingBuffer.buffer[0], chunkSSBO.buffer.buffer[0], chunkBufferCopy);
                if (compressedBlocks != null) {
                    VkBufferCopy.Buffer voxelBufferCopy = VkBufferCopy.calloc(1).srcOffset(pointer).dstOffset(pointer).size(((paletteSize + compressedBlocks.length) * 4L)+lodsByteSize);
                    vkCmdCopyBuffer(currentCmdBuffer, voxelSSBO.stagingBuffer.buffer[0], voxelSSBO.buffer.buffer[0], voxelBufferCopy);
                }
//                Vector3i ogLodPos = new Vector3i(chunkPos).mul(lodSize);
//                for (int z = ogLodPos.z(); z < ogLodPos.z() + lodSize; z++) {
//                    for (int x = ogLodPos.x(); x < ogLodPos.x() + lodSize; x++) {
//                        for (int y = ogLodPos.y(); y < ogLodPos.y() + lodSize; y++) {
//                            updateLOD(World.packLodPos(new Vector3i(x, y, z)));
//                        }
//                    }
//                }
            }
        } else {
            System.out.print("blocksSSBO ran out of space! \n");
            Main.isClosing = true;
        }
    }
    public static long allocated = 0;
    public static void updateChunkLights(long wPackedChunkPos, Vector3i chunkPos, long packedChunkPos, Chunk chunk) {
        long chunkPtr = lightChunkSSBO.stagingBuffer.pointer.get(0);
        long lightPtr = lightSSBO.stagingBuffer.pointer.get(0);
        if (initialized) {
            vmaVirtualFree(lights.get(0), chunkLightBlockAllocs.get(packedChunkPos));
        }
        VmaVirtualAllocationCreateInfo allocCreateInfo = VmaVirtualAllocationCreateInfo.create();
        allocCreateInfo.alignment(4);
        int paletteSize = chunk.getLightPaletteSize();
        int bitsPerValue = chunk.bitsPerLight();
        int valueMask = chunk.lightValueMask();
        int[] compressedLights = chunk.getLightData();
        if (compressedLights == null) {
            allocCreateInfo.size((paletteSize) * 4L);
        } else {
            allocCreateInfo.size((paletteSize + compressedLights.length) * 4L);
        }
        allocated += allocCreateInfo.size();

        PointerBuffer alloc = BufferUtils.createPointerBuffer(1);
        LongBuffer offset = BufferUtils.createLongBuffer(1);
        long res = vmaVirtualAllocate(lights.get(0), allocCreateInfo, alloc, offset);
        if (res == VK_SUCCESS) {
            chunkLightBlockAllocs.put(packedChunkPos, alloc.get());
            int pointer = (int) offset.get(0);
            long chunkBufOffset = (long)wPackedChunkPos*chunkByteSize;
            MemoryUtil.memIntBuffer(chunkPtr+chunkBufOffset, 4)
                    .put(0, pointer/4).put(1, paletteSize).put(2, bitsPerValue).put(3, valueMask);
            MemoryUtil.memIntBuffer(lightPtr+pointer, paletteSize)
                    .put(0, chunk.getLightPalette());
            if (compressedLights != null) {
                MemoryUtil.memIntBuffer(lightPtr+pointer+(paletteSize*4L), compressedLights.length)
                        .put(0, compressedLights);
            }
            if (initialized) {
                VkBufferCopy.Buffer chunkBufferCopy = VkBufferCopy.calloc(1).srcOffset(chunkBufOffset).dstOffset(chunkBufOffset).size(16L);
                vkCmdCopyBuffer(currentCmdBuffer, lightChunkSSBO.stagingBuffer.buffer[0], lightChunkSSBO.buffer.buffer[0], chunkBufferCopy);
                if (compressedLights != null) {
                    VkBufferCopy.Buffer lightBufferCopy = VkBufferCopy.calloc(1).srcOffset(pointer).dstOffset(pointer).size((paletteSize + compressedLights.length) * 4L);
                    vkCmdCopyBuffer(currentCmdBuffer, lightSSBO.stagingBuffer.buffer[0], lightSSBO.buffer.buffer[0], lightBufferCopy);
                }
            }
        } else {
            System.out.println("lightsSSBO ran out of space with "+allocated+" bytes allocated!");
            Main.isClosing = true;
        }
    }
    public static void updateRegion(int packedRegionPos) {
//        long regionBufOffset = packedRegionPos * 8L;
//        long regionPtr = regionSSBO.stagingBuffer.pointer.get(0);
//        MemoryUtil.memLongBuffer(regionPtr + regionBufOffset, 1).put(regions, packedRegionPos, 1).rewind();
//        VkBufferCopy.Buffer regionBufferCopy = VkBufferCopy.calloc(1).srcOffset(regionBufOffset).dstOffset(regionBufOffset).size(8L);
//        vkCmdCopyBuffer(currentCmdBuffer, regionSSBO.stagingBuffer.buffer[0], regionSSBO.buffer.buffer[0], regionBufferCopy);
    }
    public static void updateLOD(int packedLodPos) {
//        long lodBufOffset = packedLodPos * 8L;
//        long lodPtr = lodSSBO.stagingBuffer.pointer.get(0);
//        MemoryUtil.memLongBuffer(lodPtr + lodBufOffset, 1).put(lods, packedLodPos, 1).rewind();
//        VkBufferCopy.Buffer lodBufferCopy = VkBufferCopy.calloc(1).srcOffset(lodBufOffset).dstOffset(lodBufOffset).size(8L);
//        vkCmdCopyBuffer(currentCmdBuffer, lodSSBO.stagingBuffer.buffer[0], lodSSBO.buffer.buffer[0], lodBufferCopy);
    }

    public static void drawRaster(MemoryStack stack){
        updatePipeline(3);
        bindImagesToDrawTo(stack, currentPipeline.vkPipeline, new Texture[]{Textures.colors2, Textures.norms2}, Textures.depth2, 1, true);
        vkCmdBindVertexBuffers(currentCmdBuffer, 0, stack.longs(vertexBuf.buffer), stack.longs(0));
        vkCmdBindIndexBuffer(currentCmdBuffer, indexBuf.buffer[0], 0, VK_INDEX_TYPE_UINT32);
        pushUBO.update(0); //draw non-instanced stuff
        pushUBO.updateAtlasOffset(new Vector2i(0));
        pushUBO.updateSize(new Vector2i(EntityTypes.entityTexWidth));
        pushUBO.updateTex(null); //use no texture
        //drawChunkDebug();
        modelOffset.set(viewPos);
        drawClouds();
        drawStars();
        StarSystem.render(stack);
        modelOffset.set(0);
        pushUBO.updateTex(null); //use no texture
        for (Effect effect : effects) {
            if (effect instanceof Lightning lightning) {
                drawCube(lightning.matrix, lightning.color);
            } else if (effect instanceof Particle particle) {
                Matrix4f interpolatedMatrix = new Matrix4f(particle.matrix);
                interpolatedMatrix.setTranslation(Utils.getInterpolatedVec(particle.prevPos, particle.pos));
                drawCube(interpolatedMatrix, particle.color);
            }
        }
        pushUBO.updateTex(Textures.entities);
        for (Entity entity : entities) {
            pushUBO.updateAtlasOffset(entity.type.atlasOffset);
            Matrix4f interpolatedMatrix = new Matrix4f(entity.matrix);
            Vector3f pos = new Vector3f();
            entity.matrix.getTranslation(pos);
            pos.set(Utils.getInterpolatedVec(entity.prevPos, pos));
            interpolatedMatrix.setTranslation(pos.x()%size, pos.y()%height, pos.z()%size);
            drawCube(interpolatedMatrix, new Vector4f(1.f));
        }
        updatePipeline(4);
        pushUBO.updateTex(Textures.items);
        pushUBO.updateSize(new Vector2i(ItemTypes.itemTexSize));
        for (Item item : World.items) {
            pushUBO.updateAtlasOffset(item.type.atlasOffset);
            drawQuad(new Matrix4f().rotateY((float) Math.toRadians(item.rot)).setTranslation(new Vector3f(item.pos).add(0, item.hover, 0)).scale(0.5f), new Vector4f(1.f));
        }
        unbindImagesDrawingTo(stack, new long[]{Textures.colors2.image, Textures.norms2.image}, Textures.depth2.image);
    }
    public static final int swizzle = 16;
    public static void drawDDA(MemoryStack stack) {
        pushUBO.updateTex(Textures.colors2, Textures.depth2, Textures.norms2);
        pushUBO.updateWriteTex(Textures.colors1, Textures.depth1, Textures.norms1, null);
        pushUBO.push();
        updateComputePipeline(0);
        bindComputeImages(stack, currentComputePipeline.vkPipeline, new Texture[]{Textures.colors1, Textures.norms1}, Textures.depth1);
        float scale = Settings.upscaled ? 16.f : 8.f;
        int x = ((int)Math.ceil(eWidth/scale)), y = ((int)Math.ceil(eHeight/scale));
        int tilesPerRow = (x+swizzle-1)/swizzle;
        vkCmdDispatch(currentCmdBuffer, tilesPerRow*swizzle*y, 1, 1);
        unbindComputeImages(stack, new long[]{Textures.colors1.image, Textures.norms1.image}, Textures.depth1.image);
    }
    public static void drawSSAO(MemoryStack stack) {
        pushUBO.updateTex(Textures.blueNoise, Textures.colors1, Textures.depth1, Textures.norms1);
        pushUBO.push();
        updatePipeline(2);
        bindImagesToDrawTo(stack, currentPipeline.vkPipeline, new Texture[]{Textures.colors2}, Textures.depth2, 1, true);
        vkCmdDraw(currentCmdBuffer, 3, 1, 0, 0);
        unbindImagesDrawingTo(stack, new long[]{Textures.colors2.image}, Textures.depth2.image);
    }
    public static void drawAA(MemoryStack stack) {
        if (Settings.taaEnabled || Settings.upscaled) {
            updatePipeline(7);
            bindImagesToDrawTo(stack, currentPipeline.vkPipeline, new Texture[]{Textures.colorsOld}, Textures.depthOld, 1, false);
            unbindImagesDrawingTo(stack, new long[]{Textures.colorsOld.image}, Textures.depthOld.image);

            pushUBO.updateTex(Textures.colors2, Textures.depth1, Textures.norms1);
            pushUBO.updateWriteTex(Textures.colorsOld, Textures.depthOld, null, null);
            pushUBO.push();
            bindImagesToDrawTo(stack, currentPipeline.vkPipeline, new Texture[]{Textures.colors1}, Textures.depth2, 1, true);
            vkCmdDraw(currentCmdBuffer, 3, 1, 0, 0);
            unbindImagesDrawingTo(stack, new long[]{Textures.colors1.image}, Textures.depth2.image);

            pushUBO.updateTex(Textures.colors1, Textures.depth1, null);
            pushUBO.push();
            updatePipeline(8);
            bindImagesToDrawTo(stack, currentPipeline.vkPipeline, new Texture[]{Textures.colorsOld}, Textures.depthOld, 1, true);
            vkCmdDraw(currentCmdBuffer, 3, 1, 0, 0);
            unbindImagesDrawingTo(stack, new long[]{Textures.colorsOld.image}, Textures.depthOld.image);

            bindImagesToDrawTo(stack, currentPipeline.vkPipeline, new Texture[]{Textures.colors2}, Textures.depth2, 1, true);
            vkCmdDraw(currentCmdBuffer, 3, 1, 0, 0);
            unbindImagesDrawingTo(stack, new long[]{Textures.colors2.image}, Textures.depth2.image);
        }
    }
    public static void drawBlur(MemoryStack stack) {
        pushUBO.updateTex(Textures.colors2, null, null);
        pushUBO.push();
        updatePipeline(5);
        bindImagesToDrawTo(stack, currentPipeline.vkPipeline, new Texture[]{Textures.blurred_horizontally, Textures.bloom_horizontally}, Textures.depth2, 4, true);
        vkCmdDraw(currentCmdBuffer, 3, 1, 0, 0);
        unbindImagesDrawingTo(stack, new long[]{Textures.blurred_horizontally.image, Textures.bloom_horizontally.image}, Textures.depth2.image);
        pushUBO.updateTex(Textures.bloom_horizontally, Textures.blurred_horizontally, null);
        pushUBO.push();
        updatePipeline(6);
        bindImagesToDrawTo(stack, currentPipeline.vkPipeline, new Texture[]{Textures.blurred, Textures.bloom}, Textures.depth2, 4, true);
        vkCmdDraw(currentCmdBuffer, 3, 1, 0, 0);
        unbindImagesDrawingTo(stack, new long[]{Textures.blurred.image, Textures.bloom.image}, Textures.depth2.image);
    }
    public static void drawGUI(MemoryStack stack) {
        pushUBO.updateTex(Textures.colors2, Textures.blurred, Textures.bloom);
        pushUBO.updateWriteTex(Textures.gui, Textures.items, null, null);
        pushUBO.push();
        updatePipeline(1);
        bindImagesToDrawTo(stack, currentPipeline.vkPipeline, new Texture[]{Textures.colors1}, Textures.depth2, 1, true);
        Renderer.drawQuad(new Matrix4f().translate(-1.f, -1.f, 0.f).scale(2), new Vector4f(-1.f));
        GUI.draw();
        unbindImagesDrawingTo(stack, new long[]{Textures.colors1.image}, Textures.depth2.image);
    }

    public static void updatePipeline(int i) {
        currentPipeline = pipelines[i];
        vkCmdBindPipeline(currentCmdBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, currentPipeline.vkPipeline);
    }
    public static void updateComputePipeline(int i) {
        currentComputePipeline = computePipelines[i];
        vkCmdBindPipeline(currentCmdBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, currentComputePipeline.vkPipeline);
    }

    public static void drawChunkDebug() {
        int playerCX = (int)(player.pos.x()/chunkSize), playerCY = (int)(player.pos.y()/chunkSize), playerCZ = (int)(player.pos.z()/chunkSize);
        //long playerCp = packChunkPos((int)(player.pos.x()/chunkSize), (int)(player.pos.y()/chunkSize), (int)(player.pos.z()/chunkSize));
        for (Chunk chunk : chunks.values()) {
//            float dist = player.pos.distance(chunk.cX*chunkSize, chunk.cY*chunkSize, chunk.cZ*chunkSize);
//            if (dist < chunkSize*96) {
//                if (dist < chunkSize * 5) {
//                    for (int x = 0; x < chunkSize; x++) {
//                        for (int z = 0; z < chunkSize; z++) {
//                            for (int y = chunkSize - 1; y >= 0; y--) {
//                                if (chunk.getBlock(Chunk.condenseLocalPos(x, y, z)).x() > 0) {
//                                    drawCube(new Matrix4f().setTranslation((chunk.cX * chunkSize) + x + 0.5f, (chunk.cY * chunkSize) + y + 0.5f, (chunk.cZ * chunkSize) + z + 0.5f).scale(1),
//                                            new Vector4f(((float) x) / chunkSize, ((float) y) / chunkSize, ((float) z) / chunkSize, 1));
//                                }
//                            }
//                        }
//                    }
//                } else if (chunk.blockPalette.size() > 1) {
                if (chunk.blockPalette.size() > 1) {
                    drawCube(new Matrix4f().setTranslation((chunk.cX + 0.5f) * chunkSize, (chunk.cY + 0.5f) * chunkSize, (chunk.cZ + 0.5f) * chunkSize).scale(chunkSize), new Vector4f(Math.abs(chunk.cX-playerCX)/10.f, Math.abs(chunk.cY-playerCY)/10.f, Math.abs(chunk.cZ-playerCZ)/10.f, 1));
                }
//            }
        }
    }
    public static void drawClouds() {
        if (worldType.getFogginess() > 0.5f) {
            Random cloudRand = new Random(911);
            float dist = worldType.getPlanet().pos.distance(StarSystem.pos);
            float brightness = 1+Math.clamp((Math.min(0, StarSystem.relativePos.y()/(0.001f*dist)))/300, -0.7f, -0.01f);
            for (int i = 0; i < 196; i++) {
                float b = Math.max(0.25f, brightness - (cloudRand.nextFloat()*0.34f));
                Vector3f pos = new Vector3f(0, 0, 2000 * (cloudRand.nextFloat() + 0.05f)).rotateY((float) ((cloudRand.nextFloat() * 10) + ((Main.timeMs*0.000005f) * (3 + cloudRand.nextInt(2)))));
                drawCube(new Matrix4f().rotateY(cloudRand.nextFloat() / 10).setTranslation(pos.set(pos.x, cloudRand.nextInt(200) + 420 - ((Math.abs(pos.x) + Math.abs(pos.z)) / 10), pos.z)).scale(50 + cloudRand.nextInt(50), 10 + cloudRand.nextInt(20), 50 + cloudRand.nextInt(50)), new Vector4f(b, b, b, 1.f));
            }
        }
    }
    public static Vector3f[] starColors = new Vector3f[]{new Vector3f(1.0f, 1.05f, 1.1f), new Vector3f(1.f, 0.95f, 0.4f), new Vector3f(1.f, 0.07f, 0), new Vector3f(0.42f, 0.85f, 1.f), new Vector3f(1.f, 1.f, 0.1f)};
    public static void drawStars() {
        //Vector3f interpolatedPlayerPos = Utils.getInterpolatedVec(player.prevPos, player.pos);
        int starDist = Constants.CENTER;
        Random starRand = new Random(seed);
        float dist = worldType.getPlanet().pos.distance(StarSystem.pos);
        int amt = (int) -Math.min(0, StarSystem.relativePos.y()/(0.001f*dist));
        amt = Math.min(amt, 256);
        for (int i = 0; i < amt; i++) {
            Vector3f starPos = new Vector3f(0, starDist * 2, 0)
                    .rotateX(starRand.nextFloat() * 10)
                    .rotateY(starRand.nextFloat() * 10)
                    .rotateZ((float) (Main.timeMs*0.00001f) + starRand.nextFloat() * 10);
            starPos.set(starPos.x + (starDist / 2f), starPos.y, starPos.z + (starDist / 2f));
            Matrix4f starMatrix = new Matrix4f()
                    .rotateXYZ(starRand.nextFloat(), starRand.nextFloat(), starRand.nextFloat())
                    .setTranslation(starPos)
                    .scale(4000000);
            Vector3f color = (starRand.nextFloat() < 0.64f ? new Vector3f(0.97f, 0.98f, 1.f) : starColors[starRand.nextInt(starColors.length - 1)]);
            drawCube(starMatrix, new Vector4f(color.x()*9, color.y()*9, color.z()*9, 1.f));
        }
    }

    public static void drawLine(Vector3f og, Vector3f dest, float width, Vector4f color) {
        Vector3f dir = new Vector3f(dest).sub(og);
        float length = dir.length();
        Quaternionf rot = new Quaternionf().rotationTo(new Vector3f(0, 1, 0), dir.normalize());
        width *= Math.max(1, (Math.max(og.distance(player.pos), dest.distance(player.pos))/(width*300f))-1.5f);
        Renderer.drawCube(new Matrix4f().rotation(rot).setTranslation(og).translate(0, length*0.5f, 0).scale(width, length, width), color);
    }
    public static void drawCube(Matrix4f modelMatrix, Vector4f color) {
        pushUBO.update(modelMatrix, color);
        pushUBO.push();
        vkCmdDrawIndexed(currentCmdBuffer, Models.CUBE.indexCount, 1, Models.CUBE.indexOffset/Index.SIZE, 0, 0);
    }
    public static void drawDoubleQuad(Matrix4f modelMatrix, Vector4f color) {
        pushUBO.update(modelMatrix, color);
        pushUBO.push();
        vkCmdDrawIndexed(currentCmdBuffer, Models.DOUBLE_QUAD.indexCount, 1, Models.QUAD.indexOffset/Index.SIZE, Models.QUAD.vertexOffset/Vertex.SIZE, 0);
    }
    public static void drawQuad(Matrix4f modelMatrix, Vector4f color) {
        pushUBO.update(modelMatrix, color);
        pushUBO.push();
        vkCmdDrawIndexed(currentCmdBuffer, Models.QUAD.indexCount, 1, Models.QUAD.indexOffset/Index.SIZE, Models.QUAD.vertexOffset/Vertex.SIZE, 0);
    }
    public static void drawQuadCentered(Matrix4f modelMatrix, Vector4f color) {
        pushUBO.update(modelMatrix, color);
        pushUBO.push();
        vkCmdDrawIndexed(currentCmdBuffer, Models.QUAD_CENTERED.indexCount, 1, Models.QUAD_CENTERED.indexOffset/Index.SIZE, Models.QUAD_CENTERED.vertexOffset/Vertex.SIZE, 0);
    }

    public static boolean startCommandBuffers(MemoryStack stack) {
//        long startTime = System.currentTimeMillis();
        VkSemaphoreWaitInfo semaphoreWaitInfo = VkSemaphoreWaitInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SEMAPHORE_WAIT_INFO)
                .flags(0)
                .semaphoreCount(1)
                .pSemaphores(stack.longs(SyncObjects.timelineSemaphore))
                .pValues(stack.longs(timeline));
        int waitResult = vkWaitSemaphores(vkDevice, semaphoreWaitInfo, Long.MAX_VALUE);
        if (waitResult != VK_SUCCESS) {throw new RuntimeException("Failed to wait for timeline semaphore: "+waitResult);}
//        long took = (System.currentTimeMillis()-startTime);
//        if (took > 50) {
//            System.out.println("Took " + took + "ms to start cmd buffer. ");
//        }

        IntBuffer imageIdxBuf = stack.mallocInt(1);
        int result = vkAcquireNextImageKHR(vkDevice, vkSwapchain, Long.MAX_VALUE, imageAvailableSemaphores[frameIdx], VK_NULL_HANDLE, imageIdxBuf);
        if (result == VK_ERROR_OUT_OF_DATE_KHR) {
            System.out.print("Out of date!");
            Graphics.rebuild();
            return false;
        } else if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {System.err.println("Failed to acquire next image!");}
        imageIdx = imageIdxBuf.get(0);

        currentCmdBuffer = cmdBuffers[frameIdx];
        vkResetCommandBuffer(currentCmdBuffer, 0);
        CmdBufferHelper.recordCmdBuffer(stack, currentCmdBuffer);
        return true;
    }
    public static void submitCommandBuffers(MemoryStack stack) {
        vkEndCommandBuffer(currentCmdBuffer);
        timeline++;
        VkTimelineSemaphoreSubmitInfo timelineInfo = VkTimelineSemaphoreSubmitInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_TIMELINE_SEMAPHORE_SUBMIT_INFO)
                .pWaitSemaphoreValues(stack.longs())
                .pSignalSemaphoreValues(stack.longs(0L, timeline));
        VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .pNext(timelineInfo.address())
                .waitSemaphoreCount(1)
                .pWaitSemaphores(stack.longs(imageAvailableSemaphores[frameIdx]))
                .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                .pCommandBuffers(stack.pointers(currentCmdBuffer.address()))
                .pSignalSemaphores(stack.longs(renderFinishedSemaphores[imageIdx], timelineSemaphore));
        vkQueueSubmit(graphicsQueue, submitInfo, VK_NULL_HANDLE);

        VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                .pWaitSemaphores(stack.longs(renderFinishedSemaphores[imageIdx]))
                .swapchainCount(1)
                .pSwapchains(stack.longs(vkSwapchain))
                .pImageIndices(stack.ints(imageIdx));
        int result = vkQueuePresentKHR(graphicsQueue, presentInfo);
        if (result != VK_ERROR_OUT_OF_DATE_KHR && result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {throw new RuntimeException("Failed to queue present!");}
        incFrameIdx();
    }
    public static void incFrameIdx() {
        frameIdx++;
        if (imageIdx >= Swapchain.images.length) {
            firstImages = false;
        }
        if (frameIdx >= FRAMES_IN_FLIGHT) {frameIdx = 0;}
    }
    public static void unbindImagesDrawingTo(MemoryStack stack, long[] images, long depthImage) {
        vkCmdEndRendering(currentCmdBuffer);
        for (long image : images) {
            ImageHelper.transitionImageLayout(stack, currentCmdBuffer, VK_IMAGE_ASPECT_COLOR_BIT, image,
                    VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                    VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT, VK_ACCESS_2_SHADER_SAMPLED_READ_BIT,
                    VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT, VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT);
        }
        ImageHelper.transitionImageLayout(stack, currentCmdBuffer, VK_IMAGE_ASPECT_DEPTH_BIT, depthImage,
                VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT, VK_ACCESS_2_SHADER_SAMPLED_READ_BIT,
                VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT, VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT);
    }
    public static void bindImagesToDrawTo(MemoryStack stack, long pipeline, Texture[] textures, Texture depthTex, int upscaling, boolean clear) {
        VkRenderingAttachmentInfo.Buffer colorAttachments = getColorAttachments(stack, currentCmdBuffer, textures, clear);
        VkRenderingAttachmentInfo depthAttachment = getDepthAttachment(stack, currentCmdBuffer, depthTex.image, depthTex.imageView, depthTex.isLayoutUnset() ? VK_IMAGE_LAYOUT_UNDEFINED : VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, clear);
        depthTex.layoutUnset = false;
        int w = eWidth/upscaling, h = eHeight/upscaling;
        VkRect2D renderArea = VkRect2D.calloc(stack)
                .offset(VkOffset2D.calloc(stack).set(0, 0))
                .extent(VkExtent2D.calloc(stack).width(w).height(h));
        VkRenderingInfo renderingInfo = VkRenderingInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDERING_INFO)
                .renderArea(renderArea)
                .layerCount(1)
                .pColorAttachments(colorAttachments)
                .pDepthAttachment(depthAttachment);
//        if (upscaling == 1 && Settings.upscaled) {
//            VkRenderingFragmentShadingRateAttachmentInfoKHR rateAttachment = getRateAttachment(stack, currentCmdBuffer, Textures.vrs);
//            renderingInfo.pNext(rateAttachment);
//        }
        vkCmdBeginRendering(currentCmdBuffer, renderingInfo);
        vkCmdSetViewport(currentCmdBuffer, 0, VkViewport.calloc(1, stack).x(0).y(0).width(w).height(h).minDepth(0).maxDepth(1));
        vkCmdSetScissor(currentCmdBuffer, 0, VkRect2D.calloc(1, stack).offset(VkOffset2D.calloc(stack).set(0, 0)).extent(VkExtent2D.calloc(stack).width(w).height(h)));
    }
    public static void bindComputeImages(MemoryStack stack, long pipeline, Texture[] textures, Texture depthTex) {
        for (Texture tex : textures) {
            ImageHelper.transitionStorageWrite(stack, currentCmdBuffer, tex);
        }
        ImageHelper.transitionStorageWrite(stack, currentCmdBuffer, depthTex);
    }
    public static void unbindComputeImages(MemoryStack stack, long[] images, long depthImage) {
        for (long image : images) {
            ImageHelper.transitionImageLayout(stack, currentCmdBuffer, VK_IMAGE_ASPECT_COLOR_BIT, image,
                    VK_IMAGE_LAYOUT_GENERAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                    VK_ACCESS_2_SHADER_WRITE_BIT, VK_ACCESS_2_SHADER_SAMPLED_READ_BIT,
                    VK_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT, VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT);
        }
        ImageHelper.transitionImageLayout(stack, currentCmdBuffer, VK_IMAGE_ASPECT_COLOR_BIT, depthImage,
                VK_IMAGE_LAYOUT_GENERAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                VK_ACCESS_2_SHADER_WRITE_BIT, VK_ACCESS_2_SHADER_SAMPLED_READ_BIT,
                VK_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT, VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT);
    }
    public static VkRenderingFragmentShadingRateAttachmentInfoKHR getRateAttachment(MemoryStack stack, VkCommandBuffer cmdBuffer, Texture tex) {
        if (tex.isLayoutUnset()) {
            int totalPixels = tex.width * tex.height;
            ByteBuffer data = memAlloc(totalPixels);
            int fullResPixels = 0;
            byte value = (byte)(4 | 1); //2x2
            for (int y = 0; y < tex.height; y++) {
                for (int x = 0; x < tex.width; x++) {
                    int i = y * tex.width + x;
                    if (x < tex.width*0.33f || x >= tex.width*0.67f || ((x-2 < tex.width*0.33f || x+2 >= tex.width*0.67f) && Math.random() < 0.34)) {
                        data.put(i, value);
                    } else {
                        fullResPixels++;
                        data.put(i, (byte) 0);
                    }
                }
            }
            data.rewind();
            System.out.println(((((float)fullResPixels)/((float)totalPixels))*100)+"% of pixels are native quality.");
            Buffer stagingBuffer = new Buffer(stack, data.remaining(), VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, true);
            memCopy(memAddress(data), stagingBuffer.pointer.get(0), data.remaining());
            ImageHelper.transitionImageLayout(stack, cmdBuffer, VK_IMAGE_ASPECT_COLOR_BIT, tex.image,
                    VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    0, VK_ACCESS_2_TRANSFER_WRITE_BIT,
                    VK_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_2_TRANSFER_BIT);
            tex.layoutUnset = false;
            VkBufferImageCopy.Buffer imageCopy = VkBufferImageCopy.calloc(1, stack)
                    .bufferOffset(0)
                    .bufferRowLength(0)
                    .bufferImageHeight(0)
                    .imageSubresource(s -> s
                            .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .mipLevel(0)
                            .baseArrayLayer(0)
                            .layerCount(1))
                    .imageOffset(o -> o.set(0, 0, 0))
                    .imageExtent(e -> e.set(tex.width, tex.height, 1));
            vkCmdCopyBufferToImage(cmdBuffer, stagingBuffer.buffer[0], tex.image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, imageCopy);
            ImageHelper.transitionImageLayout(stack, cmdBuffer, VK_IMAGE_ASPECT_COLOR_BIT, tex.image,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_FRAGMENT_SHADING_RATE_ATTACHMENT_OPTIMAL_KHR,
                    VK_ACCESS_2_TRANSFER_WRITE_BIT, VK_ACCESS_FRAGMENT_SHADING_RATE_ATTACHMENT_READ_BIT_KHR,
                    VK_PIPELINE_STAGE_2_TRANSFER_BIT, VK_PIPELINE_STAGE_2_FRAGMENT_SHADING_RATE_ATTACHMENT_BIT_KHR);
        }
        return VkRenderingFragmentShadingRateAttachmentInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDERING_FRAGMENT_SHADING_RATE_ATTACHMENT_INFO_KHR)
                .imageView(tex.imageView)
                .imageLayout(VK_IMAGE_LAYOUT_FRAGMENT_SHADING_RATE_ATTACHMENT_OPTIMAL_KHR)
                .shadingRateAttachmentTexelSize(VkExtent2D.calloc(stack).width(16).height(16));
    }
    public static VkRenderingAttachmentInfo.Buffer getColorAttachments(MemoryStack stack, VkCommandBuffer cmdBuffer, Texture[] textures, boolean clear) {
        VkRenderingAttachmentInfo.Buffer attachmentInfo = VkRenderingAttachmentInfo.calloc(textures.length, stack);
        for (int i = 0; i < textures.length; i++) {
            Texture tex = textures[i];
            int prevLayout = tex.isLayoutUnset() ? VK_IMAGE_LAYOUT_UNDEFINED : VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
            ImageHelper.transitionImageLayout(stack, cmdBuffer, VK_IMAGE_ASPECT_COLOR_BIT, tex.image,
                    prevLayout, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                    prevLayout == VK_IMAGE_LAYOUT_UNDEFINED ? VK_ACCESS_2_NONE : VK_ACCESS_2_SHADER_SAMPLED_READ_BIT, clear ? VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT : VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_2_COLOR_ATTACHMENT_READ_BIT,
                    prevLayout == VK_IMAGE_LAYOUT_UNDEFINED ? VK_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT : VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT, VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT);
            tex.layoutUnset = false;
            attachmentInfo.get(i)
                    .sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO)
                    .imageView(tex.imageView)
                    .imageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                    .loadOp(clear ? VK_ATTACHMENT_LOAD_OP_CLEAR : VK_ATTACHMENT_LOAD_OP_LOAD)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            attachmentInfo.clearValue().color().float32(0, 0.0f).float32(1, 0.0f).float32(2, 0.0f).float32(3, 0.0f);
        }
        return attachmentInfo;
    }
    public static void unbindPresentImage(MemoryStack stack) {
        vkCmdEndRendering(currentCmdBuffer);
        ImageHelper.transitionImageLayout(stack, currentCmdBuffer, VK_IMAGE_ASPECT_COLOR_BIT, Swapchain.images[imageIdx],
                VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
                VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT, VK_ACCESS_2_NONE,
                VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT, VK_PIPELINE_STAGE_2_NONE);
    }
    public static void bindPresentImage(MemoryStack stack) {
        VkRenderingAttachmentInfo.Buffer colorAttachment = getPresentColorAttachment(stack, currentCmdBuffer, images[imageIdx], imageViews[imageIdx], firstImages ? VK_IMAGE_LAYOUT_UNDEFINED : VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
        VkRenderingAttachmentInfo depthAttachment = getDepthAttachment(stack, currentCmdBuffer, depthImage, depthImageView, VK_IMAGE_LAYOUT_UNDEFINED, true);
        VkRect2D renderArea = VkRect2D.calloc(stack)
                .offset(VkOffset2D.calloc(stack).set(0, 0))
                .extent(VkExtent2D.calloc(stack).width(eWidth).height(eHeight));
        VkRenderingInfo renderingInfo = VkRenderingInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDERING_INFO)
                .renderArea(renderArea)
                .layerCount(1)
                .pColorAttachments(colorAttachment)
                .pDepthAttachment(depthAttachment);
        vkCmdBeginRendering(currentCmdBuffer, renderingInfo);
        updatePipeline(0);
        vkCmdSetViewport(currentCmdBuffer, 0, VkViewport.calloc(1, stack).x(0).y(0).width(eWidth).height(eHeight).minDepth(0).maxDepth(1));
        vkCmdSetScissor(currentCmdBuffer, 0, VkRect2D.calloc(1, stack).offset(VkOffset2D.calloc(stack).set(0, 0)).extent(VkExtent2D.calloc(stack).width(eWidth).height(eHeight)));
    }

    public static VkRenderingAttachmentInfo.Buffer getPresentColorAttachment(MemoryStack stack, VkCommandBuffer cmdBuffer, long image, long imageView, int prevLayout) {
        ImageHelper.transitionImageLayout(stack, cmdBuffer, VK_IMAGE_ASPECT_COLOR_BIT, image,
                prevLayout, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                VK_ACCESS_2_NONE, VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT,
                VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT, VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT);
        VkRenderingAttachmentInfo.Buffer attachmentInfo = VkRenderingAttachmentInfo.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO)
                .imageView(imageView)
                .imageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE);
        attachmentInfo.clearValue().color().float32(0, 0.0f).float32(1, 0.0f).float32(2, 0.0f).float32(3, 0.0f);
        return attachmentInfo;
    }
    public static VkRenderingAttachmentInfo getDepthAttachment(MemoryStack stack, VkCommandBuffer cmdBuffer, long image, long imageView, int prevLayout, boolean clear) {
        if (depthImage == image) {
            ImageHelper.transitionImageLayout(stack, cmdBuffer, VK_IMAGE_ASPECT_DEPTH_BIT, image,
                    VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL,
                    VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT, clear ? VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT : VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT | VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_READ_BIT,
                    VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT, VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT);
        } else {
            ImageHelper.transitionImageLayout(stack, cmdBuffer, VK_IMAGE_ASPECT_DEPTH_BIT, image,
                    prevLayout, VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL,
                    VK_ACCESS_2_NONE, clear ? VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT : VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT | VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_READ_BIT,
                    VK_PIPELINE_STAGE_2_NONE, VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT);
        }
        VkRenderingAttachmentInfo attachmentInfo = VkRenderingAttachmentInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO)
                .imageView(imageView)
                .imageLayout(VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL)
                .loadOp(clear ? VK_ATTACHMENT_LOAD_OP_CLEAR : VK_ATTACHMENT_LOAD_OP_LOAD)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE);
        attachmentInfo.clearValue().depthStencil().depth(0.f);
        return attachmentInfo;
    }

    public static int regionSSBOByteSize = oldRegions.length*8;
    //public static int lodSSBOByteSize = lods.length*8;
    public static int gigabyte = 1000000000;
    public static int voxelSSBOSize = gigabyte*2;
    public static int lightSSBOSize = gigabyte*2;
    public static int chunkArrSize = sizeChunks*sizeChunks*heightChunks;
    public static int chunkByteSize = 4*4;
    public static int chunkSSBOSize = chunkArrSize*chunkByteSize;
    public static PointerBuffer blocks;
    public static PointerBuffer lights;
    public static Long2LongOpenHashMap chunkBlockAllocs;;
    public static Long2LongOpenHashMap chunkLightBlockAllocs;
    public static void fillSSBOs() {
        long startTime = System.currentTimeMillis();
        if (blocks != null) {
            vmaDestroyVirtualBlock(blocks.get(0));
            vmaDestroyVirtualBlock(lights.get(0));
        }
        blocks = BufferUtils.createPointerBuffer(1);
        lights = BufferUtils.createPointerBuffer(1);
        VmaVirtualBlockCreateInfo blockCreateInfo = VmaVirtualBlockCreateInfo.create();
        blockCreateInfo.size(voxelSSBOSize);
        vmaCreateVirtualBlock(blockCreateInfo, blocks);
        chunkBlockAllocs = new Long2LongOpenHashMap(chunkArrSize);
        VmaVirtualBlockCreateInfo lightBlockCreateInfo = VmaVirtualBlockCreateInfo.create();
        lightBlockCreateInfo.size(lightSSBOSize);
        vmaCreateVirtualBlock(lightBlockCreateInfo, lights);
        chunkLightBlockAllocs = new Long2LongOpenHashMap(chunkArrSize);

//        for (Chunk chunk : chunks.values()) {
//            updateChunk(chunk.condensedChunkPos);
//        }
        System.out.println("Allocated "+allocated+" bytes for light data.");

        long regionPtr = regionSSBO.stagingBuffer.pointer.get(0);
        MemoryUtil.memLongBuffer(regionPtr, oldRegions.length).put(oldRegions).rewind();
//        long lodPtr = lodSSBO.stagingBuffer.pointer.get(0);
//        MemoryUtil.memLongBuffer(lodPtr, lods.length).put(lods).rewind();

        VkBufferCopy.Buffer regionBufferCopy = VkBufferCopy.calloc(1).srcOffset(0).dstOffset(0).size(regionSSBOByteSize);
        vkCmdCopyBuffer(currentCmdBuffer, regionSSBO.stagingBuffer.buffer[0], regionSSBO.buffer.buffer[0], regionBufferCopy);
        VkBufferCopy.Buffer chunkBufferCopy = VkBufferCopy.calloc(1).srcOffset(0).dstOffset(0).size(chunkSSBOSize);
        vkCmdCopyBuffer(currentCmdBuffer, chunkSSBO.stagingBuffer.buffer[0], chunkSSBO.buffer.buffer[0], chunkBufferCopy);
        VkBufferCopy.Buffer voxelBufferCopy = VkBufferCopy.calloc(1).srcOffset(0).dstOffset(0).size(voxelSSBOSize);
        vkCmdCopyBuffer(currentCmdBuffer, voxelSSBO.stagingBuffer.buffer[0], voxelSSBO.buffer.buffer[0], voxelBufferCopy);
//        VkBufferCopy.Buffer lodBufferCopy = VkBufferCopy.calloc(1).srcOffset(0).dstOffset(0).size(lodSSBOByteSize);
//        vkCmdCopyBuffer(currentCmdBuffer, lodSSBO.stagingBuffer.buffer[0], lodSSBO.buffer.buffer[0], lodBufferCopy);
        VkBufferCopy.Buffer lightChunkBufferCopy = VkBufferCopy.calloc(1).srcOffset(0).dstOffset(0).size(chunkSSBOSize);
        vkCmdCopyBuffer(currentCmdBuffer, lightChunkSSBO.stagingBuffer.buffer[0], lightChunkSSBO.buffer.buffer[0], lightChunkBufferCopy);
        VkBufferCopy.Buffer lightBufferCopy = VkBufferCopy.calloc(1).srcOffset(0).dstOffset(0).size(lightSSBOSize);
        vkCmdCopyBuffer(currentCmdBuffer, lightSSBO.stagingBuffer.buffer[0], lightSSBO.buffer.buffer[0], lightBufferCopy);
        ssboBarriers();
        System.out.println("Took "+(System.currentTimeMillis()-startTime)+"ms to fill SSBOs.");
    }
    public static void ssboBarriers() {
        VkBufferMemoryBarrier.Buffer barrierBuf = VkBufferMemoryBarrier.calloc(6);
        barrierBuf.get(0)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(regionSSBO.buffer.buffer[0])
                .offset(0).size(regionSSBOByteSize);
        barrierBuf.get(1)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(chunkSSBO.buffer.buffer[0])
                .offset(0).size(chunkSSBOSize);
        barrierBuf.get(2)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(voxelSSBO.buffer.buffer[0])
                .offset(0).size(voxelSSBOSize);
//        barrierBuf.get(3)
//                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
//                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
//                .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
//                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
//                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
//                .buffer(lodSSBO.buffer.buffer[0])
//                .offset(0).size(lodSSBOByteSize);
        barrierBuf.get(4)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(lightChunkSSBO.buffer.buffer[0])
                .offset(0).size(chunkSSBOSize);
        barrierBuf.get(5)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(lightSSBO.buffer.buffer[0])
                .offset(0).size(lightSSBOSize);
        vkCmdPipelineBarrier(currentCmdBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, null, barrierBuf, null);
    }
    public static void atlasBarriers() {
        VkBufferMemoryBarrier.Buffer barrierBuf = VkBufferMemoryBarrier.calloc(1);
        barrierBuf.get(0)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(BlockTypes.atlasBuffer.buffer[0])
                .offset(0).size(BlockTypes.atlasBuffer.size);
        vkCmdPipelineBarrier(currentCmdBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, null, barrierBuf, null);
    }
}
