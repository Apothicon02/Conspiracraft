package org.conspiracraft.gui;

import org.conspiracraft.Main;

import java.util.concurrent.atomic.AtomicBoolean;

public class BoolButtonElement extends ButtonElement {
    public AtomicBoolean bool = null;
    public BoolButtonElement() {}

    public BoolButtonElement setBoolean(AtomicBoolean bool) {this.bool = bool; return this;}

    @Override
    public void tick(Menu menu) {
        hoveredOver = false;
        if (GUI.cursorPos.x() > globalPos.x() && GUI.cursorPos.y() > globalPos.y() && GUI.cursorPos.x() < globalPos.x()+size.x() && GUI.cursorPos.y() < globalPos.y()+size.y() && GUI.canInteract) {
            if (Main.player.inputHandler.leftButtonClick) {
                bool.set(!bool.get());
            }
            hoveredOver = true;
        }
    }
}
