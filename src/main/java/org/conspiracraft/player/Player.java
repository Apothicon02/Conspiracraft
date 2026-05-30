package org.conspiracraft.player;

import org.conspiracraft.Main;
import org.conspiracraft.audio.BlockSFX;
import org.conspiracraft.entities.Entity;
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Random;

import static org.conspiracraft.Main.timeAccum;
import static org.conspiracraft.Main.timeMul;
import static org.conspiracraft.physics.PhysicsHelper.getAnyEntity;
import static org.lwjgl.sdl.SDLScancode.*;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_LCTRL;

public class Player {
    public InputHandler inputHandler = new InputHandler();
    public Camera camera = new Camera();
    public Vector3f prevPos = new Vector3f();
    public Vector3f pos = new Vector3f();
    public Vector3f movement = new Vector3f();
    public Vector3f vel = new Vector3f();
    public Inventory inv = new Inventory();
    public Vector3f selectedBlock = new Vector3f();
    public Vector3f prevSelectedBlock = new Vector3f();
    public AABB playerAABB = new AABB(0, 0, 0,0,0, 0);

    public boolean creative = true;
    public boolean bobbingDir = true;
    public float bobbing = 0f;
    public float dynamicSpeedOld = 0;
    public float dynamicSpeed = 0;
    public float jumpStrength = 0.6f;
    public float scale = 1;
    public float baseEyeHeight = 0.86f*scale;
    public float eyeHeight = baseEyeHeight;
    public float baseHeight = eyeHeight+(0.04f*scale);
    public float height = baseHeight;
    public float bobbingScale = (height*-0.05f)/scale;
    public float width = 0.42f*scale;
    public float baseSpeed = Math.max(0.22f, 0.22f*scale);
    public float speed = baseSpeed;
    public float sprintSpeed = 1.75f;
    public boolean flying = true, forward = false, backward = false, leftward = false, rightward = false, upward = false, downward = false, sprinting = false, superSprinting = false, crouching = false, crawling = false;
    public static Entity entityOn = null;
    public static Vector3f prevEntityOnPos = new Vector3f();

    public static final Random playerRand = new Random();
    public final Source breakingSource;

    public Player() {breakingSource = new Source(pos, 1, 1, 0, 1);}
    public static Path plrPath = Path.of(Main.mainFolder + "player.data");
    public void create() throws IOException {
        inputHandler.init();
        inv.init();

        if (Files.exists(plrPath)) {
            int[] plrData = Utils.flipIntArray(Utils.byteArrayToIntArray(new FileInputStream(plrPath.toFile()).readAllBytes()));
            int i = 0;
            pos.set(new Vector3f(plrData[i++]/1000f, plrData[i++]/1000f, plrData[i++]/1000f));
            float[] camMatrix = new float[16];
            for (int cI = 0; cI < 16; cI++) {
                camMatrix[cI] = plrData[i++]/1000f;
            }
            camera.setViewMatrix(camMatrix);
            camera.pitch.set(plrData[i++]/1000f, plrData[i++]/1000f, plrData[i++]/1000f, plrData[i++]/1000f);
            movement.set(plrData[i++]/1000f, plrData[i++]/1000f, plrData[i++]/1000f);
            vel.set(plrData[i++]/1000f, plrData[i++]/1000f, plrData[i++]/1000f);
            creative = plrData[i++] != 0;
            flying = plrData[i++] != 0;
        } else {
            Main.player.pos.set(1024, 195, 0);
        }
        if (Files.exists(Inventory.invPath)) {
            Main.player.inv.load();
        } else {
            Main.player.inv.init();
        }
    }
    public void save() throws IOException {
        FileOutputStream out = new FileOutputStream(plrPath.toFile());
        float[] cam = new float[16];
        camera.getViewMatrixWithoutPitch().get(cam);
        int[] data = new int[31];
        int i = 0;
        data[i++] = (int)(pos.x()*1000);
        data[i++] = (int)(pos.y()*1000);
        data[i++] = (int)(pos.z()*1000);
        for (int cI = 0; cI < 16; cI++) {
            data[i++] = (int)(cam[cI]*1000);
        }
        data[i++] = (int)(camera.pitch.x()*1000);
        data[i++] = (int)(camera.pitch.y()*1000);
        data[i++] = (int)(camera.pitch.z()*1000);
        data[i++] = (int)(camera.pitch.w()*1000);
        data[i++] = (int)(movement.x()*1000);
        data[i++] = (int)(movement.y()*1000);
        data[i++] = (int)(movement.z()*1000);
        data[i++] = (int)(vel.x()*1000);
        data[i++] = (int)(vel.y()*1000);
        data[i++] = (int)(vel.z()*1000);
        data[i++] = Main.player.creative ? 1 : 0;
        data[i++] = Main.player.flying ? 1 : 0;
        out.write(Utils.intArrayToByteArray(data));
        out.close();
        inv.save();
    }

    public Vector3f oldCamTranslation = new Vector3f();
    public void tick() {
        oldCamTranslation.set(getCameraTranslation());
        movementTick();
        doSounds();
    }

