package org.conspiracraft.renderer.buffers;

import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.List;

public class ShaderStorageBuffer extends Buffer {
    public static List<ShaderStorageBuffer> buffers = new ArrayList<>();

    public int stageFlags;
    public ShaderStorageBuffer(MemoryStack stack, int amount, int bufferSize, int usage, int properties, int stageFlags) {
        super(stack, amount, bufferSize, usage, properties);
        this.stageFlags = stageFlags;
        buffers.add(this);
    }
}
