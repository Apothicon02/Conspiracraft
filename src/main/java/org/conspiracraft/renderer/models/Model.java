package org.conspiracraft.renderer.models;

import java.nio.ByteBuffer;

import static org.conspiracraft.renderer.Window.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Model {
    public int offset;
    public int vertexCount;
    public ByteBuffer vertexData;
    public float[] positions;
    public float[] normals;
    public float[] uvs;

    public Model(long mappedPtr, float[] verts, float[] normals, float[] uvs) {
        this.positions = verts;
        this.normals = normals;
        this.uvs = uvs;
        vertexCount = (verts.length/3);
        vertexData = memAlloc(vertexCount * Vertex.SIZE);
        for (int i = 0; i < vertexCount; i++) {
            int i3 = i*3;
            vertexData.putFloat(positions[i3]);
            vertexData.putFloat(positions[i3+1]);
            vertexData.putFloat(positions[i3+2]);

            vertexData.putFloat(normals[i3]);
            vertexData.putFloat(normals[i3+1]);
            vertexData.putFloat(normals[i3+2]);

            int i2 = i*2;
            vertexData.putFloat(uvs[i2]);
            vertexData.putFloat(uvs[i2+1]);
        }
        vertexData.flip();

        offset = vertexBufferOffset;
        memCopy(memAddress(vertexData), mappedPtr+vertexBufferOffset, vertexData.capacity());
        vertexBufferOffset+=vertexData.capacity();
    }
}
