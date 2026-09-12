package org.conspiracraft.entities;

import org.conspiracraft.audio.AudioController;
import org.conspiracraft.audio.Sounds;
import org.conspiracraft.audio.Source;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.physics.PhysicsHelper;
import org.conspiracraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class FireballEntity extends Entity {
    public FireballEntity(EntityType type, Vector3d pos, Matrix4f matrix, float scaleOffset) {super(type, pos, matrix, scaleOffset);}

    public Source sfxSource = null;
    @Override
    public boolean playerCollidesWith() {return false;}
    public final static Vector3f up = new Vector3f(0, 1, 0);
    public int ticksAlive = 0;
    @Override
    public boolean tick() {
        prevPos.set(pos);
        Vector3f scale = new Vector3f();
        matrix.getScale(scale);
        Vector3f halfScale = new Vector3f(scale).div(2);
        PhysicsHelper.BlockResult blockIn = PhysicsHelper.getClosestBlock(aabb.copy().grow(0.1f), new Vector3d(pos).sub(vel));
        if (blockIn != null && blockIn.block() != null && blockIn.block().x() > 0) {
            World.setBlock((int) blockIn.x(), (int) blockIn.y(), (int) blockIn.z(), blockIn.block().x() == BlockTypes.WATER.id ? BlockTypes.OBSIDIAN.id : BlockTypes.MAGMA.id, 0);
            Source source = new Source(matrix.getTranslation(new Vector3f()), 1.f, 1.f, 0, 0);
            source.play(Math.random() < 0.5f ? Sounds.SIZZLE1 : Sounds.SIZZLE2);
            AudioController.disposableSources.add(source);
            if (sfxSource != null) {sfxSource.stop();}
            return true;
        }
        float modifiedGrav = World.worldType.gravity();
        vel.y -= modifiedGrav;
        if (sfxSource != null) {
            sfxSource.setVel(new Vector3f(vel));
            sfxSource.setPos(new Vector3f(pos));
        }
        PhysicsHelper.move(aabb, vel);
        Vector3f dir = new Vector3f(vel).normalize().negate();
        matrix.identity().lookAlong(dir, up).invert().scale(scale);
        pos.set(aabb.xMin+halfScale.x(), aabb.yMin+halfScale.y(), aabb.zMin+halfScale.z());
        ticksAlive++;
        if (ticksAlive >= 4) {
            ticksAlive = (int)-(Math.random()*4);
            FireballEntity clone = new FireballEntity(EntityTypes.FIREBALL, new Vector3d(pos), new Matrix4f(matrix), (float)(EntityTypes.FIREBALL.size*-(0.4f+(Math.random()*0.2f))));
            clone.vel = new Vector3d(vel).mul(0.9f, 1.f, 0.9f).add( 0, (float)-(modifiedGrav*Math.random()*3), 0);
            clone.ticksAlive = Integer.MIN_VALUE;
            World.entitiesAddQueue.add(clone);
        }
        return false;
    }
}
