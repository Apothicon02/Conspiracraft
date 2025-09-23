package org.conspiracraft.game.audio;

import org.joml.Vector3f;

public class BlockSFX {
    private final int[] placeIds;
    private final float placeGain;
    private final float placePitch;
    public final int[] stepIds;
    public final float stepGain;
    public final float stepPitch;

    public BlockSFX(int[] placeIds, float placeGain, float placePitch, int[] stepIds, float stepGain, float stepPitch) {
        this.placeIds = placeIds;
        this.placeGain = placeGain;
        this.placePitch = placePitch;
        this.stepIds = stepIds;
        this.stepGain = stepGain;
        this.stepPitch = stepPitch;
    }

    public void placed(Vector3f pos) {
        Source placeSource = new Source(new Vector3f(pos.x, pos.y, pos.z), placeGain, placePitch, 0, 0);
        placeSource.play(AudioController.buffers.get(placeIds[(int) (Math.random()*placeIds.length)]));
    }
}
