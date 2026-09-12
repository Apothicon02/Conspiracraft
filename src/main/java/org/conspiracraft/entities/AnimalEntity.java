package org.conspiracraft.entities;

import org.conspiracraft.Main;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.physics.AABB;
import org.conspiracraft.physics.PhysicsHelper;
import org.conspiracraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class AnimalEntity extends Entity {
    public AnimalEntity(EntityType type, Vector3d pos, Matrix4f matrix, float scaleOffset) {
        super(type, pos, matrix, scaleOffset);
    }

    public final static Vector3f up = new Vector3f(0, 1, 0);
    @Override
    public boolean tick() {
        prevPos.set(pos);
        Vector3f scale = new Vector3f();
        matrix.getScale(scale);
        Vector3f halfScale = new Vector3f(scale).div(2);
        AABB footAABB = new AABB(aabb.xMin, aabb.xMax, aabb.yMin - 0.075f, aabb.yMin, aabb.zMin, aabb.zMax);
        Vector2i blockOn = PhysicsHelper.getAnyBlock(footAABB).block();
        boolean onSolid = BlockTypes.blockTypes[blockOn.x()].blockProperties.isCollidable;
        float friction = 0.99f; //1-airFriction=maxFriction
        if (onSolid) {
            friction *= 0.75f;
            if (Math.random() < 0.05f) {
                vel.add(new Vector3f((float) (Math.random()-0.5f), 0, (float) (Math.random()-0.5f)));
            }
        }
        vel.mul(friction);
        float modifiedGrav = World.worldType.gravity();
        if (!onSolid) {
            vel.y -= modifiedGrav;
        }
        PhysicsHelper.moveWithStepping(aabb, vel, new ArrayList<>(List.of(Main.player.playerAABB)));
        Vector3f dir = new Vector3f(vel).normalize().negate();
        matrix.identity().lookAlong(dir, up).invert().scale(scale);
        pos.set(aabb.xMin+halfScale.x(), aabb.yMin+halfScale.y(), aabb.zMin+halfScale.z());
        return false;
    }
}
