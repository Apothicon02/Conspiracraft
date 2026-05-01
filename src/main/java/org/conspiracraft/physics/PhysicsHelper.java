package org.conspiracraft.physics;

import org.conspiracraft.Main;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.graphics.Renderer;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.utils.Vector3b;
import org.conspiracraft.world.World;
import org.joml.*;

import java.lang.Math;
import java.util.ArrayList;

import static org.conspiracraft.Main.player;
import static org.conspiracraft.utils.Utils.sign;

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

            int axis = (int) (mask.x() * 0 + mask.y() * 1 + mask.z() * 2);
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
        Vector3f velAttempt = new Vector3f(vel);
        move(objAABB, vel);
        if (vel.x() != velAttempt.x() || vel.z() != velAttempt.z()) { //if obstructed horizontally
            vel.set(velAttempt);
            move(objAABB, new Vector3f(0, 1.f, 0));
            move(objAABB, vel);
            move(objAABB, new Vector3f(0, -1.f, 0));
        }
    }

    public static void move(AABB objAABB, Vector3f vel) {
        AABB regionAABB = new AABB(
                objAABB.xMin-1, objAABB.xMax+1,
                objAABB.yMin-1, objAABB.yMax+1,
                objAABB.zMin-1, objAABB.zMax+1);
        if (vel.x() < 0) {regionAABB.xMin+=vel.x();} else {regionAABB.xMax+=vel.x();}
        if (vel.y() < 0) {regionAABB.yMin+=vel.y();} else {regionAABB.yMax+=vel.y();}
        if (vel.z() < 0) {regionAABB.zMin+=vel.z();} else {regionAABB.zMax+=vel.z();}
        ArrayList<AABB> voxelAABBs = new ArrayList<>();
        for (float x = Math.max(0, regionAABB.xMin); x < Math.min(World.size-1, regionAABB.xMax); x+=1) {
            for (float y = Math.max(0, regionAABB.yMin); y < Math.min(World.height-1, regionAABB.yMax); y+=1) {
                for (float z = Math.max(0, regionAABB.zMin); z < Math.min(World.size-1, regionAABB.zMax); z+=1) {
                    Vector2i blockIn = World.getBlock(x, y, z);
                    if (BlockTypes.blockTypeMap.get(blockIn.x()).blockProperties.isSolid) {
                        voxelAABBs.add(new AABB((float) Math.floor(x), (float) Math.floor(x+1), (float) Math.floor(y), (float) Math.floor(y+1),(float) Math.floor(z), (float) Math.floor(z+1)));
                    }
                }
            }
        }

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
        if (moveVec.x() != vel.x()) {vel.x = 0;}
        if (moveVec.y() != vel.y()) {vel.y = 0;}
        if (moveVec.z() != vel.z()) {vel.z = 0;} // || Math.abs(objAABB.zMin-ogObjAABB.zMin) < 0.01f
    }
}
