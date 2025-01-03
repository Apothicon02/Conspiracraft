package org.conspiracraft.game;

import org.conspiracraft.Main;
import org.conspiracraft.engine.Camera;
import org.conspiracraft.game.blocks.Block;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.world.World;
import org.joml.*;

import java.lang.Math;

public class Player {
    private final Camera camera = new Camera();
    public static float eyeHeight = 1.625f;
    public static float height = eyeHeight+0.175f;
    public Vector3i blockPos;
    public Vector3f pos;
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

    public Player(Vector3f newPos) {
        setPos(newPos);
    }

    public void tick(long time) {
        if (vel.y >= -1+grav) {
            vel.set(vel.x, vel.y-grav, vel.z);
        }

        if (time-jump < 100) { //prevent jumping when space bar was pressed longer than 0.1s ago
            if (Main.raycast(new Matrix4f().setTranslation(pos).rotate(new Quaternionf(0.7071068, 0, 0, 0.7071068)), true, 2, false) != null) {
                jump = 1000;
                lastJump = time;
                vel.set(vel.x, Math.max(vel.y, 0.4f), vel.z);
            }
        }

        Vector2f movement = new Vector2f(0f);
        if (forward || backward) {
            Vector3f translatedPos = new Matrix4f(getCameraMatrixWithoutPitch()).translate(0, 0, speed*(sprint ? (backward ? 1 : 2) : 1)*(forward ? -1 : 1)).getTranslation(new Vector3f());
            movement.set(pos.x-translatedPos.x, pos.z-translatedPos.z);
        }
        if (rightward || leftward) {
            Vector3f translatedPos = new Matrix4f(getCameraMatrixWithoutPitch()).translate(speed*(sprint ? 2 : 1)*(rightward ? -1 : 1), 0, 0).getTranslation(new Vector3f());
            movement.add(pos.x-translatedPos.x, pos.z-translatedPos.z);
        }

        float x = vel.x+movement.x;
        float y = vel.y;
        float z = vel.z+movement.y;
        if (y < 0) {
            Vector3f offsetPos = Main.raycast(new Matrix4f().setTranslation(pos).rotate(new Quaternionf(0.7071068, 0, 0, 0.7071068)), true, (int) (y*-8)+8, false);
            float dist = y;
            if (offsetPos != null) {
                dist = (pos.y - offsetPos.y)*-1;
            }
            if (y < dist) {
                y = dist;
                vel.set(vel.x, 0, vel.z);
            }
        } else if (y > 0) {
            Vector3f offsetPos = Main.raycast(new Matrix4f().setTranslation(pos).translate(0, height, 0).rotate(new Quaternionf(0.7071068, 0, 0, -0.7071068)), true, (int) (y*8)+8, false);
            float dist = y;
            if (offsetPos != null) {
                dist = Math.abs((pos.y+height) - offsetPos.y);
            }
            if (y > dist) {
                y = dist;
                vel.set(vel.x, 0, vel.z);
            }
        }
        if (x < 0) {
            Vector3f offsetPos = Main.raycast(new Matrix4f().setTranslation(pos).translate(0, 0.3f, 0).rotate(new Quaternionf(0, 0.7071068, 0, 0.7071068)), true, (int) (x*-8)+8, false);
            float dist = x;
            if (offsetPos != null) {
                dist = (pos.x - offsetPos.x)*-1;
            }
            if (x < dist) {
                x = dist;
                vel.set(0, vel.y, vel.z);
            }
        } else if (x > 0) {
            Vector3f offsetPos = Main.raycast(new Matrix4f().setTranslation(pos).translate(0, 0.3f, 0).rotate(new Quaternionf(0, 0.7071068, 0, -0.7071068)), true, (int) (x*8)+8, false);
            float dist = x;
            if (offsetPos != null) {
                dist = Math.abs(pos.x - offsetPos.x);
            }
            if (x > dist) {
                x = dist;
                vel.set(0, vel.y, vel.z);
            }
        }
        if (z < 0) {
            Vector3f offsetPos = Main.raycast(new Matrix4f().setTranslation(pos).translate(0, 0.3f, 0).rotate(new Quaternionf(0, 1, 0, 0)), true, (int) (z*-8)+8, false);
            float dist = z;
            if (offsetPos != null) {
                dist = (pos.z - offsetPos.z)*-1;
            }
            if (z < dist) {
                z = dist;
                vel.set(vel.x, vel.y, 0);
            }
        } else if (z > 0) {
            Vector3f offsetPos = Main.raycast(new Matrix4f().setTranslation(pos).translate(0, 0.3f, 0).rotate(new Quaternionf(0, 0, 0, 1)), true, (int) (z*8)+8, false);
            float dist = z;
            if (offsetPos != null) {
                dist = Math.abs(pos.z - offsetPos.z);
            }
            if (z > dist) {
                z = dist;
                vel.set(vel.x, vel.y, 0);
            }
        }


        move(x, y, z, false);
        decayVel(0.01f);
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

    public Matrix4f getCameraMatrix() {
        Vector3f camOffset = new Vector3f();
        Matrix4f camMatrix = new Matrix4f(camera.getViewMatrix());
        camMatrix.getTranslation(camOffset);
        return camMatrix.setTranslation(pos.x+camOffset.x, pos.y+camOffset.y, pos.z+camOffset.z);
    }
    public Matrix4f getCameraMatrixWithoutPitch() {
        Vector3f camOffset = new Vector3f();
        Matrix4f camMatrix = new Matrix4f(camera.getViewMatrixWithoutPitch());
        camMatrix.getTranslation(camOffset);
        return camMatrix.setTranslation(pos.x+camOffset.x, pos.y+camOffset.y, pos.z+camOffset.z);
    }

    public void setPos(Vector3f newPos) {
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