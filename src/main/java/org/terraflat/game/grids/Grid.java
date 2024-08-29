package org.terraflat.game.grids;

import org.joml.Matrix4f;
import org.terraflat.game.elements.Element;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;

public class Grid {
    private Matrix4f matrix;
    float scale;
    Map<Vector3i, Element> elements = new HashMap<>();

    public Grid() {
        matrix = new Matrix4f();
        scale = 1;
    }
    public Grid(Matrix4f newMatrix) {
        matrix = newMatrix;
        scale = 1;
    }
    public Grid(float newScale) {
        matrix = new Matrix4f();
        scale = newScale;
    }
    public Grid(Matrix4f newMatrix, float newScale) {
        matrix = newMatrix;
        scale = newScale;
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
    public Element addElement(Element element) {
        element.setGrid(this);
        this.elements.put(new Vector3i(0, 0, 0), element);
        return element;
    }
    public Element addElement(Element element, Vector3i pos) {
        element.setGrid(this);
        this.elements.put(pos, element);
        return element;
    }
    public Element getElement(Vector3i pos) {
        return elements.get(pos);
    }
    public Map<Vector3i, Element> getElements() {
        return elements;
    }
}