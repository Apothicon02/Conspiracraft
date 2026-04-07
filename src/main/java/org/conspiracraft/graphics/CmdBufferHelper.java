package org.conspiracraft.graphics;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;

import static org.conspiracraft.graphics.CmdBuffer.cmdPool;
import static org.conspiracraft.graphics.Device.vkDevice;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public class CmdBufferHelper {
    public static VkCommandBuffer createCmdBuffer(MemoryStack stack) {
        VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .commandPool(cmdPool)
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandBufferCount(1);
        PointerBuffer cmdBufferBuf = stack.mallocPointer(1);
        int err = vkAllocateCommandBuffers(vkDevice, allocInfo, cmdBufferBuf);
        if (err != VK_SUCCESS) {throw new RuntimeException("Failed to allocate command buffer: " + err);}
        return new VkCommandBuffer(cmdBufferBuf.get(0), vkDevice);
    }

    public static void recordCmdBuffer(MemoryStack stack, VkCommandBuffer cmdBuffer) {
        VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
                .pInheritanceInfo(null);
        int err = vkBeginCommandBuffer(cmdBuffer, beginInfo);
        if (err != VK_SUCCESS) {throw new RuntimeException("Failed to begin command buffer: " + err);}
    }
}
