package org.conspiracraft.graphics.models;

import java.nio.ByteBuffer;

import static org.conspiracraft.graphics.Graphics.indexBufferOffset;
import static org.conspiracraft.graphics.Graphics.vertexBufferOffset;
import static org.lwjgl.system.MemoryUtil.*;

public class Model {
    public int vertexOffset;
    public int vertexCount;
    public ByteBuffer vertexData;
    public int indexOffset;
    public int indexCount;
    public ByteBuffer indexData;

    public Model(long vertexPtr, long indexPtr, float[] verts, int[] indices) {
        vertexCount = (verts.length/3);
        vertexData = memAlloc(vertexCount * Vertex.SIZE);
        for (int i = 0; i < verts.length; i+=3) {
            vertexData.putFloat(verts[i]);
            vertexData.putFloat(verts[i+1]);
            vertexData.putFloat(verts[i+2]);
        }
        vertexData.flip();

        vertexOffset = vertexBufferOffset;
        memCopy(memAddress(vertexData), vertexPtr+vertexBufferOffset, vertexData.capacity());
        vertexBufferOffset+=vertexData.capacity();

        indexCount = indices.length;
        indexData = memAlloc(indexCount * 4); // 4 bytes per int

        for (int i = 0; i < indices.length; i++) {
            indexData.putInt(indices[i]);
        }
        indexData.flip();

        indexOffset = indexBufferOffset;
        memCopy(memAddress(indexData), indexPtr + indexBufferOffset, indexData.capacity());
        indexBufferOffset += indexData.capacity();
    }
}
