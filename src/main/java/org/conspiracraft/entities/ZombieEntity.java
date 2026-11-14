package org.conspiracraft.entities;

import org.conspiracraft.Main;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.physics.AABB;
import org.conspiracraft.physics.PhysicsHelper;
import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.world.World;
import org.joml.*;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import static org.conspiracraft.graphics.Renderer.*;

public class ZombieEntity extends Entity {
    public ZombieEntity(EntityType type, Vector3d pos, Matrix4f matrix, float scaleOffset) {
        super(type, pos, matrix, scaleOffset);
    }

    public boolean slumpDir = false;
    public float slump = 0.f;
    public final static Vector3f up = new Vector3f(0, 1, 0);
    @Override
    public boolean tick() {
        baseTick();
        Vector3f scale = new Vector3f();
        matrix.getScale(scale);
        AABB footAABB = new AABB(aabb.xMin, aabb.xMax, aabb.yMin - 0.075f, aabb.yMin, aabb.zMin, aabb.zMax);
        int blockOn = PhysicsHelper.getAnyBlock(footAABB).block();
        boolean onSolid = BlockTypes.blockTypes[blockOn >> 16].blockProperties.isCollidable;
        ArrayList<AABB> aabbs = new ArrayList<>(List.of(Main.player.playerAABB));
        for (Entity entity : World.entities) {
            if (entity.hostile()) {
                aabbs.add(entity.aabb);
                if (footAABB.intersects(entity.aabb)) {onSolid = true;}
            }
        }
        for (Entity entity : World.entitiesComplex) {
            if (entity.hostile()) {
                aabbs.add(entity.aabb);
                if (footAABB.intersects(entity.aabb)) {onSolid = true;}
            }
        }
        float friction = 0.99f; //1-airFriction=maxFriction
        if (onSolid) {
            if (!prevOnSolid) {
                bobbing = bobbingScale;
                bobbingDir = false;
                addParticle(new Vector3d((Math.random()-0.5f)+pos.x(), pos.y(), (Math.random()-0.5f)+pos.z()), blockOn);
                addParticle(new Vector3d((Math.random()-0.5f)+pos.x(), pos.y(), (Math.random()-0.5f)+pos.z()), blockOn);
                addParticle(new Vector3d((Math.random()-0.5f)+pos.x(), pos.y(), (Math.random()-0.5f)+pos.z()), blockOn);
                addParticle(new Vector3d((Math.random()-0.5f)+pos.x(), pos.y(), (Math.random()-0.5f)+pos.z()), blockOn);
                addParticle(new Vector3d((Math.random()-0.5f)+pos.x(), pos.y(), (Math.random()-0.5f)+pos.z()), blockOn);
            } else {
                float actualSpeed = Utils.getInterpolatedFloat(dynamicSpeedOld, dynamicSpeed) * 0.02f;
                if (bobbingDir) {
                    bobbing += actualSpeed;
                    if (bobbing >= 0) {
                        bobbing = 0;
                        bobbingDir = false;
                    }
                } else {
                    bobbing -= actualSpeed;
                    if (bobbing <= bobbingScale) {
                        bobbing = bobbingScale;
                        bobbingDir = true;
                        stepFx(rand, blockOn);
                        addParticle(new Vector3d((Math.random()-0.5f)+pos.x(), pos.y(), (Math.random()-0.5f)+pos.z()), blockOn);
                        addParticle(new Vector3d((Math.random()-0.5f)+pos.x(), pos.y(), (Math.random()-0.5f)+pos.z()), blockOn);
                    }
                }
            }
            friction *= 0.75f;
            if (Math.random() < 0.02f && slump <= 0) {
                vel.add(new Vector3f((float) Utils.mix(Math.random()-0.5f, -dir.x(), 0.7f), 0, (float) Utils.mix(Math.random()-0.5f, -dir.z(), 0.7f)));
                slump += 0.25f;
                slumpDir = true;
            } else if (slumpDir) {
                slump += 0.2f;
                if (slump >= 1.f) {
                    slumpDir = false;
                }
            } else if (slump > 0) {
                slump = Math.max(0, slump-0.085f);
            }
            double playerDist = Main.player.pos.distance(pos);
            if (playerDist < 1.05f+(slump*0.2f)) {
                Main.player.hurt(150);
                Main.player.vel.add(Utils.normalize(new Vector3d(Main.player.pos).sub(pos)).mul(0.5f+slump));
            } else if (playerDist < 20) {
                vel.add(Utils.normalize(new Vector3d(Main.player.pos).sub(pos)).mul(0.1f, 0.f, 0.1f));
            }
        }
        vel.mul(friction);
        float modifiedGrav = World.worldType.gravity();
        if (!onSolid) {
            vel.y -= modifiedGrav;
        }
        PhysicsHelper.moveWithStepping(aabb, vel, aabbs, 1.98f);
        Utils.normalize(dir.set(vel.x(), 0, vel.z())).negate();
        Matrix4f newMatrix = new Matrix4f().lookAlong(dir, up).invert().scale(scale);
        baseTickEnd(scale, newMatrix);
        return false;
    }

