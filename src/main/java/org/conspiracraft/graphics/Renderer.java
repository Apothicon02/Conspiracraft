package org.conspiracraft.graphics;

import org.conspiracraft.graphics.buffers.CmdBufferHelper;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;

import static org.conspiracraft.graphics.Graphics.globalUBO;
import static org.conspiracraft.graphics.Pipeline.pipelineLayout;
import static org.conspiracraft.graphics.buffers.CmdBuffer.cmdBuffer;
import static org.conspiracraft.graphics.Device.*;
import static org.conspiracraft.graphics.Pipeline.graphicsPipeline;
import static org.conspiracraft.graphics.Swapchain.*;
import static org.conspiracraft.graphics.SyncObjects.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK14.*;

public class Renderer {
    public static int imageIdx = 0;
    public static int frameIdx = 0;
    public static boolean firstFrames = true;
    public static VkCommandBuffer currentCmdBuffer;
    public static void render() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (startCommandBuffers(stack)) {
                vkCmdBindDescriptorSets(currentCmdBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, stack.longs(Descriptors.descriptorSets[frameIdx]), null);
                globalUBO.update(stack);
                globalUBO.submit();

                bindImageToDrawTo(stack);
                vkCmdDraw(currentCmdBuffer, 3, 1, 0, 0);
                unbindImageDrawingTo(stack);

                submitCommandBuffers(stack);

                frameIdx++;
                if (frameIdx >= FRAMES_IN_FLIGHT) {
                    frameIdx = 0;
                    firstFrames = false;
                }
            }
        }
    }


    public static boolean startCommandBuffers(MemoryStack stack) {
        vkWaitForFences(vkDevice, inFlightFences[frameIdx], false, Long.MAX_VALUE);
        vkResetFences(vkDevice, inFlightFences[frameIdx]);

        IntBuffer imageIdxBuf = stack.mallocInt(1);
        int result = vkAcquireNextImageKHR(vkDevice, vkSwapchain, Long.MAX_VALUE, imageAvailableSemaphores[frameIdx], VK_NULL_HANDLE, imageIdxBuf);
        if (result == VK_ERROR_OUT_OF_DATE_KHR) {
            //Window.graphics.recreateSwapchain();
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
            // recreateSwapchain();
        } else if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {
            throw new RuntimeException("Failed to queue present!");
        }
    }
    public static void unbindImageDrawingTo(MemoryStack stack) {
        vkCmdEndRendering(currentCmdBuffer);
        ImageHelper.transitionImageLayout(stack, currentCmdBuffer, VK_IMAGE_ASPECT_COLOR_BIT, Swapchain.images[imageIdx],
                VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
                0, VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT,
                VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT, VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT);
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
                firstFrames ? VK_IMAGE_LAYOUT_UNDEFINED : VK_IMAGE_LAYOUT_PRESENT_SRC_KHR, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
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

//    public static Matrix4f iPos = new Matrix4f();
//    public static Vector4f iColor = new Vector4f();
//    public static int instances = 1536*1536;
//    public static int size = 20*instances;
//    public static int sizeBytes = (20*instances)*4;
//    public static FloatBuffer testBuf = MemoryUtil.memAllocFloat(size);
//    public static VkBufferCopy.Buffer bufferCopy = VkBufferCopy.calloc(1).srcOffset(0).dstOffset(0).size(sizeBytes);
//    public static void prepareInstancedTestScene() {
//        int i = 0;
//        int max = 0;
//        for (int x = 0; x < 1536; x++) {
//            for (int z = 0; z < 1536; z++) {
//                if (new Vector2i(x, z).distance(new Vector2i(768, 768)) < 768) {
//                    max = Math.max(max, x);
//                    double mountainNoise = (1 + Math.max(0, SimplexNoise.noise(x / 500.f, z / 500.f)));
//                    double elevationNoise = SimplexNoise.noise(x / 1000.f, z / 1000.f) + 0.5f;
//                    double elevationMul = mountainNoise * elevationNoise;
//                    double detailNoise = (SimplexNoise.noise(x / 100.f, z / 100.f) * 16);
//                    int y = (int) (detailNoise * Math.max(0.f, elevationMul));
//                    float waterTint = Math.min(0.f, y * 0.05f);
//                    if (y < 0) {
//                        y *= -0.25f;
//                    }
//                    iPos.setTranslation(x, y, z).get(i * 20, testBuf);
//                    (y < 1 ? iColor.set(0.15f, 0.75f - waterTint, 0.95f, 1.f) : (y < 2 ? iColor.set(0.95f * 0.97f, 0.93f * 0.97f, 0.85f * 0.97f, 1.f) : y < 3 ? iColor.set(0.95f, 0.93f, 0.85f, 1.f) : (iColor.set(0.5f, Utils.gradient(y, 3, 24, 0.6f, 0.95f), 0.5f, 1.f)))).get((i * 20) + 16, testBuf);
//                    i++;
//                }
//            }
//        }
//    }
}
