package org.conspiracraft.entities;

import org.conspiracraft.Main;
import org.conspiracraft.audio.AudioController;
import org.conspiracraft.audio.SFX;
import org.conspiracraft.audio.Sounds;
import org.conspiracraft.audio.Source;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.graphics.Renderer;
import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.physics.AABB;
import org.conspiracraft.physics.PhysicsHelper;
import org.conspiracraft.player.Player;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.world.World;
import org.joml.*;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

import static org.conspiracraft.graphics.Renderer.pushUBO;
import static org.conspiracraft.graphics.Renderer.warpOffset;

public class FrogEntity extends Entity {
    public FrogEntity(EntityType type, Vector3d pos, Matrix4f matrix, float scaleOffset) {
        super(type, pos, matrix, scaleOffset);
    }

    public final static Vector3f up = new Vector3f(0, 1, 0);
    public static final SFX[] croakSFXs = new SFX[]{Sounds.FROG_CROAK1, Sounds.FROG_CROAK2, Sounds.FROG_CROAK3, Sounds.FROG_CROAK4};
    public static final SFX[] tongueSFXs = new SFX[]{Sounds.MUD_STEP1, Sounds.MUD_STEP2};
    public static final float tongueSpeed = 0.15f;
    public boolean tongueRetracting = false;
    public float length = 0.f, prevLength = 0.f;
    public Source tongueSource = null;
    public int tongueSFXI = 0;
    public Source croakSource = null;
    public int croakSFXI = 0;
    @Override
    public boolean tick() {
        prevPos.set(pos);
        prevLength = length;
        Vector3f scale = new Vector3f();
        matrix.getScale(scale);
        Vector3f halfScale = new Vector3f(scale).div(2);
        AABB footAABB = new AABB(aabb.xMin, aabb.xMax, aabb.yMin - 0.075f, aabb.yMin, aabb.zMin, aabb.zMax);
        int blockOn = PhysicsHelper.getAnyBlock(footAABB).block();
        boolean onSolid = BlockTypes.blockTypes[blockOn >> 16].blockProperties.isCollidable;
        float friction = 0.99f; //1-airFriction=maxFriction
        if (onSolid) {
            friction *= 0.75f;
            if (length <= 0.f && Math.random() < 0.01f) {
                double playerDist = Main.player.pos.distance(pos);
                if (playerDist > 3f && playerDist < 20) {
                    length+=tongueSpeed;
                    tongueRetracting = false;
                    if (tongueSource == null || !tongueSource.isPlaying()) {
                        tongueSource = new Source(new Vector3f(pos), 0.85f + (Player.playerRand.nextFloat()*0.1f), 1.5f + (Player.playerRand.nextFloat()*0.1f), 0.f, 0);
                        tongueSource.play(tongueSFXs[tongueSFXI], false);
                        tongueSFXI+=1+Player.playerRand.nextInt(2);
                        if (tongueSFXI >= tongueSFXs.length) {tongueSFXI = 0;}
                        AudioController.disposableSources.add(tongueSource);
                    }
                }
            }
            if (length <= 0.f) {
                if (Math.random() < 0.02f) {
                    vel.add(new Vector3f((float) (Math.random() - 0.5f), (float)((Math.random()*0.5f)+0.5f), (float) (Math.random() - 0.5f)));
                    addParticle(new Vector3d((Math.random() - 0.5f) + pos.x(), pos.y(), (Math.random() - 0.5f) + pos.z()), blockOn);
                    addParticle(new Vector3d((Math.random() - 0.5f) + pos.x(), pos.y(), (Math.random() - 0.5f) + pos.z()), blockOn);
                    addParticle(new Vector3d((Math.random() - 0.5f) + pos.x(), pos.y(), (Math.random() - 0.5f) + pos.z()), blockOn);
                    addParticle(new Vector3d((Math.random() - 0.5f) + pos.x(), pos.y(), (Math.random() - 0.5f) + pos.z()), blockOn);

                    if (croakSource == null || !croakSource.isPlaying()) {
                        croakSource = new Source(new Vector3f(pos), 0.3f + (Player.playerRand.nextFloat()*0.03f), 0.95f + (Player.playerRand.nextFloat()*0.1f), 0.f, 0);
                        croakSource.play(croakSFXs[croakSFXI], false);
                        croakSFXI+=1+Player.playerRand.nextInt(2);
                        if (croakSFXI >= croakSFXs.length) {croakSFXI = 0;}
                        AudioController.disposableSources.add(croakSource);
                    }
                }
            }
        }
        vel.mul(friction);
        float modifiedGrav = World.worldType.gravity();
        if (!onSolid) {
            vel.y -= modifiedGrav;
        }
        PhysicsHelper.moveWithStepping(aabb, vel, new ArrayList<>(List.of(Main.player.playerAABB)));
        if (length > 2.f) {
            tongueRetracting = true;
            length-=tongueSpeed;
            dir.set(pos.x()-Main.player.pos.x(), vel.y(), pos.z()-Main.player.pos.z()).normalize();
        } else if (length > 0.f) {
            if (tongueRetracting) {
                length-=tongueSpeed;
            } else {
                length+=tongueSpeed;
            }
            dir.set(pos.x()-Main.player.pos.x(), vel.y(), pos.z()-Main.player.pos.z()).normalize();
            if (prevLength < 1.f && length >= 1.f) {
                double playerDist = Main.player.pos.distance(pos);
                Main.player.vel.add(new Vector3d(Main.player.pos).sub(pos).mul(-0.1f, 0.f, -0.1f).add(0, playerDist*0.05f, 0));
            }
        } else if (vel.length() > 0.01f) {
            dir.set(vel).normalize().negate();
        }
        Matrix4f newMatrix = new Matrix4f().lookAlong(dir, up).invert().scale(scale);
        if (newMatrix.isFinite()) {matrix.set(newMatrix);}
        pos.set(aabb.xMin+halfScale.x(), aabb.yMin+halfScale.y(), aabb.zMin+halfScale.z());
        return false;
    }

