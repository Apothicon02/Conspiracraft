package org.conspiracraft.items.types;

import org.conspiracraft.Main;
import org.conspiracraft.audio.AudioController;
import org.conspiracraft.audio.Sounds;
import org.conspiracraft.audio.Source;
import org.conspiracraft.entities.EntityTypes;
import org.conspiracraft.entities.FireballEntity;
import org.conspiracraft.items.Item;
import org.conspiracraft.physics.DDAResult;
import org.joml.Matrix4f;
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
    public int use(DDAResult dda, Item item) {
        if (player.inputHandler.leftButtonPressed) {
            if (!player.creative) {item.amount--;}
            Vector3f earPos = new Vector3f(player.pos).add(0, player.eyeHeight, 0);
            FireballEntity entity = new FireballEntity(EntityTypes.FIREBALL, new Matrix4f().translate(earPos), (float) (Math.random() * -0.1f));
            entity.vel = player.camera.getForward().mul(2).add(player.vel);
            entitiesAddQueue.addLast(entity);
            Source summonSource = new Source(earPos, 1.f, 1.f, 1.f, 0);
            AL11.alSourcef(summonSource.sourceID, AL10.AL_ROLLOFF_FACTOR, 0.5f);
            summonSource.play(Sounds.FIREBALL_QUICK);
            AudioController.disposableSources.add(summonSource);
            Source source = new Source(earPos, 1.f, 1.f, 1.f, 1);
            AL11.alSourcef(source.sourceID, AL10.AL_ROLLOFF_FACTOR, 0.75f);
            source.play(Sounds.MAGMA);
            AudioController.disposableSources.add(source);
            entity.sfxSource = source;
            return 500;
        } else {
            return 0;
        }
    }
}
