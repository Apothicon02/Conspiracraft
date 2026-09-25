package org.conspiracraft.effects;

import org.conspiracraft.graphics.textures.Texture;
import org.conspiracraft.utils.Utils;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector4f;

import static org.conspiracraft.graphics.Renderer.*;
import static org.conspiracraft.graphics.Renderer.warpOffset;
import static org.conspiracraft.world.World.height;
import static org.conspiracraft.world.World.size;

public class Effect {
    public Matrix4f matrix;
    public final Vector3d prevPos = new Vector3d();
    public final Vector3d pos;
    public final Vector4f color = new Vector4f(0.95f, 0.95f, 0.95f, 1.f);
    public Texture tex = null;
    public final Vector2i texOffset = new Vector2i();
    public final Vector2i texSize = new Vector2i(16);
    public Effect(Vector3d pos, Matrix4f matrix) {
        prevPos.set(pos);
        this.pos = pos;
        this.matrix = matrix;
    }
    public boolean tick() {
        return false;
    }
    public void draw() {
        pushUBO.updateTex(tex);
        pushUBO.updateAtlasOffset(texOffset);
        pushUBO.updateSize(texSize);
        Matrix4f interpolatedMatrix = new Matrix4f(matrix);
        Vector3d interpolatedPos = Utils.getInterpolatedVec(prevPos, pos);
        interpolatedMatrix.setTranslation((float) (interpolatedPos.x()-warpOffset.x()), (float) (interpolatedPos.y()-warpOffset.y()), (float) (interpolatedPos.z()-warpOffset.z()));
        drawCube(interpolatedMatrix, color);
    }
}
