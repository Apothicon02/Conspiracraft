package org.conspiracraft.player;

import org.conspiracraft.utils.Utils;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.sdl.SDLScancode.*;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_LCTRL;

public class Player {
    public InputHandler inputHandler = new InputHandler();
    public Camera camera = new Camera();
    public Vector3f pos = new Vector3f();

    public float bobbing = 0f;
    public float scale = 2f;
    public float baseEyeHeight = 1.625f*scale;
    public float eyeHeight = baseEyeHeight;
    public float baseHeight = eyeHeight+(0.175f*scale);
    public float height = baseHeight;
    public float width = 0.2f*scale;
    public float baseSpeed = Math.max(0.33f, 0.33f*scale);
    public float speed = baseSpeed;
    public float sprintSpeed = 1.5f;
    public boolean forward = false, backward = false, leftward = false, rightward = false, upward = false, downward = false, sprinting = false, superSprinting = false;

    public Player() {
        inputHandler.init();
        pos.set(730, 80, 840);
    }

    public Vector3f oldCamTranslation = new Vector3f();
    public void tick() {
        oldCamTranslation.set(getCameraTranslation());
        movementTick();
    }

    public void movementTick() {
        movementInputs();
        Matrix4f cam = camera.getViewMatrixWithoutPitch().setTranslation(0, 0, 0).invert();
        Vector3f movement = new Vector3f();
        if (forward) {movement.add(cam.positiveZ(new Vector3f()).negate());}
        if (backward) {movement.add(cam.positiveZ(new Vector3f()));}
        if (rightward) {movement.add(cam.positiveX(new Vector3f()));}
        if (leftward) {movement.add(cam.positiveX(new Vector3f()).negate());}
        if (upward) {movement.add(0, 1, 0);}
        if (downward) {movement.add(0, -1, 0);}
        pos.add(movement.mul(speed*((sprinting||superSprinting)?(superSprinting ? sprintSpeed*10 : sprintSpeed):(downward?0.5f:1.f))));
    }

    public void movementInputs() {
        forward = inputHandler.isKeyDown(SDL_SCANCODE_W);
        backward = inputHandler.isKeyDown(SDL_SCANCODE_S);
        rightward = inputHandler.isKeyDown(SDL_SCANCODE_D);
        leftward = inputHandler.isKeyDown(SDL_SCANCODE_A);
        upward = inputHandler.isKeyDown(SDL_SCANCODE_SPACE);
        downward = inputHandler.isKeyDown(SDL_SCANCODE_LCTRL);
        sprinting = inputHandler.isKeyDown(SDL_SCANCODE_LSHIFT);
        superSprinting = inputHandler.isKeyDown(SDL_SCANCODE_CAPSLOCK);
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
