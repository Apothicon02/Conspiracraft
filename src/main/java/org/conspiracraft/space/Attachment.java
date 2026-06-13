package org.conspiracraft.space;

import org.conspiracraft.entities.EntityType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import static org.conspiracraft.graphics.Renderer.drawCube;
import static org.conspiracraft.graphics.Renderer.pushUBO;

public class Attachment {
    public final EntityType type;
    public final float scale;
    public final Vector3f rot;
    public Attachment(EntityType type, float scale, Vector3f rot) {
        this.type = type;
        this.scale = scale;
        this.rot = rot;
    }
    public void render(MemoryStack stack, Matrix4f parent) {
        pushUBO.updateAtlasOffset(type.atlasOffset);
        drawCube(parent.scale(scale).rotateXYZ(rot), new Vector4f(1));
    }
}
