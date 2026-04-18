package org.conspiracraft.gui.buttons;

import org.conspiracraft.Settings;
import org.conspiracraft.audio.AudioController;

public class DynamicFoVButton extends Button {
    public DynamicFoVButton() {}

    @Override
    public void clicked() {
        Settings.dynamicFoVEnabled = !Settings.dynamicFoVEnabled;
        AudioController.playButtonSound();
    }
}
