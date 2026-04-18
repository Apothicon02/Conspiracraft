package org.conspiracraft.gui.buttons;

import org.conspiracraft.Main;
import org.conspiracraft.audio.AudioController;

public class SaveWorldButton extends Button {
    public SaveWorldButton() {}

    @Override
    public void clicked() {
        Main.isSaving = true;
        AudioController.playButtonSound();
    }
}
