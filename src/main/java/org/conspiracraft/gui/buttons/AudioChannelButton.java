package org.conspiracraft.gui.buttons;

import org.lwjgl.openal.SOFTOutputMode;
import org.conspiracraft.audio.AudioController;

public class AudioChannelButton extends Button {
    public AudioChannelButton() {}

    @Override
    public void clicked() {
        if (AudioController.outputMode == SOFTOutputMode.ALC_SURROUND_7_1_SOFT) {
            AudioController.outputMode = SOFTOutputMode.ALC_MONO_SOFT;
        } else if (AudioController.outputMode == SOFTOutputMode.ALC_MONO_SOFT) {
            AudioController.outputMode = SOFTOutputMode.ALC_STEREO_HRTF_SOFT;
        } else if (AudioController.outputMode == SOFTOutputMode.ALC_STEREO_HRTF_SOFT) {
            AudioController.outputMode = SOFTOutputMode.ALC_SURROUND_5_1_SOFT;
        } else if (AudioController.outputMode == SOFTOutputMode.ALC_SURROUND_5_1_SOFT) {
            AudioController.outputMode = SOFTOutputMode.ALC_SURROUND_6_1_SOFT;
        } else if (AudioController.outputMode == SOFTOutputMode.ALC_SURROUND_6_1_SOFT) {
            AudioController.outputMode = SOFTOutputMode.ALC_SURROUND_7_1_SOFT;
        }
        AudioController.playButtonSound();
    }
}
