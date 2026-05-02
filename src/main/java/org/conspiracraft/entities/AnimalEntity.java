package org.conspiracraft.entities;

import org.conspiracraft.Main;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.physics.AABB;
import org.conspiracraft.physics.PhysicsHelper;
import org.conspiracraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class AnimalEntity extends Entity {
    public AnimalEntity(EntityType type, Matrix4f matrix, float scaleOffset) {
        super(type, matrix, scaleOffset);
    }

    @Override
    public void tick() {
        matrix.getTranslation(prevPos);
        Vector3f scale = new Vector3f();
        matrix.getScale(scale);
        Vector3f halfScale = new Vector3f(scale).div(2);
        Vector3f pos = new Vector3f(aabb.xMin+halfScale.x(), aabb.yMin+halfScale.y(), aabb.zMin+halfScale.z());
        boolean inBounds = World.inBounds(1, (int) pos.x(), (int) pos.y(), (int) pos.z());
        AABB footAABB = new AABB(aabb.xMin, aabb.xMax, aabb.yMin - 0.075f, aabb.yMin, aabb.zMin, aabb.zMax);
        Vector2i blockOn = inBounds ? PhysicsHelper.getAnyBlock(footAABB) : new Vector2i(0);
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
        PhysicsHelper.moveWithStepping(aabb, vel, new ArrayList<>(List.of(Main.player.playerAABB)));
        matrix.setTranslation(aabb.xMin, aabb.yMin, aabb.zMin+scale.z());
    }
}
