package org.conspiracraft.effects;

import org.joml.Matrix4f;

public class Effect {
    public Matrix4f matrix;
    public Effect(Matrix4f matrix) {
        this.matrix = matrix;
    }
    public boolean tick() {
        return false;
    }
}
