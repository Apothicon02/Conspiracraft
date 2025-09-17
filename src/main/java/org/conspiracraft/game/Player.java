package org.conspiracraft.game;

import org.conspiracraft.engine.Camera;
import org.conspiracraft.engine.Utils;
import org.joml.*;

import java.lang.Math;

public class Player {
    private final Camera camera = new Camera();
    public static float scale = 1f;
    public static float eyeHeight = 1.625f*scale;
    public Vector3i blockPos;
    public Vector3f pos;
    public Vector3f oldPos;
    public Vector3f vel = new Vector3f(0f);
    public Vector3f movement = new Vector3f(0f);
    public float friction = 0.75f;
    public float grav = 0f;
    public float speed = Math.max(0.15f, 0.15f*scale);
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
    public static Vector4f voxelColor = new Vector4f(1);

    public Player(Vector3f newPos) {
        setPos(newPos);
        oldPos = newPos;
    }

    public void tick() {
        float modifiedSpeed = speed;
        friction = 0.75f;

        Vector3f newMovement = new Vector3f(0f);
        boolean canMove = true;
        if (forward || backward) {
            Vector3f translatedPos = new Matrix4f(getCameraMatrixWithoutPitch()).translate(0, 0, (modifiedSpeed * (canMove ? 1 : 0.1f)) * (sprint || superSprint ? (backward ? (superSprint && sprint ? 100 : (superSprint ? 10 : 1)) : (superSprint ? 100 : 10)) : 1) * (forward ? -1 : 1)).getTranslation(new Vector3f());
            newMovement.add(pos.x - translatedPos.x,0, pos.z - translatedPos.z);
        }
        if (rightward || leftward) {
            Vector3f translatedPos = new Matrix4f(getCameraMatrixWithoutPitch()).translate((modifiedSpeed * (canMove ? 1 : 0.1f)) * (sprint || superSprint ? (superSprint ? 100 : 10) : 1) * (rightward ? -1 : 1), 0, 0).getTranslation(new Vector3f());
            newMovement.add(pos.x - translatedPos.x, 0, pos.z - translatedPos.z);
        }
        if (upward || downward) {
            Vector3f translatedPos = new Matrix4f(getCameraMatrixWithoutPitch()).translate(0, speed * (downward ? (-10 * (sprint || superSprint ? (superSprint ? -5 : 0) : 1f)) : -12 * (sprint || superSprint ? (superSprint ? 20 : 2) : 1)), 0).getTranslation(new Vector3f());
            newMovement.add(0, pos.y - translatedPos.y, 0);
        }
        movement = new Vector3f(Utils.furthestFromZero(newMovement.x, movement.x*friction), Utils.furthestFromZero(newMovement.y, movement.y*friction), Utils.furthestFromZero(newMovement.z, movement.z*friction));
        vel = new Vector3f(vel.x*friction, vel.y*friction, vel.z*friction);

        vel = new Vector3f(Math.clamp(vel.x, -1, 1), Math.clamp(vel.y, -1, 1), Math.clamp(vel.z, -1, 1));
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
            hitPos = rayPos;
        }
        setPos(hitPos);
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
        blockPos = new Vector3i((int) newPos.x, (int) newPos.y, (int) newPos.z);
    }

    public void rotate(float pitch, float yaw) {
        camera.rotate(pitch, yaw);
    }
}