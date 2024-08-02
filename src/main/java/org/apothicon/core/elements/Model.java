package org.apothicon.core.elements;

public class Model {
    private final float[] positions;
    private final int[] indices;

    public Model(float[] positions, int[] indices) {
        this.positions = positions;
        this.indices = indices;
    }

    public float[] getPositions() {
        return positions;
    }

    public int[] getIndices() {
        return indices;
    }
}
