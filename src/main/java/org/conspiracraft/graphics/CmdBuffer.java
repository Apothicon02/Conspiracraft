package org.conspiracraft.graphics;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import java.nio.LongBuffer;

import static org.conspiracraft.graphics.Device.vkDevice;
import static org.lwjgl.vulkan.VK14.*;

public class CmdBuffer {
    public static long cmdPool;
    public static VkCommandBuffer cmdBuffer;

    public static void init(MemoryStack stack) {
        createCommandPool(stack);
        createCommandBuffer(stack);
    }

    public static void createCommandBuffer(MemoryStack stack) {
        cmdBuffer = CmdBufferHelper.createCmdBuffer(stack);
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
