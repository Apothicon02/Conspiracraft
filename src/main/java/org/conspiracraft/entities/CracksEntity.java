package org.conspiracraft.entities;

import org.conspiracraft.Main;
import org.conspiracraft.audio.AudioController;
import org.conspiracraft.audio.BlockSFX;
import org.conspiracraft.audio.Source;
import org.conspiracraft.effects.Particle;
import org.conspiracraft.player.HandManager;
import org.conspiracraft.player.Player;
import org.conspiracraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static org.conspiracraft.world.World.effects;

public class CracksEntity extends Entity {
    public CracksEntity(EntityType type, Vector3d pos, Matrix4f matrix, float scaleOffset) {
        super(type, pos, matrix, scaleOffset);
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
        prevPos.set(pos);
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
            Source source = new Source(new Vector3f(prevPos), sfx.placeGain+((sfx.placeGain*Player.playerRand.nextFloat())/3), sfx.placePitch+((sfx.placePitch*Player.playerRand.nextFloat())/3), 0.f, 0);
            source.play(sfx.placeIds[sfx.placeIds.length == 1 ? 0 : Player.playerRand.nextInt(sfx.placeIds.length-1)], true);
            AudioController.disposableSources.add(source);
        }
        soundDelay--;
        if (durability <= 0) {spawnParticle(); spawnParticle(); spawnParticle(); spawnParticle(); spawnParticle(); spawnParticle(); spawnParticle(); spawnParticle();}
        return durability <= 0;
    }

    public void spawnParticle() {
        if (HandManager.ddaResult != null && HandManager.ddaResult.hitAnything) {
            addParticle(new Vector3d(HandManager.ddaResult.hitD), World.getBlock(pos.x(), pos.y(), pos.z()));
        }
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
