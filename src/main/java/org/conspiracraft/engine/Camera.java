package org.conspiracraft.engine;

import org.conspiracraft.game.Player;
import org.joml.*;

public class Camera {
    private final Matrix4f viewMatrix = new Matrix4f().setTranslation(new Vector3f(0, Player.eyeHeight, 0));
    public Quaternionf pitch = new Quaternionf();

    public void setPitch(Quaternionf newPitch) {
        pitch = newPitch;
    }
    public void setViewMatrix(float[] matrix) {
        viewMatrix.set(matrix);
    }
    public Matrix4f getViewMatrix() {
        return getViewMatrixWithoutPitch().rotate(pitch);
    }
    public Matrix4f getViewMatrixWithoutPitch() {
        return new Matrix4f(viewMatrix);
    }
    public void rotate(float x, float y) {
        pitch.rotateX(x);
        viewMatrix.rotateY(y);
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