package org.conspiracraft.renderer.buffers;

import org.joml.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.conspiracraft.graphics.Graphics.*;
import static org.conspiracraft.graphics.Device.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;


public class BufferHelper {
    public static final int FLOAT_SIZE = 4;
    public static final int VEC2_SIZE = FLOAT_SIZE*2;
    public static final int VEC3_SIZE = FLOAT_SIZE*3;
    public static final int VEC4_SIZE = FLOAT_SIZE*4;
    public static final int MAT2_SIZE = FLOAT_SIZE*4*2;
    public static final int MAT3_SIZE = FLOAT_SIZE*4*3;
    public static final int MAT4_SIZE = FLOAT_SIZE*4*4;
    public static final int FLOAT_ALIGN = 4;
    public static final int VEC2_ALIGN = 8;
    public static final int OTHER_ALIGN = 16;

    public static int calculateSize(Object[] uniforms) {
        int size = 0;
        for (Object obj : uniforms) {
            size += switch (obj) {
                case Float v -> FLOAT_SIZE;
                case Integer v -> FLOAT_SIZE;
                case Vector2f v -> VEC2_SIZE;
                case Vector3f v -> VEC3_SIZE;
                case Vector4f v -> VEC4_SIZE;
                case Vector2i v -> VEC2_SIZE;
                case Vector3i v -> VEC3_SIZE;
                case Vector4i v -> VEC4_SIZE;
                case Matrix2f v -> MAT2_SIZE;
                case Matrix3f v -> MAT3_SIZE;
                case Matrix4f v -> MAT4_SIZE;
                default -> throw new IllegalArgumentException("Cannot create uniform for object type: "+obj.getClass().getName());
            };
        }
        return size;
    }

    public static void copyBuffer(MemoryStack stack, long src, long dst, long size) {
        VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .commandPool(commandPool)
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandBufferCount(1);
        PointerBuffer commandBuffersBuf = stack.mallocPointer(1);
        if (vkAllocateCommandBuffers(vkDevice, allocInfo, commandBuffersBuf) != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate command buffer!");
        }
        VkCommandBuffer cmdBuffer = new VkCommandBuffer(commandBuffersBuf.get(0), vkDevice);
        VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
        vkBeginCommandBuffer(cmdBuffer, beginInfo);

        VkBufferCopy.Buffer copyRegion = VkBufferCopy.calloc(1, stack)
                .srcOffset(0) // Optional
                .dstOffset(0) // Optional
                .size(size);
        vkCmdCopyBuffer(cmdBuffer, src, dst, copyRegion);

        vkEndCommandBuffer(cmdBuffer);
        VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .pCommandBuffers(commandBuffersBuf);
        vkQueueSubmit(graphicsQueue, submitInfo, VK_NULL_HANDLE);
        vkQueueWaitIdle(graphicsQueue);
        vkFreeCommandBuffers(vkDevice, commandPool, cmdBuffer);
    }

    public static void createBuffer(MemoryStack stack, int bufferSize, int usage, int properties, long[] buffer, long[] memory, int i) {
        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .size(bufferSize)
                .usage(usage)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE);
        LongBuffer bufferBuf = stack.mallocLong(1);
        if (vkCreateBuffer(vkDevice, bufferInfo, null, bufferBuf) != VK_SUCCESS) {
            throw new RuntimeException("Failed to create vertex buffer!");
        }
        buffer[i] = bufferBuf.get(0);
        VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc(stack);
        vkGetBufferMemoryRequirements(vkDevice, buffer[i], memRequirements);

        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .allocationSize(memRequirements.size())
                .memoryTypeIndex(findMemoryType(stack, memRequirements.memoryTypeBits(), properties));
        LongBuffer bufferMemoryBuf = stack.mallocLong(1);
        if (vkAllocateMemory(vkDevice, allocInfo, null, bufferMemoryBuf) != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate buffer memory!");
        }
        memory[i] = bufferMemoryBuf.get(0);
        if (vkBindBufferMemory(vkDevice, buffer[i], memory[i], 0) != VK_SUCCESS) {
            throw new RuntimeException("Failed to bind buffer memory!");
        }
    }
    public static int findMemoryType(MemoryStack stack, int typeFilter, int properties) {
        VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.calloc(stack);
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProperties);
        for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) != 0) {
                if ((memProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                    return i;
                }
            }
        }
        throw new RuntimeException("Failed to find suitable memory type!");
    }
}
