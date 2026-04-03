package org.conspiracraft.renderer.buffers;

import org.joml.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.conspiracraft.renderer.Renderer.currentFrame;
import static org.conspiracraft.renderer.Window.commandBuffers;
import static org.conspiracraft.renderer.Window.pipelineLayout;
import static org.conspiracraft.renderer.buffers.BufferHelper.*;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT;
import static org.lwjgl.vulkan.VK10.vkCmdPushConstants;

public class InstancingBuffer {
    private int offset = 0;
    public void submit(Object[] uniforms) {
        offset = 0;
        ByteBuffer buf = ByteBuffer.allocateDirect(BufferHelper.calculateSize(uniforms)).order(ByteOrder.nativeOrder());
        for (Object obj : uniforms) {
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
        vkCmdPushConstants(commandBuffers[currentFrame], pipelineLayout,
                VK_SHADER_STAGE_VERTEX_BIT, 0, buf);
    }

    private int align(int alignment, int size) {
        int mask = alignment - 1;
        int alignedOffset = (offset + mask) & ~mask;
        offset = alignedOffset+size;
        return alignedOffset;
    }
}
