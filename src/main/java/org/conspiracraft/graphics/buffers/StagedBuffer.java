package org.conspiracraft.graphics.buffers;

import org.lwjgl.system.MemoryStack;

import static org.lwjgl.vulkan.VK14.*;

public class StagedBuffer {
    public Buffer stagingBuffer;
    public Buffer buffer;
    public StagedBuffer(MemoryStack stack, int bufferSize, int usage, boolean temporary) {
        stagingBuffer = new Buffer(stack, bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, temporary);
        buffer = new Buffer(stack, bufferSize, usage | VK_BUFFER_USAGE_TRANSFER_DST_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, temporary);
    }
}
