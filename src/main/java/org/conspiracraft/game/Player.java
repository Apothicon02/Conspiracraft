package org.conspiracraft.game;

import org.conspiracraft.Main;
import org.conspiracraft.engine.Camera;
import org.conspiracraft.engine.Utils;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.rendering.Renderer;
import org.conspiracraft.game.world.World;
import org.joml.*;

import java.lang.Math;

public class Player {
    private final Camera camera = new Camera();
    public static float eyeHeight = 1.625f;
    public static float height = eyeHeight+0.175f;
    public static float width = 0.4f;
    public Vector3i blockPos;
    public Vector3f pos;
    public Vector3f oldPos;
    public Vector3f vel = new Vector3f(0f);
    public float grav = 0.05f;
    public float speed = 0.15f;
    public long lastJump = 1000;
    public long jump = 0;
    public boolean sprint = false;
    public boolean forward = false;
    public boolean backward = false;
    public boolean rightward = false;
    public boolean leftward = false;
    public boolean upward = false;
    public boolean downward = false;
    public boolean flying = true;

    public Player(Vector3f newPos) {
        setPos(newPos);
        oldPos = newPos;
    }

    public int[] getData() {
        float[] cam = new float[16];
        camera.getViewMatrixWithoutPitch().get(cam);
        int[] data = new int[27];
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
        return data;
    }

    public boolean solid(float x, float y, float z) {
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
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public boolean solid(float x, float y, float z, float w, float h) {
        for (float newX = x-w; newX <= x+w; newX+=0.125f) {
            for (float newY = y; newY <= y+h; newY+=0.125f) {
                for (float newZ = z-w; newZ <= z+w; newZ+=0.125f) {
                    if (solid(newX, newY, newZ)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void tick(long time) {
        if (!Renderer.worldChanged) {
            if (!flying && vel.y >= -1+grav) {
                vel.set(vel.x, vel.y-grav, vel.z);
            }

            if (time-jump < 100 && !flying) { //prevent jumping when space bar was pressed longer than 0.1s ago or when flying
                if (Main.raycast(new Matrix4f().setTranslation(pos).rotate(new Quaternionf(0.7071068, 0, 0, 0.7071068)), true, 2, false) != null) {
                    jump = 1000;
                    lastJump = time;
                    vel.set(vel.x, Math.max(vel.y, 0.4f), vel.z);
                }
            }

            Vector2f movement = new Vector2f(0f);
            if (forward || backward) {
                Vector3f translatedPos = new Matrix4f(getCameraMatrixWithoutPitch()).translate(0, 0, speed * (sprint ? (backward ? 1 : (flying ? 10 : 2)) : 1) * (forward ? -1 : 1)).getTranslation(new Vector3f());
                movement.add(pos.x - translatedPos.x, pos.z - translatedPos.z);
            }
            if (rightward || leftward) {
                Vector3f translatedPos = new Matrix4f(getCameraMatrixWithoutPitch()).translate(speed * (sprint ? (flying ? 10 : 2) : 1) * (rightward ? -1 : 1), 0, 0).getTranslation(new Vector3f());
                movement.add(pos.x - translatedPos.x, pos.z - translatedPos.z);
            }

            float mX = vel.x + movement.x;
            float mY = vel.y;
            float mZ = vel.z + movement.y;
            if (flying) {
                if (upward || downward) {
                    Vector3f translatedPos = new Matrix4f(getCameraMatrixWithoutPitch()).translate(0, speed * (downward ? (-10 * (sprint ? 0f : 1f)) : -12 * (sprint ? 2 : 1)), 0).getTranslation(new Vector3f());
                    mY += (pos.y - translatedPos.y);
                }
            }
            Vector3f hitPos = pos;
            Vector3f destPos = new Vector3f(mX+pos.x, mY+pos.y, mZ+pos.z);
            Float maxX = null;
            Float maxY = null;
            Float maxZ = null;
            float detail = 1+Math.max(Math.abs(mX*256f), Math.max(Math.abs(mY*256f), Math.abs(mZ*256f)));
            for (int i = 0; i <= detail; i++) {
                Vector3f rayPos = new Vector3f(maxX != null ? maxX : pos.x+((destPos.x-pos.x)*(i/detail)), maxY != null ? maxY : pos.y+((destPos.y-pos.y)*(i/detail)), maxZ != null ? maxZ : pos.z+((destPos.z-pos.z)*(i/detail)));
                if (solid(rayPos.x, rayPos.y, rayPos.z, width, height)) {
                    if (maxX == null) {
                        if (solid(rayPos.x, hitPos.y, hitPos.z, width, height)) {
                            maxX = hitPos.x;
                        }
                    }
                    if (maxY == null) {
                        if (solid(hitPos.x, rayPos.y, hitPos.z, width, height)) {
                            maxY = hitPos.y;
                        }
                    }
                    if (maxZ == null) {
                        if (solid(hitPos.x, hitPos.y, rayPos.z, width, height)) {
                            maxZ = hitPos.z;
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
            decayVel(0.01f);
        }
    }

    public void decayVel(float factor) {
        float newX = vel.x;
        float newY = vel.y;
        float newZ = vel.z;
        if (newX != 0) {
            if (newX < 0.0f) {
                newX = Math.min(0.0f, newX + factor);
            } else {
                newX = Math.max(0.0f, newX - factor);
            }
        }
        if (newY != 0) {
            if (newY < 0.0f) {
                newY = Math.min(0.0f, newY + factor);
            } else {
                newY = Math.max(0.0f, newY - factor);
            }
        }
        if (newZ != 0) {
            if (newZ < 0.0f) {
                newZ = Math.min(0.0f, newZ + factor);
            } else {
                newZ = Math.max(0.0f, newZ - factor);
            }
        }
        vel.set(newX, newY, newZ);
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