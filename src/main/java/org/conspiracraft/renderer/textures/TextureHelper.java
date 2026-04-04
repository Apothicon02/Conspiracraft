package org.conspiracraft.renderer.textures;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.conspiracraft.renderer.Window.*;
import static org.lwjgl.vulkan.VK10.*;

public class TextureHelper {
    public static void createImage(MemoryStack stack, int width, int height, int format, int tiling, int usage, int memoryProperties, LongBuffer imageBuf, LongBuffer imageMemoryBuf) {
        VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                .imageType(VK_IMAGE_TYPE_2D)
                .format(format)
                .extent(e -> e
                        .width(width)
                        .height(height)
                        .depth(1)
                )
                .mipLevels(1)
                .arrayLayers(1)
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .tiling(tiling)
                .usage(usage)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
        int error = vkCreateImage(device, imageInfo, null, imageBuf);
        if (error != VK_SUCCESS) {throw new RuntimeException("Failed to create image: "+error);}
        long image = imageBuf.get(0);
        VkMemoryRequirements memReq = VkMemoryRequirements.calloc(stack);
        vkGetImageMemoryRequirements(device, image, memReq);
        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .allocationSize(memReq.size())
                .memoryTypeIndex(findMemoryType(stack, memReq.memoryTypeBits(), memoryProperties));
        error = vkAllocateMemory(device, allocInfo, null, imageMemoryBuf);
        if (error != VK_SUCCESS) {throw new RuntimeException("Failed to allocate image memory: " + error);}
        long imageMemory = imageMemoryBuf.get(0);

        vkBindImageMemory(device, image, imageMemory, 0);
    }
    public static long createImageView(MemoryStack stack, long image, int format, int aspectMask) {
        VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .image(image)
                .viewType(VK_IMAGE_VIEW_TYPE_2D)
                .format(format)
                .components(c -> c
                        .r(VK_COMPONENT_SWIZZLE_IDENTITY)
                        .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                        .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                        .a(VK_COMPONENT_SWIZZLE_IDENTITY))
                .subresourceRange(r -> r
                        .aspectMask(aspectMask)
                        .baseMipLevel(0)
                        .levelCount(1)
                        .baseArrayLayer(0)
                        .layerCount(1));
        LongBuffer imageViewBuf = stack.mallocLong(1);
        int err = vkCreateImageView(device, viewInfo, null, imageViewBuf);
        if (err != VK_SUCCESS) {throw new RuntimeException("Failed to create image view: " + err);}
        return imageViewBuf.get(0);
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
}
