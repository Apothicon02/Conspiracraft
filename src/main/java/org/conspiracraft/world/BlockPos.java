package org.conspiracraft.world;

import org.joml.Vector3i;

public class BlockPos extends Vector3i {
    public BlockPos(int x, int y, int z) {
        this.set(x, y, z);
    }
    public BlockPos() {
        this.set(0);
    }

    public BlockPos immutable() {
        return new BlockPos(this.x, this.y, this.z);
    }

    public BlockPos above(int i) {
        return new BlockPos(x(), y()+i, z());
    }
    public BlockPos below(int i) {
        return new BlockPos(x(), y()-i, z());
    }
    public BlockPos north(int i) {
        return new BlockPos(x()+i, y(), z());
    }
    public BlockPos south(int i) {
        return new BlockPos(x()-i, y(), z());
    }
    public BlockPos east(int i) {
        return new BlockPos(x(), y(), z()+i);
    }
    public BlockPos west(int i) {
        return new BlockPos(x(), y(), z()-i);
    }
    public BlockPos above() {
        return above(1);
    }
    public BlockPos below() {
        return below(1);
    }
    public BlockPos north() {
        return north(1);
    }
    public BlockPos south() {
        return south(1);
    }
    public BlockPos east() {
        return east(1);
    }
    public BlockPos west() {
        return west(1);
    }
}
