package org.apothicon.core.elements;

public class Models {
    public static Model CUBE = new Model(new float[] {
            -0.5f,  0.5f, 0.0f,
            -0.5f, -0.5f, 0.0f,
            0.5f, -0.5f, 0.0f,
            0.5f,  0.5f, 0.0f,
    }, new int[] {
            0, 1, 3, 3, 1, 2,
    });
}
