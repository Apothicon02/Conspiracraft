package org.conspiracraft.gui.buttons;

import org.conspiracraft.audio.AudioController;
import org.conspiracraft.gui.GUI;

public class BackButton extends Button {
    public BackButton() {}

    @Override
    public void clicked() {
        if (GUI.accessibilitySettingMenuOpen) {
            GUI.accessibilitySettingMenuOpen = false;
        } else if (GUI.graphicsSettingMenuOpen) {
            GUI.graphicsSettingMenuOpen = false;
        } else if (GUI.controlsSettingMenuOpen) {
            GUI.controlsSettingMenuOpen = false;
        } else if (GUI.audioSettingMenuOpen) {
            GUI.audioSettingMenuOpen = false;
        } else if (GUI.settingMenuOpen) {
            GUI.settingMenuOpen = false;
        } else {
            GUI.pauseMenuOpen = false;
        }
        AudioController.playButtonSound();
    }
}
