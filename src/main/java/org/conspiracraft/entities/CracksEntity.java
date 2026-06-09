package org.conspiracraft.entities;

import org.conspiracraft.Main;
import org.conspiracraft.audio.AudioController;
import org.conspiracraft.audio.BlockSFX;
import org.conspiracraft.audio.Source;
import org.conspiracraft.player.Player;
import org.joml.Matrix4f;

public class CracksEntity extends Entity {
    public CracksEntity(EntityType type, Matrix4f matrix, float scaleOffset) {
        super(type, matrix, scaleOffset);
        matrix.getTranslation(prevPos);
        minedLast = Main.currentTick;
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
        updateType();
        if (soundDelay <= 0 && sfx != null) {
            soundDelay = 20-(Math.max(0, damage-20)/2);
            Source source = new Source(prevPos, sfx.placeGain+((sfx.placeGain*Player.playerRand.nextFloat())/3), sfx.placePitch+((sfx.placePitch*Player.playerRand.nextFloat())/3), 0.f, 0);
            source.play(sfx.placeIds[sfx.placeIds.length == 1 ? 0 : Player.playerRand.nextInt(sfx.placeIds.length-1)], true);
            AudioController.disposableSources.add(source);
        }
        soundDelay--;
        return durability <= 0;
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
