package org.conspiracraft.gui.buttons;

import org.conspiracraft.audio.AudioController;
import org.conspiracraft.gui.GUI;

public class SettingsButton extends Button {
    public SettingsButton() {}

    @Override
    public void clicked() {
        GUI.settingMenuOpen = true;
        AudioController.playButtonSound();
    }
}
