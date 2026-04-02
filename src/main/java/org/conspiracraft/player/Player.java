package org.conspiracraft.player;

import org.conspiracraft.Utils;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Player {
    public InputHandler inputHandler = new InputHandler();
    public Camera camera = new Camera();
    public Vector3f pos = new Vector3f();

    public float bobbing = 0f;
    public float scale = 2f;
    public float baseEyeHeight = 1.625f*scale;
    public float eyeHeight = baseEyeHeight;

    public Player() {
        inputHandler.init();
    }

    public Vector3f oldCamTranslation = new Vector3f();
    public void tick() {
        oldCamTranslation.set(getCameraTranslation());
        movementTick();
    }

    public void movementTick() {
    }

    public void rotate(float pitch, float yaw) {
        camera.rotate(pitch, yaw);
    }

    public Vector3f getCameraTranslation() {
        Vector3f translation = new Vector3f();
        camera.getViewMatrix().getTranslation(translation);
        return translation.add(pos.x(), pos.y()+eyeHeight+(bobbing*1.5f), pos.z());
    }
    public Matrix4f getCameraMatrix() {
        return new Matrix4f(camera.getViewMatrix()).setTranslation(Utils.getInterpolatedVec(oldCamTranslation, getCameraTranslation())).invert();
    }
}
