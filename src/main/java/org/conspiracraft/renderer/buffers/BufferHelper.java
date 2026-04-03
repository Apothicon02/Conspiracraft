package org.conspiracraft.renderer.buffers;

import org.joml.*;


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
}
