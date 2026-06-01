package org.conspiracraft.graphics;

import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.effects.Effect;
import org.conspiracraft.effects.Lightning;
import org.conspiracraft.entities.Entity;
import org.conspiracraft.entities.EntityTypes;
import org.conspiracraft.gui.GUI;
import org.conspiracraft.Main;
import org.conspiracraft.graphics.buffers.ubos.PushUBO;
import org.conspiracraft.graphics.models.Index;
import org.conspiracraft.graphics.models.Models;
import org.conspiracraft.graphics.models.Vertex;
import org.conspiracraft.graphics.textures.Texture;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.ItemTypes;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.graphics.buffers.CmdBufferHelper;
import org.conspiracraft.graphics.textures.ImageHelper;
import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.world.Chunk;
import org.conspiracraft.world.LightHelper;
import org.conspiracraft.world.World;
import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaVirtualAllocationCreateInfo;
import org.lwjgl.util.vma.VmaVirtualBlockCreateInfo;
import org.lwjgl.vulkan.*;

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
import static org.conspiracraft.world.Earth.sunPos;
import static org.conspiracraft.world.World.*;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
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
    public static boolean reloadAtlas = false;
    public static void render() throws Exception {
        if (!initialized && !LightHelper.lightQueue.isEmpty()) {return;}
        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (startCommandBuffers(stack)) {
                clearedDepth = false;
                if (!initialized) {
                    generating = false;
                    fillSSBOs();
                    long startTime = System.currentTimeMillis();
                    BlockTypes.fillTexture(stack);
                    ByteBuffer noisesBuffer = Utils.imageToBuffer(Utils.loadImage("generic/texture/coherent_noise"));
                    ImageHelper.fillImage(stack, Textures.noises, noisesBuffer);
                    memFree(noisesBuffer);
                    ByteBuffer blueNoiseBuffer = Utils.imageToBuffer(Utils.loadImage("generic/texture/blue_noise"));
                    ImageHelper.fillImage(stack, Textures.blueNoise, blueNoiseBuffer);
                    memFree(blueNoiseBuffer);
                    GUI.fillTexture();
                    EntityTypes.fillTexture(stack);
                    initialized = true;
                    System.out.println("Texture initialization took "+(System.currentTimeMillis()-startTime)+"ms");
                } else {
                    //long startTime = System.nanoTime();
                    //boolean wasEmpty = updateQueue.isEmpty();
                    while (!updateQueue.isEmpty()) {
                        Vector3i chunkPos = updateQueue.pollFirst();
                        updateChunk(chunkPos);
                        updateSet.remove(chunkPos);
                    }
                    //if (!wasEmpty) {System.out.println("SSBO uploads took " + String.format("%.2f", (System.nanoTime() - startTime)/1000000.d) + "ms");}
                    ssboBarriers();
                    if (reloadAtlas) {
                        reloadAtlas = false;
                        long startTime = System.currentTimeMillis();
                        BlockTypes.fillTexture(stack);
                        //atlasBarriers();
                        System.out.println("Atlas reloading took "+(System.currentTimeMillis()-startTime)+"ms");
                    }
                }
                vkCmdBindDescriptorSets(currentCmdBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, stack.longs(Descriptors.descriptorSets[frameIdx]), null);
                globalUBO.update(stack);
                globalUBO.submit();

                drawRaster(stack);
                drawDDA(stack);
                drawSSAO(stack);
                drawBlur(stack);
                drawGUI(stack);

                bindPresentImage(stack);
                vkCmdDraw(currentCmdBuffer, 3, 1, 0, 0);
                unbindPresentImage(stack);

                submitCommandBuffers(stack);
            }
        }
    }

    public static void updateChunk(Vector3i chunkPos) {
        int packedChunkPos = packChunkPos(chunkPos);
        Chunk chunk = chunks[packedChunkPos];
        updateChunkBlocks(chunkPos, packedChunkPos, chunk);
        updateChunkLights(chunkPos, packedChunkPos, chunk);
    }
    public static void updateChunkBlocks(Vector3i chunkPos, int packedChunkPos, Chunk chunk) {
        long chunkPtr = chunkSSBO.stagingBuffer.pointer.get(0);
        long voxelPtr = voxelSSBO.stagingBuffer.pointer.get(0);
        if (initialized) {
            vmaVirtualFree(blocks.get(0), chunkBlockAllocs[packedChunkPos]);
        }
        VmaVirtualAllocationCreateInfo allocCreateInfo = VmaVirtualAllocationCreateInfo.create();
        int paletteSize = chunk.getBlockPaletteSize();
        int bitsPerValue = chunk.bitsPerBlock();
        int valueMask = chunk.blockValueMask();
        int[] compressedBlocks = chunk.getBlockData();
        if (compressedBlocks == null) {
            allocCreateInfo.size((paletteSize) * 4L);
        } else {
            allocCreateInfo.size((paletteSize + compressedBlocks.length) * 4L);
        }

        PointerBuffer alloc = BufferUtils.createPointerBuffer(1);
        LongBuffer offset = BufferUtils.createLongBuffer(1);
        long res = vmaVirtualAllocate(blocks.get(0), allocCreateInfo, alloc, offset);
        if (res == VK_SUCCESS) {
            chunkBlockAllocs[packedChunkPos] = alloc.get();
            int pointer = (int) offset.get(0);
            long chunkBufOffset = (long)packedChunkPos*chunkByteSize;
            MemoryUtil.memIntBuffer(chunkPtr+chunkBufOffset, 4)
                    .put(0, pointer/4).put(1, paletteSize).put(2, bitsPerValue).put(3, valueMask);
            MemoryUtil.memIntBuffer(voxelPtr+pointer, paletteSize)
                    .put(0, chunk.getBlockPalette());
            if (compressedBlocks != null) {
                MemoryUtil.memIntBuffer(voxelPtr+pointer+(paletteSize*4L), compressedBlocks.length)
                        .put(0, compressedBlocks);
            }
            if (initialized) {
                updateRegion(World.packRegionPos(new Vector3i(chunkPos).div(regionSizeChunks)));
                VkBufferCopy.Buffer chunkBufferCopy = VkBufferCopy.calloc(1).srcOffset(chunkBufOffset).dstOffset(chunkBufOffset).size(16L);
                vkCmdCopyBuffer(currentCmdBuffer, chunkSSBO.stagingBuffer.buffer[0], chunkSSBO.buffer.buffer[0], chunkBufferCopy);
                if (compressedBlocks != null) {
                    VkBufferCopy.Buffer voxelBufferCopy = VkBufferCopy.calloc(1).srcOffset(pointer).dstOffset(pointer).size((paletteSize + compressedBlocks.length) * 4L);
                    vkCmdCopyBuffer(currentCmdBuffer, voxelSSBO.stagingBuffer.buffer[0], voxelSSBO.buffer.buffer[0], voxelBufferCopy);
                }
                Vector3i ogLodPos = new Vector3i(chunkPos).mul(lodSize);
                for (int z = ogLodPos.z(); z < ogLodPos.z() + lodSize; z++) {
                    for (int x = ogLodPos.x(); x < ogLodPos.x() + lodSize; x++) {
                        for (int y = ogLodPos.y(); y < ogLodPos.y() + lodSize; y++) {
                            updateLOD(World.packLodPos(new Vector3i(x, y, z)));
                        }
                    }
                }
            }
        } else {
            System.out.print("blocksSSBO ran out of space! \n");
            Main.isClosing = true;
        }
    }
    public static long allocated = 0;
    public static void updateChunkLights(Vector3i chunkPos, int packedChunkPos, Chunk chunk) {
        long chunkPtr = lightChunkSSBO.stagingBuffer.pointer.get(0);
        long lightPtr = lightSSBO.stagingBuffer.pointer.get(0);
        if (initialized) {
            vmaVirtualFree(lights.get(0), chunkLightBlockAllocs[packedChunkPos]);
        }
        VmaVirtualAllocationCreateInfo allocCreateInfo = VmaVirtualAllocationCreateInfo.create();
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
            chunkLightBlockAllocs[packedChunkPos] = alloc.get();
            int pointer = (int) offset.get(0);
            long chunkBufOffset = (long)packedChunkPos*chunkByteSize;
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
        long regionBufOffset = packedRegionPos * 8L;
        long regionPtr = regionSSBO.stagingBuffer.pointer.get(0);
        MemoryUtil.memLongBuffer(regionPtr + regionBufOffset, 1).put(regions, packedRegionPos, 1).rewind();
        VkBufferCopy.Buffer regionBufferCopy = VkBufferCopy.calloc(1).srcOffset(regionBufOffset).dstOffset(regionBufOffset).size(8L);
        vkCmdCopyBuffer(currentCmdBuffer, regionSSBO.stagingBuffer.buffer[0], regionSSBO.buffer.buffer[0], regionBufferCopy);
    }
    public static void updateLOD(int packedLodPos) {
        long lodBufOffset = packedLodPos * 8L;
        long lodPtr = lodSSBO.stagingBuffer.pointer.get(0);
        MemoryUtil.memLongBuffer(lodPtr + lodBufOffset, 1).put(lods, packedLodPos, 1).rewind();
        VkBufferCopy.Buffer lodBufferCopy = VkBufferCopy.calloc(1).srcOffset(lodBufOffset).dstOffset(lodBufOffset).size(8L);
        vkCmdCopyBuffer(currentCmdBuffer, lodSSBO.stagingBuffer.buffer[0], lodSSBO.buffer.buffer[0], lodBufferCopy);
    }

    public static void drawRaster(MemoryStack stack){
        currentPipeline = pipelines[4];
        bindImagesToDrawTo(stack, currentPipeline.vkPipeline, new Texture[]{Textures.colors2, Textures.norms2}, Textures.depth2);
        vkCmdBindVertexBuffers(currentCmdBuffer, 0, stack.longs(vertexBuf.buffer), stack.longs(0));
        vkCmdBindIndexBuffer(currentCmdBuffer, indexBuf.buffer[0], 0, VK_INDEX_TYPE_UINT32);
        pushUBO.update(0); //draw non-instanced stuff
        pushUBO.updateAtlasOffset(new Vector2i(0));
        pushUBO.updateSize(new Vector2i(8));
        pushUBO.updateLayer(-1);
        //drawClouds();
        drawStars();
        worldType.renderCelestialBodies(stack);
        for (Effect effect : effects) {
            if (effect instanceof Lightning lightning) {
                pushUBO.updateAtlasOffset(new Vector2i(0));
                drawCube(lightning.matrix, new Vector4f(1.f, 0.95f, 0.1f, 4.f));
            }
        }
        pushUBO.updateLayer(0);
        for (Entity entity : entities) {
            pushUBO.updateAtlasOffset(entity.type.atlasOffset);
            Matrix4f interpolatedMatrix = new Matrix4f(entity.matrix);
            Vector3f pos = new Vector3f();
            entity.matrix.getTranslation(pos);
            interpolatedMatrix.setTranslation(Utils.getInterpolatedVec(entity.prevPos, pos));
            drawCube(interpolatedMatrix, new Vector4f(1.f));
        }
        pushUBO.updateTex(1);
        pushUBO.updateSize(new Vector2i(ItemTypes.itemTexSize));
        for (Item item : World.items) {
            pushUBO.updateAtlasOffset(item.type.atlasOffset);
            drawQuad(new Matrix4f().rotateY((float) Math.toRadians(item.rot)).setTranslation(new Vector3f(item.pos).add(0, item.hover, 0)).scale(0.5f), new Vector4f(1.f));
        }
        unbindImagesDrawingTo(stack, new long[]{Textures.colors2.image, Textures.norms2.image}, Textures.depth2.image);
    }
    public static void drawDDA(MemoryStack stack) {
        currentPipeline = pipelines[3];
        bindImagesToDrawTo(stack, currentPipeline.vkPipeline, new Texture[]{Textures.colors1, Textures.norms1}, Textures.depth1);
        vkCmdDraw(currentCmdBuffer, 3, 1, 0, 0);
        unbindImagesDrawingTo(stack, new long[]{Textures.colors1.image, Textures.norms1.image}, Textures.depth1.image);
    }
    public static void drawSSAO(MemoryStack stack) {
        currentPipeline = pipelines[2];
        bindImagesToDrawTo(stack, currentPipeline.vkPipeline, new Texture[]{Textures.colors2}, Textures.depth2);
        vkCmdDraw(currentCmdBuffer, 3, 1, 0, 0);
        unbindImagesDrawingTo(stack, new long[]{Textures.colors2.image}, Textures.depth2.image);
    }
    public static void drawBlur(MemoryStack stack) {
        currentPipeline = pipelines[5];
        bindImagesToDrawTo(stack, currentPipeline.vkPipeline, new Texture[]{Textures.blurred_horizontally}, Textures.depth2);
        vkCmdDraw(currentCmdBuffer, 3, 1, 0, 0);
        unbindImagesDrawingTo(stack, new long[]{Textures.blurred_horizontally.image}, Textures.depth2.image);
        currentPipeline = pipelines[6];
        bindImagesToDrawTo(stack, currentPipeline.vkPipeline, new Texture[]{Textures.blurred}, Textures.depth2);
        vkCmdDraw(currentCmdBuffer, 3, 1, 0, 0);
        unbindImagesDrawingTo(stack, new long[]{Textures.blurred.image}, Textures.depth2.image);
    }
    public static void drawGUI(MemoryStack stack) {
        currentPipeline = pipelines[1];
        bindImagesToDrawTo(stack, currentPipeline.vkPipeline, new Texture[]{Textures.colors1}, Textures.depth2);
        Renderer.drawQuad(new Matrix4f().translate(-1.f, -1.f, 0.f).scale(2), new Vector4f(-1.f));
        GUI.draw();
        unbindImagesDrawingTo(stack, new long[]{Textures.colors1.image}, Textures.depth2.image);
    }

    public static void drawClouds() {
//        FloatBuffer modelBuffer = BufferUtils.createFloatBuffer(196 * 16);
//        FloatBuffer colorBuffer = BufferUtils.createFloatBuffer(196 * 4);
        Random cloudRand = new Random(911);
        float brightness = Math.clamp((640 + sunPos.y()) / 640, 0.3f, 1.f);
        for (int i = 0; i < 196; i++) {
            float b = Math.max(0.25f, brightness - (cloudRand.nextFloat() / 2));
            Vector3f pos = new Vector3f(0, 0, 2000 * (cloudRand.nextFloat() + 0.05f)).rotateY((float) ((cloudRand.nextFloat() * 10) + ((Main.timeMs*0.000005f) * (3 + cloudRand.nextInt(2)))));
            drawCube(new Matrix4f().rotateY(cloudRand.nextFloat() / 10).setTranslation(pos.set(pos.x + World.halfSize, cloudRand.nextInt(200) + 420 - ((Math.abs(pos.x) + Math.abs(pos.z)) / 10), pos.z + World.halfSize)).scale(50 + cloudRand.nextInt(50), 10 + cloudRand.nextInt(20), 50 + cloudRand.nextInt(50)), new Vector4f(b, b, b, 1.f));
//            try (MemoryStack stack = MemoryStack.stackPush()) {
//                modelBuffer.put(new Matrix4f().rotateY(cloudRand.nextFloat() / 10).setTranslation(pos.set(pos.x + World.halfSize, cloudRand.nextInt(200) + 320 - ((Math.abs(pos.x) + Math.abs(pos.z)) / 10), pos.z + World.halfSize)).scale(5 + cloudRand.nextInt(5), 1 + cloudRand.nextInt(2), 5 + cloudRand.nextInt(5)).get(stack.mallocFloat(16)));
//            }
//            colorBuffer.put(b);
//            colorBuffer.put(b);
//            colorBuffer.put(b);
//            colorBuffer.put(-1);
        }
//        modelBuffer.flip();
//        glBindBuffer(GL_SHADER_STORAGE_BUFFER, modelsSSBOId);
//        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, modelsSSBOId);
//        glBufferData(GL_SHADER_STORAGE_BUFFER, modelBuffer, GL_DYNAMIC_DRAW);
//        colorBuffer.flip();
//        glBindBuffer(GL_SHADER_STORAGE_BUFFER, colorsSSBOId);
//        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, colorsSSBOId);
//        glBufferData(GL_SHADER_STORAGE_BUFFER, colorBuffer, GL_DYNAMIC_DRAW);
//        drawCubes(1024);
    }
    public static Vector3f[] starColors = new Vector3f[]{new Vector3f(0.9f, 0.95f, 1.f), new Vector3f(1, 0.95f, 0.4f), new Vector3f(0.72f, 0.05f, 0), new Vector3f(0.42f, 0.85f, 1.f), new Vector3f(0.04f, 0.3f, 1.f), new Vector3f(1, 1, 0.1f)};
    public static int starDist = (World.size*2)+200;
    public static void drawStars() {
//        FloatBuffer modelBuffer = BufferUtils.createFloatBuffer(1024*16);
//        FloatBuffer colorBuffer = BufferUtils.createFloatBuffer(1024*4);
        Random starRand = new Random(seed);
        for (int i = 0; i < 512; i++) {
            Vector3f starPos = new Vector3f(0, starDist * 2, 0)
                    .rotateX(starRand.nextFloat() * 10)
                    .rotateY(starRand.nextFloat() * 10)
                    .rotateZ((float) (Main.timeMs*0.00001f) + starRand.nextFloat() * 10);
            starPos.set(starPos.x + (starDist / 2f), starPos.y, starPos.z + (starDist / 2f));
            float starSize = ((starRand.nextFloat()*40)+40)-Math.max(0, 150*(sunPos.y/World.size));
            if (starSize > 0.01f) {
                Matrix4f starMatrix = new Matrix4f()
                        .rotateXYZ(starRand.nextFloat(), starRand.nextFloat(), starRand.nextFloat())
                        .setTranslation(starPos)
                        .scale(starSize*3);
                if (starMatrix.getTranslation(new Vector3f()).y > 63-player.pos.y()) {
                    //modelBuffer.put(starMatrix.get(stack.mallocFloat(16)));
                    Vector3f color = (starRand.nextFloat() < 0.64f ? new Vector3f(0.97f, 0.98f, 1.f) : starColors[starRand.nextInt(starColors.length - 1)]);
//                    colorBuffer.put(color.x*12);
//                    colorBuffer.put(color.y*12);
//                    colorBuffer.put(color.z*12);
//                    colorBuffer.put(2);
                    drawCube(starMatrix, new Vector4f(color.x()*5, color.y()*5, color.z()*5, 1.f));
                }
            }
        }
//        modelBuffer.flip();
//        glBindBuffer(GL_SHADER_STORAGE_BUFFER, modelsSSBOId);
//        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, modelsSSBOId);
//        glBufferData(GL_SHADER_STORAGE_BUFFER, modelBuffer, GL_DYNAMIC_DRAW);
//        colorBuffer.flip();
//        glBindBuffer(GL_SHADER_STORAGE_BUFFER, colorsSSBOId);
//        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, colorsSSBOId);
//        glBufferData(GL_SHADER_STORAGE_BUFFER, colorBuffer, GL_DYNAMIC_DRAW);
//        drawCubes(1024);
    }

    public static void drawCube(Matrix4f modelMatrix, Vector4f color) {
        pushUBO.update(modelMatrix, color);
        pushUBO.submit();
        vkCmdDrawIndexed(currentCmdBuffer, Models.CUBE.indexCount, 1, Models.CUBE.indexOffset/Index.SIZE, 0, 0);
    }
    public static void drawDoubleQuad(Matrix4f modelMatrix, Vector4f color) {
        pushUBO.update(modelMatrix, color);
        pushUBO.submit();
        vkCmdDrawIndexed(currentCmdBuffer, Models.DOUBLE_QUAD.indexCount, 1, Models.QUAD.indexOffset/Index.SIZE, Models.QUAD.vertexOffset/Vertex.SIZE, 0);
    }
    public static void drawQuad(Matrix4f modelMatrix, Vector4f color) {
        pushUBO.update(modelMatrix, color);
        pushUBO.submit();
        vkCmdDrawIndexed(currentCmdBuffer, Models.QUAD.indexCount, 1, Models.QUAD.indexOffset/Index.SIZE, Models.QUAD.vertexOffset/Vertex.SIZE, 0);
    }
    public static void drawQuadCentered(Matrix4f modelMatrix, Vector4f color) {
        pushUBO.update(modelMatrix, color);
        pushUBO.submit();
        vkCmdDrawIndexed(currentCmdBuffer, Models.QUAD_CENTERED.indexCount, 1, Models.QUAD_CENTERED.indexOffset/Index.SIZE, Models.QUAD_CENTERED.vertexOffset/Vertex.SIZE, 0);
    }

    public static boolean startCommandBuffers(MemoryStack stack) {
        VkSemaphoreWaitInfo semaphoreWaitInfo = VkSemaphoreWaitInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SEMAPHORE_WAIT_INFO)
                .flags(0)
                .semaphoreCount(1)
                .pSemaphores(stack.longs(SyncObjects.timelineSemaphore))
                .pValues(stack.longs(timeline));
        int waitResult = vkWaitSemaphores(vkDevice, semaphoreWaitInfo, Long.MAX_VALUE);
        if (waitResult != VK_SUCCESS) {throw new RuntimeException("Failed to wait for timeline semaphore: "+waitResult);}

        IntBuffer imageIdxBuf = stack.mallocInt(1);
        int result = vkAcquireNextImageKHR(vkDevice, vkSwapchain, Long.MAX_VALUE, imageAvailableSemaphores[frameIdx], VK_NULL_HANDLE, imageIdxBuf);
        if (result == VK_ERROR_OUT_OF_DATE_KHR) {
            System.out.print("Out of date!");
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
    public static void bindImagesToDrawTo(MemoryStack stack, long pipeline, Texture[] textures, Texture depthTex) {
        VkRenderingAttachmentInfo.Buffer colorAttachments = getColorAttachments(stack, currentCmdBuffer, textures);
        VkRenderingAttachmentInfo depthAttachment = getDepthAttachment(stack, currentCmdBuffer, depthTex.image, depthTex.imageView, depthTex.isLayoutUnset() ? VK_IMAGE_LAYOUT_UNDEFINED : VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        VkRect2D renderAreaData = VkRect2D.calloc(stack)
                .offset(VkOffset2D.calloc(stack).set(0, 0))
                .extent(VkExtent2D.calloc(stack).width(eWidth).height(eHeight));
        VkRenderingInfo renderingInfo = VkRenderingInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDERING_INFO)
                .renderArea(renderAreaData)
                .layerCount(1)
                .pColorAttachments(colorAttachments)
                .pDepthAttachment(depthAttachment);
        vkCmdBeginRendering(currentCmdBuffer, renderingInfo);
        vkCmdBindPipeline(currentCmdBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
        vkCmdSetViewport(currentCmdBuffer, 0, VkViewport.calloc(1, stack).x(0).y(0).width(eWidth).height(eHeight).minDepth(0).maxDepth(1));
        vkCmdSetScissor(currentCmdBuffer, 0, VkRect2D.calloc(1, stack).offset(VkOffset2D.calloc(stack).set(0, 0)).extent(VkExtent2D.calloc(stack).width(eWidth).height(eHeight)));
    }
    public static VkRenderingAttachmentInfo.Buffer getColorAttachments(MemoryStack stack, VkCommandBuffer cmdBuffer, Texture[] textures) {
        VkRenderingAttachmentInfo.Buffer attachmentInfo = VkRenderingAttachmentInfo.calloc(textures.length, stack);
        for (int i = 0; i < textures.length; i++) {
            Texture tex = textures[i];
            int prevLayout = tex.isLayoutUnset() ? VK_IMAGE_LAYOUT_UNDEFINED : VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
            ImageHelper.transitionImageLayout(stack, cmdBuffer, VK_IMAGE_ASPECT_COLOR_BIT, tex.image,
                    prevLayout, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                    prevLayout == VK_IMAGE_LAYOUT_UNDEFINED ? VK_ACCESS_2_NONE : VK_ACCESS_2_SHADER_SAMPLED_READ_BIT, VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT,
                    prevLayout == VK_IMAGE_LAYOUT_UNDEFINED ? VK_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT : VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT, VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT);
            attachmentInfo.get(i)
                    .sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO)
                    .imageView(tex.imageView)
                    .imageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
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
        VkRenderingAttachmentInfo depthAttachment = getDepthAttachment(stack, currentCmdBuffer, depthImage, depthImageView, VK_IMAGE_LAYOUT_UNDEFINED);
        VkRect2D renderAreaData = VkRect2D.calloc(stack)
                .offset(VkOffset2D.calloc(stack).set(0, 0))
                .extent(VkExtent2D.calloc(stack).width(eWidth).height(eHeight));
        VkRenderingInfo renderingInfo = VkRenderingInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDERING_INFO)
                .renderArea(renderAreaData)
                .layerCount(1)
                .pColorAttachments(colorAttachment)
                .pDepthAttachment(depthAttachment);
        vkCmdBeginRendering(currentCmdBuffer, renderingInfo);
        currentPipeline = pipelines[0];
        vkCmdBindPipeline(currentCmdBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, currentPipeline.vkPipeline);
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
    public static VkRenderingAttachmentInfo getDepthAttachment(MemoryStack stack, VkCommandBuffer cmdBuffer, long image, long imageView, int prevLayout) {
        if (depthImage == image) {
            ImageHelper.transitionImageLayout(stack, cmdBuffer, VK_IMAGE_ASPECT_DEPTH_BIT, image,
                    VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL,
                    VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT, VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT,
                    VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT, VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT);
        } else {
            ImageHelper.transitionImageLayout(stack, cmdBuffer, VK_IMAGE_ASPECT_DEPTH_BIT, image,
                    prevLayout, VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL,
                    VK_ACCESS_2_NONE, VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT,
                    VK_PIPELINE_STAGE_2_NONE, VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT);
        }
        VkRenderingAttachmentInfo attachmentInfo = VkRenderingAttachmentInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO)
                .imageView(imageView)
                .imageLayout(VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE);
        attachmentInfo.clearValue().depthStencil().depth(0.f);
        return attachmentInfo;
    }

    public static int regionSSBOByteSize = regions.length*8;
    public static int lodSSBOByteSize = lods.length*8;
    public static int gigabyte = 1000000000;
    public static int voxelSSBOSize = gigabyte/2;
    public static int lightSSBOSize = gigabyte*2;
    public static int chunkArrSize = sizeChunks*sizeChunks*heightChunks;
    public static int chunkByteSize = 4*4;
    public static int chunkSSBOSize = chunkArrSize*chunkByteSize;
    public static PointerBuffer blocks = BufferUtils.createPointerBuffer(1);
    public static PointerBuffer lights = BufferUtils.createPointerBuffer(1);
    public static long[] chunkBlockAllocs;;
    public static long[] chunkLightBlockAllocs;
    public static void fillSSBOs() {
        long startTime = System.currentTimeMillis();
        VmaVirtualBlockCreateInfo blockCreateInfo = VmaVirtualBlockCreateInfo.create();
        blockCreateInfo.size(voxelSSBOSize);
        vmaCreateVirtualBlock(blockCreateInfo, blocks);
        chunkBlockAllocs = new long[chunkArrSize];
        VmaVirtualBlockCreateInfo lightBlockCreateInfo = VmaVirtualBlockCreateInfo.create();
        lightBlockCreateInfo.size(lightSSBOSize);
        vmaCreateVirtualBlock(lightBlockCreateInfo, lights);
        chunkLightBlockAllocs = new long[chunkArrSize];

        for (int chunkX = 0; chunkX < sizeChunks; chunkX++) {
            for (int chunkZ = 0; chunkZ < sizeChunks; chunkZ++) {
                for (int chunkY = 0; chunkY < heightChunks; chunkY++) {
                    updateChunk(new Vector3i(chunkX, chunkY, chunkZ));
                }
            }
        }
        System.out.println("Allocated "+allocated+" bytes for light data.");

        long regionPtr = regionSSBO.stagingBuffer.pointer.get(0);
        MemoryUtil.memLongBuffer(regionPtr, regions.length).put(regions).rewind();
        long lodPtr = lodSSBO.stagingBuffer.pointer.get(0);
        MemoryUtil.memLongBuffer(lodPtr, lods.length).put(lods).rewind();

        VkBufferCopy.Buffer regionBufferCopy = VkBufferCopy.calloc(1).srcOffset(0).dstOffset(0).size(regionSSBOByteSize);
        vkCmdCopyBuffer(currentCmdBuffer, regionSSBO.stagingBuffer.buffer[0], regionSSBO.buffer.buffer[0], regionBufferCopy);
        VkBufferCopy.Buffer chunkBufferCopy = VkBufferCopy.calloc(1).srcOffset(0).dstOffset(0).size(chunkSSBOSize);
        vkCmdCopyBuffer(currentCmdBuffer, chunkSSBO.stagingBuffer.buffer[0], chunkSSBO.buffer.buffer[0], chunkBufferCopy);
        VkBufferCopy.Buffer voxelBufferCopy = VkBufferCopy.calloc(1).srcOffset(0).dstOffset(0).size(voxelSSBOSize);
        vkCmdCopyBuffer(currentCmdBuffer, voxelSSBO.stagingBuffer.buffer[0], voxelSSBO.buffer.buffer[0], voxelBufferCopy);
        VkBufferCopy.Buffer lodBufferCopy = VkBufferCopy.calloc(1).srcOffset(0).dstOffset(0).size(lodSSBOByteSize);
        vkCmdCopyBuffer(currentCmdBuffer, lodSSBO.stagingBuffer.buffer[0], lodSSBO.buffer.buffer[0], lodBufferCopy);
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
        barrierBuf.get(3)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(lodSSBO.buffer.buffer[0])
                .offset(0).size(lodSSBOByteSize);
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
