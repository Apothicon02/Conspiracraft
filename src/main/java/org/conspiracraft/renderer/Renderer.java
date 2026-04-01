package org.conspiracraft.renderer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.conspiracraft.renderer.Window.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK13.*;

public class Renderer {
    public static int imageIdx = 0;

    public static void render() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            startRenderPass(stack);
            drawFrame(stack);
            endRenderPass(stack);
        }
    }

    public static void drawFrame(MemoryStack stack) {
        vkCmdDraw(commandBuffer, 3, 1, 0, 0);
    }
    public static void endRenderPass(MemoryStack stack) {
        vkCmdEndRenderPass(commandBuffer);
        vkEndCommandBuffer(commandBuffer);

        VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .waitSemaphoreCount(1)
                .pWaitSemaphores(stack.mallocLong(1).put(imageAvailableSemaphore).flip())
                .pWaitDstStageMask(stack.mallocInt(1).put(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT).flip())
                .pCommandBuffers(stack.pointers(commandBuffer))
                .pSignalSemaphores(stack.mallocLong(1).put(renderFinishedSemaphore).flip());
        if (vkQueueSubmit(graphicsQueue, submitInfo, inFlightFence) != VK_SUCCESS) {
            throw new RuntimeException("Failed to submit draw command buffer!");
        }
        LongBuffer swapchainBuf = stack.longs(1).put(swapchain).flip();
        VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                .pWaitSemaphores(stack.mallocLong(1).put(renderFinishedSemaphore).flip())
                .pSwapchains(swapchainBuf)
                .pImageIndices(stack.mallocInt(1).put(imageIdx).flip())
                .swapchainCount(swapchainBuf.remaining());
        if (vkQueuePresentKHR(presentQueue, presentInfo) != VK_SUCCESS) {
            throw new RuntimeException("Failed to queue present!");
        }
    }
    public static void startRenderPass(MemoryStack stack) {
        vkWaitForFences(device, inFlightFence, true, Long.MAX_VALUE);
        vkResetFences(device, inFlightFence);
        IntBuffer imageIdxBuf = stack.mallocInt(1);
        if (vkAcquireNextImageKHR(device, swapchain, Long.MAX_VALUE, imageAvailableSemaphore, VK_NULL_HANDLE, imageIdxBuf) != VK_SUCCESS) {
            System.err.println("Failed to acquire next image!");
        }
        imageIdx = imageIdxBuf.get(0);

        vkResetCommandBuffer(commandBuffer, 0);

        VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
                .pInheritanceInfo(null);
        if (vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
            throw new RuntimeException("Failed to begin recording command buffer!");
        }

        VkRect2D renderAreaData = VkRect2D.calloc(stack)
                .offset(VkOffset2D.calloc(stack).set(0, 0))
                .extent(VkExtent2D.calloc(stack).width(eWidth).height(eHeight));
        VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
        clearValues.color().float32(0, 1.0f).float32(1, 0.0f).float32(2, 0.0f).float32(3, 1.0f);
        VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                .renderPass(renderPass)
                .framebuffer(swapchainFramebuffers[imageIdx])
                .renderArea(renderAreaData)
                .clearValueCount(1)
                .pClearValues(clearValues);
        vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);

        VkViewport.Buffer viewport = VkViewport.calloc(1, stack)
                .x(0.0f)
                .y(0.0f)
                .width(eWidth)
                .height(eHeight)
                .minDepth(0.0f)
                .maxDepth(1.0f);
        vkCmdSetViewport(commandBuffer, 0, viewport);

        VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack)
                .offset(VkOffset2D.calloc(stack).set(0, 0))
                .extent(VkExtent2D.calloc(stack).width(eWidth).height(eHeight));
        vkCmdSetScissor(commandBuffer, 0, scissor);
    }
}
