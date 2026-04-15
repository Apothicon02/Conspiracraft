package org.conspiracraft.graphics.buffers;

import org.conspiracraft.graphics.Device;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import java.nio.LongBuffer;

import static org.conspiracraft.graphics.Device.vkDevice;
import static org.conspiracraft.graphics.Swapchain.FRAMES_IN_FLIGHT;
import static org.lwjgl.vulkan.VK14.*;

public class CmdBuffer {
    public static long cmdPool;
    public static VkCommandBuffer[] cmdBuffers;

    public static void init(MemoryStack stack) {
        createCommandPool(stack);
        createCommandBuffer(stack);
    }

    public static void recreate(MemoryStack stack) {
        for (VkCommandBuffer cmdBuffer : cmdBuffers) {
            vkFreeCommandBuffers(vkDevice, cmdPool, cmdBuffer);
        }
        createCommandPool(stack);
        createCommandBuffer(stack);
    }

    public static void createCommandBuffer(MemoryStack stack) {
        cmdBuffers = new VkCommandBuffer[FRAMES_IN_FLIGHT];
        for (int i = 0; i < FRAMES_IN_FLIGHT; i++) {
            cmdBuffers[i] = CmdBufferHelper.createCmdBuffer(stack);
        }
    }
    public static void createCommandPool(MemoryStack stack) {
        VkCommandPoolCreateInfo cmdPoolCreateInfo = VkCommandPoolCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                .queueFamilyIndex(Device.vkQueueFamilyIdx);
        LongBuffer cmdPoolBuf = stack.mallocLong(1);
        int err = vkCreateCommandPool(vkDevice, cmdPoolCreateInfo, null, cmdPoolBuf);
        if (err != VK_SUCCESS) {throw new RuntimeException("Failed to create command pool: " + err);}
        cmdPool = cmdPoolBuf.get(0);
    }
}
