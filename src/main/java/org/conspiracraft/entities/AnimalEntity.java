package org.conspiracraft.entities;

import org.conspiracraft.Main;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.physics.AABB;
import org.conspiracraft.physics.PhysicsHelper;
import org.conspiracraft.utils.Utils;
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
        int blockOn = PhysicsHelper.getAnyBlock(footAABB).block();
        boolean onSolid = BlockTypes.blockTypes[blockOn >> 16].blockProperties.isCollidable;
        float friction = 0.99f; //1-airFriction=maxFriction
        if (onSolid) {
            friction *= 0.75f;
            if (Math.random() < 0.05f) {
                vel.add(new Vector3f((float) Utils.mix(Math.random()-0.5f, -dir.x(), 0.4f), 0, (float) Utils.mix(Math.random()-0.5f, -dir.z(), 0.4f)));
                addParticle(new Vector3d((Math.random()-0.5f)+pos.x(), pos.y(), (Math.random()-0.5f)+pos.z()), blockOn);
                addParticle(new Vector3d((Math.random()-0.5f)+pos.x(), pos.y(), (Math.random()-0.5f)+pos.z()), blockOn);
                addParticle(new Vector3d((Math.random()-0.5f)+pos.x(), pos.y(), (Math.random()-0.5f)+pos.z()), blockOn);
                addParticle(new Vector3d((Math.random()-0.5f)+pos.x(), pos.y(), (Math.random()-0.5f)+pos.z()), blockOn);
            }
        }
        vel.mul(friction);
        float modifiedGrav = World.worldType.gravity();
        if (!onSolid) {
            vel.y -= modifiedGrav;
        }
        PhysicsHelper.moveWithStepping(aabb, vel, new ArrayList<>(List.of(Main.player.playerAABB)));
        Utils.normalize(dir.set(vel)).negate();
        Matrix4f newMatrix = new Matrix4f().lookAlong(dir, up).invert().scale(scale);
        if (newMatrix.isFinite()) {matrix.set(newMatrix);}
        pos.set(aabb.xMin+halfScale.x(), aabb.yMin+halfScale.y(), aabb.zMin+halfScale.z());
        return false;
    }
}
