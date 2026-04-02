package org.conspiracraft.renderer.models;

import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

import static org.conspiracraft.renderer.Window.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Model {
    public int offset;
    public int vertexCount;
    public ByteBuffer vertexData;
    public float[] positions;
    public float[] normals;

    public Model(long mappedPtr, float[] verts, float[] normals) {
        this.positions = verts;
        this.normals = normals;
        vertexCount = (verts.length/3);
        vertexData = memAlloc(vertexCount * Vertex.size);
        for (int i = 0; i < verts.length; i+=3) {
            vertexData.putFloat(positions[i]);
            vertexData.putFloat(positions[i+1]);
            vertexData.putFloat(positions[i+2]);

            vertexData.putFloat(normals[i]);
            vertexData.putFloat(normals[i+1]);
            vertexData.putFloat(normals[i+2]);
        }
        vertexData.flip();

        offset = vertexBufferOffset;
        memCopy(memAddress(vertexData), mappedPtr+vertexBufferOffset, vertexData.capacity());
        vertexBufferOffset+=vertexData.capacity();
    }
}
