package org.conspiracraft.gui;

import org.conspiracraft.Main;
import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.items.types.ItemTypes;
import org.joml.Vector2i;

import static org.conspiracraft.graphics.Renderer.pushUBO;
import static org.conspiracraft.gui.GUI.color;

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

    @Override
    public void draw(Menu menu) {
        color.set(1);
        pushUBO.updateTex(Textures.gui);
        pushUBO.updateLayer(4);
        pushUBO.updateAtlasOffset(new Vector2i(0, 0));
        pushUBO.updateSize(new Vector2i(ItemTypes.itemTexSize));
        menu.drawQuad(posRaw.x(), posRaw.y(), 12, 12, 1);
    }
}