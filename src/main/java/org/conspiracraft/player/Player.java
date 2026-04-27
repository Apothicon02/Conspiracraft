package org.conspiracraft.player;

import org.conspiracraft.Main;
import org.conspiracraft.physics.AABB;
import org.conspiracraft.physics.PhysicsHelper;
import org.conspiracraft.audio.AudioController;
import org.conspiracraft.audio.Source;
import org.conspiracraft.blocks.BlockTags;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.gui.GUI;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;

import static org.lwjgl.sdl.SDLScancode.*;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_LCTRL;

public class Player {
    public InputHandler inputHandler = new InputHandler();
    public Camera camera = new Camera();
    public Vector3f pos = new Vector3f();
    public Vector3f movement = new Vector3f();
    public Vector3f vel = new Vector3f();
    public Inventory inv = new Inventory();
    public Vector3f selectedBlock = new Vector3f();
    public Vector3f prevSelectedBlock = new Vector3f();

    public boolean chiselMode = false;
    public boolean creative = true;
    public float bobbing = 0f;
    public float grav = 0.1f;
    public float jumpStrength = 0.6f;
    public float scale = 1.f;
    public float baseEyeHeight = 0.8125f*scale;
    public float eyeHeight = baseEyeHeight;
    public float baseHeight = eyeHeight+(0.0875f*scale);
    public float height = baseHeight;
    public float width = 0.2f*scale;
    public float baseSpeed = Math.max(0.425f, 0.425f*scale);
    public float speed = baseSpeed;
    public float sprintSpeed = 1.5f;
    public float airSpeed = 0.33f;
    public boolean flying = true, forward = false, backward = false, leftward = false, rightward = false, upward = false, downward = false, sprinting = false, superSprinting = false, crouching = false, crawling = false;

    public final Source breakingSource;

    public Player() {
        breakingSource = new Source(pos, 1, 1, 0, 1);
    }
    public static void create() {
        Main.player = new Player();
        Main.player.inputHandler.init();
        Main.player.pos.set(1332, 195, 2338);
        Main.player.inv.init();
    }

    public Vector3f oldCamTranslation = new Vector3f();
    public void tick() {
        oldCamTranslation.set(getCameraTranslation());
        movementTick();
        doSounds();
    }

    public float friction = 0.75f;
    public void movementTick() {
        Matrix4f cam = camera.getViewMatrixWithoutPitch().setTranslation(0, 0, 0).invert();
        Vector3f newMovement = new Vector3f();
        if (forward) {newMovement.add(cam.positiveZ(new Vector3f()));}
        if (backward) {newMovement.add(cam.positiveZ(new Vector3f()).negate());}
        if (rightward) {newMovement.add(cam.positiveX(new Vector3f()));}
        if (leftward) {newMovement.add(cam.positiveX(new Vector3f()).negate());}
        if (flying) {
            if (upward) {newMovement.add(0, 1, 0);}
            if (downward) {newMovement.add(0, -1, 0);}
        }
        if (newMovement.length() > 0) {newMovement.normalize();}
        newMovement.mul(speed*(downward?0.65f:1.f));
        newMovement.mul(sprinting ? ((flying ? 2 : 1) * sprintSpeed) : 1.f);
        newMovement.mul(superSprinting ? 10.f : 1.f);
        Vector2i blockIn = World.getBlock(pos.x(), pos.y(), pos.z());
        Vector2i blockOn = World.getBlock(pos.x(), pos.y()-1.05f, pos.z());
        boolean canMove = flying || BlockTypes.blockTypeMap.get(blockOn.x()).blockProperties.isCollidable || blockIn.x() == 1;
        float modifiedGrav = grav;
        friction = 0.99f; //1-airFriction=maxFriction
        if (!flying) {
            if (blockIn.x() == BlockTypes.getId(BlockTypes.WATER)) {
                modifiedGrav *= 0.2f;
                friction *= 0.9f;
                newMovement.mul(0.5f);
            } else if (BlockTags.leaves.tagged.contains(blockIn.x())) {
                if (blockIn.y() == 0) {
                    friction *= 0.5f;
                    newMovement.mul(0.5f);
                } else {
                    friction *= 0.9f;
                    newMovement.mul(0.9f);
                }
            } else if (canMove) {
                friction *= 0.75f;
            } else {
                newMovement.mul(airSpeed);
            }
        } else {
            friction *= 0.5f;
        }
        movement.mul(friction);
        if (canMove) {
            movement.set(Utils.furthestFromZero(newMovement.x(), movement.x()), Utils.furthestFromZero(newMovement.y(), movement.y()), Utils.furthestFromZero(newMovement.z(), movement.z()));
        }
        vel.mul(friction);
        if (!flying) {
            boolean onSolid = PhysicsHelper.colliding(pos.x(), (pos.y() - height) - 0.075f, pos.z(), new Vector3f(width, 0.075f, width));
            if (!onSolid) {
                vel.y -= modifiedGrav;
            } else {
                if (vel.y < 0) {
                    vel.y = 0;
                }
                if (upward) {
                    vel.y = Math.max(jumpStrength, vel.y());
                }
            }
        }
        vel.max(new Vector3f(-3)).min((new Vector3f(3)));
        AABB playerAABB = new AABB(
                pos.x()-width, pos.x()+width,
                pos.y()-height, pos.y()+height,
                pos.z()-width, pos.z()+width);
        PhysicsHelper.move(playerAABB, new Vector3f(movement).add(vel), vel);
        pos.set(playerAABB.xMin+width, playerAABB.yMin+height, playerAABB.zMin+width);
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
            if (inputHandler.keyRelease(SDL_SCANCODE_X)) {
                flying = !flying;
            }
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
