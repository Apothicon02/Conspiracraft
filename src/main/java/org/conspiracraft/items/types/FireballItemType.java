package org.conspiracraft.items.types;

import org.conspiracraft.audio.AudioController;
import org.conspiracraft.audio.Sounds;
import org.conspiracraft.audio.Source;
import org.conspiracraft.entities.EntityTypes;
import org.conspiracraft.entities.FireballEntity;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.ItemUseResult;
import org.conspiracraft.physics.DDAResult;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

import static org.conspiracraft.Main.player;
import static org.conspiracraft.world.World.entitiesAddQueue;

public class FireballItemType extends ItemType {
    public FireballItemType(String name) {
        super(name);
    }
    @Override
    public ItemUseResult use(DDAResult dda, Item item) {
        if (player.inputHandler.leftButtonPressed) {
            if (!player.creative) {item.amount--;}
            Vector3d earPos = new Vector3d(player.pos).add(0, player.eyeHeight, 0);
            FireballEntity entity = new FireballEntity(EntityTypes.FIREBALL, earPos, new Matrix4f(), (float) (Math.random() * -0.1f));
            entity.vel = new Vector3d(player.camera.getForward()).mul(2).add(player.vel);
            entitiesAddQueue.addLast(entity);
            Source summonSource = new Source(new Vector3f(earPos), 1.f, 1.f, 1.f, 0);
            AL11.alSourcef(summonSource.sourceID, AL10.AL_ROLLOFF_FACTOR, 0.5f);
            summonSource.play(Sounds.FIREBALL_QUICK);
            AudioController.disposableSources.add(summonSource);
            Source source = new Source(new Vector3f(earPos), 1.f, 1.f, 1.f, 1);
            AL11.alSourcef(source.sourceID, AL10.AL_ROLLOFF_FACTOR, 0.75f);
            source.play(Sounds.MAGMA);
            AudioController.disposableSources.add(source);
            entity.sfxSource = source;
            return new ItemUseResult(500, item);
        } else {
            return new ItemUseResult(0, item);
        }
    }
}
