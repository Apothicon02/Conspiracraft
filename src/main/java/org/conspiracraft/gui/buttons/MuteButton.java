package org.conspiracraft.gui.buttons;

import org.lwjgl.openal.AL10;
import org.conspiracraft.audio.AudioController;

public class MuteButton extends Button {
    public MuteButton() {}

    @Override
    public void clicked() {
        AudioController.muted = !AudioController.muted;
        if (AudioController.muted) {
            AL10.alListenerf(AL10.AL_GAIN, 0.f);
        } else {
            AL10.alListenerf(AL10.AL_GAIN, AudioController.masterVolume);
            AudioController.playButtonSound();
        }
    }
}
