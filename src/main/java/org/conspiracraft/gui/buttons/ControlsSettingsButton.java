package org.conspiracraft.gui.buttons;

import org.conspiracraft.audio.AudioController;
import org.conspiracraft.gui.GUI;

public class ControlsSettingsButton extends Button {
    public ControlsSettingsButton() {}

    @Override
    public void clicked() {
        GUI.controlsSettingMenuOpen = true;
        AudioController.playButtonSound();
    }
}
