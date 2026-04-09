package org.conspiracraft.graphics.buffers;

import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;

public class ShaderStorageBuffer extends StagedBuffer {
    public static List<ShaderStorageBuffer> storageBuffers = new ArrayList<>();

    public int stageFlags;
    public ShaderStorageBuffer(MemoryStack stack, int bufferSize, int stageFlags) {
        super(stack, bufferSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT );
        this.stageFlags = stageFlags;
        storageBuffers.add(this);
    }
}