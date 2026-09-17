package org.conspiracraft.gui.buttons;

import org.conspiracraft.Settings;
import org.conspiracraft.audio.AudioController;

public class GUIScaleButton extends Button {
    public GUIScaleButton() {}

    @Override
    public void clicked() {
        Settings.guiScale += 0.25f;
        if (Settings.guiScale > 1.25f) {Settings.guiScale = 0.5f;}
        AudioController.playButtonSound();
    }
}