    public Vector2i blockOn = new Vector2i();
    public boolean onSolid = true;
    public float friction = 0.75f;
    public void movementTick() {
        bobbingScale = (height*-0.05f)/scale;
        Vector3f forwardDir = camera.getForwardWithoutPitch();
        Vector3f rightDir = camera.getRightWithoutPitch();
        Vector3f newMovement = new Vector3f();
        if (forward) {newMovement.add(forwardDir);}
        if (backward) {newMovement.sub(forwardDir);}
        if (rightward) {newMovement.add(rightDir);}
        if (leftward) {newMovement.sub(rightDir);}
        if (flying) {
            if (upward) {newMovement.add(0, 1, 0);}
            if (downward) {newMovement.add(0, -1, 0);}
        }
        if (newMovement.length() > 0) {newMovement.normalize();}
        newMovement.mul(speed*(downward?0.65f:1.f));
        newMovement.mul(sprinting ? ((flying ? 2 : 1) * sprintSpeed) : 1.f);
        newMovement.mul(superSprinting ? 10.f : 1.f);
        boolean inBounds = World.inBounds(1, (int) pos.x(), (int) pos.y(), (int) pos.z());
        Vector2i blockIn = inBounds ? World.getBlock(pos.x(), pos.y(), pos.z()) : new Vector2i(0);
        AABB footAABB = new AABB(playerAABB.xMin, playerAABB.xMax, playerAABB.yMin-0.075f, playerAABB.yMax, playerAABB.zMin, playerAABB.zMax);
        blockOn.set(inBounds ? PhysicsHelper.getAnyBlock(footAABB) : new Vector2i(0));
        boolean prevOnSolid = onSolid;
        onSolid = BlockTypes.blockTypes[blockOn.x()].blockProperties.isCollidable;
        if (!onSolid) {
            entityOn = getAnyEntity(footAABB);
            if (entityOn != null) {
                onSolid = true;
            }
        }
        if (onSolid) {
            if (!prevOnSolid) { //when landing from a fall
                bobbing = bobbingScale;
                bobbingDir = false;
            } else {
                float factor = (float) (0.0002f*timeAccum);
                float actualSpeed = Utils.getInterpolatedFloat(dynamicSpeedOld, dynamicSpeed)*0.02f;
                float bobbingInc = actualSpeed;//(float) (actualSpeed*(height*factor*1.2f)*timeMul);//((float) (factor*(1.5f+playerRand.nextFloat())))));
                if (bobbingDir) {
                    bobbing += bobbingInc;
                    if (bobbing >= 0) {
                        bobbing = 0;
                        bobbingDir = false;
                    }
                } else {
                    bobbing -= bobbingInc;
                    if (bobbing <= bobbingScale) {
                        bobbing = bobbingScale;
                        bobbingDir = true;
                        stepFx();
                    }
                }
            }
        }
        float modifiedGrav = World.worldType.gravity();
        friction = 0.99f; //1-airFriction=maxFriction
        if (!flying) {
            if (blockIn.x() == BlockTypes.WATER.id) {
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
            } else if (onSolid) {
                friction *= 0.75f;
            }
        } else {
            friction *= 0.5f;
        }
        movement.mul(friction);
        movement.set(Utils.furthestFromZero(newMovement.x(), movement.x()), Utils.furthestFromZero(newMovement.y(), movement.y()), Utils.furthestFromZero(newMovement.z(), movement.z()));
        vel.mul(friction);
        if (!flying) {
            if (!onSolid) {
                vel.y -= modifiedGrav;
            } else {
                if (vel.y < 0) {
                    vel.y = 0;
                }
                if (upward) {
                    vel.y = Math.max(jumpStrength, vel.y());
                    bobbing = bobbingScale;
                    bobbingDir = false;
                    stepFx();
                }
            }
        }
        vel.max(new Vector3f(-3)).min((new Vector3f(3)));
        Vector3f entityMoveFactor = new Vector3f();
        if (entityOn != null) {
            Vector3f entityOnPos = new Vector3f();
            entityOn.matrix.getTranslation(entityOnPos);
            if (prevEntityOnPos != null) {
                entityMoveFactor.set(new Vector3f(entityOnPos).sub(prevEntityOnPos));
            }
            prevEntityOnPos = entityOnPos;
        } else {
            prevEntityOnPos = null;
        }
        playerAABB.set(
                pos.x()-width, pos.x()+width,
                pos.y()-height, pos.y()+height,
                pos.z()-width, pos.z()+width);
        Vector3f totalVel = new Vector3f(movement).add(vel).add(entityMoveFactor);
        ArrayList<AABB> aabbs = new ArrayList<>();
        for (Entity entity : World.entities) {
            aabbs.add(entity.aabb);
        }
        if (sprinting && !flying) {
            PhysicsHelper.moveWithStepping(playerAABB, totalVel, aabbs);
        } else {
            PhysicsHelper.move(playerAABB, totalVel, aabbs);
        }
        if (totalVel.x() == 0) {movement.x = 0; vel.x = 0;}
        if (totalVel.y() == 0) {movement.y = 0; vel.y = 0;}
        if (totalVel.z() == 0) {movement.z = 0; vel.z = 0;}
        prevPos.set(pos);
        pos.set(playerAABB.xMin+width, playerAABB.yMin+height, playerAABB.zMin+width);
        dynamicSpeedOld = dynamicSpeed;
        dynamicSpeed = Math.clamp((movement.length()-0.1f)*4, 0, 1);
    }
    public void stepFx() {
        BlockSFX stepSFX = BlockTypes.blockTypes[blockOn.x()].blockProperties.blockSFX;
        if (stepSFX != null && stepSFX.stepIds.length > 0) {
            Source stepSource = new Source(pos, stepSFX.stepGain + ((stepSFX.stepGain * playerRand.nextFloat()) / 3), stepSFX.stepPitch + ((stepSFX.stepPitch * playerRand.nextFloat()) / 3), 0, 0);
            AudioController.disposableSources.add(stepSource);
            stepSource.setVel(new Vector3f(vel).add(movement));
            stepSource.play((stepSFX.stepIds[stepSFX.stepIds.length == 1 ? 0 : playerRand.nextInt(stepSFX.stepIds.length - 1)]), true);
        }
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
        camera.rotate((float) -Math.toRadians(pitch), (float) -Math.toRadians(yaw));
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
