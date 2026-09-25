package org.conspiracraft.entities;

import org.conspiracraft.Main;
import org.conspiracraft.blocks.Material;
import org.conspiracraft.blocks.Materials;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.effects.Particle;
import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.physics.AABB;
import org.conspiracraft.physics.PhysicsHelper;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.world.World;
import org.joml.*;

import java.lang.Math;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.conspiracraft.graphics.Renderer.*;
import static org.conspiracraft.graphics.Renderer.warpOffset;
import static org.conspiracraft.world.World.*;
import static org.conspiracraft.world.World.size;

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

    public void addParticle(Vector3d particlePos, Vector2i blockOn) {
        Particle particle = new Particle(new Vector3d(particlePos), new Matrix4f().scale(0.075f + (float) (0.1f * Math.random())));
        particle.vel.set((float) (Math.random() - 0.5f) / 4, (float) (Math.random()) / 15, (float) (Math.random() - 0.5f) / 4);
        particle.tex = Textures.materials;
        Material[] mats = BlockTypes.blockTypes[blockOn.x()].materialsArr;
        int id = mats[(int) (Math.random()*mats.length)].id();
        particle.texOffset.set((id* Materials.materialWidth)%Textures.materials.width, Materials.materialHeight*(id/(Textures.materials.width/Materials.materialWidth)));
        effects.addLast(particle);
    }

    public void draw() {
        pushUBO.updateTex(Textures.entities);
        pushUBO.updateAtlasOffset(type.atlasOffset);
        pushUBO.updateSize(new Vector2i(EntityTypes.entityTexWidth));
        Matrix4f interpolatedMatrix = new Matrix4f(matrix);
        Vector3d interpolatedPos = Utils.getInterpolatedVec(prevPos, pos);
        interpolatedMatrix.setTranslation((float) (interpolatedPos.x()-warpOffset.x()), (float) (interpolatedPos.y()-warpOffset.y()), (float) (interpolatedPos.z()-warpOffset.z()));
        drawCube(interpolatedMatrix, new Vector4f(0.95f, 0.95f, 0.95f, 1.f));
    }
}
