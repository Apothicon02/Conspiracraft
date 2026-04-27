package org.conspiracraft.physics;

import org.conspiracraft.Main;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.world.World;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.ArrayList;

public class PhysicsHelper {
    public static float voxelSize = 0.125f;
    public static boolean colliding(float startX, float startY, float startZ, Vector3f size) {
        for (float x = startX-size.x(); x <= startX+size.x(); x += voxelSize) {
            for (float y = startY-size.y(); y <= startY+size.y(); y += voxelSize) {
                for (float z = startZ - size.z(); z <= startZ+size.z(); z += voxelSize) {
                    Vector2i blockIn = World.getBlock(x, y, z);
                    if (BlockTypes.blockTypeMap.get(blockIn.x()).blockProperties.isSolid) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public static void move(AABB objAABB, Vector3f vel, Vector3f velMutable) {
        AABB regionAABB = new AABB(
                objAABB.xMin-1, objAABB.xMax+1,
                objAABB.yMin-1, objAABB.yMax+1,
                objAABB.zMin-1, objAABB.zMax+1);
        if (vel.x() < 0) {regionAABB.xMin+=vel.x();} else {regionAABB.xMax+=vel.x();}
        if (vel.y() < 0) {regionAABB.yMin+=vel.y();} else {regionAABB.yMax+=vel.y();}
        if (vel.z() < 0) {regionAABB.zMin+=vel.z();} else {regionAABB.zMax+=vel.z();}
        ArrayList<AABB> voxelAABBs = new ArrayList<>();
        for (float x = regionAABB.xMin; x < regionAABB.xMax; x+=1) {
            for (float y = regionAABB.yMin; y < regionAABB.yMax; y+=1) {
                for (float z = regionAABB.zMin; z < regionAABB.zMax; z+=1) {
                    Vector2i blockIn = World.getBlock(x, y, z);
                    if (BlockTypes.blockTypeMap.get(blockIn.x()).blockProperties.isSolid) {
                        voxelAABBs.add(new AABB((float) Math.floor(x), (float) Math.floor(x+1), (float) Math.floor(y), (float) Math.floor(y+1),(float) Math.floor(z), (float) Math.floor(z+1)));
                    }
                }
            }
        }

        AABB ogObjAABB = objAABB.copy();
        Vector3f moveVec = new Vector3f(vel);
        for (AABB aabb : voxelAABBs) {
            moveVec.x = objAABB.clipX(aabb, moveVec.x());
        }
        objAABB.move(moveVec.x(), 0, 0);
        for (AABB aabb : voxelAABBs) {
            moveVec.y = objAABB.clipY(aabb, moveVec.y());
        }
        objAABB.move(0, moveVec.y(), 0);
        for (AABB aabb : voxelAABBs) {
            moveVec.z = objAABB.clipZ(aabb, moveVec.z());
        }
        objAABB.move(0, 0, moveVec.z());
        if (moveVec.x() != vel.x() || Math.abs(objAABB.xMin-ogObjAABB.xMin) < 0.01f) {velMutable.x = 0; Main.player.movement.x = 0;}
        if (moveVec.y() != vel.y() || Math.abs(objAABB.yMin-ogObjAABB.yMin) < 0.01f) {velMutable.y = 0; Main.player.movement.y = 0;}
        if (moveVec.z() != vel.z() || Math.abs(objAABB.zMin-ogObjAABB.zMin) < 0.01f) {velMutable.z = 0; Main.player.movement.z = 0;}
    }
}
