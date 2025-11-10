package org.conspiracraft.game;

import org.conspiracraft.engine.Camera;
import org.conspiracraft.engine.Utils;
import org.joml.*;

import java.lang.Math;

public class Player {
    private final Camera camera = new Camera();
    public static float scale = 1f;
    public float speed = Math.max(0.15f, 0.15f*scale);
    public boolean sprint = false;
    public boolean superSprint = false;
    public boolean forward = false;
    public boolean backward = false;
    public boolean rightward = false;
    public boolean leftward = false;
    public boolean upward = false;
    public boolean downward = false;
    public static Vector4f voxelColor = new Vector4f(1);

    public Player() {}

    public void tick() {
        camera.oldViewMatrix = new Matrix4f(camera.getViewMatrix());
        float modifiedSpeed = speed;
        if (sprint) {
            modifiedSpeed *= 10;
        }
        if (superSprint) {
            modifiedSpeed *= 100;
        }
        if (forward) {
            camera.moveForward(modifiedSpeed);
        } else if (backward) {
            camera.moveBackwards(modifiedSpeed);
        }
        if (rightward) {
            camera.moveRight(modifiedSpeed);
        } else if (leftward) {
            camera.moveLeft(modifiedSpeed);
        }
        if (upward) {
            camera.moveUp(modifiedSpeed);
        } else if (downward) {
            camera.moveDown(modifiedSpeed);
        }
    }

    public Matrix4f getCameraMatrix() {
        return Utils.getInterpolatedMat4(camera.oldViewMatrix, camera.getViewMatrix());
//        Matrix4f camMatrix = new Matrix4f(camera.getViewMatrix());
//        return camMatrix.setTranslation(Utils.getInterpolatedVec(camera.oldViewMatrix.getTranslation(new Vector3f(0)), camMatrix.getTranslation(new Vector3f(0))));
    }


    public void rotate(float pitch, float yaw) {
        camera.addRotation(pitch, yaw);
    }
}