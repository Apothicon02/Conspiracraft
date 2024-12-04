package org.conspiracraft.game;

import org.conspiracraft.engine.Camera;
import org.conspiracraft.game.blocks.Block;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class Player {
    private final Camera camera = new Camera();
    public Vector3i blockPos;
    public Vector3f pos;

    public Player(Vector3f newPos) {
        setPos(newPos);
    }

    public void tick() {
        Block belowBlock = World.getBlock(new Vector3i(blockPos.x, (int) (pos.y-0.125f), blockPos.z));
        if (belowBlock == null || !BlockTypes.blockTypeMap.get(belowBlock.blockTypeId).isCollidable) {
            move(0, -0.125f, 0, false);
        }
    }

    public Matrix4f getCameraMatrix() {
        Vector3f camOffset = new Vector3f();
        Matrix4f camMatrix = new Matrix4f(camera.getViewMatrix());
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