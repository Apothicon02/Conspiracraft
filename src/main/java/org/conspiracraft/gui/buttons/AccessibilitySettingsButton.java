package org.conspiracraft.gui.buttons;

import org.conspiracraft.audio.AudioController;
import org.conspiracraft.gui.GUI;

public class AccessibilitySettingsButton extends Button {
    public AccessibilitySettingsButton() {}

    @Override
    public void clicked() {
        GUI.accessibilitySettingMenuOpen = true;
        AudioController.playButtonSound();
    }
}
