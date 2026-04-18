package org.conspiracraft.gui.buttons;

import org.conspiracraft.Settings;
import org.conspiracraft.audio.AudioController;

public class ShadowsButton extends Button {
    public ShadowsButton() {}

    @Override
    public void clicked() {
        Settings.shadowsEnabled = !Settings.shadowsEnabled;
        AudioController.playButtonSound();
    }
}
