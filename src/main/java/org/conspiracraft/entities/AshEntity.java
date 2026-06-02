package org.conspiracraft.entities;

import org.conspiracraft.Main;
import org.conspiracraft.audio.AudioController;
import org.conspiracraft.audio.Sounds;
import org.conspiracraft.audio.Source;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.physics.AABB;
import org.conspiracraft.physics.PhysicsHelper;
import org.conspiracraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.ArrayList;

public class AshEntity extends Entity {
    public AshEntity(EntityType type, Matrix4f matrix, float scaleOffset) {
        super(type, matrix, scaleOffset);
    }

    @Override
    public boolean playerCollidesWith() {return false;}
    public final static Vector3f up = new Vector3f(0, 1, 0);
    public int randomTicksAlive = 0;
    public boolean everOnSolid = false;
    @Override
    public boolean tick() {
        if (Math.random() < 0.1f) {
            randomTicksAlive++;
            if (randomTicksAlive >= 40) {
                return true;
            }
        }
        matrix.getTranslation(prevPos);
        Vector3f scale = new Vector3f();
        matrix.getScale(scale);
        Vector3f halfScale = new Vector3f(scale).div(2);
        Vector3f pos = new Vector3f(aabb.xMin+halfScale.x(), aabb.yMin+halfScale.y(), aabb.zMin+halfScale.z());
        if (!World.inBounds(1, (int) pos.x(), (int) pos.y(), (int) pos.z())) {return true;}
        float modifiedGrav = World.worldType.gravity();
        vel.y -= modifiedGrav;
        AABB footAABB = new AABB(aabb.xMin, aabb.xMax, aabb.yMin - 0.075f, aabb.yMin, aabb.zMin, aabb.zMax);
        Vector2i blockOn = PhysicsHelper.getAnyBlock(footAABB).block();
        boolean onSolid = BlockTypes.blockTypes[blockOn.x()].blockProperties.isCollidable;
        float friction = 0.99f; //1-airFriction=maxFriction
        if (onSolid) {
            friction *= 0.75f;
            if (!everOnSolid) {
                everOnSolid = true;
                Source source = new Source(matrix.getTranslation(new Vector3f()), 0.3f+(float)(Math.random()*0.2f), 0.7f+(float)(Math.random()*0.3f), 0, 0);
                source.play(Sounds.ASH);
                AudioController.disposableSources.add(source);
            }
        }
        vel.mul(friction);
        PhysicsHelper.move(aabb, vel, new ArrayList<>());
        Vector3f dir = new Vector3f(vel).normalize().negate();
        matrix.identity().lookAlong(dir, up).invert().setTranslation(aabb.xMin+halfScale.x(), aabb.yMin+halfScale.y(), aabb.zMin+halfScale.z()).scale(scale);
        return false;
    }
}
