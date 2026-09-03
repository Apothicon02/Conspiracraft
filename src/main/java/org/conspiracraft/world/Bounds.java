package org.conspiracraft.world;

import org.joml.Vector3i;

public record Bounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
    public boolean in(int x, int y, int z) {
        return x >= minX && x < maxX && y >= minY && y < maxY && z >= minZ && z < maxZ;
    }
    public boolean in(Vector3i pos) {
        return pos.x() >= minX && pos.x() < maxX && pos.y() >= minY && pos.y() < maxY && pos.z() >= minZ && pos.z() < maxZ;
    }
    public boolean out(int x, int y, int z) {
        return x < minX || x >= maxX || y < minY || y >= maxY || z < minZ || z >= maxZ;
    }
    public boolean out(Vector3i pos) {
        return pos.x() < minX || pos.x() >= maxX || pos.y() < minY || pos.y() >= maxY || pos.z() < minZ || pos.z() >= maxZ;
    }
}