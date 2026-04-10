package org.conspiracraft.graphics.buffers;

import org.conspiracraft.graphics.Graphics;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.List;

import static org.conspiracraft.graphics.Device.vkDevice;
import static org.conspiracraft.graphics.buffers.BufferHelper.createBuffer;
import static org.lwjgl.vulkan.VK10.*;

public class Buffer {
    public static List<Buffer> buffers = new ArrayList<>();

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
            buffers.addLast(this);
            Graphics.recreateDescriptors(stack);
        }
    }
}