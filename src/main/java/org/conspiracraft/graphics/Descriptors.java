package org.conspiracraft.graphics;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.conspiracraft.graphics.Device.vkDevice;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.*;

public class Descriptors {
    public Descriptors(MemoryStack stack) {
        createDescriptorSetLayout(stack);
        Pipelines.createPipeline(stack);
        createDescriptorPool(stack);
        createDescriptorSet(stack);
    }

    public static long descriptorPool;
    public static long descriptorSet;
    public static long descriptorSetLayout;
    public static void createDescriptorSet(MemoryStack stack) {
        LongBuffer layouts = stack.mallocLong(1).put(0, descriptorSetLayout);
        VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                .descriptorPool(descriptorPool)
                .pSetLayouts(layouts);
        LongBuffer descriptorSetsBuf = stack.mallocLong(1);
        if (vkAllocateDescriptorSets(vkDevice, allocInfo, descriptorSetsBuf) != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate descriptor sets!");
        }
        descriptorSet = descriptorSetsBuf.get(0);
    }
    public static final int UBO_BINDING = 0;
    public static final int SSBO_BINDING = 1;
    public static final int SAMP_IMG_BINDING = 2;
    public static final int STORE_IMG_BINDING = 3;
    public void createDescriptorPool(MemoryStack stack) {
        VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(4, stack);
        poolSizes.get(UBO_BINDING)
                .type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                .descriptorCount(1);
        poolSizes.get(SSBO_BINDING)
                .type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                .descriptorCount(Short.MAX_VALUE);
        poolSizes.get(SAMP_IMG_BINDING)
                .type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .descriptorCount(Short.MAX_VALUE);
        poolSizes.get(STORE_IMG_BINDING)
                .type(VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
                .descriptorCount(Short.MAX_VALUE);
        VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                .pPoolSizes(poolSizes)
                .maxSets(1)
                .flags(VK_DESCRIPTOR_POOL_CREATE_UPDATE_AFTER_BIND_BIT);
        LongBuffer descriptorPoolBuf = stack.mallocLong(1);
        if (vkCreateDescriptorPool(vkDevice, poolInfo, null, descriptorPoolBuf) != VK_SUCCESS) {
            throw new RuntimeException("Failed to create descriptor pool!");
        }
        descriptorPool = descriptorPoolBuf.get(0);
    }
    public void createDescriptorSetLayout(MemoryStack stack) {
        VkDescriptorSetLayoutBinding.Buffer layoutBindings = VkDescriptorSetLayoutBinding.calloc(4, stack);
        layoutBindings.get(UBO_BINDING)
                .binding(UBO_BINDING)
                .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                .descriptorCount(1)
                .stageFlags(VK_SHADER_STAGE_ALL);
        layoutBindings.get(SSBO_BINDING)
                .binding(SSBO_BINDING)
                .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                .descriptorCount(Short.MAX_VALUE)
                .stageFlags(VK_SHADER_STAGE_ALL);
        layoutBindings.get(SAMP_IMG_BINDING)
                .binding(SAMP_IMG_BINDING)
                .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .descriptorCount(Short.MAX_VALUE)
                .stageFlags(VK_SHADER_STAGE_ALL);
        layoutBindings.get(STORE_IMG_BINDING)
                .binding(STORE_IMG_BINDING)
                .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
                .descriptorCount(Short.MAX_VALUE)
                .stageFlags(VK_SHADER_STAGE_ALL);

        int singleFlags = VK_DESCRIPTOR_BINDING_UPDATE_AFTER_BIND_BIT;
        int arrFlags = VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT | VK_DESCRIPTOR_BINDING_UPDATE_AFTER_BIND_BIT;
        VkDescriptorSetLayoutBindingFlagsCreateInfo layoutBindingFlags = VkDescriptorSetLayoutBindingFlagsCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_BINDING_FLAGS_CREATE_INFO)
                .pBindingFlags(stack.ints(singleFlags, arrFlags, arrFlags, arrFlags));

        VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                .pNext(layoutBindingFlags)
                .flags(VK_DESCRIPTOR_SET_LAYOUT_CREATE_UPDATE_AFTER_BIND_POOL_BIT)
                .pBindings(layoutBindings);
        LongBuffer descriptorSetLayoutsBuf = stack.callocLong(1);
        if (vkCreateDescriptorSetLayout(vkDevice, layoutInfo, null, descriptorSetLayoutsBuf) != VK_SUCCESS) {
            throw new RuntimeException("Failed to create descriptor set layout!");
        }
        descriptorSetLayout = descriptorSetLayoutsBuf.get(0);
    }
}
