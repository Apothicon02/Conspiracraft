package org.conspiracraft.gui;

import org.conspiracraft.Main;

public class Close extends GUIElement {
    public Close() {}

    @Override
    public void tick(Menu menu) {
        if (GUI.cursorPos.x() > globalPos.x() && GUI.cursorPos.y() > globalPos.y() && GUI.cursorPos.x() < globalPos.x()+size.x() && GUI.cursorPos.y() < globalPos.y()+size.y() && GUI.canInteract) {
            if (Main.player.inputHandler.leftButtonClick && GUI.removeMenusSet.add(menu)) {
                GUI.removeMenus.add(menu);
            }
        }
    }
}