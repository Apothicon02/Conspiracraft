package org.terraflat.engine.scene;

import org.joml.*;

public class Entity {

    private final String id;
    private final String modelId;
    private Matrix4f matrix;
    private float scale;

    public Entity(String id, String modelId) {
        this.id = id;
        this.modelId = modelId;
        matrix = new Matrix4f();
        scale = 1;
    }

    public String getId() {
        return id;
    }
    public String getModelId() {
        return modelId;
    }
    public void setMatrix(Matrix4f newMatrix) {
        matrix = newMatrix;
    }
    public Matrix4f getMatrix() {
        return matrix;
    }
    public float getScale() {
        return scale;
    }
    public void setScale(float scale) {
        this.scale = scale;
    }
}