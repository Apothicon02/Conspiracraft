package org.conspiracraft.physics;

public class AABB {
    public double xMin, xMax, yMin, yMax, zMin, zMax;
    public AABB(double xMin, double xMax, double yMin, double yMax, double zMin, double zMax) {
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
    public AABB set(double xMin, double xMax, double yMin, double yMax, double zMin, double zMax) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
        this.zMin = zMin;
        this.zMax = zMax;
        return this;
    }
    public AABB expand(double x, double y, double z) {
        if (x > 0) {xMax += x;} else {xMin += x;}
        if (y > 0) {yMax += y;} else {yMin += y;}
        if (z > 0) {zMax += z;} else {zMax += z;}
        return this;
    }
    public AABB grow(double xyz) {
        return grow(xyz, xyz, xyz);
    }
    public AABB grow(double x, double y, double z) {
        this.xMin -= x;
        this.xMax += x;
        this.yMin -= y;
        this.yMax += y;
        this.zMin -= z;
        this.zMax += z;
        return this;
    }
    public AABB move(double x, double y, double z) {
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
    public double clipX(AABB against, double deltaX) {
        if(intersectsY(against) && intersectsZ(against)) {
            if (xMin < against.xMax && xMax > against.xMin) { //push out of AABB if embedded in.
                double up = against.xMax - xMin, down = xMax - against.xMin;
                return (up < down) ? up : -down;
            }
            if(deltaX > 0 && xMax <= against.xMin) {
                double clip = against.xMin - xMax;
                if (deltaX > clip) {
                    deltaX = clip;
                }
            }
            if (deltaX < 0 && xMin >= against.xMax) {
                double clip = against.xMax - xMin;
                if (deltaX < clip) {
                    deltaX = clip;
                }
            }
            return deltaX;
        }
        return deltaX;
    }
    public double clipY(AABB against, double deltaY) {
        if (intersectsX(against) && intersectsZ(against)) {
            if (yMin < against.yMax && yMax > against.yMin) { //push out of AABB if embedded in.
                double up = against.yMax - yMin, down = yMax - against.yMin;
                return (up < down) ? up : -down;
            }
            if (deltaY > 0 && yMax <= against.yMin) {
                double clip = against.yMin - yMax;
                if (deltaY > clip) {
                    deltaY = clip;
                }
            }
            if (deltaY < 0 && yMin >= against.yMax) {
                double clip = against.yMax - yMin;
                if (deltaY < clip) {
                    deltaY = clip;
                }
            }
            return deltaY;
        }
        return deltaY;
    }
    public double clipZ(AABB against, double deltaZ) {
        if (intersectsX(against) && intersectsY(against)) {
            if (zMin < against.zMax && zMax > against.zMin) { //push out of AABB if embedded in.
                double up = against.zMax - zMin, down = zMax - against.zMin;
                return (up < down) ? up : -down;
            }
            if (deltaZ > 0 && zMax <= against.zMin) {
                double clip = against.zMin - zMax;
                if (deltaZ > clip) {
                    deltaZ = clip;
                }
            }
            if (deltaZ < 0 && zMin >= against.zMax) {
                double clip = against.zMax - zMin;
                if (deltaZ < clip) {
                    deltaZ = clip;
                }
            }
            return deltaZ;
        }
        return deltaZ;
    }
}
