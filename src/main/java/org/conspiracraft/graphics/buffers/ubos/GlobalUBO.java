package org.conspiracraft.graphics.buffers.ubos;

import org.conspiracraft.Main;
import org.conspiracraft.Settings;
import org.conspiracraft.graphics.Graphics;
import org.conspiracraft.graphics.Pipelines;
import org.conspiracraft.graphics.Renderer;
import org.conspiracraft.graphics.Swapchain;
import org.conspiracraft.space.StarSystem;
import org.conspiracraft.world.World;
import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.conspiracraft.graphics.buffers.BufferHelper.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class GlobalUBO extends UBO {
    private Object[] uniformStorage = new Object[]{new Matrix4f(), new Matrix4f(), new Matrix4f(), new Matrix4f(), new Vector4i(), new Vector4f(), new Vector3f(), 0, 0.f, new Vector2i(), new Vector4f(), new Vector4f(), new Vector4f(), new Vector4f(), 0.f, new Vector3f(), 0};
    @Override public Object[] uniforms() {return uniformStorage;}
    private int size = 0;
    @Override public int size(){return size;}
    public GlobalUBO() {
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
    public void update(MemoryStack stack) {
        ((Matrix4f)uniformStorage[2]).identity().set((Matrix4f)uniformStorage[0]);
        ((Matrix4f)uniformStorage[3]).set((Matrix4f)uniformStorage[1]);
        ((Matrix4f)uniformStorage[0]).identity().set(Main.player.getCameraMatrix());
        ((Matrix4f)uniformStorage[1]).set(Main.window.updateProjectionMatrix());
        ((Vector4i)uniformStorage[4]).set(Settings.shadowsEnabled ? 1 : 0, Settings.reflectionsEnabled ? 1 : 0, Settings.upscaleEnabled ? 1 : 0, Settings.taaEnabled ? 1 : 0);
        ((Vector4f)uniformStorage[5]).set(World.worldType.getSkylight());
        ((Vector3f)uniformStorage[6]).set(StarSystem.relativePos);
        uniformStorage[7] = Swapchain.hdr ? 1 : 0;
        uniformStorage[8] = (float)(Main.timeMs);
        ((Vector2i)uniformStorage[9]).set(Settings.width, Settings.height);
        ((Vector4f)uniformStorage[10]).set(World.worldType.getAtmosphereColor());
        ((Vector4f)uniformStorage[11]).set(World.worldType.getNightAtmosphereColor());
        ((Vector4f)uniformStorage[12]).set(World.worldType.getSunsetAtmosphereColor());
        ((Vector4f)uniformStorage[13]).set(World.worldType.getDeepSunsetAtmosphereColor());
        uniformStorage[14] = World.worldType.getFogginess();
        uniformStorage[15] = World.worldType.getSkylightMul();
        uniformStorage[16] = Renderer.jitterFrame;
    }
    private int offset = 0;
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
        memCopy(memAddress(buf), Graphics.globalUBOBuf.pointer.get(0), buf.remaining());
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