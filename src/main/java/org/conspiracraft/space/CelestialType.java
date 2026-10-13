package org.conspiracraft.space;

import org.joml.Vector2i;

public class CelestialType {
    public String name;
    public Vector2i atlasOffset = null;
    public float size = 1.f;
    public CelestialType(String name) {
        this.name = name;
    }
    public CelestialType atlasOffset(int x, int y) {
        atlasOffset = new Vector2i(x, y);
        return this;
    }
}
