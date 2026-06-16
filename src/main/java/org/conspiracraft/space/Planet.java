package org.conspiracraft.space;

import org.conspiracraft.Constants;
import org.conspiracraft.Main;
import org.conspiracraft.entities.EntityType;
import org.conspiracraft.graphics.Renderer;
import org.conspiracraft.gui.GUI;
import org.conspiracraft.world.World;
import org.conspiracraft.world.WorldTypes;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import static org.conspiracraft.graphics.Renderer.*;

public class Planet {
    public final EntityType type;
    public final Vector3f pos;
    public final Vector3f localPos;
    public final Vector3f rotatedPos;
    public final Vector4f color;
    public final float scale;
    public final Attachment[] attachments;
    public final Planet[] moons;
    public final float yearLengthNs;
    public final float yearDelta;
    public final float dayLengthNs;
    public final float dayDelta;
    public final Vector3f rot = new Vector3f(0.f);
    public final float apoapsis;
    public final float periapsis;
    public final float lineInc;
    public final int points;

    public Planet(EntityType type, Vector3f pos, Vector4f color, float scale, float yearLengthNs, float dayLengthNs, Attachment[] attachments, Planet[] moons) {
        this.type = type;
        this.pos = pos;
        this.localPos = new Vector3f(pos);
        this.rotatedPos = new Vector3f(pos);
        this.color = color;
        this.scale = scale;
        this.yearLengthNs = yearLengthNs;
        this.yearDelta = (float)(2*Math.PI/yearLengthNs);
        this.dayLengthNs = dayLengthNs;
        this.dayDelta = (float)(2*Math.PI/dayLengthNs);
        this.apoapsis = pos.x();
        this.periapsis = pos.z();
        this.attachments = attachments;
        this.moons = moons;
        this.lineInc = Math.clamp((Constants.AU/apoapsis)/2, 0.005f, 0.025f);
        this.points = (int)(1/lineInc);
    }

    public void render(MemoryStack stack, Vector3f activeRot, Vector3f activePos, Vector3f parent, float orbitThickness) {
        Planet activePlanet = World.worldType.getPlanet();
        if (activePlanet != this) {
            drawOrbit(activeRot, activePos, parent, orbitThickness);
            pushUBO.updateLayer(0);
            pushUBO.updateAtlasOffset(type.atlasOffset);
            tick(Main.timeNs, activeRot, activePos, parent);
            Matrix4f matrix = new Matrix4f()
                    .rotateXYZ(rot)
                    .setTranslation(rotatedPos)
                    .scale(scale);
            drawCube(matrix, new Vector4f(1));
            for (Attachment attachment : attachments) {
                attachment.render(stack, new Matrix4f(matrix));
            }
        } else {
            tick(Main.timeNs, new Vector3f(), new Vector3f(), parent);
        }
        for (Planet moon : moons) {
            moon.render(stack, activeRot, activePos, pos, orbitThickness*0.5f);
        }
    }

    public void drawOrbit(Vector3f activeRot, Vector3f activePos, Vector3f parent, float orbitThickness) {
        if (World.worldType == WorldTypes.SPACE && GUI.showUI) {
            pushUBO.updateLayer(-1);
            float mul = 0.f;
            Vector3f prevPoint = new Vector3f();
            for (int i = 0; i <= points; i++) {
                tick(yearLengthNs * mul, activeRot, activePos, parent);
                if (i > 0) {
                    Renderer.drawLine(prevPoint, rotatedPos, orbitThickness, color);
                }
                prevPoint.set(rotatedPos);
                mul += lineInc;
            }
        }
    }

    public void tick(float timeNs, Vector3f activeRot, Vector3f activePos, Vector3f parent) {
        getPosAtTime(timeNs, localPos);
        pos.set(parent.x() + localPos.x(), parent.y() + localPos.y(), parent.z() + localPos.z());
        rot.set(0, 0, timeNs*dayDelta);
        Vector3f relativePos = new Vector3f(pos).sub(activePos);
        Quaternionf quaternion = new Quaternionf();
        quaternion.rotationXYZ(-activeRot.x, -activeRot.y, -activeRot.z);
        quaternion.transform(relativePos, rotatedPos);
    }

    public void getPosAtTime(float timeNs, Vector3f dest) {
        float range = (apoapsis+periapsis);
        float a = range*0.5f, e = (apoapsis-periapsis)/range;
        float month = timeNs*yearDelta;
        float theta = month+2*e*(float)Math.sin(month)+1.25f*e*e*(float)Math.sin(2.f*month);
        float cosTheta = (float)Math.cos(theta);
        float radius = (a*(1-e*e))/(1+e*cosTheta);
        dest.set(radius*cosTheta, 0.f, radius*(float)Math.sin(theta));
    }
}
