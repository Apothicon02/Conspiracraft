package org.conspiracraft.entities;

import org.conspiracraft.physics.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Entity {
    public EntityType type;
    public Matrix4f matrix;
    public AABB aabb;
    public Vector3f vel;
    public Entity(EntityType type, Matrix4f matrix, float scaleOffset) {
        this.type = type;
        this.matrix = matrix;
        float scale = this.type.size+scaleOffset;
        this.matrix.scale(scale);
        Vector3f pos = new Vector3f();
        matrix.getTranslation(pos);
        float halfScale = scale/2;
        aabb = new AABB(pos.x()-halfScale, pos.x()+halfScale, pos.y()-halfScale, pos.y()+halfScale, pos.z()-halfScale, pos.z()+halfScale);
        vel = new Vector3f();
    }
}
