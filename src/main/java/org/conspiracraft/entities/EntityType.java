package org.conspiracraft.entities;

import org.joml.Vector2i;

public class EntityType {
    public String name;
    public Vector2i atlasOffset = null;
    public EntityType(String name) {
        this.name = name;
    }
    public EntityType atlasOffset(int x, int y) {
        atlasOffset = new Vector2i(x, y);
        return this;
    }
}
