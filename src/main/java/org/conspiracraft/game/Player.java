package org.conspiracraft.game;

import org.conspiracraft.Main;
import org.conspiracraft.engine.Camera;
import org.conspiracraft.engine.Utils;
import org.conspiracraft.game.audio.AudioController;
import org.conspiracraft.game.audio.Source;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.rendering.Renderer;
import org.conspiracraft.game.world.World;
import org.joml.*;

import java.lang.Math;

public class Player {
    private final Camera camera = new Camera();
    public final Source jumpSource;
    public final Source passthroughSource;
    public final Source stepSource;
    public final Source swimSource;
    public final Source splashSource;
    public final Source submergeSource;
    public final Source musicSource;
    public static float eyeHeight = 1.625f;
    public static float height = eyeHeight+0.175f;
    public static float width = 0.4f;
    public Vector3i blockPos;
    public Vector3f pos;
    public Vector3f oldPos;
    public Vector3f vel = new Vector3f(0f);
    public Vector3f movement = new Vector3f(0f);
    public float bounciness = 0.66f;
    public float friction = 0.75f;
    public float grav = 0.05f;
    public float speed = 0.15f;
    public float jumpStrength = 0.33f;
    public long lastJump = 1000;
    public long jump = 0;
    public boolean sprint = false;
    public boolean superSprint = false;
    public boolean forward = false;
    public boolean backward = false;
    public boolean rightward = false;
    public boolean leftward = false;
    public boolean upward = false;
    public boolean downward = false;
    public boolean flying = true;
    public static Vector3i selectedBlock = new Vector3i(0);

    public Player(Vector3f newPos) {
        jumpSource = new Source(newPos, 1, 1, 0);
        passthroughSource = new Source(newPos, 1, 1, 0);
        stepSource = new Source(newPos, -0.25f, 0.66f, 0); //make quieter for some reason gain aint workin on this sound
        swimSource = new Source(newPos, -1, 1, 0);
        splashSource = new Source(newPos, 1, 1, 0);
        submergeSource = new Source(newPos, 1, 1, 0);
        musicSource = new Source(newPos, 0.15f, 1, 0.25f);
        setPos(newPos);
        oldPos = newPos;
        musicSource.play(AudioController.buffers.get(9));
    }

    public int[] getData() {
        float[] cam = new float[16];
        camera.getViewMatrixWithoutPitch().get(cam);
        int[] data = new int[30];
        for (int i = 0; i < 16; i++) {
            data[i] = (int)(cam[i]*1000);
        }
        data[16] = (int)(camera.pitch.x*1000);
        data[17] = (int)(camera.pitch.y*1000);
        data[18] = (int)(camera.pitch.z*1000);
        data[19] = (int)(camera.pitch.w*1000);
        float[] posVelData = {pos.x, pos.y, pos.z, vel.x, vel.y, vel.z};
        for (int i = 20; i < 26; i++) {
            data[i] = (int)(posVelData[i-20]*1000);
        }
        data[26] = flying ? 1 : 0;
        data[27] = selectedBlock.x;
        data[28] = selectedBlock.y;
        data[29] = selectedBlock.z;
        return data;
    }

