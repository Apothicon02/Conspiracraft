package org.conspiracraft.renderer.buffers;

import org.conspiracraft.Main;
import org.conspiracraft.world.World;
import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.conspiracraft.renderer.Renderer.currentFrame;
import static org.conspiracraft.renderer.Window.uniformBuffersMapped;
import static org.conspiracraft.renderer.buffers.BufferHelper.*;
import static org.lwjgl.system.MemoryUtil.*;

public class DefaultUBO extends UBO {

    private Object[] uniformStorage = new Object[]{new Matrix4f(), new Matrix4f(), new Vector3f()};
    @Override public Object[] uniforms() {return uniformStorage;}
    private int size = 0;
    @Override public int size(){return size;}
    public DefaultUBO() {
        super();
        calculateSize();
    }
    public void calculateSize() {
        size = 0;
        for (Object obj : uniforms()) {
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
    }
    public void update(MemoryStack stack) {
        ((Matrix4f)uniformStorage[0]).identity().set(Main.player.getCameraMatrix());
        ((Matrix4f)uniformStorage[1]).set(Main.window.updateProjectionMatrix());
        ((Vector3f)uniformStorage[2]).set(World.worldType.getSunPos());
    }
    private int offset = 0;
    public void submit() {
        offset = 0;
        ByteBuffer buf = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        for (Object obj : uniforms()) {
            switch (obj) {
                case Float v -> buf.putFloat(align(FLOAT_ALIGN, FLOAT_SIZE), v);
                case Integer v -> buf.putInt(align(FLOAT_ALIGN, FLOAT_SIZE), v);
                case Vector2f v -> v.get(align(VEC2_ALIGN, VEC2_SIZE), buf);
                case Vector3f v -> v.get(align(OTHER_ALIGN, VEC3_SIZE), buf);
                case Vector4f v -> v.get(align(OTHER_ALIGN, VEC4_SIZE), buf);
                case Vector2i v -> v.get(align(OTHER_ALIGN, VEC2_SIZE), buf);
                case Vector3i v -> v.get(align(OTHER_ALIGN, VEC3_SIZE), buf);
                case Vector4i v -> v.get(align(OTHER_ALIGN, VEC4_SIZE), buf);
                case Matrix2f v -> v.get(align(OTHER_ALIGN, MAT2_SIZE), buf);
                case Matrix3f v -> v.get(align(OTHER_ALIGN, MAT3_SIZE), buf);
                case Matrix4f v -> v.get(align(OTHER_ALIGN, MAT4_SIZE), buf);
                default -> throw new IllegalArgumentException("Cannot read uniform for object type: "+obj.getClass().getName());
            };
        }
        buf.rewind();
        memCopy(memAddress(buf), uniformBuffersMapped[currentFrame].get(0), buf.remaining());
    }

    private int align(int alignment, int size) {
        int mask = alignment - 1;
        int alignedOffset = (offset + mask) & ~mask;
        offset = alignedOffset+size;
        return alignedOffset;
    }
}
