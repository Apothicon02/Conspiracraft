package org.conspiracraft.renderer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import java.nio.IntBuffer;

import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.VK13.*;

public class QueueFamilyIndices {
    public Integer graphicsFamily;
    public Integer presentFamily;

    public boolean isComplete() {
        return graphicsFamily != null && presentFamily != null;
    }

    public static QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device, long surface) {
        QueueFamilyIndices indices = new QueueFamilyIndices();

        try (MemoryStack stack = MemoryStack.stackPush()) {

            // Query number of queue families
            IntBuffer count = stack.ints(0);
            vkGetPhysicalDeviceQueueFamilyProperties(device, count, null);

            VkQueueFamilyProperties.Buffer families =
                    VkQueueFamilyProperties.calloc(count.get(0), stack);

            vkGetPhysicalDeviceQueueFamilyProperties(device, count, families);

            // Iterate all queue families
            for (int i = 0; i < families.capacity(); i++) {
                VkQueueFamilyProperties family = families.get(i);

                // Graphics support
                if ((family.queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    indices.graphicsFamily = i;
                }

                // Present support
                IntBuffer presentSupport = stack.ints(VK_FALSE);
                vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, presentSupport);

                if (presentSupport.get(0) == VK_TRUE) {
                    indices.presentFamily = i;
                }

                if (indices.isComplete()) break;
            }
        }

        return indices;
    }
}
