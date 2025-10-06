package org.conspiracraft.game.audio;

import org.joml.Vector3f;

public class BlockSFX {
    public final SFX[] placeIds;
    public final float placeGain;
    public final float placePitch;
    public final SFX[] stepIds;
    public final float stepGain;
    public final float stepPitch;

    public BlockSFX(SFX[] placeIds, float placeGain, float placePitch, SFX[] stepIds, float stepGain, float stepPitch) {
        this.placeIds = placeIds;
        this.placeGain = placeGain;
        this.placePitch = placePitch;
        this.stepIds = stepIds;
        this.stepGain = stepGain;
        this.stepPitch = stepPitch;
    }

    public void placed(Vector3f pos) {
        Source placeSource = new Source(new Vector3f(pos.x, pos.y, pos.z), (float) (placeGain+((placeGain*Math.random())/3)), (float) (placePitch+((placePitch*Math.random())/3)), 0, 0);
        AudioController.disposableSources.add(placeSource);
        placeSource.play(placeIds[(int) (Math.random()*placeIds.length)]);
    }
}
