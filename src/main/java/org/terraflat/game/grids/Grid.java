package org.terraflat.game.grids;

import org.joml.Quaternionf;
import org.terraflat.game.elements.Element;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;

public class Grid {
    Vector3f pos;
    Quaternionf rot;
    float scale;
    Map<Vector3i, Element> elements = new HashMap<>();

    public Grid() {
        this.pos = new Vector3f(0, 0, 0);
        this.rot = new Quaternionf(0f, 0f, 0f, 0f);
        this.scale = 1;
    }

    public Grid(Vector3f pos) {
        this.pos = pos;
        this.rot = new Quaternionf(0f, 0f, 0f, 0f);
        this.scale = 1;
    }

    public Grid(float scale) {
        this.pos = new Vector3f(0, 0, 0);
        this.rot = new Quaternionf(0f, 0f, 0f, 0f);
        this.scale = scale;
    }

    public Grid(Vector3f pos, float scale) {
        this.pos = pos;
        this.rot = new Quaternionf(0f, 0f, 0f, 0f);
        this.scale = scale;
    }

    public Grid(Vector3f pos, Quaternionf rot) {
        this.pos = pos;
        this.rot = rot;
        this.scale = 1;
    }

    public Grid(Vector3f pos, Quaternionf rot, float scale) {
        this.pos = pos;
        this.rot = rot;
        this.scale = scale;
    }

    public Vector3f getPos() {
        return pos;
    }

    public void setPos(Vector3f pos) {
        this.pos = pos;
    }

    public Quaternionf getRot() {
        return rot;
    }

    public void setRot(Quaternionf rot) {
        this.rot = rot;
    }

    public void setRotFromRad(float x, float y, float z, float angle) {
        this.rot.fromAxisAngleRad(x, y, z, angle);
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public Element addElement(Element element) {
        element.setGrid(this);
        this.elements.put(element.getPos(), element);
        return element;
    }

    public Element getElement(Vector3i pos) {
        return elements.get(pos);
    }

    public Map<Vector3i, Element> getElements() {
        return elements;
    }
}