package org.conspiracraft.engine;

import org.joml.*;

public class Camera {
    public Matrix4f viewMatrix = new Matrix4f().setTranslation(new Vector3f(100, 150, 100));

    public void rotate(float x, float y) {
        viewMatrix.rotateXYZ(x, y, 0);
    }
    public void move(float x, float y, float z) {
        viewMatrix.translate(x, y, z);
    }
}