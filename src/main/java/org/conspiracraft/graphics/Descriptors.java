package org.conspiracraft.graphics;

import org.conspiracraft.graphics.buffers.ShaderStorageBuffer;
import org.conspiracraft.graphics.buffers.ubos.UniformBuffer;
import org.conspiracraft.graphics.textures.Texture;
import org.conspiracraft.graphics.textures.Textures;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.conspiracraft.graphics.Device.vkDevice;
import static org.conspiracraft.graphics.Swapchain.FRAMES_IN_FLIGHT;
import static org.conspiracraft.graphics.textures.Textures.textures;
import static org.lwjgl.vulkan.VK10.*;

public class Descriptors {
    public Descriptors(MemoryStack stack) {
        createDescriptorSetLayout(stack);
        Pipeline.createPipeline(stack);
        createDescriptorPool(stack);
        createDescriptorSets(stack);
    }

    public static long descriptorPool;
    public static long[] descriptorSets;
    public static long[] descriptorSetLayouts;
    public static void createDescriptorSets(MemoryStack stack) {
        LongBuffer layouts = stack.mallocLong(FRAMES_IN_FLIGHT);
        for (int i = 0; i < FRAMES_IN_FLIGHT; i++) {
            layouts.put(i, descriptorSetLayouts[0]);
        }
        VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                .descriptorPool(descriptorPool)
                .pSetLayouts(layouts);
        LongBuffer descriptorSetsBuf = stack.mallocLong(FRAMES_IN_FLIGHT);
        if (vkAllocateDescriptorSets(vkDevice, allocInfo, descriptorSetsBuf) != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate descriptor sets!");
        }
        descriptorSets = new long[FRAMES_IN_FLIGHT];
        for (int i = 0; i < descriptorSets.length; i++) {
            descriptorSets[i] = descriptorSetsBuf.get(i);
            VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(descriptorCount(), stack);
            int b = 0;
            for (UniformBuffer buf : UniformBuffer.uniformBuffers) {
                VkDescriptorBufferInfo.Buffer bufInfo = VkDescriptorBufferInfo.calloc(1, stack)
                        .buffer(buf.buffer[0])
                        .offset(0)
                        .range(buf.size);
                descriptorWrites.get(b)
                        .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .dstSet(descriptorSets[i])
                        .dstBinding(b)
                        .dstArrayElement(0)
                        .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                        .descriptorCount(1)
                        .pBufferInfo(bufInfo);
                b++;
            }
            for (ShaderStorageBuffer buf : ShaderStorageBuffer.storageBuffers) {
                VkDescriptorBufferInfo.Buffer bufInfo = VkDescriptorBufferInfo.calloc(1, stack)
                        .buffer(buf.buffer.buffer[0])
                        .offset(0)
                        .range(buf.buffer.size);
                descriptorWrites.get(b)
                        .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .dstSet(descriptorSets[i])
                        .dstBinding(b)
                        .dstArrayElement(0)
                        .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                        .descriptorCount(1)
                        .pBufferInfo(bufInfo);
                b++;
            }
            for (Texture tex : Textures.textures) {
                VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack)
                        .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                        .imageView(tex.imageView)
                        .sampler(tex.sampler);
                descriptorWrites.get(b)
                        .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .dstSet(descriptorSets[i])
                        .dstBinding(b)
                        .dstArrayElement(0)
                        .descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
                        .descriptorCount(1)
                        .pImageInfo(imageInfo);
                b++;
            }
            vkUpdateDescriptorSets(vkDevice, descriptorWrites, null);
        }
    }
    public void createDescriptorPool(MemoryStack stack) {
        VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(descriptorCount(), stack);
        int b = 0;
        for (int i = 0; i < UniformBuffer.uniformBuffers.size(); i++) {
            poolSizes.get(b)
                    .type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(FRAMES_IN_FLIGHT);
            b++;
        }
        for (int i = 0; i < ShaderStorageBuffer.storageBuffers.size(); i++) {
            poolSizes.get(b)
                    .type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                    .descriptorCount(FRAMES_IN_FLIGHT);
            b++;
        }
        for (int i = 0; i < textures.size(); i++) {
            poolSizes.get(b)
                    .type(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
                    .descriptorCount(FRAMES_IN_FLIGHT);
            b++;
        }
        VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                .pPoolSizes(poolSizes)
                .maxSets(FRAMES_IN_FLIGHT);
        LongBuffer descriptorPoolBuf = stack.mallocLong(1);
        if (vkCreateDescriptorPool(vkDevice, poolInfo, null, descriptorPoolBuf) != VK_SUCCESS) {
            throw new RuntimeException("Failed to create descriptor pool!");
        }
        descriptorPool = descriptorPoolBuf.get(0);
    }
    public void createDescriptorSetLayout(MemoryStack stack) {
        VkDescriptorSetLayoutBinding.Buffer layoutBindings = VkDescriptorSetLayoutBinding.calloc(descriptorCount(), stack);
        int b = 0;
        for (int i = 0; i < UniformBuffer.uniformBuffers.size(); i++) {
            layoutBindings.get(b)
                    .binding(b)
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(1)
                    .stageFlags(UniformBuffer.uniformBuffers.get(i).stageFlags);
            b++;
        }
        for (int i = 0; i < ShaderStorageBuffer.storageBuffers.size(); i++) {
            layoutBindings.get(b)
                    .binding(b)
                    .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                    .descriptorCount(1)
                    .stageFlags(ShaderStorageBuffer.storageBuffers.get(i).stageFlags);
            b++;
        }
        for (int i = 0; i < textures.size(); i++) {
            layoutBindings.get(b)
                    .binding(b)
                    .descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
                    .descriptorCount(1)
                    .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
            b++;
        }

        VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                .pBindings(layoutBindings);
        LongBuffer descriptorSetLayoutsBuf = stack.callocLong(1);
        if (vkCreateDescriptorSetLayout(vkDevice, layoutInfo, null, descriptorSetLayoutsBuf) != VK_SUCCESS) {
            throw new RuntimeException("Failed to create descriptor set layout!");
        }
        descriptorSetLayouts = new long[]{descriptorSetLayoutsBuf.get(0)};
    }
    public static int descriptorCount() {
        return UniformBuffer.uniformBuffers.size()+ShaderStorageBuffer.storageBuffers.size()+textures.size();
    }
}
