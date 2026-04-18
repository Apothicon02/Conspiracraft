package org.conspiracraft.gui.buttons;

import org.conspiracraft.Main;
import org.conspiracraft.audio.AudioController;

public class QuitToDesktopButton extends Button {
    public QuitToDesktopButton() {}

    @Override
    public void clicked() {
        Main.isClosing = true;
        AudioController.playButtonSound();
    }
}
