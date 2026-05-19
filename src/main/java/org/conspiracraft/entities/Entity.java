package org.conspiracraft.entities;

import org.conspiracraft.Main;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.ItemTypes;
import org.conspiracraft.physics.AABB;
import org.conspiracraft.physics.PhysicsHelper;
import org.conspiracraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class Entity {
    public static int dataLength = 20; //excludes this int
    public EntityType type;
    public Matrix4f matrix;
    public Vector3f vel;

    public AABB aabb;
    public Entity(EntityType type, Matrix4f matrix, float scaleOffset) {
        this.type = type;
        this.matrix = matrix;
        if (scaleOffset > Float.MIN_VALUE) {
            this.matrix.scale(this.type.size + scaleOffset);
        }
        Vector3f scale = new Vector3f();
        matrix.getScale(scale);
        Vector3f pos = new Vector3f();
        matrix.getTranslation(pos);
        scale.div(2);
        aabb = new AABB(pos.x()-scale.x(), pos.x()+scale.x(), pos.y()-scale.y(), pos.y()+scale.y(), pos.z()-scale.z(), pos.z()+scale.z());
        vel = new Vector3f();
    }

    public static Entity load(IntBuffer data) {
        EntityType entityType = EntityTypes.entityTypeMap.get(data.get());
        float[] matrixData = new float[16];
        for (int cI = 0; cI < 16; cI++) {
            matrixData[cI] = data.get()/1000f;
        }
        Matrix4f newMatrix = new Matrix4f().set(matrixData);
        Entity entity = new Entity(entityType, newMatrix, Float.MIN_VALUE);
        entity.vel.set(data.get(), data.get(), data.get()).div(1000);
        return entity;
    }
    public int[] getData() {
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

    public Vector3f prevPos = new Vector3f();
    public void tick() {
        matrix.getTranslation(prevPos);
        Vector3f scale = new Vector3f();
        matrix.getScale(scale);
        matrix.identity();
        Vector3f halfScale = new Vector3f(scale).div(2);
        Vector3f pos = new Vector3f(aabb.xMin+halfScale.x(), aabb.yMin+halfScale.y(), aabb.zMin+halfScale.z());
        boolean inBounds = World.inBounds(1, (int) pos.x(), (int) pos.y(), (int) pos.z());
        AABB footAABB = new AABB(aabb.xMin, aabb.xMax, aabb.yMin - 0.075f, aabb.yMin, aabb.zMin, aabb.zMax);
        Vector2i blockOn = inBounds ? PhysicsHelper.getAnyBlock(footAABB) : new Vector2i(0);
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
        matrix.setTranslation(aabb.xMin+scale.x(), aabb.yMin+scale.y(), aabb.zMin+scale.z());
    }
}
