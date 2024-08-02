package org.apothicon.core.grids;

import org.apothicon.core.elements.Element;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;

public class Grid {
    Vector3f pos;
    Vector3f rot;
    float scale;
    Map<Vector3i, Element> elements = new HashMap<>();

    public Grid() {
        this.pos = new Vector3f(0, 0, 0);
        this.rot = new Vector3f(0, 0, 0);
        this.scale = 1;
    }

    public Grid(Vector3f pos) {
        this.pos = pos;
        this.rot = new Vector3f(0, 0, 0);
        this.scale = 1;
    }

    public Grid(float scale) {
        this.pos = new Vector3f(0, 0, 0);
        this.rot = new Vector3f(0, 0, 0);
        this.scale = scale;
    }

    public Grid(Vector3f pos, float scale) {
        this.pos = pos;
        this.rot = new Vector3f(0, 0, 0);
        this.scale = scale;
    }

    public Grid(Vector3f pos, Vector3f rot) {
        this.pos = pos;
        this.rot = rot;
        this.scale = 1;
    }

    public Grid(Vector3f pos, Vector3f rot, float scale) {
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

    public Vector3f getRot() {
        return rot;
    }

    public void setRot(Vector3f rot) {
        this.rot = rot;
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