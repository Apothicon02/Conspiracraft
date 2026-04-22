package org.conspiracraft;

import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.world.World;
import org.joml.Vector2i;
import org.joml.Vector3f;

public class PhysicsHelper {
    public static float voxelSize = 0.125f;
    public static boolean colliding(float startX, float startY, float startZ, Vector3f size) {
        for (float x = startX-size.x(); x <= startX+size.x(); x+=voxelSize) {
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
    public static float detail = 0.1f;
    public static Vector3f moveTo(Vector3f from, Vector3f size, Vector3f to) {
        Float x = null;
        Float y = null;
        Float z = null;
        Vector3f dif = new Vector3f(to).sub(from);
        Vector3f inc = new Vector3f(dif).normalize().mul(detail);
        int steps = (int)Math.ceil(dif.length() / detail);
        Vector3f prevRayPos = new Vector3f(from);
        Vector3f rayPos = new Vector3f(from);
        for (int i = 0; i < steps; i++) {
            rayPos.set(x != null ? x : from.x() + (inc.x() * i), y != null ? y : from.y() + (inc.y() * i), z != null ? z : from.z() + (inc.z() * i));
            if (colliding(rayPos.x(), rayPos.y(), rayPos.z(), size)) {
                if (x == null) {
                    if (colliding(rayPos.x, prevRayPos.y, prevRayPos.z, size)) {
                        rayPos.x = prevRayPos.x();
                        x = prevRayPos.x;
                    }
                }
                if (y == null) {
                    if (colliding(prevRayPos.x, rayPos.y, prevRayPos.z, size)) {
                        rayPos.y = prevRayPos.y();
                        y = prevRayPos.y;
                    }
                }
                if (z == null) {
                    if (colliding(prevRayPos.x, prevRayPos.y, rayPos.z, size)) {
                        rayPos.z = prevRayPos.z();
                        z = prevRayPos.z;
                    }
                }
                if (x != null && y != null && z != null) {
                    break;
                }
            }
            prevRayPos.set(rayPos);
        }
        return prevRayPos;
    }
}
