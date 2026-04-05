package org.conspiracraft.renderer;

import org.conspiracraft.Main;
import org.conspiracraft.Utils;
import org.conspiracraft.renderer.buffers.PushUBO;
import org.conspiracraft.renderer.models.Index;
import org.conspiracraft.renderer.models.Models;
import org.conspiracraft.world.World;
import org.joml.Matrix4f;
import org.joml.SimplexNoise;
import org.joml.Vector2i;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.conspiracraft.renderer.Window.*;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK13.*;

public class Renderer {
    public static int imageIdx = 0;
    public static int currentFrame = 0;
    public static int frame = 0;

    public static void render() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (startCommandBuffers(stack)) {
                if (frame < 2) {
                    prepareInstancedTestScene();
                    frame++;
                }
                if (startRenderPass(stack)) {
                    drawFrame(stack);
                    endRenderPass(stack);
                    currentFrame++;
                    if (currentFrame >= MAX_FRAMES_IN_FLIGHT) {currentFrame = 0;}
                }
            }
        }
    }

    public static PushUBO pushUBO = new PushUBO();
    public static void drawFrame(MemoryStack stack) {
        vkCmdBindDescriptorSets(commandBuffers[currentFrame], VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, stack.longs(descriptorSets[currentFrame]), null);
        defaultUBO.update(stack);
        defaultUBO.submit();

        pushUBO.update(1); //draw instanced stuff
        pushUBO.submit();
        drawInstancedTestScene();

        pushUBO.update(0); //draw non-instanced stuff
        pushUBO.submit();
        World.worldType.renderCelestialBodies(stack);
        drawCube(new Matrix4f().translate(512, 15, 512), new Vector4f(1));
    }
    public static void drawCube(Matrix4f modelMatrix, Vector4f color) {
        pushUBO.update(modelMatrix, color);
        pushUBO.submit();
        vkCmdDrawIndexed(commandBuffers[currentFrame], Models.CUBE.indexCount, 1, Models.CUBE.indexOffset/Index.SIZE, 0, 0);
    }
    public static Matrix4f iPos = new Matrix4f();
    public static Vector4f iColor = new Vector4f();
    public static int instances = 1536*1536;
    public static int size = 20*instances;
    public static int sizeBytes = (20*instances)*4;
    public static FloatBuffer testBuf = MemoryUtil.memAllocFloat(size);
    public static VkBufferCopy.Buffer bufferCopy = VkBufferCopy.calloc(1).srcOffset(0).dstOffset(0).size(sizeBytes);
    public static void prepareInstancedTestScene() {
        int i = 0;
        int max = 0;
        for (int x = 0; x < 1536; x++) {
            for (int z = 0; z < 1536; z++) {
                if (new Vector2i(x, z).distance(new Vector2i(768, 768)) < 768) {
                    max = Math.max(max, x);
                    double mountainNoise = (1 + Math.max(0, SimplexNoise.noise(x / 500.f, z / 500.f)));
                    double elevationNoise = SimplexNoise.noise(x / 1000.f, z / 1000.f) + 0.5f;
                    double elevationMul = mountainNoise * elevationNoise;
                    double detailNoise = (SimplexNoise.noise(x / 100.f, z / 100.f) * 16);
                    int y = (int) (detailNoise * Math.max(0.f, elevationMul));
                    float waterTint = Math.min(0.f, y * 0.05f);
                    if (y < 0) {
                        y *= -0.25f;
                    }
                    iPos.setTranslation(x, y, z).get(i * 20, testBuf);
                    (y < 1 ? iColor.set(0.15f, 0.75f - waterTint, 0.95f, 1.f) : (y < 2 ? iColor.set(0.95f * 0.97f, 0.93f * 0.97f, 0.85f * 0.97f, 1.f) : y < 3 ? iColor.set(0.95f, 0.93f, 0.85f, 1.f) : (iColor.set(0.5f, Utils.gradient(y, 3, 24, 0.6f, 0.95f), 0.5f, 1.f)))).get((i * 20) + 16, testBuf);
                    i++;
                }
            }
        }
        memCopy(memAddress(testBuf), Main.window.instanceStagingBufMemPointer[currentFrame], sizeBytes);
        vkCmdCopyBuffer(commandBuffers[currentFrame], instanceStagingBuffers[currentFrame], instanceBuffers[currentFrame], bufferCopy);
        VkBufferMemoryBarrier.Buffer barrierBuf = VkBufferMemoryBarrier.calloc(1)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(instanceBuffers[currentFrame])
                .offset(0).size(sizeBytes);
        vkCmdPipelineBarrier(commandBuffers[currentFrame], VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_VERTEX_SHADER_BIT, 0, null, barrierBuf, null);
    }
    public static void drawInstancedTestScene() {
        vkCmdDrawIndexed(commandBuffers[currentFrame], Models.CUBE.indexCount, instances, Models.CUBE.indexOffset/Index.SIZE, 0, 0);
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
    }
    public static boolean startRenderPass(MemoryStack stack) {
        VkRect2D renderAreaData = VkRect2D.calloc(stack)
                .offset(VkOffset2D.calloc(stack).set(0, 0))
                .extent(VkExtent2D.calloc(stack).width(eWidth).height(eHeight));
        VkClearValue.Buffer clearValues = VkClearValue.calloc(2, stack);
        clearValues.get(0).color().float32(0, 10.0f).float32(1, 10.0f).float32(2, 10.0f).float32(3, 0.0f);
        clearValues.get(1).depthStencil().depth(0.f).stencil(0);
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
        vkCmdBindIndexBuffer(commandBuffers[currentFrame], indexBuffer[0], 0, VK_INDEX_TYPE_UINT32);
        return true;
    }
    public static boolean startCommandBuffers(MemoryStack stack) {
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
        return true;
    }
}
