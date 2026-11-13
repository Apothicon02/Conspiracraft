package org.conspiracraft.gui;

import org.conspiracraft.Main;
import org.conspiracraft.graphics.textures.Texture;
import org.conspiracraft.graphics.textures.Textures;
import org.joml.Vector2i;

import static org.conspiracraft.graphics.Renderer.pushUBO;
import static org.conspiracraft.gui.GUI.color;
import static org.conspiracraft.gui.GUI.enlargedSlotSize;

public class MenuButtonElement extends ButtonElement {
    public Menu controlledMenu = null;
    public MenuButtonElement() {}

    public MenuButtonElement setMenu(Menu controlledMenu) {this.controlledMenu = controlledMenu; return this;}

    @Override
    public void tick(Menu menu) {
        hoveredOver = false;
        if (GUI.cursorPos.x() > globalPos.x() && GUI.cursorPos.y() > globalPos.y() && GUI.cursorPos.x() < globalPos.x()+size.x() && GUI.cursorPos.y() < globalPos.y()+size.y() && GUI.canInteract) {
            if (Main.player.inputHandler.leftButtonClick) {
                if (!GUI.menus.contains(controlledMenu)) {
                    if (GUI.addMenusSet.add(controlledMenu)) {
                        GUI.addMenus.add(controlledMenu);
                    }
                } else if (GUI.removeMenusSet.add(controlledMenu)) {
                    GUI.removeMenus.add(controlledMenu);
                }
                if (GUI.removeMenusSet.add(menu)) {
                    GUI.removeMenus.add(menu);
                }
            }
            hoveredOver = true;
        }
    }
}
