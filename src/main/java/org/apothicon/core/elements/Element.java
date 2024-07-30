package org.apothicon.core.elements;

import org.joml.Vector3f;

public class Element {
    private Model model;
    private Vector3f pos, rotation;
    private float scale;

    public Element(Model model, Vector3f pos, Vector3f rotation, float scale) {
        this.model = model;
        this.pos = pos;
        this.rotation = rotation;
        this.scale = scale;
    }

    public void incrementPos(float x, float y, float z) {
        this.pos.x += x;
        this.pos.y += y;
        this.pos.z +=z ;
    }

    public void setPos(float x, float y, float z) {
        this.pos.x = x;
        this.pos.y = y;
        this.pos.z =z ;
    }

    public void incrementRot(float x, float y, float z) {
        this.rotation.x += x;
        this.rotation.y += y;
        this.rotation.z +=z ;
    }

    public void setRot(float x, float y, float z) {
        this.rotation.x = x;
        this.rotation.y = y;
        this.rotation.z =z ;
    }

    public Model getModel() {
        return model;
    }

    public Vector3f getPos() {
        return pos;
    }

    public Vector3f getRotation() {
        return rotation;
    }

    public float getScale() {
        return scale;
    }
}