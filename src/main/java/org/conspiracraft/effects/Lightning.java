package org.conspiracraft.effects;

import org.conspiracraft.audio.AudioController;
import org.conspiracraft.audio.SFX;
import org.conspiracraft.audio.Sounds;
import org.conspiracraft.audio.Source;
import org.conspiracraft.blocks.BlockTags;
import org.conspiracraft.entities.AshEntity;
import org.conspiracraft.entities.EntityTypes;
import org.conspiracraft.world.World;
import org.conspiracraft.world.types.WorldTypes;
import org.joml.*;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

import java.lang.Math;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

import static org.conspiracraft.world.World.entitiesAddQueue;

public class Lightning extends Effect {
    public int lifetime = 0;
    public final Vector3i intPos = new Vector3i();
    public final Vector4f color = new Vector4f(0);
    public Lightning(Vector3d pos, Matrix4f matrix) {
        super(matrix);
        if (World.worldType == WorldTypes.EARTH) {
            color.set(2.f, 1.9f, 0.2f, 4.f);
        } else if (World.worldType == WorldTypes.AKSALA) {
            color.set(2.f, 0.2f, 0.2f, 4.f);
        } else if (World.worldType == WorldTypes.VERA) {
            color.set(1.2f, 0.4f, 2.0f, 4.f);
        } else {
            color.set(2.f, 1.9f, 0.2f, 4.f);
        }
        this.pos.set(pos);
        intPos.set((int)pos.x(), (int)pos.y()-1, (int)pos.z());
        pos.add(+0.5f, matrix.getScale(new Vector3f()).y()/2.f, 0.5f);
        SFX sfx = Math.random() < 0.5f ? Sounds.THUNDER_1 : Sounds.THUNDER_2;
        Source source = new Source(new Vector3f(pos), 2.f, 1.f, 1.f, 0);
        AL11.alSourcef(source.sourceID, AL10.AL_ROLLOFF_FACTOR, 0.3f);
        source.play(sfx);
        AudioController.disposableSources.add(source);

        Vector3i iPos = new Vector3i(intPos);
        int blockStruck = World.getBlock(iPos).x();
        if (BlockTags.leaves.tagged.contains(blockStruck)) {
            Source sizzleSource = new Source(new Vector3f((float) pos.x(), (float) pos.y(), (float) pos.z()), 1.f, 1.f, 0, 0);
            sizzleSource.play(Math.random() < 0.5f ? Sounds.SIZZLE1 : Sounds.SIZZLE2);
            AudioController.disposableSources.add(sizzleSource);
            spawnAsh(iPos);
            removalQueue.add(iPos);
            removalSet.add(iPos);
            while (!removalQueue.isEmpty()) {
                incinerate(blockStruck, removalQueue.poll());
            }
            Vector3i[] horizontalPositons = new Vector3i[removalSet.size()];
            AtomicInteger size = new AtomicInteger(-1);
            removalSet.iterator().forEachRemaining(rPos -> {
                World.setBlock(rPos.x(), rPos.y(), rPos.z(), 0, 0, false);
                horizontalPositons[size.addAndGet(1)] = rPos;
                if (Math.random() < 0.001f) {
                    spawnAsh(rPos);
                }
            });
            for (int i = 0; i <= size.get(); i++) {
                Vector3i hPos = horizontalPositons[i];
                World.updateHeightmap(hPos.x(), 0, hPos.z());
            }
        }
    }

    public void spawnAsh(Vector3i pos) {
        AshEntity ashEntity = new AshEntity(EntityTypes.ASH, new Vector3d(pos.x(), pos.y(), pos.z()), new Matrix4f(), (float) (Math.random() * -0.1f));
        ashEntity.vel = new Vector3d(Math.random(), Math.random(), Math.random());
        entitiesAddQueue.addLast(ashEntity);
    }

    public final int RADIUS = 3000;
    public final ArrayDeque<Vector3i> removalQueue = new ArrayDeque<>();
    public final HashSet<Vector3i> removalSet = new HashSet<>();
    public void incinerate(int blockStruck, Vector3i iPos) {
        int dx = iPos.x - intPos.x(), dy = iPos.y - intPos.y(), dz = iPos.z - intPos.z();
        if (dx * dx + ((dy * dy)/2) + dz * dz <= RADIUS) {
            if (World.getBlock(iPos).x() == blockStruck) {
                for (Vector3i nPos : new Vector3i[]{
                        new Vector3i(iPos.x, iPos.y, iPos.z + 1), new Vector3i(iPos.x + 1, iPos.y, iPos.z), new Vector3i(iPos.x, iPos.y, iPos.z - 1),
                        new Vector3i(iPos.x - 1, iPos.y, iPos.z), new Vector3i(iPos.x, iPos.y + 1, iPos.z), new Vector3i(iPos.x, iPos.y - 1, iPos.z)
                }) {
                    if (removalSet.add(nPos)) {
                        removalQueue.add(nPos);
                    }
                }
            }
        }
    }

    @Override
    public boolean tick() {
        lifetime++;
        if (lifetime > 10) {
            return true;
        }
        return false;
    }
}