    @Override
    public void draw() {
        drawBase();
        if (length > 0.02f) {
            float interpolatedLength = Utils.getInterpolatedFloat(prevLength, length);
            float minLength = Math.min(1, interpolatedLength);
            pushUBO.updateAtlasOffset(EntityTypes.ROPE.atlasOffset);
            pushUBO.updateSize(new Vector2i(-6, -6));
            Vector3f scale = new Vector3f();
            matrix.getScale(scale);
            Vector3f halfScale = new Vector3f(scale).mul(0.5f);
            Vector3d interpolatedPos = Utils.getInterpolatedVec(prevPos, pos);
            Vector3f startPos = new Vector3f((float) (interpolatedPos.x() - warpOffset.x()), (float) (interpolatedPos.y() - warpOffset.y()), (float) (interpolatedPos.z() - warpOffset.z())).sub(new Vector3f(dir).mul(halfScale));
            Vector3d interpolatedPlrPos = Utils.getInterpolatedVec(Main.player.prevPos, Main.player.pos);
            Vector3f endPos = new Vector3f((float) (interpolatedPlrPos.x() - warpOffset.x()), (float) (interpolatedPlrPos.y() - warpOffset.y()), (float) (interpolatedPlrPos.z() - warpOffset.z()));
            endPos.set(Utils.mix(startPos.x(), endPos.x(), minLength), Utils.mix(startPos.y(), endPos.y(), minLength), Utils.mix(startPos.z(), endPos.z(), minLength));
            Renderer.drawLineFixedWidth(startPos, endPos, matrix.getNormalizedRotation(new Quaternionf()), 0.3f, 0.04f, new Vector4f(0.85f, 0.075f, 0.075f, 1.f));//0.03125f
        }
    }
}
