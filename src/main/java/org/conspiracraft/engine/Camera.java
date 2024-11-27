package org.conspiracraft.engine;

import org.joml.*;

public class Camera {
    public Matrix4f viewMatrix = new Matrix4f().setTranslation(new Vector3f(100, 150, 100));

    public void rotate(float x, float y) {
        viewMatrix.rotateXYZ(x, y, 0);
    }
    public void move(float x, float y, float z, boolean countRotation) {
        if (countRotation) {
            viewMatrix.translate(x, y, z);
        } else {
            Vector3f prevTranslation = new Vector3f();
            viewMatrix.getTranslation(prevTranslation);
            viewMatrix.setTranslation(new Vector3f(x+prevTranslation.x, y+prevTranslation.y, z+prevTranslation.z));
        }
    }
}