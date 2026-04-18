package org.conspiracraft.gui.buttons;

import org.conspiracraft.audio.AudioController;
import org.conspiracraft.gui.GUI;

public class GraphicsSettingsButton extends Button {
    public GraphicsSettingsButton() {}

    @Override
    public void clicked() {
        GUI.graphicsSettingMenuOpen = true;
        AudioController.playButtonSound();
    }
}
