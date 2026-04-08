package org.conspiracraft.graphics.buffers.ubos;

import org.conspiracraft.graphics.buffers.Buffer;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.List;

public class UniformBuffer extends Buffer {
    public static List<UniformBuffer> uniformBuffers = new ArrayList<>();

    public UBO ubo;
    public int stageFlags;
    public UniformBuffer(MemoryStack stack, int bufferSize, int usage, int properties, int stageFlags, UBO ubo) {
        super(stack, bufferSize, usage, properties);
        this.stageFlags = stageFlags;
        this.ubo = ubo;
        uniformBuffers.addLast(this);
    }
}
