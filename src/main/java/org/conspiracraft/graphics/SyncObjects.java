package org.conspiracraft.graphics;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreTypeCreateInfo;

import java.nio.LongBuffer;

import static org.conspiracraft.graphics.Device.vkDevice;
import static org.conspiracraft.graphics.Swapchain.FRAMES_IN_FLIGHT;
import static org.lwjgl.vulkan.VK14.*;

public class SyncObjects {
    public static long[] imageAvailableSemaphores;
    public static long[] renderFinishedSemaphores;
    public static long timelineSemaphore;
    public static long timeline = 0;

    public static void init(MemoryStack stack) {
        createSyncObjects(stack);
    }

    public static void createSyncObjects(MemoryStack stack) {
        imageAvailableSemaphores = new long[FRAMES_IN_FLIGHT];
        renderFinishedSemaphores = new long[Swapchain.images.length];

        VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack);
        semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

        for (int i = 0; i < Swapchain.images.length; i++) {
            LongBuffer renderFinishedSemBuf = stack.mallocLong(1);
            if (vkCreateSemaphore(vkDevice, semaphoreInfo, null, renderFinishedSemBuf) != VK_SUCCESS) {throw new RuntimeException("Failed to create semaphores!");}
            renderFinishedSemaphores[i] = renderFinishedSemBuf.get(0);
        }

        for (int i = 0; i < FRAMES_IN_FLIGHT; i++) {
            LongBuffer imageAvailableSemBuf = stack.mallocLong(1);
            if (vkCreateSemaphore(vkDevice, semaphoreInfo, null, imageAvailableSemBuf) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create semaphores!");
            }
            imageAvailableSemaphores[i] = imageAvailableSemBuf.get(0);
        }

        VkSemaphoreTypeCreateInfo timelineInfo = VkSemaphoreTypeCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SEMAPHORE_TYPE_CREATE_INFO)
                .semaphoreType(VK_SEMAPHORE_TYPE_TIMELINE)
                .initialValue(0);
        VkSemaphoreCreateInfo timelineSemaphoreInfo = VkSemaphoreCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
                .pNext(timelineInfo.address());
        LongBuffer pSemaphore = stack.mallocLong(1);
        vkCreateSemaphore(vkDevice, timelineSemaphoreInfo, null, pSemaphore);
        timelineSemaphore = pSemaphore.get(0);
        timeline = 0;
    }
}
