package org.conspiracraft.graphics.textures;

import org.conspiracraft.graphics.Descriptors;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkSamplerCreateInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.nio.LongBuffer;

import static org.conspiracraft.graphics.Device.vkDevice;
import static org.lwjgl.vulkan.VK14.*;

public class Texture {
    public int handle;
    public float resDiv = 1;
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

    public Texture(float resDiv, int width, int height, int channels, int format, int usage, boolean windowResizable) {
        this.handle = Textures.textures.size();
        this.resDiv = resDiv;
        this.width = (int)Math.ceil(width/resDiv);
        this.height = (int)Math.ceil(height/resDiv);
        this.channels = channels;
        this.format = format;
        this.usage = usage;
        this.windowResizable = windowResizable;
    }
    public Texture(int width, int height, int channels, int format, int usage, boolean windowResizable) {
        this.handle = Textures.textures.size();
        this.width = width;
        this.height = height;
        this.channels = channels;
        this.format = format;
        this.usage = usage;
        this.windowResizable = windowResizable;
    }

    public boolean layoutUnset = true;
    public boolean isLayoutUnset() {
        return layoutUnset;
    }

    public void create(MemoryStack stack) {
        boolean isDepth = format == VK_FORMAT_D32_SFLOAT;
        long[] imageData = ImageHelper.createImage(stack, width, height, this instanceof Texture3D tex3D ? tex3D.depth : 1, format, VK_IMAGE_TILING_OPTIMAL, usage, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
        image = imageData[0];
        //System.out.println("Created image: 0x"+Long.toHexString(image));
        memory = imageData[1];
        imageView = ImageHelper.createImageView(stack, this instanceof Texture3D, image, format, channels);
        VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                .magFilter(isDepth ? VK_FILTER_NEAREST : VK_FILTER_LINEAR)
                .minFilter(isDepth ? VK_FILTER_NEAREST : VK_FILTER_LINEAR)
                .addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                .addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                .addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                .anisotropyEnable(false)
                .maxAnisotropy(16)
                .borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK)
                .unnormalizedCoordinates(false);
        LongBuffer samplerBuf = stack.mallocLong(1);
        vkCreateSampler(vkDevice, samplerInfo, null, samplerBuf);
        sampler = samplerBuf.get(0);

        VkDescriptorImageInfo.Buffer descriptorImageInfo = VkDescriptorImageInfo.calloc(1, stack);
        descriptorImageInfo.get(0)
                .imageLayout(VK_IMAGE_LAYOUT_GENERAL)
                .imageView(this.imageView)
                .sampler(this.sampler);
        VkDescriptorImageInfo.Buffer storageImageInfo = VkDescriptorImageInfo.calloc(1, stack);
        storageImageInfo.get(0)
                .imageLayout(VK_IMAGE_LAYOUT_GENERAL)
                .imageView(this.imageView);
        VkWriteDescriptorSet.Buffer write = VkWriteDescriptorSet.calloc(2, stack);
        write.get(0)
                .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                .dstSet(Descriptors.descriptorSet)
                .dstBinding(Descriptors.SAMP_IMG_BINDING)
                .dstArrayElement(handle)
                .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .descriptorCount(1)
                .pImageInfo(descriptorImageInfo);
        write.get(1)
                .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                .dstSet(Descriptors.descriptorSet)
                .dstBinding(Descriptors.STORE_IMG_BINDING)
                .dstArrayElement(handle)
                .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
                .descriptorCount(1)
                .pImageInfo(storageImageInfo);
        vkUpdateDescriptorSets(vkDevice, write, null);
    }
    public void destroy() {
        vkDestroyImageView(vkDevice, imageView, null);
        vkDestroySampler(vkDevice, sampler, null);
        vkDestroyImage(vkDevice, image, null);
        vkFreeMemory(vkDevice, memory, null);
        layoutUnset = true;
    }
}