    @Override
    public void draw() {
        drawDebug();
        Vector4f color = new Vector4f(1.f);
        Vector3f scale = new Vector3f();
        matrix.getScale(scale);
        pushUBO.updateTex(Textures.complexEntities);
        Matrix4f interpolatedMatrix = new Matrix4f(matrix);
        Vector3d interpolatedPos = Utils.getInterpolatedVec(prevPos, pos);
        interpolatedMatrix.setTranslation((float) (interpolatedPos.x()-warpOffset.x()), (float) (interpolatedPos.y()-warpOffset.y()), (float) (interpolatedPos.z()-warpOffset.z()));

        float legDist = 0.125f;
        pushUBO.updateAtlasOffset(new Vector2i(type.atlasOffset).add(8, 0));
        pushUBO.updateSize(new Vector2i(4, 12));
        float legSwing = (float)Math.toRadians(((bobbing/bobbingScale)-0.5f)*90);
        drawCube(new Matrix4f(interpolatedMatrix).translate(legDist, 0.4f, 0).rotateX(legSwing).translate(0, -0.4f, 0).scale(0.25f, 0.75f, 0.25f), color);
        drawCube(new Matrix4f(interpolatedMatrix).translate(-legDist, 0.4f, 0).rotateX(-legSwing).translate(0, -0.4f, 0).scale(0.25f, 0.75f, 0.25f), color);

        interpolatedMatrix.translate(0, 0.5f, 0);
        interpolatedMatrix.rotateX((float)Math.toRadians(slump*35.f));
        interpolatedMatrix.translate(0, -0.5f, 0);

        pushUBO.updateAtlasOffset(type.atlasOffset);
        pushUBO.updateSize(new Vector2i(8, 8));
        drawCube(new Matrix4f(interpolatedMatrix).translate(0, 1.35f, 0.f).scale(0.5f, 0.5f, 0.5f), color);
        pushUBO.updateAtlasOffset(new Vector2i(type.atlasOffset).add(12, 0));
        pushUBO.updateSize(new Vector2i(8, 12));
        drawCube(new Matrix4f(interpolatedMatrix).translate(0, 0.75f, 0.f).scale(0.5f, 0.75f, 0.25f), color);
        float armDist = 0.375f;
        pushUBO.updateAtlasOffset(new Vector2i(type.atlasOffset).add(20, 0));
        pushUBO.updateSize(new Vector2i(4, 12));
        float armSwing = legSwing*0.025f;
        drawCube(new Matrix4f(interpolatedMatrix).translate(armDist, 1.f, 0).rotateX((float)Math.toRadians(-85.f)-armSwing).translate(0, -0.4f, 0).scale(0.25f, 0.75f, 0.25f), color);
        drawCube(new Matrix4f(interpolatedMatrix).translate(-armDist, 1.f, 0).rotateX((float)Math.toRadians(-90.f)+armSwing).translate(0, -0.4f, 0).scale(0.25f, 0.75f, 0.25f), color);
    }
}
