package org.conspiracraft.gui.buttons;

import org.conspiracraft.Settings;
import org.conspiracraft.audio.AudioController;

public class UpscaleButton extends Button {
    public UpscaleButton() {}

    @Override
    public void clicked() {
        Settings.upscaleEnabled = !Settings.upscaleEnabled;
        AudioController.playButtonSound();
    }
}
