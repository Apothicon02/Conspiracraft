package org.conspiracraft.graphics;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.conspiracraft.graphics.CmdBuffer.cmdBuffer;
import static org.conspiracraft.graphics.Pipeline.graphicsPipeline;
import static org.conspiracraft.graphics.Swapchain.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK14.*;

public class Renderer {
    public static int currentFrame = 0;
    public static void render() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            CmdBufferHelper.recordCmdBuffer(stack, cmdBuffer);

            bindImageToDrawTo(stack);
            vkCmdDraw(cmdBuffer, 3, 1, 0, 0);
            unbindImageDrawingTo(stack);

            vkEndCommandBuffer(cmdBuffer);
            currentFrame++;
            if (currentFrame >= FRAMES_IN_FLIGHT) {
                currentFrame = 0;
            }
        }
    }



    public static void unbindImageDrawingTo(MemoryStack stack) {
        vkCmdEndRendering(cmdBuffer);
        ImageHelper.transitionImageLayout(stack, cmdBuffer, VK_IMAGE_ASPECT_COLOR_BIT, Swapchain.images[currentFrame], VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR, 0, 0, VK_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_2_BOTTOM_OF_PIPE_BIT);
    }
    public static void bindImageToDrawTo(MemoryStack stack) {
        VkRenderingAttachmentInfo.Buffer colorAttachment = getColorAttachment(stack, cmdBuffer);
        VkRenderingAttachmentInfo depthAttachment = getDepthAttachment(stack, cmdBuffer);
        VkRect2D renderAreaData = VkRect2D.calloc(stack)
                .offset(VkOffset2D.calloc(stack).set(0, 0))
                .extent(VkExtent2D.calloc(stack).width(eWidth).height(eHeight));
        VkRenderingInfo renderingInfo = VkRenderingInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDERING_INFO)
                .renderArea(renderAreaData)
                .layerCount(1)
                .pColorAttachments(colorAttachment)
                .pDepthAttachment(depthAttachment);
        vkCmdBeginRendering(cmdBuffer, renderingInfo);
        vkCmdBindPipeline(cmdBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);
        vkCmdSetViewport(cmdBuffer, 0, VkViewport.calloc(1, stack).x(0).y(0).width(eWidth).height(eHeight).minDepth(0).maxDepth(1));
        vkCmdSetScissor(cmdBuffer, 0, VkRect2D.calloc(1, stack).offset(VkOffset2D.calloc(stack).set(0, 0)).extent(VkExtent2D.calloc(stack).width(eWidth).height(eHeight)));
    }

    public static VkRenderingAttachmentInfo.Buffer getColorAttachment(MemoryStack stack, VkCommandBuffer cmdBuffer) {
        ImageHelper.transitionImageLayout(stack, cmdBuffer, VK_IMAGE_ASPECT_COLOR_BIT, Swapchain.images[currentFrame], VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, 0, 0, VK_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT);
        VkRenderingAttachmentInfo.Buffer attachmentInfo = VkRenderingAttachmentInfo.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO)
                .imageView(Swapchain.imageViews[currentFrame])
                .imageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE);
        attachmentInfo.clearValue().color().float32(0, 10.0f).float32(1, 10.0f).float32(2, 10.0f).float32(3, 0.0f);
        return attachmentInfo;
    }
    public static VkRenderingAttachmentInfo getDepthAttachment(MemoryStack stack, VkCommandBuffer cmdBuffer) {
        ImageHelper.transitionImageLayout(stack, cmdBuffer, VK_IMAGE_ASPECT_DEPTH_BIT, Swapchain.depthImages[currentFrame], VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL, 0, VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT, VK_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT);
        VkRenderingAttachmentInfo attachmentInfo = VkRenderingAttachmentInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO)
                .imageView(Swapchain.depthImageViews[currentFrame])
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
