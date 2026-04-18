package org.conspiracraft.gui.buttons;

import org.conspiracraft.Settings;
import org.conspiracraft.audio.AudioController;

public class TAAButton extends Button {
    public TAAButton() {}

    @Override
    public void clicked() {
        Settings.taaEnabled = !Settings.taaEnabled;
        AudioController.playButtonSound();
    }
}
