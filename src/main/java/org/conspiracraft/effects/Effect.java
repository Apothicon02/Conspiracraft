package org.conspiracraft.effects;

import org.joml.Matrix4f;
import org.joml.Vector3d;

public class Effect {
    public Matrix4f matrix;
    public final Vector3d pos = new Vector3d();
    public Effect(Matrix4f matrix) {
        this.matrix = matrix;
    }
    public boolean tick() {
        return false;
    }
}
