package org.conspiracraft.game;

import org.conspiracraft.Main;
import org.conspiracraft.engine.Camera;
import org.conspiracraft.game.blocks.Block;
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

    public boolean solid(float x, float y, float z) {
        Block block = World.getBlock(x, y, z);
        if (block != null) {
            int typeId = block.typeId();
            if (BlockTypes.blockTypeMap.get(typeId).isCollidable) {
                if (Renderer.collisionData[(9984 * ((typeId * 8) + (int) ((x - Math.floor(x)) * 8))) + (block.subtypeId() * 64) + ((Math.abs(((int) ((y - Math.floor(y)) * 8)) - 8) - 1) * 8) + (int) ((z - Math.floor(z)) * 8)]) {
                    return true;
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
            movement.add(pos.x-translatedPos.x, pos.z-translatedPos.z);
        }
        if (rightward || leftward) {
            Vector3f translatedPos = new Matrix4f(getCameraMatrixWithoutPitch()).translate(speed*(sprint ? 2 : 1)*(rightward ? -1 : 1), 0, 0).getTranslation(new Vector3f());
            movement.add(pos.x-translatedPos.x, pos.z-translatedPos.z);
        }

        float mX = vel.x+movement.x;
        float mY = vel.y;
        float mZ = vel.z+movement.y;
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

//        float x = vel.x+movement.x;
//        float y = vel.y;
//        float z = vel.z+movement.y;
//        if (y < 0) {
//            Vector3f offsetPos = Main.raycast(new Matrix4f().setTranslation(pos).rotate(new Quaternionf(0.7071068, 0, 0, 0.7071068)), true, (int) (y*-8)+8, false);
//            float dist = y;
//            if (offsetPos != null) {
//                dist = (pos.y - offsetPos.y)*-1;
//            }
//            if (y < dist) {
//                y = dist;
//                vel.set(vel.x, 0, vel.z);
//            }
//        } else if (y > 0) {
//            Vector3f offsetPos = Main.raycast(new Matrix4f().setTranslation(pos).translate(0, height, 0).rotate(new Quaternionf(0.7071068, 0, 0, -0.7071068)), true, (int) (y*8)+8, false);
//            float dist = y;
//            if (offsetPos != null) {
//                dist = Math.abs((pos.y+height) - offsetPos.y);
//            }
//            if (y > dist) {
//                y = dist;
//                vel.set(vel.x, 0, vel.z);
//            }
//        }
//        if (x < 0) {
//            Vector3f offsetPos = Main.raycast(new Matrix4f().setTranslation(pos).translate(0, 0.3f, 0).rotate(new Quaternionf(0, 0.7071068, 0, 0.7071068)), true, (int) (x*-8)+8, false);
//            float dist = x;
//            if (offsetPos != null) {
//                dist = (pos.x - offsetPos.x)*-1;
//            }
//            if (x < dist) {
//                x = dist;
//                vel.set(0, vel.y, vel.z);
//            }
//        } else if (x > 0) {
//            Vector3f offsetPos = Main.raycast(new Matrix4f().setTranslation(pos).translate(0, 0.3f, 0).rotate(new Quaternionf(0, 0.7071068, 0, -0.7071068)), true, (int) (x*8)+8, false);
//            float dist = x;
//            if (offsetPos != null) {
//                dist = Math.abs(pos.x - offsetPos.x);
//            }
//            if (x > dist) {
//                x = dist;
//                vel.set(0, vel.y, vel.z);
//            }
//        }
//        if (z < 0) {
//            Vector3f offsetPos = Main.raycast(new Matrix4f().setTranslation(pos).translate(0, 0.3f, 0).rotate(new Quaternionf(0, 1, 0, 0)), true, (int) (z*-8)+8, false);
//            float dist = z;
//            if (offsetPos != null) {
//                dist = (pos.z - offsetPos.z)*-1;
//            }
//            if (z < dist) {
//                z = dist;
//                vel.set(vel.x, vel.y, 0);
//            }
//        } else if (z > 0) {
//            Vector3f offsetPos = Main.raycast(new Matrix4f().setTranslation(pos).translate(0, 0.3f, 0).rotate(new Quaternionf(0, 0, 0, 1)), true, (int) (z*8)+8, false);
//            float dist = z;
//            if (offsetPos != null) {
//                dist = Math.abs(pos.z - offsetPos.z);
//            }
//            if (z > dist) {
//                z = dist;
//                vel.set(vel.x, vel.y, 0);
//            }
//        }
//
//        move(x, y, z, false);
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