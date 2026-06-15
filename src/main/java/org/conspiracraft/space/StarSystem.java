package org.conspiracraft.space;

import org.conspiracraft.Main;
import org.conspiracraft.entities.EntityTypes;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.world.World;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import static org.conspiracraft.Constants.*;
import static org.conspiracraft.graphics.Renderer.drawCube;
import static org.conspiracraft.graphics.Renderer.pushUBO;

public class StarSystem {
    final public static int SCALE = AU*5;
    final public static Vector3f pos = new Vector3f(CENTER, CENTER, CENTER);
    final public static Vector3f relativePos = new Vector3f(pos);
    final public static Planet[] planets = new Planet[]{
            new Planet(EntityTypes.OLIVIUS, new Vector3f(-(AU*35), 0, 0), new Vector4f(0.34f, 0.949f, 0.475f, 1), 10000000, 10000000000000.f, new Attachment[]{
                    new Attachment(EntityTypes.OLIVIUS_CLOUDS, 1.04f, new Vector3f()),
                    new Attachment(EntityTypes.OLIVIUS_CLOUDS, 1.08f, new Vector3f(0, 0, (float)Math.toRadians(180.f)))
            }, new Planet[]{
                    new Planet(EntityTypes.MARB, new Vector3f(AU*2, 0, 0), new Vector4f(1, 1, 1, 1), 1300000, 600000000000.f, new Attachment[]{}, new Planet[]{}),
                    new Planet(EntityTypes.VERA, new Vector3f(AU*3, 0, AU), new Vector4f(0.6f, 0.9f, 0.15f, 1), 2250000, 200000000000.f, new Attachment[]{
                            new Attachment(EntityTypes.VERA_CLOUDS, 1.04f, new Vector3f()),
                            new Attachment(EntityTypes.VERA_ORANGE_CLOUDS, 1.07f, new Vector3f(0, 0, (float)Math.toRadians(180.f)))
                    }, new Planet[]{})
            }),
            new Planet(EntityTypes.AKSALA, new Vector3f(-(AU*45), 0, 0), new Vector4f(0.44f, 0.949f, 0.975f, 1), 3000000, 15000000000000.f, new Attachment[]{
                    new Attachment(EntityTypes.AKSALA_AURORAS, 1.05f, new Vector3f())
            }, new Planet[]{}),
            new Planet(EntityTypes.FIREBALL, new Vector3f(-(AU*15), 0, 0), new Vector4f(1.f, 0.59f, 0.05f, 1), 3000000, 1000000000000.f, new Attachment[]{
                    new Attachment(EntityTypes.ASH, 1.02f, new Vector3f())
            }, new Planet[]{})
    };

    public static Planet getNearestPlanet(Vector3f pos) {
        Planet nearestPlanet = null;
        double nearestDist = Long.MAX_VALUE;
        for (Planet planet : planets) {
            double dist = pos.distance(planet.pos);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearestPlanet = planet;
            }
            for (Planet moon : planet.moons) {
                dist = pos.distance(moon.pos);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearestPlanet = moon;
                }
            }
        }
        return nearestPlanet;
    }

    public static void render(MemoryStack stack) {
        pushUBO.updateLayer(0);
        pushUBO.updateAtlasOffset(EntityTypes.SUN.atlasOffset);
        Planet activePlanet = Main.player.getPlanet();
        Vector3f activePos = activePlanet == null ? new Vector3f(0) : Utils.getInterpolatedVec(activePlanet.prevPos, activePlanet.pos);
        relativePos.set(pos).sub(activePos);
        Vector3f activeRot = activePlanet == null ? new Vector3f(0) : Utils.getInterpolatedVec(activePlanet.prevRot, activePlanet.rot);
        if (activePlanet != null) {
            Quaternionf quaternion = new Quaternionf();
            quaternion.rotationXYZ(-activeRot.x, -activeRot.y, -activeRot.z);
            quaternion.rotateX(World.worldType.getLongitude());
            Vector3f rotatedPos = new Vector3f();
            quaternion.transform(relativePos, rotatedPos);
            relativePos.set(rotatedPos);
        }
        Matrix4f sunMatrix = new Matrix4f().rotateXYZ(0.5f, 0.5f, 0.5f).setTranslation(relativePos).scale(SCALE);
        Vector4f sunColor = new Vector4f(2.5f, 2.5f, 2.5f, 1);
        drawCube(sunMatrix, sunColor);
        for (Planet planet : planets) {
            planet.render(stack, activeRot, activePos);
        }
//        pushUBO.updateAtlasOffset(EntityTypes.OLIVIUS.atlasOffset);
//        Matrix4f oliviusMatrix = new Matrix4f().rotateXYZ(0.5f, 0.5f, 0.5f).setTranslation(Utils.getInterpolatedVec(oliviusPrevPos, oliviusPos)).scale(10000000);
//        drawCube(oliviusMatrix, oliviusColor);
//        pushUBO.updateAtlasOffset(EntityTypes.OLIVIUS_CLOUDS.atlasOffset);
//        drawCube(oliviusMatrix.scale(1.04f), oliviusColor);
//        drawCube(oliviusMatrix.scale(1.04f).rotateXYZ(0, 0, (float)Math.toRadians(180.f)), oliviusColor);
    }
    
    public static void tick() {
        for (Planet planet : planets) {
            planet.tick(pos);
        }
    }
}
