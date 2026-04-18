package org.conspiracraft.gui.buttons;

import org.conspiracraft.audio.AudioController;
import org.conspiracraft.gui.GUI;

public class AudioSettingsButton extends Button {
    public AudioSettingsButton() {}

    @Override
    public void clicked() {
        GUI.audioSettingMenuOpen = true;
        AudioController.playButtonSound();
    }
}
