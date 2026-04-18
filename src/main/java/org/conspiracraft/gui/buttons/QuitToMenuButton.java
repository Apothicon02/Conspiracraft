package org.conspiracraft.gui.buttons;

import org.conspiracraft.audio.AudioController;

public class QuitToMenuButton extends Button {
    public QuitToMenuButton() {}

    @Override
    public void clicked() {
        AudioController.playButtonSound();
    }
}
