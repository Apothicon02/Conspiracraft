package org.conspiracraft.renderer.buffers;

import org.conspiracraft.Main;
import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static org.conspiracraft.renderer.Renderer.currentFrame;
import static org.conspiracraft.renderer.Window.uniformBuffersMapped;
import static org.lwjgl.system.MemoryUtil.*;

public class DEFAULT_UBO extends UBO {

    private Object[] uniformStorage = new Object[]{new Matrix4f(), new Matrix4f(), new Matrix4f()};
    @Override public Object[] uniforms() {return uniformStorage;}
    private int size = 0;
    @Override public int size(){return size;}
    public DEFAULT_UBO() {
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
        ((Matrix4f)uniformStorage[1]).identity(); //view
        ((Matrix4f)uniformStorage[2]).set(Main.window.getProjectionMatrix()); //proj
    }
    public void submit(MemoryStack stack) {
        int offset = 0;
        ByteBuffer buf = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        for (Object obj : uniforms()) {
            switch (obj) {
                case Float v -> {buf.putFloat(offset, v);offset += FLOAT_SIZE;}
                case Integer v -> {buf.putInt(offset, v);offset += FLOAT_SIZE;}
                case Vector2f v -> {v.get(offset, buf);offset += VEC2_SIZE;}
                case Vector3f v -> {v.get(offset, buf);offset += VEC3_SIZE;}
                case Vector4f v -> {v.get(offset, buf);offset += VEC4_SIZE;}
                case Vector2i v -> {v.get(offset, buf);offset += VEC2_SIZE;}
                case Vector3i v -> {v.get(offset, buf);offset += VEC3_SIZE;}
                case Vector4i v -> {v.get(offset, buf);offset += VEC4_SIZE;}
                case Matrix2f v -> {v.get(offset, buf);offset += MAT2_SIZE;}
                case Matrix3f v -> {v.get(offset, buf);offset += MAT3_SIZE;}
                case Matrix4f v -> {v.get(offset, buf);offset += MAT4_SIZE;}
                default -> throw new IllegalArgumentException("Cannot read uniform for object type: "+obj.getClass().getName());
            };
        }
        buf.rewind();
        memCopy(memAddress(buf), uniformBuffersMapped[currentFrame].get(0), buf.remaining());
    }
}
