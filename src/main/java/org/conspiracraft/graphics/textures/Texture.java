package org.conspiracraft.graphics.textures;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import java.nio.LongBuffer;

import static org.conspiracraft.graphics.Device.vkDevice;
import static org.lwjgl.vulkan.VK14.*;

public class Texture {
    public int width;
    public int height;
    public int channels;
    public int format;
    public long image = -1;
    public long memory = -1;
    public long imageView = -1;
    public long sampler = -1;
    public int usage;
    public boolean windowResizable;

    public Texture(int width, int height, int channels, int format, int usage, boolean windowResizable) {
        this.width = width;
        this.height = height;
        this.channels = channels;
        this.format = format;
        this.usage = usage;
        this.windowResizable = windowResizable;
    }

    private boolean layoutUnset = true;
    public boolean isLayoutUnset() {
        if (layoutUnset) {
            layoutUnset = false;
            return true;
        }
        return layoutUnset;
    }

    public void create(MemoryStack stack) {
        int texFormat = format;
        long[] imageData = ImageHelper.createImage(stack, width, height, this instanceof Texture3D tex3D ? tex3D.depth : 1, texFormat, VK_IMAGE_TILING_OPTIMAL, usage, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
        image = imageData[0];
        memory = imageData[1];
        imageView = ImageHelper.createImageView(stack, this instanceof Texture3D, image, texFormat, channels);
        VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                .magFilter(VK_FILTER_LINEAR)
                .minFilter(VK_FILTER_LINEAR)
                .addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                .addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                .addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                .anisotropyEnable(true)
                .maxAnisotropy(16)
                .borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK)
                .unnormalizedCoordinates(false);
        LongBuffer samplerBuf = stack.mallocLong(1);
        vkCreateSampler(vkDevice, samplerInfo, null, samplerBuf);
        sampler = samplerBuf.get(0);
    }
    public void destroy() {
        vkDestroyImageView(vkDevice, imageView, null);
        vkDestroySampler(vkDevice, sampler, null);
        vkDestroyImage(vkDevice, image, null);
        vkFreeMemory(vkDevice, memory, null);
        layoutUnset = true;
    }
}
