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

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class Entity {
    public static int dataLength = 20; //excludes this int
    public EntityType type;
    public Matrix4f matrix;
    public Vector3d pos;
    public Vector3d vel;

    public AABB aabb;
    public Entity(EntityType type, Vector3d pos, Matrix4f matrix, float scaleOffset) {
        this.type = type;
        this.matrix = matrix;
        if (scaleOffset != Float.MAX_VALUE) {
            this.matrix.scale(this.type.size + scaleOffset);
        }
        this.pos = pos;
        Vector3f scale = new Vector3f();
        matrix.getScale(scale);
        scale.div(2);
        aabb = new AABB(pos.x()-scale.x(), pos.x()+scale.x(), pos.y()-scale.y(), pos.y()+scale.y(), pos.z()-scale.z(), pos.z()+scale.z());
        vel = new Vector3d();
    }

    public boolean playerCollidesWith() {return true;}
    public static Entity load(IntBuffer data) {
        EntityType entityType = EntityTypes.entityTypeMap.get(data.get());
        float[] matrixData = new float[16];
        for (int cI = 0; cI < 16; cI++) {
            matrixData[cI] = data.get()/1000f;
        }
        Matrix4f newMatrix = new Matrix4f().set(matrixData);
        Entity entity = new Entity(entityType, new Vector3d(), newMatrix, Float.MAX_VALUE); //needs to load pos
        entity.vel.set(data.get(), data.get(), data.get()).div(1000);
        return entity;
    }
    public int[] getData() { //needs to save pos
        int[] data = new int[dataLength+1];
        int offset = 0;
        data[offset++] = dataLength;
        data[offset++] = EntityTypes.getId(type);
        float[] matrixData = new float[16];
        matrix.get(matrixData);
        for (int cI = 0; cI < 16; cI++) {
            data[offset++] = (int)(matrixData[cI]*1000);
        }
        data[offset++] = (int) (vel.x()*1000);
        data[offset++] = (int) (vel.y()*1000);
        data[offset++] = (int) (vel.z()*1000);
        return data;
    }

    public Vector3d prevPos = new Vector3d();
    public boolean tick() {
        prevPos.set(pos);
        Vector3f scale = new Vector3f();
        matrix.getScale(scale);
        matrix.identity();
        Vector3f halfScale = new Vector3f(scale).div(2);
        AABB footAABB = new AABB(aabb.xMin, aabb.xMax, aabb.yMin - 0.075f, aabb.yMin, aabb.zMin, aabb.zMax);
        Vector2i blockOn = PhysicsHelper.getAnyBlock(footAABB).block();
        boolean onSolid = BlockTypes.blockTypes[blockOn.x()].blockProperties.isCollidable;
        float friction = 0.99f; //1-airFriction=maxFriction
        if (onSolid) {
            friction *= 0.75f;
        }
        vel.mul(friction);
        float modifiedGrav = World.worldType.gravity();
        if (!onSolid) {
            vel.y -= modifiedGrav;
        }
        PhysicsHelper.move(aabb, vel, new ArrayList<>(List.of(Main.player.playerAABB)));
        pos.set(aabb.xMin+halfScale.x(), aabb.yMin+halfScale.y(), aabb.zMin+halfScale.z());
        return false;
    }
}
