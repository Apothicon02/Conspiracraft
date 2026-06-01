package org.conspiracraft.effects;

import org.conspiracraft.audio.AudioController;
import org.conspiracraft.audio.SFX;
import org.conspiracraft.audio.Sounds;
import org.conspiracraft.audio.Source;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

public class Lightning extends Effect {
    public int lifetime = 0;
    public Vector3f pos = new Vector3f();
    public Lightning(Matrix4f matrix) {
        super(matrix);
        matrix.getTranslation(pos);
        matrix.setTranslation(pos.x(), pos.y()+matrix.getScale(new Vector3f()).y()/2.f, pos.z());
        SFX sfx = Math.random() < 0.5f ? Sounds.THUNDER_1 : Sounds.THUNDER_2;
        Source source = new Source(pos, 2.f, 1.f, 0, 0);
        AL11.alSourcef(source.sourceID, AL10.AL_ROLLOFF_FACTOR, 0.3f);
        source.play(sfx);
        AudioController.disposableSources.add(source);
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
