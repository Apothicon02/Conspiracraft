package org.conspiracraft.gui.buttons;

import org.conspiracraft.Settings;
import org.conspiracraft.audio.AudioController;
import org.conspiracraft.gui.Languages;
import org.lwjgl.openal.AL10;

public class LanguageButton extends Button {
    public LanguageButton() {}

    @Override
    public void clicked() {
        Settings.language++;
        if (Settings.language >= Languages.languages.size()) {
            Settings.language = 0;
        }
    }
}
