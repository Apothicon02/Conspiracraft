package org.conspiracraft.physics;

import org.conspiracraft.Main;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.entities.Entity;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.world.World;
import org.joml.*;

import java.lang.Math;
import java.util.ArrayList;

import static org.conspiracraft.utils.Utils.sign;
import static org.conspiracraft.world.World.entities;

public class PhysicsHelper {
    public static float voxelSize = 0.125f;
    public static Vector2i getAnyBlock(AABB aabb) {
        Vector2i block = new Vector2i();
        for (float x = aabb.xMin; x <= aabb.xMax; x += voxelSize) {
            for (float y = aabb.yMin; y <= aabb.yMax; y += voxelSize) {
                for (float z = aabb.zMin; z <= aabb.zMax; z += voxelSize) {
                    Vector2i blockIn = World.getBlock(x, y, z);
                    if (BlockTypes.blockTypeMap.get(blockIn.x()).blockProperties.isCollidable) {
                        AABB blockAABB = new AABB((float) Math.floor(x), (float) Math.floor(x+1), (float) Math.floor(y), (float) Math.floor(y+1),(float) Math.floor(z), (float) Math.floor(z+1));
                        if (blockAABB.intersects(aabb)) {
                            return blockIn;
                        }
                    } else if (blockIn.x() > block.x()) {
                        block.set(blockIn); //return non-collidable block if none are collidable
                    }
                }
            }
        }
        return block;
    }
    public static Entity getAnyEntity(AABB aabb) {
        for (Entity entity : entities) {
            if (aabb.intersects(entity.aabb)) {
                return entity;
            }
        }
        return null;
    }
    public static DDAResult dda(Vector3f pos, Vector3f ogDir, float maxDist) {
        Vector3i ddaPos = new Vector3i((int) pos.x(), (int) pos.y(), (int) pos.z());
        Vector3i prevDDAPos = new Vector3i(ddaPos);
        Vector3f dir = Utils.unzeroVec(ogDir.normalize());
        Vector3f raySign = sign(dir);
        Vector3f dist = new Vector3f(1.0f).div(new Vector3f(dir).absolute());
        Vector3f sideDist = new Vector3f();
        sideDist.x = (raySign.x > 0 ? (ddaPos.x + 1 - pos.x) : (pos.x - ddaPos.x)) * dist.x;
        sideDist.y = (raySign.y > 0 ? (ddaPos.y + 1 - pos.y) : (pos.y - ddaPos.y)) * dist.y;
        sideDist.z = (raySign.z > 0 ? (ddaPos.z + 1 - pos.z) : (pos.z - ddaPos.z)) * dist.z;
        Vector3f mask = new Vector3f(0);

        float travelled = 0;
        for (int i = 0; i < maxDist*2; i++) {
            if (!World.inBounds(ddaPos.x(), ddaPos.y(), ddaPos.z())) {return new DDAResult(prevDDAPos, ddaPos, false);}
            if (!BlockTypes.blockTypeMap.get(World.getBlock(ddaPos).x()).blockProperties.isFluidReplaceable) {return new DDAResult(prevDDAPos, ddaPos, true);}
            mask.set(Utils.step(sideDist, Math.min(Math.min(sideDist.x(), sideDist.y()), sideDist.z()) + 0.000000001f));
            prevDDAPos.set(ddaPos);

            int axis = (int) (mask.y() + (mask.z() * 2));
            travelled = sideDist.get(axis);
            sideDist.setComponent(axis, travelled + dist.get(axis));
            ddaPos.setComponent(axis, ddaPos.get(axis) + (int) raySign.get(axis));

            if (travelled > maxDist) {
                return new DDAResult(prevDDAPos, ddaPos, false);
            }
        }
        return null;
    }
    public static void moveWithStepping(AABB objAABB, Vector3f vel) {
        moveWithStepping(objAABB, vel, new ArrayList<>());
    }
    public static void moveWithStepping(AABB objAABB, Vector3f vel, ArrayList<AABB> aabbs) {
        Vector3f ogVel = new Vector3f(vel);
        move(objAABB, vel, new ArrayList<>(aabbs));
        if (vel.x() != ogVel.x() || vel.z() != ogVel.z()) { //if obstructed horizontally
            vel.set(ogVel);
            move(objAABB, new Vector3f(0, 1.f, 0), new ArrayList<>(aabbs));
            move(objAABB, vel, new ArrayList<>(aabbs));
            move(objAABB, new Vector3f(0, -1.f, 0), new ArrayList<>(aabbs));
        }
    }

    public static void move(AABB objAABB, Vector3f vel) {
        move(objAABB, vel, new ArrayList<>());
    }
    public static void move(AABB objAABB, Vector3f vel, ArrayList<AABB> aabbs) {
        AABB regionAABB = new AABB(
                objAABB.xMin-1, objAABB.xMax+1,
                objAABB.yMin-1, objAABB.yMax+1,
                objAABB.zMin-1, objAABB.zMax+1);
        if (vel.x() < 0) {regionAABB.xMin+=vel.x();} else {regionAABB.xMax+=vel.x();}
        if (vel.y() < 0) {regionAABB.yMin+=vel.y();} else {regionAABB.yMax+=vel.y();}
        if (vel.z() < 0) {regionAABB.zMin+=vel.z();} else {regionAABB.zMax+=vel.z();}
        for (float x = Math.max(0, regionAABB.xMin); x < Math.min(World.size-1, regionAABB.xMax); x+=1) {
            for (float y = Math.max(0, regionAABB.yMin); y < Math.min(World.height-1, regionAABB.yMax); y+=1) {
                for (float z = Math.max(0, regionAABB.zMin); z < Math.min(World.size-1, regionAABB.zMax); z+=1) {
                    Vector2i blockIn = World.getBlock(x, y, z);
                    if (BlockTypes.blockTypeMap.get(blockIn.x()).blockProperties.isSolid) {
                        aabbs.add(new AABB((float) Math.floor(x), (float) Math.floor(x+1), (float) Math.floor(y), (float) Math.floor(y+1),(float) Math.floor(z), (float) Math.floor(z+1)));
                    }
                }
            }
        }

        Vector3f moveVec = new Vector3f(vel);
        for (AABB aabb : aabbs) {
            moveVec.x = objAABB.clipX(aabb, moveVec.x());
        }
        objAABB.move(moveVec.x(), 0, 0);
        for (AABB aabb : aabbs) {
            moveVec.y = objAABB.clipY(aabb, moveVec.y());
        }
        objAABB.move(0, moveVec.y(), 0);
        for (AABB aabb : aabbs) {
            moveVec.z = objAABB.clipZ(aabb, moveVec.z());
        }
        objAABB.move(0, 0, moveVec.z());
        if (moveVec.x() != vel.x()) {vel.x = 0;}
        if (moveVec.y() != vel.y()) {vel.y = 0;}
        if (moveVec.z() != vel.z()) {vel.z = 0;} // || Math.abs(objAABB.zMin-ogObjAABB.zMin) < 0.01f
    }
}
