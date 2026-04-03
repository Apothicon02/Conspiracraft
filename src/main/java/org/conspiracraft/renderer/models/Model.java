package org.conspiracraft.renderer.models;

import java.nio.ByteBuffer;

import static org.conspiracraft.renderer.Window.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Model {
    public int offset;
    public int vertexCount;
    public ByteBuffer vertexData;
    public float[] positions;

    public Model(long mappedPtr, float[] verts) {
        this.positions = verts;
        vertexCount = (verts.length/3);
        vertexData = memAlloc(vertexCount * Vertex.SIZE);
        for (int i = 0; i < verts.length; i+=3) {
            vertexData.putFloat(positions[i]);
            vertexData.putFloat(positions[i+1]);
            vertexData.putFloat(positions[i+2]);
        }
        vertexData.flip();

        offset = vertexBufferOffset;
        memCopy(memAddress(vertexData), mappedPtr+vertexBufferOffset, vertexData.capacity());
        vertexBufferOffset+=vertexData.capacity();
    }
}