    public boolean solid(float x, float y, float z, boolean recordFriction, boolean recordBounciness) {
        Vector2i block = World.getBlock(x, y, z);
        if (block != null) {
            int typeId = block.x;
            if (BlockTypes.blockTypeMap.get(typeId).isCollidable) {
                int cornerData = World.getCorner((int) x, (int) y, (int) z);
                int cornerIndex = (y < (int)(y)+0.5 ? 0 : 4) + (z < (int)(z)+0.5 ? 0 : 2) + (x < (int)(x)+0.5 ? 0 : 1);
                int temp = cornerData;
                temp &= (~(1 << (cornerIndex - 1)));
                if (temp == cornerData) {
                    if (Renderer.collisionData[(9984 * ((typeId * 8) + (int) ((x - Math.floor(x)) * 8))) + (block.y() * 64) + ((Math.abs(((int) ((y - Math.floor(y)) * 8)) - 8) - 1) * 8) + (int) ((z - Math.floor(z)) * 8)]) {
                        if (recordFriction) {
                            if (typeId == 7) { //kyanite
                                friction = Math.min(friction, 0.95f);
                            } else if (typeId == 11 || typeId == 12 || typeId == 13) { //glass
                                friction = Math.min(friction, 0.85f);
                            } else if (typeId == 15 || typeId == 16) { //wood
                                friction = Math.min(friction, 0.5f);
                            } else if (BlockTypes.blockTypeMap.get(typeId).isCollidable) {
                                friction = Math.min(friction, 0.75f);
                            }
                        }
                        if (recordBounciness) {
                            if (typeId == 7) { //kyanite
                                bounciness = Math.min(bounciness, -2f);
                            } else if (typeId == 11 || typeId == 12 || typeId == 13) { //glass
                                bounciness = Math.min(bounciness, -0.33f);
                            }
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public boolean solid(float x, float y, float z, float w, float h, boolean recordFriction, boolean recordBounciness) {
        boolean returnValue = false;
        for (float newX = x-w; newX <= x+w; newX+=0.125f) {
            for (float newY = y; newY <= y+h; newY+=0.125f) {
                for (float newZ = z-w; newZ <= z+w; newZ+=0.125f) {
                    if (solid(newX, newY, newZ, recordFriction, recordBounciness)) {
                        if (recordFriction) {
                            returnValue = true;
                        } else {
                            return true;
                        }
                    }
                }
            }
        }
        return returnValue;
    }

    public void tick() {
        if (!Renderer.worldChanged) {
            friction = 1f;
            boolean onGround = solid(pos.x, pos.y-0.125f, pos.z, width, 0.125f, true, false);
            Vector2i blockIn = World.getBlock(blockPos.x, blockPos.y, blockPos.z);
            Vector2i blockBreathing = World.getBlock(blockPos.x, blockPos.y+eyeHeight, blockPos.z);
            float modifiedSpeed = speed;
            float modifiedGrav = grav;
            if (blockIn.x == 1) { //water
                modifiedGrav *= 0.2f;
                friction *= 0.9f;
                modifiedSpeed *= 0.5f;
            } else if (blockIn.x == 4 || blockIn.x == 5) { //grass
                friction *= 0.9f;
                modifiedSpeed *= 0.9f;
            } else if (blockIn.x == 17 || blockIn.x == 21) { //leaves
                if (blockIn.y == 0) {
                    friction *= 0.5f;
                    modifiedSpeed *= 0.5f;
                } else {
                    friction *= 0.9f;
                    modifiedSpeed *= 0.9f;
                }
            }
            friction = Math.min(0.99f, friction); //1-airFriction=maxFriction
            if (flying) {
                modifiedSpeed = speed;
                modifiedGrav = grav;
                friction = 0.75f;
            }

            Vector3f newMovement = new Vector3f(0f);
            boolean canMove = (flying || onGround || blockIn.x == 1);
            if (forward || backward) {
                Vector3f translatedPos = new Matrix4f(getCameraMatrixWithoutPitch()).translate(0, 0, (modifiedSpeed * (canMove ? 1 : 0.1f)) * (sprint || superSprint ? (backward ? (superSprint && sprint ? 100 : (superSprint ? 10 : 1)) : (flying ? (superSprint ? 100 : 10) : 2)) : 1) * (forward ? -1 : 1)).getTranslation(new Vector3f());
                newMovement.add(pos.x - translatedPos.x,0, pos.z - translatedPos.z);
            }
            if (rightward || leftward) {
                Vector3f translatedPos = new Matrix4f(getCameraMatrixWithoutPitch()).translate((modifiedSpeed * (canMove ? 1 : 0.1f)) * (sprint || superSprint ? (flying ? (superSprint ? 100 : 10) : 2) : 1) * (rightward ? -1 : 1), 0, 0).getTranslation(new Vector3f());
                newMovement.add(pos.x - translatedPos.x, 0, pos.z - translatedPos.z);
            }
            if (upward || downward) {
                if (flying) {
                    Vector3f translatedPos = new Matrix4f(getCameraMatrixWithoutPitch()).translate(0, speed * (downward ? (-10 * (sprint || superSprint ? (superSprint ? -5 : 0) : 1f)) : -12 * (sprint || superSprint ? (superSprint ? 20 : 2) : 1)), 0).getTranslation(new Vector3f());
                    newMovement.add(0, pos.y - translatedPos.y, 0);
                } else if (blockIn.x == 1 && blockBreathing.x == 1) {
                    Vector3f translatedPos = new Matrix4f(getCameraMatrixWithoutPitch()).translate(0, speed * (downward ? -10 : -12), 0).getTranslation(new Vector3f());
                    newMovement.add(0, pos.y - translatedPos.y, 0);
                }
            }
            movement = new Vector3f(Utils.furthestFromZero(newMovement.x, movement.x*friction), Utils.furthestFromZero(newMovement.y, movement.y*friction), Utils.furthestFromZero(newMovement.z, movement.z*friction));
            vel = new Vector3f(vel.x*friction, vel.y*friction, vel.z*friction);

            if (!flying && vel.y >= -1+modifiedGrav && !onGround) {
                vel.set(vel.x, vel.y-modifiedGrav, vel.z);
            }

            if (Main.timeMS-jump < 100 && !flying) { //prevent jumping when space bar was pressed longer than 0.1s ago or when flying
                if ((onGround || (blockIn.x == 1 && solid(pos.x, pos.y, pos.z, width*1.125f, height, false, false))) && blockBreathing.x != 1) {
                    jump = 1000;
                    lastJump = Main.timeMS;
                    vel.y = Math.max(vel.y, jumpStrength);
                    jumpSource.setPos(pos);
                    jumpSource.setVel(new Vector3f(vel.x+movement.x, Math.max(vel.y+movement.y, jumpStrength), vel.z+movement.z));
                    jumpSource.play(AudioController.buffers.get(0));
                }
            }

            float mX = vel.x + movement.x;
            float mY = vel.y + movement.y;
            float mZ = vel.z + movement.z;
            Vector3f hitPos = pos;
            Vector3f destPos = new Vector3f(mX+pos.x, mY+pos.y, mZ+pos.z);
            Float maxX = null;
            Float maxY = null;
            Float maxZ = null;
            float detail = 1+Math.max(Math.abs(mX*256f), Math.max(Math.abs(mY*256f), Math.abs(mZ*256f)));
            for (int i = 0; i <= detail; i++) {
                Vector3f rayPos = new Vector3f(maxX != null ? maxX : pos.x+((destPos.x-pos.x)*(i/detail)), maxY != null ? maxY : pos.y+((destPos.y-pos.y)*(i/detail)), maxZ != null ? maxZ : pos.z+((destPos.z-pos.z)*(i/detail)));
                if (solid(rayPos.x, rayPos.y, rayPos.z, width, height, false, false)) {
                    if (maxX == null) {
                        bounciness = 0.66f;
                        if (solid(rayPos.x, hitPos.y, hitPos.z, width, height, false, !flying)) {
                            bounciness = Math.max(bounciness, -0.66f);
                            maxX = hitPos.x;
                            vel.x *= bounciness;
                            movement.x *= bounciness;
                        }
                    }
                    if (maxY == null) {
                        bounciness = 0.66f;
                        if (solid(hitPos.x, rayPos.y, hitPos.z, width, height, false, !flying)) {
                            bounciness = (upward && mY <= 0.f) ? bounciness : Math.max(bounciness, -0.66f); //dont limit bounciness if jumping and moving downwards
                            maxY = hitPos.y;
                            vel.y *= bounciness;
                            movement.y *= bounciness;
                        }
                    }
                    if (maxZ == null) {
                        bounciness = 0.66f;
                        if (solid(hitPos.x, hitPos.y, rayPos.z, width, height, false, !flying)) {
                            bounciness = Math.max(bounciness, -0.66f);
                            maxZ = hitPos.z;
                            vel.z *= bounciness;
                            movement.z *= bounciness;
                        }
                    }
                    if (maxX != null && maxY != null && maxZ != null) {
                        break;
                    }
                } else {
                    hitPos = rayPos;
                }
            }
            setPos(hitPos);
        }
    }

    public void setCameraMatrix(float[] matrix) {
        camera.setViewMatrix(matrix);
    }
    public void setCameraPitch(Quaternionf pitch) {
        camera.setPitch(pitch);
    }
    public Matrix4f getCameraMatrix() {
        Vector3f camOffset = new Vector3f();
        Matrix4f camMatrix = new Matrix4f(camera.getViewMatrix());
        camMatrix.getTranslation(camOffset);
        Vector3f interpolatedPos = Utils.getInterpolatedVec(oldPos, pos);
        return camMatrix.setTranslation(interpolatedPos.x+camOffset.x, interpolatedPos.y+camOffset.y, interpolatedPos.z+camOffset.z);
    }
    public Matrix4f getCameraMatrixWithoutPitch() {
        Vector3f camOffset = new Vector3f();
        Matrix4f camMatrix = new Matrix4f(camera.getViewMatrixWithoutPitch());
        camMatrix.getTranslation(camOffset);
        return camMatrix.setTranslation(pos.x+camOffset.x, pos.y+camOffset.y, pos.z+camOffset.z);
    }

    public void setPos(Vector3f newPos) {
        oldPos = pos;
        pos = newPos;
        blockPos = new Vector3i((int)newPos.x, (int)newPos.y, (int)newPos.z);
        AudioController.setListenerData(newPos, vel);
        musicSource.setPos(newPos);
        Vector3f combinedVel = new Vector3f(vel.x+movement.x, vel.y+movement.y, vel.z+movement.z);
        musicSource.setVel(combinedVel);
        if (Math.abs(combinedVel.x) > 0.01 || Math.abs(combinedVel.y) > 0.01 || Math.abs(combinedVel.z) > 0.01) {
            int block = World.getBlock(newPos.x, newPos.y, newPos.z).x;
            if (block == 1) {
                if (swimSource.soundPlaying == -1) {
                    submergeSource.play(AudioController.buffers.get(8));
                }
                swimSource.play(AudioController.buffers.get(7));
            } else {
                if (swimSource.soundPlaying != -1) {
                    swimSource.stop();
                    splashSource.play(AudioController.buffers.get(8));
                }
                if (block == 17 || block == 4 || block == 5) {
                    passthroughSource.play(AudioController.buffers.get((int) (1 + (Math.random() * 2))));
                }
            }
            int blockOn = World.getBlock(newPos.x, newPos.y-0.1f, newPos.z).x;
            if (blockOn == 2) {
                stepSource.play(AudioController.buffers.get((int) (1+(Math.random()*2))));
            } else if (blockOn == 3) {
                stepSource.play(AudioController.buffers.get((int) (4+(Math.random()*2))));
            }
        }

        splashSource.setPos(newPos);
        splashSource.setVel(combinedVel);
        submergeSource.setPos(newPos);
        submergeSource.setVel(combinedVel);
        swimSource.setPos(newPos);
        swimSource.setVel(combinedVel);
        passthroughSource.setPos(newPos);
        passthroughSource.setVel(combinedVel);
        stepSource.setPos(newPos);
        stepSource.setVel(combinedVel);
    }

    public void move(float x, float y, float z, boolean countRotation) {
        if (countRotation) {
            Vector3f returnPos = new Vector3f();
            Matrix4f tempMatrix = new Matrix4f(camera.getViewMatrixWithoutPitch()).setTranslation(0, 0, 0).translate(x, y, z);
            tempMatrix.getTranslation(returnPos);
            setPos(new Vector3f(pos.x + returnPos.x, pos.y + returnPos.y, pos.z + returnPos.z));
        } else {
            setPos(new Vector3f(pos.x + x, pos.y + y, pos.z + z));
        }
    }

    public void rotate(float pitch, float yaw) {
        camera.rotate(pitch, yaw);
    }
}