package org.conspiracraft.player;

import org.conspiracraft.Settings;
import org.joml.*;

public class Camera {
    private final Matrix4f viewMatrix = new Matrix4f();
    public Quaternionf pitch = new Quaternionf();
    public float FOV = Settings.fov;
    public void setViewMatrix(float[] matrix) {viewMatrix.set(matrix);}
    public Matrix4f getViewMatrix() {
        return getViewMatrixWithoutPitch().rotate(pitch);
    }
    public Matrix4f getViewMatrixWithoutPitch() {return new Matrix4f(viewMatrix);}
    public Vector3f getForward() {
        Matrix4f viewMatrix = getViewMatrix();
        return new Vector3f(viewMatrix.m20(), viewMatrix.m21(), viewMatrix.m22());
    }
    public Vector3f getForwardWithoutPitch() {
        Matrix4f viewMatrix = getViewMatrixWithoutPitch();
        return new Vector3f(viewMatrix.m20(), viewMatrix.m21(), viewMatrix.m22());
    }
    public Vector3f getRightWithoutPitch() {
        Matrix4f viewMatrix = getViewMatrixWithoutPitch();
        return new Vector3f(viewMatrix.m00(), viewMatrix.m01(), viewMatrix.m02());
    }
    public void rotate(float x, float y) {
        pitch.rotateX(-x);
        viewMatrix.rotateY(-y);
    }
}