package org.conspiracraft.renderer.assets;

public class Model {
    public int vaoId;
    public float[] positions;
    public float[] normals;

    public Model(float[] verts) {
        this.positions = verts;
    }

    public Model(float[] verts, float[] normals) {
        this.positions = verts;
        this.normals = normals;
    }
}
