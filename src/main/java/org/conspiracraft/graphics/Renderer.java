package org.conspiracraft.graphics;

import org.conspiracraft.graphics.buffers.CmdBufferHelper;
import org.conspiracraft.world.World;
import org.joml.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.lang.Math;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.conspiracraft.graphics.Graphics.globalUBO;
import static org.conspiracraft.graphics.Pipeline.pipelineLayout;
import static org.conspiracraft.graphics.buffers.CmdBuffer.cmdBuffer;
import static org.conspiracraft.graphics.Device.*;
import static org.conspiracraft.graphics.Pipeline.graphicsPipeline;
import static org.conspiracraft.graphics.Swapchain.*;
import static org.conspiracraft.graphics.SyncObjects.*;
import static org.conspiracraft.world.World.packPos;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK14.*;

public class Renderer {
    public static int imageIdx = 0;
    public static int frameIdx = 0;
    public static boolean firstImages = true;
    public static boolean initialized = false;
    public static VkCommandBuffer currentCmdBuffer;
    public static void render() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (startCommandBuffers(stack)) {
                if (!initialized) {
                    prepareTestScene();
                    initialized = true;
                } else {
                    long basePtr = Graphics.voxelSSBO.stagingBuffer.pointer.get(0);
                    long offsetSize = (packPos(512, 196, 512)*4L);
                    long offsetPtr = basePtr+offsetSize;
                    VkBufferCopy.Buffer bufferCopy = VkBufferCopy.calloc(1).srcOffset(offsetSize).dstOffset(offsetSize).size(4);
                    MemoryUtil.memPutInt(offsetPtr, 2);
                    vkCmdCopyBuffer(currentCmdBuffer, Graphics.voxelSSBO.stagingBuffer.buffer[0], Graphics.voxelSSBO.buffer.buffer[0], bufferCopy);
                    VkBufferMemoryBarrier.Buffer barrierBuf = VkBufferMemoryBarrier.calloc(1)
                            .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                            .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                            .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                            .buffer(Graphics.voxelSSBO.buffer.buffer[0])
                            .offset(0).size(sizeBytes);
                    vkCmdPipelineBarrier(currentCmdBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_VERTEX_SHADER_BIT, 0, null, barrierBuf, null);
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
        vkWaitForFences(vkDevice, inFlightFences[frameIdx], false, Long.MAX_VALUE);
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

    public static int size = World.size*World.height*World.size;
    public static int sizeBytes = size*4;
    public static void prepareTestScene() {
        VkBufferCopy.Buffer bufferCopy = VkBufferCopy.calloc(1).srcOffset(0).dstOffset(0).size(sizeBytes);
        long basePtr = Graphics.voxelSSBO.stagingBuffer.pointer.get(0);
        for (int x = 0; x < World.size; x++) {
            for (int z = 0; z < World.size; z++) {
                double mountainNoise = (1 + Math.max(0, SimplexNoise.noise(x / 500.f, z / 500.f)));
                double elevationNoise = SimplexNoise.noise(x / 1000.f, z / 1000.f) + 0.5f;
                double elevationMul = mountainNoise * elevationNoise;
                double detailNoise = (SimplexNoise.noise(x / 100.f, z / 100.f) * 16);
                int elevation = (int) (detailNoise * Math.max(0.f, elevationMul));
                if (elevation < 0) {
                    elevation *= -0.25f;
                }
                elevation += 63;
                for (int y = elevation; y >= 0; y--) {
                    MemoryUtil.memPutInt(basePtr+(packPos(x, y, z)*4L), y <= 63 ? 1 : (y < 66 ? 2 : 3));
                }
            }
        }
        vkCmdCopyBuffer(currentCmdBuffer, Graphics.voxelSSBO.stagingBuffer.buffer[0], Graphics.voxelSSBO.buffer.buffer[0], bufferCopy);
        VkBufferMemoryBarrier.Buffer barrierBuf = VkBufferMemoryBarrier.calloc(1)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(Graphics.voxelSSBO.buffer.buffer[0])
                .offset(0).size(sizeBytes);
        vkCmdPipelineBarrier(currentCmdBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_VERTEX_SHADER_BIT, 0, null, barrierBuf, null);
    }
}
