package org.conspiracraft.renderer.buffers.ubos;

import org.conspiracraft.renderer.buffers.Buffer;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.List;

public class UniformBuffer extends Buffer {
    public static List<UniformBuffer> buffers = new ArrayList<>();

    public UBO ubo;
    public int stageFlags;
    public UniformBuffer(MemoryStack stack, int amount, int bufferSize, int usage, int properties, int stageFlags, UBO ubo) {
        super(stack, amount, bufferSize, usage, properties);
        this.stageFlags = stageFlags;
        this.ubo = ubo;
        buffers.add(this);
    }
}
