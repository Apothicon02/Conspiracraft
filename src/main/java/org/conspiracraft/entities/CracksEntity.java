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
        if (Main.currentTick-minedLast > 50) {durability+=50;}
        if (durability <= 0 || durability > 1000) {return true;}
        prevPos.set(pos);
        updateType();
        return false;
    }
    public int durability = 950;
    public boolean mine(int damage, BlockSFX sfx) {
        minedLast = Main.currentTick;
        durability-=damage;
        EntityType prevType = type;
        updateType();
        if (prevType != type) {
            spawnParticle(); spawnParticle(); spawnParticle(); spawnParticle(); spawnParticle(); spawnParticle(); spawnParticle(); spawnParticle();
        }
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
