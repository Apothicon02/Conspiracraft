package org.conspiracraft.renderer;

import org.conspiracraft.Main;
import org.conspiracraft.renderer.buffers.PUSH_UBO;
import org.conspiracraft.renderer.models.Models;
import org.conspiracraft.renderer.models.Vertex;
import org.conspiracraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.conspiracraft.renderer.Window.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK13.*;

public class Renderer {
    public static int imageIdx = 0;
    public static int currentFrame = 0;

    public static void render() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            boolean started = startRenderPass(stack);
            if (started) {
                drawFrame(stack);
                endRenderPass(stack);
            }
        }
    }

    public static PUSH_UBO pushUBO = new PUSH_UBO();
    public static void drawFrame(MemoryStack stack) {
        vkCmdBindDescriptorSets(commandBuffers[currentFrame], VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, stack.longs(descriptorSets[currentFrame]), null);
        defaultUBO.update(stack);
        defaultUBO.submit();
        drawCube(stack, new Matrix4f().translate(-5, 0, -5), new Vector4f(1));
        World.worldType.renderCelestialBodies(stack);
    }
    public static void drawCube(MemoryStack stack, Matrix4f modelMatrix, Vector4f color) {
        pushUBO.update(stack, modelMatrix, color);
        pushUBO.submit();
        vkCmdDraw(commandBuffers[currentFrame], Models.CUBE.vertexCount, 1, Models.CUBE.offset/Vertex.SIZE, 0);
    }

    public static void endRenderPass(MemoryStack stack) {
        vkCmdEndRenderPass(commandBuffers[currentFrame]);
        vkEndCommandBuffer(commandBuffers[currentFrame]);

        VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .waitSemaphoreCount(1)
                .pWaitSemaphores(stack.mallocLong(1).put(imageAvailableSemaphores[currentFrame]).flip())
                .pWaitDstStageMask(stack.mallocInt(1).put(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT).flip())
                .pCommandBuffers(stack.pointers(commandBuffers[currentFrame]))
                .pSignalSemaphores(stack.mallocLong(1).put(renderFinishedSemaphores[currentFrame]).flip());
        if (vkQueueSubmit(graphicsQueue, submitInfo, inFlightFences[currentFrame]) != VK_SUCCESS) {
            throw new RuntimeException("Failed to submit draw command buffer!");
        }
        LongBuffer swapchainBuf = stack.longs(1).put(swapchain).flip();
        VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                .pWaitSemaphores(stack.mallocLong(1).put(renderFinishedSemaphores[currentFrame]).flip())
                .pSwapchains(swapchainBuf)
                .pImageIndices(stack.mallocInt(1).put(imageIdx).flip())
                .swapchainCount(swapchainBuf.remaining());
        int result = vkQueuePresentKHR(presentQueue, presentInfo);
        if (result == VK_ERROR_OUT_OF_DATE_KHR) {
            Main.window.recreateSwapchain();
        } else if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {
            throw new RuntimeException("Failed to queue present!");
        }
        currentFrame++;
        if (currentFrame >= MAX_FRAMES_IN_FLIGHT) {currentFrame = 0;}
    }
    public static boolean startRenderPass(MemoryStack stack) {
        vkWaitForFences(device, inFlightFences[currentFrame], false, Long.MAX_VALUE);
        IntBuffer imageIdxBuf = stack.mallocInt(1);
        int result = vkAcquireNextImageKHR(device, swapchain, Long.MAX_VALUE, imageAvailableSemaphores[currentFrame], VK_NULL_HANDLE, imageIdxBuf);
        if (result == VK_ERROR_OUT_OF_DATE_KHR) {
            Main.window.recreateSwapchain();
            return false;
        } else if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {
            System.err.println("Failed to acquire next image!");
        }
        imageIdx = imageIdxBuf.get(0);
        vkResetFences(device, inFlightFences[currentFrame]);

        vkResetCommandBuffer(commandBuffers[currentFrame], 0);

        VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
                .pInheritanceInfo(null);
        if (vkBeginCommandBuffer(commandBuffers[currentFrame], beginInfo) != VK_SUCCESS) {
            throw new RuntimeException("Failed to begin recording command buffer!");
        }

        VkRect2D renderAreaData = VkRect2D.calloc(stack)
                .offset(VkOffset2D.calloc(stack).set(0, 0))
                .extent(VkExtent2D.calloc(stack).width(eWidth).height(eHeight));
        VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
        clearValues.color().float32(0, 0.0f).float32(1, 0.0f).float32(2, 0.0f).float32(3, 0.0f);
        VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                .renderPass(renderPass)
                .framebuffer(swapchainFramebuffers[imageIdx])
                .renderArea(renderAreaData)
                .clearValueCount(1)
                .pClearValues(clearValues);
        vkCmdBeginRenderPass(commandBuffers[currentFrame], renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

        vkCmdBindPipeline(commandBuffers[currentFrame], VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);

        VkViewport.Buffer viewport = VkViewport.calloc(1, stack)
                .x(0.0f)
                .y(0.0f)
                .width(eWidth)
                .height(eHeight)
                .minDepth(0.0f)
                .maxDepth(1.0f);
        vkCmdSetViewport(commandBuffers[currentFrame], 0, viewport);

        VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack)
                .offset(VkOffset2D.calloc(stack).set(0, 0))
                .extent(VkExtent2D.calloc(stack).width(eWidth).height(eHeight));
        vkCmdSetScissor(commandBuffers[currentFrame], 0, scissor);
        vkCmdBindVertexBuffers(commandBuffers[currentFrame], 0, stack.longs(vertexBuffer), stack.longs(0));
        return true;
    }
}
