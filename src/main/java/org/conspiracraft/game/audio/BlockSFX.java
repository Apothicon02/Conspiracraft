package org.conspiracraft.game.audio;

import org.joml.Vector3f;

public class BlockSFX {
    private final int[] placeIds;
    private final float placeGain;
    private final float placePitch;

    public BlockSFX(int[] placeIds, float placeGain, float placePitch) {
        this.placeIds = placeIds;
        this.placeGain = placeGain;
        this.placePitch = placePitch;
    }

    public void placed(Vector3f pos) {
        Source placeSource = new Source(new Vector3f(pos.x, pos.y, pos.z), placeGain, placePitch, 0, 0);
        placeSource.play(AudioController.buffers.get(placeIds[(int) (Math.random()*placeIds.length)]));
    }
}
