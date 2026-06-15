package org.conspiracraft.space;

import org.conspiracraft.Main;
import org.conspiracraft.entities.EntityType;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.world.World;
import org.conspiracraft.world.WorldType;
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
    public final Vector3f prevPos;
    public final Vector3f localPos;
    public final Vector4f color;
    public final float scale;
    public final Attachment[] attachments;
    public final Planet[] moons;
    public final float yearLengthNs;
    public final float dayLengthNs;
    public final Vector3f rot = new Vector3f(0.f);
    public final Vector3f prevRot = new Vector3f(0.f);

    public Planet(EntityType type, Vector3f pos, Vector4f color, float scale, float yearLengthNs, Attachment[] attachments, Planet[] moons) {
        this.type = type;
        this.pos = pos;
        this.prevPos = new Vector3f(pos);
        this.localPos = new Vector3f(pos);
        this.color = color;
        this.scale = scale;
        this.yearLengthNs = yearLengthNs;
        this.dayLengthNs = 30000000000.f;
        this.attachments = attachments;
        this.moons = moons;
    }

    public void render(MemoryStack stack, Vector3f activeRot, Vector3f activePos) {
        Planet activePlanet = World.worldType.getPlanet();
        if (activePlanet != this) {
            pushUBO.updateAtlasOffset(type.atlasOffset);
            Vector3f relativePos = Utils.getInterpolatedVec(prevPos, pos).sub(activePos);
            Quaternionf quaternion = new Quaternionf();
            quaternion.rotationXYZ(-activeRot.x, -activeRot.y, -activeRot.z);
            Vector3f rotatedPos = new Vector3f();
            quaternion.transform(relativePos, rotatedPos);
            Matrix4f matrix = new Matrix4f()
                    .rotateXYZ(rot)
                    .setTranslation(rotatedPos)
                    .scale(scale);
            drawCube(matrix, new Vector4f(1));
            for (Attachment attachment : attachments) {
                attachment.render(stack, new Matrix4f(matrix));
            }
        }
        for (Planet moon : moons) {
            moon.render(stack, activeRot, activePos);
        }
    }
    
    public void tick(Vector3f parent) {
        prevPos.set(pos);
        localPos.rotateY(((float)Main.tickTimeNs)/yearLengthNs);
        pos.set(parent.x()+localPos.x(), parent.y()+localPos.y(), parent.z()+localPos.z());
        prevRot.set(rot);
        float rotF = (float)(Main.tickTimeNs)/dayLengthNs;
        rot.add(0, 0, rotF);
        for (Planet moon : moons) {
            moon.tick(pos);
        }
    }
}
