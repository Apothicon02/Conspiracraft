package org.conspiracraft.graphics.buffers.ubos;

import org.conspiracraft.graphics.Pipelines;
import org.conspiracraft.graphics.Renderer;
import org.joml.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.conspiracraft.graphics.buffers.BufferHelper.*;
import static org.lwjgl.vulkan.VK10.*;

public class PushUBO {
    private Object[] uniformStorage = new Object[]{new Matrix4f(), new Vector4f(), 0, new Vector2i(), new Vector2i(), 0, 0};
    public Object[] uniforms() {return uniformStorage;}
    private int size = 0;
    private int offset = 0;
    public int size(){return size;}
    public PushUBO() {
        super();
        calculateSize();
    }
    public void calculateSize() {
        size = 0;
        offset = 0;
        for (Object obj : uniforms()) {
            int fieldSize = switch (obj) {
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
            int alignment = switch (obj) {
                case Integer v -> FLOAT_ALIGN;
                case Float v -> FLOAT_ALIGN;
                case Vector2i v -> VEC2_ALIGN;
                case Vector2f v -> VEC2_ALIGN;
                default -> OTHER_ALIGN;
            };
            offset = align(alignment) + fieldSize;
            size = offset;
        }
    }
    public void update(Matrix4f modelMatrix, Vector4f color) {
        ((Matrix4f)uniformStorage[0]).set(modelMatrix);
        ((Vector4f)uniformStorage[1]).set(color);
    }
    public void update(int instanced) {uniformStorage[2] = instanced;}
    public void updateAtlasOffset(Vector2i atlasOffset) {uniformStorage[3] = atlasOffset;}
    public void updateSize(Vector2i size) {uniformStorage[4] = size;}
    public void updateLayer(int layer) {uniformStorage[5] = layer;}
    public void updateTex(int tex) {uniformStorage[6] = tex;}
    public void submit() {
        offset = 0;
        ByteBuffer buf = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        for (Object obj : uniforms()) {
            switch (obj) {
                case Float v -> buf.putFloat(alignAndOffset(FLOAT_ALIGN, FLOAT_SIZE), v);
                case Integer v -> buf.putInt(alignAndOffset(FLOAT_ALIGN, FLOAT_SIZE), v);
                case Vector2f v -> v.get(alignAndOffset(VEC2_ALIGN, VEC2_SIZE), buf);
                case Vector2i v -> v.get(alignAndOffset(VEC2_ALIGN, VEC2_SIZE), buf);
                case Vector3f v -> v.get(alignAndOffset(OTHER_ALIGN, VEC3_SIZE), buf);
                case Vector3i v -> v.get(alignAndOffset(OTHER_ALIGN, VEC3_SIZE), buf);
                case Vector4f v -> v.get(alignAndOffset(OTHER_ALIGN, VEC4_SIZE), buf);
                case Vector4i v -> v.get(alignAndOffset(OTHER_ALIGN, VEC4_SIZE), buf);
                case Matrix2f v -> v.get(alignAndOffset(OTHER_ALIGN, MAT2_SIZE), buf);
                case Matrix3f v -> v.get(alignAndOffset(OTHER_ALIGN, MAT3_SIZE), buf);
                case Matrix4f v -> v.get(alignAndOffset(OTHER_ALIGN, MAT4_SIZE), buf);
                default -> throw new IllegalArgumentException("Cannot read uniform for object type: "+obj.getClass().getName());
            };
        }
        buf.rewind();
        vkCmdPushConstants(Renderer.currentCmdBuffer, Pipelines.pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT, 0, buf);
    }

    private int align(int alignment) {
        int mask = alignment - 1;
        return (offset + mask) & ~mask;
    }
    private int alignAndOffset(int alignment, int size) {
        int alignedOffset = align(alignment);
        offset = alignedOffset+size;
        return alignedOffset;
    }
}
