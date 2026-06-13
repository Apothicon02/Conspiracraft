package org.conspiracraft.space;

import org.conspiracraft.Main;
import org.conspiracraft.entities.EntityType;
import org.conspiracraft.utils.Utils;
import org.joml.Matrix4f;
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
    public final float period;

    public Planet(EntityType type, Vector3f pos, Vector4f color, float scale, float period, Attachment[] attachments, Planet[] moons) {
        this.type = type;
        this.pos = pos;
        this.prevPos = new Vector3f(pos);
        this.localPos = new Vector3f(pos);
        this.color = color;
        this.scale = scale;
        this.period = period;
        this.attachments = attachments;
        this.moons = moons;
    }

    public void render(MemoryStack stack) {
        pushUBO.updateAtlasOffset(type.atlasOffset);
        Matrix4f matrix = new Matrix4f().rotateXYZ(0.5f, 0.5f, 0.5f).setTranslation(Utils.getInterpolatedVec(prevPos, pos)).scale(scale);
        drawCube(matrix, new Vector4f(1));
        for (Attachment attachment : attachments) {
            attachment.render(stack, new Matrix4f(matrix));
        }
        for (Planet moon : moons) {
            moon.render(stack);
        }
    }
    
    public void tick(Vector3f parent) {
        prevPos.set(pos);
        localPos.rotateY(((float)Main.tickTimeNs)/period);
        pos.set(parent.x()+localPos.x(), parent.y()+localPos.y(), parent.z()+localPos.z());
        for (Planet moon : moons) {
            moon.tick(pos);
        }
    }
}
