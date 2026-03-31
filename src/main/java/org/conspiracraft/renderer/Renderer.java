package org.conspiracraft.renderer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.conspiracraft.renderer.Window.*;
import static org.lwjgl.vulkan.VK13.*;

public class Renderer {
    public static void render() {
        MemoryStack stack = MemoryStack.stackPush();
        startRenderPass(stack);

        vkCmdDraw(commandBuffer, 3, 1, 0, 0);

        vkCmdEndRenderPass(commandBuffer);
        stack.close();
    }

    public static void startRenderPass(MemoryStack stack) {
        VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
                .pInheritanceInfo(null);
        if (vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
            throw new RuntimeException("Failed to begin recording command buffer!");
        }

        VkRect2D renderAreaData = VkRect2D.calloc(stack);
        renderAreaData.offset(VkOffset2D.calloc(stack).set(0, 0));
        renderAreaData.extent(extent);
        VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
            .renderPass(renderPass)
            .framebuffer(swapchainFramebuffers[imageIdx])
            .renderArea(renderAreaData);

        renderPassInfo.clearValueCount(1);
        VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
        clearValues.color().float32(0, 0.0f).float32(1, 0.0f).float32(2, 0.0f).float32(3, 1.0f);
        renderPassInfo.pClearValues(clearValues);

        vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);

        VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
        viewport.x(0.0f);
        viewport.y(0.0f);
        viewport.width(extent.width());
        viewport.height(extent.height());
        viewport.minDepth(0.0f);
        viewport.maxDepth(1.0f);
        vkCmdSetViewport(commandBuffer, 0, viewport);

        VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
        scissor.offset(VkOffset2D.calloc(stack).set(0, 0));
        scissor.extent(extent);
        vkCmdSetScissor(commandBuffer, 0, scissor);
    }
}
