package org.conspiracraft.gui;

public class Slot extends GUIElement {
    int id = 0;
    public Slot() {}

    public void id(int id) {this.id = id;}

    @Override
    public void tick(Menu menu) {
        if (GUI.cursorPos.x() > globalPos.x() && GUI.cursorPos.y() > globalPos.y() && GUI.cursorPos.x() < globalPos.x()+size.x() && GUI.cursorPos.y() < globalPos.y()+size.y()) {
            menu.selectedSlot = this;
        }
    }
}