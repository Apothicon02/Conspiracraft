package org.conspiracraft.renderer.buffers;

import org.conspiracraft.Main;
import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

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
        ((Matrix4f)uniformStorage[1]).set(new Matrix4f()); //view
        ((Matrix4f)uniformStorage[1]).set(Main.window.updateProjectionMatrix()); //proj
    }
    public void submit(MemoryStack stack) {
        ByteBuffer buf = ByteBuffer.allocateDirect(size);
        for (Object obj : uniforms()) {
            switch (obj) {
                case Float v -> buf.putFloat(v);
                case Integer v -> buf.putInt(v);
                case Vector2f v -> v.get(buf);
                case Vector3f v -> v.get(buf);
                case Vector4f v -> v.get(buf);
                case Vector2i v -> v.get(buf);
                case Vector3i v -> v.get(buf);
                case Vector4i v -> v.get(buf);
                case Matrix2f v -> v.get(buf);
                case Matrix3f v -> v.get(buf);
                case Matrix4f v -> v.get(buf);
                default -> throw new IllegalArgumentException("Cannot read uniform for object type: "+obj.getClass().getName());
            };
        }
        buf.flip();
        memCopy(memAddress(buf), uniformBuffersMapped[currentFrame].get(0), buf.remaining());
    }
}
