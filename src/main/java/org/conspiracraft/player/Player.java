package org.conspiracraft.player;

import org.conspiracraft.Main;
import org.conspiracraft.audio.AudioController;
import org.conspiracraft.audio.Source;
import org.conspiracraft.gui.GUI;
import org.conspiracraft.utils.Utils;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.sdl.SDLScancode.*;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_LCTRL;

public class Player {
    public InputHandler inputHandler = new InputHandler();
    public Camera camera = new Camera();
    public Vector3f pos = new Vector3f();
    public Vector3f vel = new Vector3f();
    public Inventory inv = new Inventory();
    public Vector3f selectedBlock = new Vector3f();
    public Vector3f prevSelectedBlock = new Vector3f();

    public boolean chiselMode = false;
    public boolean creative = true;
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
    public boolean forward = false, backward = false, leftward = false, rightward = false, upward = false, downward = false, sprinting = false, superSprinting = false, crouching = false, crawling = false;

    public final Source breakingSource;

    public Player() {
        breakingSource = new Source(pos, 1, 1, 0, 1);
    }
    public static void create() {
        Main.player = new Player();
        Main.player.inputHandler.init();
        Main.player.pos.set(730, 90, 840);
        Main.player.inv.init();
    }

    public Vector3f oldCamTranslation = new Vector3f();
    public void tick() {
        oldCamTranslation.set(getCameraTranslation());
        movementTick();
        doSounds();
    }

    public void movementTick() {
        movementInputs();
        Matrix4f cam = camera.getViewMatrixWithoutPitch().setTranslation(0, 0, 0).invert();
        Vector3f movement = new Vector3f();
        if (forward) {movement.add(cam.positiveZ(new Vector3f()));}
        if (backward) {movement.add(cam.positiveZ(new Vector3f()).negate());}
        if (rightward) {movement.add(cam.positiveX(new Vector3f()));}
        if (leftward) {movement.add(cam.positiveX(new Vector3f()).negate());}
        if (upward) {movement.add(0, 1, 0);}
        if (downward) {movement.add(0, -1, 0);}
        pos.add(movement.mul(speed*((sprinting||superSprinting)?(superSprinting ? sprintSpeed*10 : sprintSpeed):(downward?0.5f:1.f))));
    }

    public void movementInputs() {
        if (GUI.inventoryOpen || GUI.pauseMenuOpen) {
            forward = false;
            backward = false;
            rightward = false;
            leftward = false;
            upward = false;
            downward = false;
            sprinting = false;
            superSprinting = false;
        } else {
            forward = inputHandler.isKeyDown(SDL_SCANCODE_W);
            backward = inputHandler.isKeyDown(SDL_SCANCODE_S);
            rightward = inputHandler.isKeyDown(SDL_SCANCODE_D);
            leftward = inputHandler.isKeyDown(SDL_SCANCODE_A);
            upward = inputHandler.isKeyDown(SDL_SCANCODE_SPACE);
            downward = inputHandler.isKeyDown(SDL_SCANCODE_LCTRL);
            sprinting = inputHandler.isKeyDown(SDL_SCANCODE_LSHIFT);
            superSprinting = inputHandler.isKeyDown(SDL_SCANCODE_CAPSLOCK);
        }
    }

    public void doSounds() {
        Matrix4f cam = getCameraMatrix();
        Vector3f forward = new Vector3f();
        Vector3f up = new Vector3f();
        cam.positiveZ(forward).negate();
        cam.positiveY(up);
        AudioController.setListenerData(new Vector3f(pos.x(), pos.y()+eyeHeight, pos.z()), vel, new float[]{forward.x(), forward.y(), forward.z(), up.x(), up.y(), up.z()});
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
