package org.conspiracraft.graphics;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import java.nio.LongBuffer;
import java.util.Arrays;

import static org.conspiracraft.graphics.Device.vkDevice;
import static org.conspiracraft.graphics.Swapchain.FRAMES_IN_FLIGHT;
import static org.lwjgl.vulkan.VK14.*;

public class SyncObjects {
    public static long[] imageAvailableSemaphores;
    public static long[] renderFinishedSemaphores;
    public static long[] inFlightFences;
    public static long[] imagesInFlight;

    public static void init(MemoryStack stack) {
        createSyncObjects(stack);
    }

    public static void createSyncObjects(MemoryStack stack) {
        imageAvailableSemaphores = new long[FRAMES_IN_FLIGHT];
        renderFinishedSemaphores = new long[FRAMES_IN_FLIGHT];
        inFlightFences = new long[FRAMES_IN_FLIGHT];
        imagesInFlight = new long[Swapchain.images.length];

        VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack);
        semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
        VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack);
        fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
        fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

        for (int i = 0; i < Swapchain.images.length; i++) {
            LongBuffer renderFinishedSemBuf = stack.mallocLong(1);
            if (vkCreateSemaphore(vkDevice, semaphoreInfo, null, renderFinishedSemBuf) != VK_SUCCESS) {throw new RuntimeException("Failed to create semaphores!");}
            renderFinishedSemaphores[i] = renderFinishedSemBuf.get(0);
        }

        for (int i = 0; i < FRAMES_IN_FLIGHT; i++) {
            LongBuffer imageAvailableSemBuf = stack.mallocLong(1);
            LongBuffer inFlightFenceBuf = stack.mallocLong(1);
            if (vkCreateSemaphore(vkDevice, semaphoreInfo, null, imageAvailableSemBuf) != VK_SUCCESS ||
                    vkCreateFence(vkDevice, fenceInfo, null, inFlightFenceBuf) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create semaphores!");
            }
            imageAvailableSemaphores[i] = imageAvailableSemBuf.get(0);
            inFlightFences[i] = inFlightFenceBuf.get(0);
        }
    }
}
