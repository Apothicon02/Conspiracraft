package org.conspiracraft.graphics;

import org.conspiracraft.Main;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.graphics.buffers.CmdBufferHelper;
import org.conspiracraft.graphics.textures.ImageHelper;
import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.world.World;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaVirtualAllocationCreateInfo;
import org.lwjgl.util.vma.VmaVirtualBlockCreateInfo;
import org.lwjgl.vulkan.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.conspiracraft.graphics.Graphics.globalUBO;
import static org.conspiracraft.graphics.Graphics.lodSSBO;
import static org.conspiracraft.graphics.Pipeline.pipelineLayout;
import static org.conspiracraft.graphics.buffers.CmdBuffer.cmdBuffer;
import static org.conspiracraft.graphics.Device.*;
import static org.conspiracraft.graphics.Pipeline.graphicsPipeline;
import static org.conspiracraft.graphics.Swapchain.*;
import static org.conspiracraft.graphics.SyncObjects.*;
import static org.conspiracraft.world.World.*;
import static org.lwjgl.util.vma.Vma.vmaCreateVirtualBlock;
import static org.lwjgl.util.vma.Vma.vmaVirtualAllocate;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK14.*;

public class Renderer {
    public static int imageIdx = 0;
    public static int frameIdx = 0;
    public static boolean firstImages = true;
    public static boolean initialized = false;
    public static VkCommandBuffer currentCmdBuffer;
    public static void render() throws IOException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (startCommandBuffers(stack)) {
                if (!initialized) {
                    prepareTestScene();
                    BufferedImage atlasImage = Utils.loadImage("generic/texture/atlas");
//                    for (int x = 0; x < Textures.atlas.width; x++) {
//                        for (int y = 0; y < 1024; y++) {
//                            Color color = new Color(atlasImage.getRGB(x, y), true);
//                            collisionData[(x * 1024) + y] = color.getAlpha() != 0;
//                        }
//                    }
                    ImageHelper.fillImage(stack, Textures.atlas, Utils.imageToBuffer(atlasImage));
                    initialized = true;
                } else {
//                    long basePtr = Graphics.voxelSSBO.stagingBuffer.pointer.get(0);
//                    long offsetSize = (packPos(512, 196, 512)*4L);
//                    long offsetPtr = basePtr+offsetSize;
//                    VkBufferCopy.Buffer bufferCopy = VkBufferCopy.calloc(1).srcOffset(offsetSize).dstOffset(offsetSize).size(4);
//                    MemoryUtil.memPutInt(offsetPtr, 2);
//                    vkCmdCopyBuffer(currentCmdBuffer, Graphics.voxelSSBO.stagingBuffer.buffer[0], Graphics.voxelSSBO.buffer.buffer[0], bufferCopy);
//                    VkBufferMemoryBarrier.Buffer barrierBuf = VkBufferMemoryBarrier.calloc(1)
//                            .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
//                            .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
//                            .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
//                            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
//                            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
//                            .buffer(Graphics.voxelSSBO.buffer.buffer[0])
//                            .offset(0).size(defaultSSBOSize);
//                    vkCmdPipelineBarrier(currentCmdBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_VERTEX_SHADER_BIT, 0, null, barrierBuf, null);
                }
                vkCmdBindDescriptorSets(currentCmdBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, stack.longs(Descriptors.descriptorSets[frameIdx]), null);
                globalUBO.update(stack);
                globalUBO.submit();

                bindImageToDrawTo(stack);
                vkCmdDraw(currentCmdBuffer, 3, 1, 0, 0);
                unbindImageDrawingTo(stack);

                submitCommandBuffers(stack);

                frameIdx++;
                if (frameIdx >= FRAMES_IN_FLIGHT) {frameIdx = 0;}
                if (imageIdx >= Swapchain.images.length) {firstImages = false;}
            }
        }
    }


    public static boolean startCommandBuffers(MemoryStack stack) {
        int waitResult = vkWaitForFences(vkDevice, inFlightFences[frameIdx], false, Long.MAX_VALUE);
        if (waitResult != VK_SUCCESS) {throw new RuntimeException("Failed to wait for fences: "+waitResult);}
        vkResetFences(vkDevice, inFlightFences[frameIdx]);

        vkResetFences(vkDevice, inFlightFences[frameIdx]);

        IntBuffer imageIdxBuf = stack.mallocInt(1);
        int result = vkAcquireNextImageKHR(vkDevice, vkSwapchain, Long.MAX_VALUE, imageAvailableSemaphores[frameIdx], VK_NULL_HANDLE, imageIdxBuf);
        if (result == VK_ERROR_OUT_OF_DATE_KHR) {
            Graphics.rebuild();
            return false;
        } else if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {System.err.println("Failed to acquire next image!");}
        imageIdx = imageIdxBuf.get(0);

        currentCmdBuffer = cmdBuffer[frameIdx];
        vkResetCommandBuffer(currentCmdBuffer, 0);
        CmdBufferHelper.recordCmdBuffer(stack, currentCmdBuffer);
        return true;
    }
    public static void submitCommandBuffers(MemoryStack stack) {
        vkEndCommandBuffer(currentCmdBuffer);
        VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .waitSemaphoreCount(1)
                .pWaitSemaphores(stack.longs(imageAvailableSemaphores[frameIdx]))
                .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                .pCommandBuffers(stack.pointers(currentCmdBuffer.address()))
                .pSignalSemaphores(stack.longs(renderFinishedSemaphores[imageIdx]));
        vkQueueSubmit(graphicsQueue, submitInfo, inFlightFences[frameIdx]);

        VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                .pWaitSemaphores(stack.longs(renderFinishedSemaphores[imageIdx]))
                .swapchainCount(1)
                .pSwapchains(stack.longs(vkSwapchain))
                .pImageIndices(stack.ints(imageIdx));
        int result = vkQueuePresentKHR(graphicsQueue, presentInfo);
        if (result == VK_ERROR_OUT_OF_DATE_KHR) {
            Graphics.rebuild();
        } else if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {
            throw new RuntimeException("Failed to queue present!");
        }
    }
    public static void unbindImageDrawingTo(MemoryStack stack) {
        vkCmdEndRendering(currentCmdBuffer);
        ImageHelper.transitionImageLayout(stack, currentCmdBuffer, VK_IMAGE_ASPECT_COLOR_BIT, Swapchain.images[imageIdx],
                VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
                VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT, VK_ACCESS_2_NONE,
                VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT, VK_PIPELINE_STAGE_2_NONE);
    }
    public static void bindImageToDrawTo(MemoryStack stack) {
        VkRenderingAttachmentInfo.Buffer colorAttachment = getColorAttachment(stack, currentCmdBuffer);
        VkRenderingAttachmentInfo depthAttachment = getDepthAttachment(stack, currentCmdBuffer);
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
        vkCmdBindPipeline(currentCmdBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);
        vkCmdSetViewport(currentCmdBuffer, 0, VkViewport.calloc(1, stack).x(0).y(0).width(eWidth).height(eHeight).minDepth(0).maxDepth(1));
        vkCmdSetScissor(currentCmdBuffer, 0, VkRect2D.calloc(1, stack).offset(VkOffset2D.calloc(stack).set(0, 0)).extent(VkExtent2D.calloc(stack).width(eWidth).height(eHeight)));
    }

    public static VkRenderingAttachmentInfo.Buffer getColorAttachment(MemoryStack stack, VkCommandBuffer cmdBuffer) {
        ImageHelper.transitionImageLayout(stack, cmdBuffer, VK_IMAGE_ASPECT_COLOR_BIT, Swapchain.images[imageIdx],
                firstImages ? VK_IMAGE_LAYOUT_UNDEFINED : VK_IMAGE_LAYOUT_PRESENT_SRC_KHR, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                VK_ACCESS_2_NONE, VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT,
                VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT, VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT);
        VkRenderingAttachmentInfo.Buffer attachmentInfo = VkRenderingAttachmentInfo.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO)
                .imageView(Swapchain.imageViews[imageIdx])
                .imageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE);
        attachmentInfo.clearValue().color().float32(0, 0.0f).float32(1, 0.0f).float32(2, 0.0f).float32(3, 0.0f);
        return attachmentInfo;
    }
    public static VkRenderingAttachmentInfo getDepthAttachment(MemoryStack stack, VkCommandBuffer cmdBuffer) {
        ImageHelper.transitionImageLayout(stack, cmdBuffer, VK_IMAGE_ASPECT_DEPTH_BIT, Swapchain.depthImage,
                VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL,
                VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT, VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT,
                VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT, VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT);
        VkRenderingAttachmentInfo attachmentInfo = VkRenderingAttachmentInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO)
                .imageView(Swapchain.depthImageView)
                .imageLayout(VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE);
        attachmentInfo.clearValue().depthStencil().depth(0.f);
        return attachmentInfo;
    }

    public static int lodSSBOSize = World.lods.length*8;
    public static int gigabyte = 1000000000;
    public static int voxelSSBOSize = gigabyte/6;
    public static int chunkArrSize = sizeChunks*sizeChunks*heightChunks;
    public static int chunkByteSize = 4*4;
    public static int chunkSSBOSize = chunkArrSize*chunkByteSize;
    public static PointerBuffer blocks = BufferUtils.createPointerBuffer(1);
    public static long[] chunkBlockAllocs;
    public static void prepareTestScene() {
        worldType.generate();
        VmaVirtualBlockCreateInfo blockCreateInfo = VmaVirtualBlockCreateInfo.create();
        blockCreateInfo.size(voxelSSBOSize);
        vmaCreateVirtualBlock(blockCreateInfo, blocks);
        long chunkPtr = Graphics.chunkSSBO.stagingBuffer.pointer.get(0);
        long voxelPtr = Graphics.voxelSSBO.stagingBuffer.pointer.get(0);
        chunkBlockAllocs = new long[chunkArrSize];

        outerLoop:
        for (int chunkX = 0; chunkX < sizeChunks; chunkX++) {
            for (int chunkZ = 0; chunkZ < sizeChunks; chunkZ++) {
                for (int chunkY = 0; chunkY < heightChunks; chunkY++) {
                    VmaVirtualAllocationCreateInfo allocCreateInfo = VmaVirtualAllocationCreateInfo.create();
                    int packedChunkPos = World.packChunkPos(chunkX, chunkY, chunkZ);
                    int paletteSize = chunks[packedChunkPos].getBlockPaletteSize();
                    int bitsPerValue = chunks[packedChunkPos].bitsPerBlock();
                    int valueMask = chunks[packedChunkPos].blockValueMask();
                    int[] compressedBlocks = chunks[packedChunkPos].getBlockData();
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
                        MemoryUtil.memCopy(MemoryUtil.memAddress(MemoryUtil.memAlloc(chunkByteSize).asIntBuffer()
                                        .put(new int[]{pointer/4, paletteSize, bitsPerValue, valueMask}).rewind()),
                                chunkPtr+((long) packedChunkPos * chunkByteSize), chunkByteSize);
                        int[] palette = chunks[packedChunkPos].getBlockPalette();
                        int paletteSizeBytes = paletteSize*4;
                        MemoryUtil.memCopy(MemoryUtil.memAddress(MemoryUtil.memAlloc(paletteSizeBytes).asIntBuffer().put(palette).rewind()), voxelPtr+pointer, paletteSizeBytes);
                        if (compressedBlocks != null) {
                            MemoryUtil.memCopy(MemoryUtil.memAddress(MemoryUtil.memAlloc(compressedBlocks.length*4).asIntBuffer().put(compressedBlocks).rewind()), voxelPtr+pointer+paletteSizeBytes, compressedBlocks.length*4L);
                        }
                    } else {
                        System.out.print("blocksSSBO ran out of space! \n");
                        Main.isClosing = true;
                        break outerLoop;
                    }
                }
            }
        }

        long lodPtr = Graphics.lodSSBO.stagingBuffer.pointer.get(0);
        MemoryUtil.memByteBuffer(lodPtr, lodSSBOSize).asLongBuffer().put(World.lods).rewind();

        VkBufferCopy.Buffer chunkBufferCopy = VkBufferCopy.calloc(1).srcOffset(0).dstOffset(0).size(chunkSSBOSize);
        vkCmdCopyBuffer(currentCmdBuffer, Graphics.chunkSSBO.stagingBuffer.buffer[0], Graphics.chunkSSBO.buffer.buffer[0], chunkBufferCopy);
        VkBufferCopy.Buffer voxelBufferCopy = VkBufferCopy.calloc(1).srcOffset(0).dstOffset(0).size(voxelSSBOSize);
        vkCmdCopyBuffer(currentCmdBuffer, Graphics.voxelSSBO.stagingBuffer.buffer[0], Graphics.voxelSSBO.buffer.buffer[0], voxelBufferCopy);
        VkBufferCopy.Buffer lodBufferCopy = VkBufferCopy.calloc(1).srcOffset(0).dstOffset(0).size(lodSSBOSize);
        vkCmdCopyBuffer(currentCmdBuffer, Graphics.lodSSBO.stagingBuffer.buffer[0], Graphics.lodSSBO.buffer.buffer[0], lodBufferCopy);
        VkBufferMemoryBarrier.Buffer barrierBuf = VkBufferMemoryBarrier.calloc(3);
        barrierBuf.get(0)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(Graphics.chunkSSBO.buffer.buffer[0])
                .offset(0).size(chunkSSBOSize);
        barrierBuf.get(1)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(Graphics.voxelSSBO.buffer.buffer[0])
                .offset(0).size(voxelSSBOSize);
        barrierBuf.get(2)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(Graphics.lodSSBO.buffer.buffer[0])
                .offset(0).size(lodSSBOSize);
        vkCmdPipelineBarrier(currentCmdBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, null, barrierBuf, null);
    }
}
