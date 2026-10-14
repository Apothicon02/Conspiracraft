package org.conspiracraft.gui.buttons;

import org.conspiracraft.Settings;
import org.conspiracraft.audio.AudioController;

public class ShadowsButton extends Button {
    public ShadowsButton() {}

    @Override
    public void clicked() {
        Settings.shadows++;
        if (Settings.shadows > 2) {Settings.shadows = 0;}
        AudioController.playButtonSound();
    }
}
