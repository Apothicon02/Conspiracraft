package org.conspiracraft.entities;

import org.conspiracraft.Main;
import org.conspiracraft.audio.AudioController;
import org.conspiracraft.audio.BlockSFX;
import org.conspiracraft.audio.Source;
import org.conspiracraft.effects.Particle;
import org.conspiracraft.player.Player;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import static org.conspiracraft.world.World.effects;

public class CracksEntity extends Entity {
    public CracksEntity(EntityType type, Matrix4f matrix, float scaleOffset) {
        super(type, matrix, scaleOffset);
        matrix.getTranslation(prevPos);
        minedLast = Main.currentTick;
        spawnParticle(); spawnParticle(); spawnParticle(); spawnParticle(); spawnParticle(); spawnParticle();
    }

    @Override
    public boolean playerCollidesWith() {return false;}
    public long minedLast = 0;
    @Override
    public boolean tick() {
        if (Main.currentTick-minedLast > 5) {durability+=50;}
        if (durability <= 0 || durability > 1000) {return true;}
        updateType();
        return false;
    }
    public static int soundDelay = 0;
    public int durability = 950;
    public boolean mine(int damage, BlockSFX sfx) {
        minedLast = Main.currentTick;
        durability-=damage;
        EntityType prevType = type;
        updateType();
        if (prevType != type) {
            spawnParticle(); spawnParticle(); spawnParticle(); spawnParticle(); spawnParticle(); spawnParticle(); spawnParticle(); spawnParticle();
        }
        if (soundDelay <= 0 && sfx != null) {
            soundDelay = 20-(Math.max(0, damage-20)/2);
            Source source = new Source(prevPos, sfx.placeGain+((sfx.placeGain*Player.playerRand.nextFloat())/3), sfx.placePitch+((sfx.placePitch*Player.playerRand.nextFloat())/3), 0.f, 0);
            source.play(sfx.placeIds[sfx.placeIds.length == 1 ? 0 : Player.playerRand.nextInt(sfx.placeIds.length-1)], true);
            AudioController.disposableSources.add(source);
        }
        soundDelay--;
        if (durability <= 0) {spawnParticle(); spawnParticle(); spawnParticle(); spawnParticle(); spawnParticle(); spawnParticle(); spawnParticle(); spawnParticle();}
        return durability <= 0;
    }

    public void spawnParticle() {
        Particle particle = new Particle(new Matrix4f().translate(prevPos.x(), prevPos.y()-0.5f, prevPos.z()).scale(0.075f+(float)(0.05f*Math.random())), new Vector4f(0.8f, 0.85f, 0.85f, 1.f));
        particle.vel.set((float) (Math.random()-0.5f)/2, (float) (0.5f+Math.random())/3, (float) (Math.random()-0.5f)/2);
        effects.addLast(particle);
    }

    public void updateType() {
        if (durability <= 250) {
            type = EntityTypes.COMPLETELY_CRACKED;
        } else if (durability <= 500) {
            type = EntityTypes.VERY_CRACKED;
        } else if (durability <= 750) {
            type = EntityTypes.CRACKED;
        } else {
            type = EntityTypes.SLIGHTLY_CRACKED;
        }
    }
}
