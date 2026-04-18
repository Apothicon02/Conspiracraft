package org.conspiracraft.gui.sliders;

import org.conspiracraft.Main;
import org.conspiracraft.Settings;
import org.conspiracraft.audio.AudioController;

public class FoVSlider extends Slider {
    public FoVSlider() {}

    @Override
    public void clicked(int cursorX) {
        float relX = Math.abs(((float) (bounds.x()-cursorX)) / (bounds.z()-bounds.x()));
        if (relX < 0.01f) {relX = 0.f;}
        if (relX > 0.495f && relX < 0.505f) {relX = 0.5f;}
        if (relX > 0.99f) {relX = 1.f;}
        Settings.fov = (relX*150)+30;
        Main.player.camera.FOV = Settings.fov;
        AudioController.playSliderSound();
    }
}
