package org.conspiracraft.effects;

import org.conspiracraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Particle extends Effect {
    public int randomTicksAlive = 0;
    public final Vector3f prevPos = new Vector3f();
    public final Vector3f pos = new Vector3f();
    public final Vector3f vel = new Vector3f();
    public final Vector4f color = new Vector4f();
    public final static Vector3f up = new Vector3f(0, 1, 0);
    public Particle(Matrix4f matrix, Vector4f color) {
        super(matrix);
        this.color.set(color);
        prevPos.set(pos);
        matrix.getTranslation(pos);
        matrix.setTranslation(pos.x(), pos.y(), pos.z());
    }

    @Override
    public boolean tick() {
        if (Math.random() < 0.1f) {
            randomTicksAlive++;
            if (randomTicksAlive >= 40) {
                return true;
            }
        }
        if (!World.inBounds(1, (int) pos.x(), (int) pos.y(), (int) pos.z())) {return true;}
        prevPos.set(pos);
        float modifiedGrav = World.worldType.gravity()/3;
        vel.y -= modifiedGrav;
        vel.mul(0.9f);
        pos.add(vel);
        Vector3f dir = new Vector3f(vel).normalize().negate();
        Vector3f scale = new Vector3f();
        matrix.getScale(scale);
        matrix.identity().lookAlong(dir, up).invert().setTranslation(pos).scale(scale);
        return false;
    }
}
