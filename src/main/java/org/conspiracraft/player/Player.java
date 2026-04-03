package org.conspiracraft.player;

import org.conspiracraft.Utils;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.conspiracraft.Main.player;
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
    public boolean forward = false, backward = false, leftward = false, rightward = false, upward = false, downward = false;

    public Player() {
        inputHandler.init();
    }

    public Vector3f oldCamTranslation = new Vector3f();
    public void tick() {
        oldCamTranslation.set(getCameraTranslation());
        movementTick();
    }

    public void movementTick() {
        movementInputs();
        if (player.forward) {pos.add(0, 0, 1);}
        if (player.backward) {pos.add(0, 0, -1);}
        if (player.rightward) {pos.add(1, 0, 0);}
        if (player.leftward) {pos.add(-1, 0, 0);}
        if (player.upward) {pos.add(0, 1, 0);}
        if (player.downward) {pos.add(0, -1, 0);}
    }

    public void movementInputs() {
        player.forward = inputHandler.isKeyDown(SDL_SCANCODE_W);
        player.backward = inputHandler.isKeyDown(SDL_SCANCODE_S);
        player.rightward = inputHandler.isKeyDown(SDL_SCANCODE_D);
        player.leftward = inputHandler.isKeyDown(SDL_SCANCODE_A);
        player.upward = inputHandler.isKeyDown(SDL_SCANCODE_SPACE);
        player.downward = inputHandler.isKeyDown(SDL_SCANCODE_LCTRL);
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
