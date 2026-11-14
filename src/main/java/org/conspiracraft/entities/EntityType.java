package org.conspiracraft.entities;

import org.joml.Vector2i;
import org.joml.Vector3f;

public class EntityType {
    public int id;
    public String name;
    public Vector2i atlasOffset = null;
    public float size = 1.f;
    public Vector3f scale = new Vector3f(1);
    public EntityType() {}
    public EntityType(String name) {this.id = EntityTypes.entityTypeMap.size(); this.name = name;}
    public EntityType atlasOffset(int x, int y) {
        atlasOffset = new Vector2i(x, y);
        return this;
    }
    public EntityType setScale(float x, float y, float z) {
        scale.set(x, y, z);
        return this;
    }
    public Vector3f getScale() {
        return new Vector3f(scale);
    }
}
