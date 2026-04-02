package org.conspiracraft.renderer.buffers;

import org.lwjgl.system.MemoryStack;

public class UBO {
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

    public int size(){return 0;}
    public Object[] uniforms() {return null;}
    public UBO() {}

    public void update(MemoryStack stack) {}
}
