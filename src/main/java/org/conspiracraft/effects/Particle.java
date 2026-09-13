package org.conspiracraft.effects;

import org.conspiracraft.Main;
import org.conspiracraft.blocks.Materials;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.physics.AABB;
import org.conspiracraft.physics.PhysicsHelper;
import org.conspiracraft.world.World;
import org.joml.*;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

public class Particle extends Effect {
    public int randomTicksAlive = 0;
    public final Vector3d vel = new Vector3d();
    public final static Vector3f up = new Vector3f(0, 1, 0);
    public AABB aabb;
    public Particle(Vector3d pos, Matrix4f matrix) {
        super(pos, matrix);
        Vector3f scale = new Vector3f();
        matrix.getScale(scale);
        this.texSize.set((int) Math.max(1, Math.max(scale.x(), Math.max(scale.y(), scale.z()))*2*Materials.materialWidth));
        scale.div(2);
        aabb = new AABB(pos.x()-scale.x(), pos.x()+scale.x(), pos.y()-scale.y(), pos.y()+scale.y(), pos.z()-scale.z(), pos.z()+scale.z());
    }

    @Override
    public boolean tick() {
        if (Math.random() < 0.1f) {
            randomTicksAlive++;
            if (randomTicksAlive >= 20) {
                return true;
            }
        }
        prevPos.set(pos);
        AABB footAABB = new AABB(aabb.xMin, aabb.xMax, aabb.yMin - 0.075f, aabb.yMin, aabb.zMin, aabb.zMax);
        Vector2i blockOn = PhysicsHelper.getAnyBlock(footAABB).block();
        boolean onSolid = BlockTypes.blockTypes[blockOn.x()].blockProperties.isCollidable;
        float friction = 0.99f; //1-airFriction=maxFriction
        if (onSolid) {
            friction *= 0.75f;
        }
        vel.mul(friction);
        float modifiedGrav = World.worldType.gravity()/3;
        if (!onSolid) {
            vel.y -= modifiedGrav;
        }
        PhysicsHelper.move(aabb, vel, new ArrayList<>(List.of(Main.player.playerAABB)));
        Vector3f scale = new Vector3f();
        matrix.getScale(scale);
        Vector3f halfScale = new Vector3f(scale).div(2);
        pos.set(aabb.xMin+halfScale.x(), aabb.yMin+halfScale.y(), aabb.zMin+halfScale.z());
        Vector3f dir = new Vector3f(vel).normalize().negate();
        matrix.getScale(scale);
        matrix.identity().lookAlong(dir, up).invert().scale(scale);
        return false;
    }
}
