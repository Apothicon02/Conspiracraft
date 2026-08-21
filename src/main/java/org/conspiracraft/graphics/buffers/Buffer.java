package org.conspiracraft.graphics.buffers;

import org.conspiracraft.graphics.Descriptors;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.ArrayList;
import java.util.List;

import static org.conspiracraft.graphics.Device.vkDevice;
import static org.conspiracraft.graphics.buffers.BufferHelper.createBuffer;
import static org.lwjgl.vulkan.VK10.*;

public class Buffer {
    public static List<Buffer> buffers = new ArrayList<>();

    public int handle = -1;
    public int size;
    public long[] buffer;
    public long[] memory;
    public PointerBuffer pointer;
    public Buffer(MemoryStack stack, int bufferSize, int usage, int properties, boolean temporary) {
        size = bufferSize;
        buffer = new long[1];
        memory = new long[1];
        pointer = PointerBuffer.allocateDirect(1);
        createBuffer(stack, bufferSize, usage, properties, buffer, memory);
        if ((properties & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0) {
            int error = vkMapMemory(vkDevice, memory[0], 0, bufferSize, 0, pointer);
            if (error != VK_SUCCESS) {throw new RuntimeException("vkMapMemory failed: " + error);}
        }
        if (!temporary) {
            handle = buffers.size();
            buffers.addLast(this);
            if ((usage & VK_BUFFER_USAGE_STORAGE_BUFFER_BIT) != 0) {
                VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
                bufferInfo.get(0)
                        .buffer(buffer[0])
                        .offset(0)
                        .range(VK_WHOLE_SIZE);
                VkWriteDescriptorSet.Buffer write = VkWriteDescriptorSet.calloc(1, stack);
                write.get(0)
                        .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .dstSet(Descriptors.descriptorSet)
                        .dstBinding(Descriptors.SSBO_BINDING)
                        .dstArrayElement(handle)
                        .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                        .descriptorCount(1)
                        .pBufferInfo(bufferInfo);
                vkUpdateDescriptorSets(vkDevice, write, null);
                //Graphics.recreateDescriptors(stack);
            }
        }
    }
}