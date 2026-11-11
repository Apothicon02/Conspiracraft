package org.conspiracraft.gui;

import org.conspiracraft.Main;
import org.conspiracraft.Settings;
import org.joml.Vector2i;

public class Dragger extends GUIElement {
    Vector2i prevPos = new Vector2i();
    public Dragger() {}

    @Override
    public void tick(Menu menu) {
        if (GUI.cursorPos.x() > globalPos.x() && GUI.cursorPos.y() > globalPos.y() && GUI.cursorPos.x() < globalPos.x()+size.x() && GUI.cursorPos.y() < globalPos.y()+size.y()) {
            if (GUI.canInteract && !GUI.dragging && Main.player.inputHandler.leftButtonPressed && prevPos.x() < 0) {
                prevPos.set(GUI.cursorPos);
                GUI.dragging = true;
            }
        }
        if (!Main.player.inputHandler.leftButtonPressed) {prevPos.set(-1); GUI.dragging = false;} else if (prevPos.x() >= 0) {
            if (prevPos.x() >= 0) {
                menu.setPos(menu.menuPosRaw.x() + ((float)(GUI.cursorPos.x()-prevPos.x())/Settings.width), menu.menuPosRaw.y() + ((float)(GUI.cursorPos.y()-prevPos.y())/Settings.height));
            }
            prevPos.set(GUI.cursorPos);
        }
    }
}