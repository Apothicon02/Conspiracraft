package org.conspiracraft.gui;

import org.conspiracraft.Main;
import org.joml.Vector2i;

public class Focuser extends GUIElement {
    public Focuser() {}

    @Override
    public void tick(Menu menu) {
        if (GUI.cursorPos.x() > globalPos.x() && GUI.cursorPos.y() > globalPos.y() && GUI.cursorPos.x() < globalPos.x()+size.x() && GUI.cursorPos.y() < globalPos.y()+size.y() && !GUI.dragging && Main.player.inputHandler.mousePressed) {
            Focuser focuser = GUI.menus.getLast().focuser;
            Vector2i focuserPos = focuser.globalPos;
            if (!(GUI.cursorPos.x() > focuserPos.x() && GUI.cursorPos.y() > focuserPos.y() && GUI.cursorPos.x() < focuserPos.x()+focuser.size.x() && GUI.cursorPos.y() < focuserPos.y()+focuser.size.y())) {
                if (GUI.moveUpfrontMenusSet.add(menu)) {
                    GUI.moveUpfrontMenus.add(menu);
                }
            }
        }
    }
}