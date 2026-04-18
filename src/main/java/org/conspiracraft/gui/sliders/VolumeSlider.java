package org.conspiracraft.gui.sliders;

import org.lwjgl.openal.AL10;
import org.conspiracraft.audio.AudioController;

public class VolumeSlider extends Slider {
    public VolumeSlider() {}

    @Override
    public void clicked(int cursorX) {
        float relX = Math.abs(((float) (bounds.x()-cursorX)) / (bounds.z()-bounds.x()));
        if (relX < 0.01f) {relX = 0.f;}
        if (relX > 0.495f && relX < 0.505f) {relX = 0.5f;}
        if (relX > 0.99f) {relX = 1.f;}
        AudioController.masterVolume = relX*2;
        AL10.alListenerf(AL10.AL_GAIN, AudioController.masterVolume);
        AudioController.playSliderSound();
    }
}
