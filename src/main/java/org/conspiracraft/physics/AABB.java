package org.conspiracraft.physics;

public class AABB {
    public float xMin, xMax, yMin, yMax, zMin, zMax;
    public AABB(float xMin, float xMax, float yMin, float yMax, float zMin, float zMax) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
        this.zMin = zMin;
        this.zMax = zMax;
    }
    public AABB copy() {
        return new AABB(xMin, xMax, yMin, yMax, zMin, zMax);
    }
    public AABB set(AABB source) {
        this.xMin = source.xMin;
        this.xMax = source.xMax;
        this.yMin = source.yMin;
        this.yMax = source.yMax;
        this.zMin = source.zMin;
        this.zMax = source.zMax;
        return this;
    }
    public AABB set(float xMin, float xMax, float yMin, float yMax, float zMin, float zMax) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
        this.zMin = zMin;
        this.zMax = zMax;
        return this;
    }
    public AABB expand(float x, float y, float z) {
        if (x > 0) {xMax += x;} else {xMin += x;}
        if (y > 0) {yMax += y;} else {yMin += y;}
        if (z > 0) {zMax += z;} else {zMax += z;}
        return this;
    }
    public AABB grow(float xyz) {
        return grow(xyz, xyz, xyz);
    }
    public AABB grow(float x, float y, float z) {
        this.xMin -= x;
        this.xMax += x;
        this.yMin -= y;
        this.yMax += y;
        this.zMin -= z;
        this.zMax += z;
        return this;
    }
    public AABB move(float x, float y, float z) {
        this.xMin += x;
        this.xMax += x;
        this.yMin += y;
        this.yMax += y;
        this.zMin += z;
        this.zMax += z;
        return this;
    }

    public boolean intersectsX(AABB against) {
        return (xMin < against.xMax && xMax > against.xMin);
    }
    public boolean intersectsY(AABB against) {
        return (yMin < against.yMax && yMax > against.yMin);
    }
    public boolean intersectsZ(AABB against) {
        return (zMin < against.zMax && zMax > against.zMin);
    }
    public boolean intersects(AABB against) {
        return intersectsX(against) && intersectsY(against) && intersectsZ(against);
    }
    public float clipX(AABB against, float deltaX) {
        if(intersectsY(against) && intersectsZ(against)) {
            if(deltaX > 0 && xMax <= against.xMin) {
                float clip = against.xMin - xMax;
                if (deltaX > clip) {
                    deltaX = clip;
                }
            }
            if (deltaX < 0 && xMin >= against.xMax) {
                float clip = against.xMax - xMin;
                if (deltaX < clip) {
                    deltaX = clip;
                }
            }
            return deltaX;
        }
        return deltaX;
    }
    public float clipY(AABB against, float deltaY) {
        if (intersectsX(against) && intersectsZ(against)) {
            if (deltaY > 0 && yMax <= against.yMin) {
                float clip = against.yMin - yMax;
                if (deltaY > clip) {
                    deltaY = clip;
                }
            }
            if (deltaY < 0 && yMin >= against.yMax) {
                float clip = against.yMax - yMin;
                if (deltaY < clip) {
                    deltaY = clip;
                }
            }
            return deltaY;
        }
        return deltaY;
    }
    public float clipZ(AABB against, float deltaZ) {
        if (intersectsX(against) && intersectsY(against)) {
            if (deltaZ > 0 && zMax <= against.zMin) {
                float clip = against.zMin - zMax;
                if (deltaZ > clip) {
                    deltaZ = clip;
                }
            }
            if (deltaZ < 0 && zMin >= against.zMax) {
                float clip = against.zMax - zMin;
                if (deltaZ < clip) {
                    deltaZ = clip;
                }
            }
            return deltaZ;
        }
        return deltaZ;
    }
}
