package org.conspiracraft.gui.buttons;

import org.conspiracraft.Settings;
import org.conspiracraft.audio.AudioController;

public class ReflectionsButton extends Button {
    public ReflectionsButton() {}

    @Override
    public void clicked() {
        Settings.reflectionsEnabled = !Settings.reflectionsEnabled;
        AudioController.playButtonSound();
    }
}
