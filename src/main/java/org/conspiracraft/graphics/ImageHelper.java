package org.conspiracraft.graphics;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.conspiracraft.graphics.Device.physicalDevice;
import static org.conspiracraft.graphics.Device.vkDevice;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.*;

public class ImageHelper {
    public static long createImageView(MemoryStack stack, boolean threeDimensional, long image, int format, int channels) {
        VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .image(image)
                .viewType(threeDimensional ? VK_IMAGE_VIEW_TYPE_3D : VK_IMAGE_VIEW_TYPE_2D)
                .format(format);
        if (channels > 0) {
            createInfo.components().r(VK_COMPONENT_SWIZZLE_IDENTITY);
            if (channels > 1) {
                createInfo.components().g(VK_COMPONENT_SWIZZLE_IDENTITY);
                if (channels > 2) {
                    createInfo.components().b(VK_COMPONENT_SWIZZLE_IDENTITY);
                    if (channels > 3) {
                        createInfo.components().a(VK_COMPONENT_SWIZZLE_IDENTITY);
                    }
                }
            }
        }
        createInfo.subresourceRange()
                .aspectMask(channels == 0 ? VK_IMAGE_ASPECT_DEPTH_BIT : VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1);
        LongBuffer pView = stack.mallocLong(1);
        int err = vkCreateImageView(vkDevice, createInfo, null, pView);
        if (err != VK_SUCCESS) throw new RuntimeException("Failed to create image view: " + err);
        return pView.get(0);
    }

    public static long createImage(MemoryStack stack, int width, int height, int format, int tiling, int usage, int memoryProperties) {
        VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                .imageType(VK_IMAGE_TYPE_2D)
                .format(format)
                .extent(e -> e.width(width).height(height).depth(1))
                .mipLevels(1)
                .arrayLayers(1)
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .tiling(tiling)
                .usage(usage)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
        LongBuffer imageBuf = stack.mallocLong(1);
        int err = vkCreateImage(vkDevice, imageInfo, null, imageBuf);
        if (err != VK_SUCCESS) {throw new RuntimeException("Failed to create image: " + err);}
        long image = imageBuf.get(0);

        VkMemoryRequirements memReq = VkMemoryRequirements.malloc(stack);
        vkGetImageMemoryRequirements(vkDevice, image, memReq);
        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .allocationSize(memReq.size())
                .memoryTypeIndex(findMemoryType(stack, memReq.memoryTypeBits(), memoryProperties));
        LongBuffer memoryBuf = stack.mallocLong(1);
        err = vkAllocateMemory(vkDevice, allocInfo, null, memoryBuf);
        if (err != VK_SUCCESS) {throw new RuntimeException("Failed to allocate image memory: " + err);}
        long memory = memoryBuf.get(0);
        vkBindImageMemory(vkDevice, image, memory, 0);
        return image;
    }

    public static int findMemoryType(MemoryStack stack, int typeFilter, int properties) {
        VkPhysicalDeviceMemoryProperties memProps = VkPhysicalDeviceMemoryProperties.calloc(stack);
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProps);
        for (int i = 0; i < memProps.memoryTypeCount(); i++) {
            boolean typeSupported = (typeFilter & (1 << i)) != 0;
            boolean hasProperties = (memProps.memoryTypes(i).propertyFlags() & properties) == properties;
            if (typeSupported && hasProperties) {
                return i;
            }
        }
        throw new RuntimeException("Failed to find suitable memory type");
    }

    public static void transitionImageLayout(MemoryStack stack, VkCommandBuffer cmdBuffer, int aspectMask, long image, int oldLayout, int newLayout, long srcAccessMask, long dstAccessMask, long srcStageMask, long dstStageMask) {
        VkImageMemoryBarrier2.Buffer barrier = VkImageMemoryBarrier2.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
                .srcStageMask(srcStageMask)
                .srcAccessMask(srcAccessMask)
                .dstStageMask(dstStageMask)
                .dstAccessMask(dstAccessMask)
                .oldLayout(oldLayout)
                .newLayout(newLayout)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .image(image);
        barrier.subresourceRange()
                .aspectMask(aspectMask)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1);
        VkDependencyInfo depInfo = VkDependencyInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO)
                .pImageMemoryBarriers(barrier);
        vkCmdPipelineBarrier2(cmdBuffer, depInfo);
    }
}
