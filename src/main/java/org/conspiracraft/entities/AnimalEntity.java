package org.conspiracraft.entities;

import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.physics.PhysicsHelper;
import org.conspiracraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;

public class AnimalEntity extends Entity {
    public AnimalEntity(EntityType type, Matrix4f matrix, float scaleOffset) {
        super(type, matrix, scaleOffset);
    }

    @Override
    public void tick() {
        Vector3f scale = new Vector3f();
        matrix.getScale(scale);
        Vector3f halfScale = new Vector3f(scale).div(2);
        Vector3f pos = new Vector3f(aabb.xMin+halfScale.x(), aabb.yMin+halfScale.y(), aabb.zMin+halfScale.z());
        boolean inBounds = World.inBounds(1, (int) pos.x(), (int) pos.y(), (int) pos.z());
        Vector2i blockOn = inBounds ?
                PhysicsHelper.getAnyBlock(pos.x(), (pos.y() - halfScale.y()) - 0.075f, pos.z(), new Vector3f(halfScale.x(), 0.075f, halfScale.z())) :
                new Vector2i(0);
        boolean onSolid = BlockTypes.blockTypeMap.get(blockOn.x()).blockProperties.isCollidable;
        float friction = 0.99f; //1-airFriction=maxFriction
        if (onSolid) {
            friction *= 0.75f;
            if (Math.random() < 0.05f) {
                vel.add(new Vector3f((float) (Math.random()-0.5f), 0, (float) (Math.random()-0.5f)).div(2));
            }
        }
        vel.mul(friction);
        float modifiedGrav = World.worldType.gravity();
        if (!onSolid) {
            vel.y -= modifiedGrav;
        }
        PhysicsHelper.moveWithStepping(aabb, vel);
        matrix.setTranslation(aabb.xMin, aabb.yMin, aabb.zMin+scale.z());
    }
}
