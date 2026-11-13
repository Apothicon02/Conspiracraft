package org.conspiracraft.gui;

import org.conspiracraft.Main;
import org.conspiracraft.graphics.textures.Texture;
import org.conspiracraft.graphics.textures.Textures;
import org.joml.Vector2i;

import static org.conspiracraft.graphics.Renderer.pushUBO;
import static org.conspiracraft.gui.GUI.color;
import static org.conspiracraft.gui.GUI.enlargedSlotSize;

public class ButtonElement extends GUIElement {
    public int pushLayer = 1;
    public Vector2i atlasOffset = new Vector2i();
    public Vector2i texSize = new Vector2i();
    public Texture tex = Textures.gui;
    public boolean hoveredOver = false;
    public ButtonElement() {}

    public ButtonElement setPushLayer(int pushLayer) {this.pushLayer = pushLayer; return this;}
    public ButtonElement setTex(Texture tex) {this.tex = tex; return this;}
    public ButtonElement setAtlasOffset(Vector2i atlasOffset) {this.atlasOffset.set(atlasOffset); return this;}
    public ButtonElement setTexSize(Vector2i texSize) {this.texSize.set(texSize); return this;}
    public ButtonElement setTexSize(int x, int y) {this.texSize.set(x, y); return this;}
    public ButtonElement setTexSize(int xy) {this.texSize.set(xy, xy); return this;}

    @Override
    public void tick(Menu menu) {
        hoveredOver = false;
        if (GUI.cursorPos.x() > globalPos.x() && GUI.cursorPos.y() > globalPos.y() && GUI.cursorPos.x() < globalPos.x()+size.x() && GUI.cursorPos.y() < globalPos.y()+size.y() && GUI.canInteract) {
            if (Main.player.inputHandler.leftButtonClick) {

            }
            hoveredOver = true;
        }
    }

    @Override
    public void draw(Menu menu) {
        pushUBO.updateTex(Textures.gui);
        pushUBO.updateLayer(1); //inventory
        pushUBO.updateAtlasOffset(new Vector2i(0, 0));
        menu.menuColor(color);
        menu.drawQuad(posRaw.x(), posRaw.y(), sizeRaw.x(), sizeRaw.y(), 1);
        color.set(1);
        if (hoveredOver) {
            pushUBO.updateLayer(2); //selector
            menu.drawQuad(posRaw.x() - 1, posRaw.y() - 1, enlargedSlotSize, enlargedSlotSize, 0.9f, 1);
        }
        pushUBO.updateTex(tex);
//        pushUBO.updateLayer(pushLayer);
        pushUBO.updateAtlasOffset(atlasOffset);
        menu.drawQuad(true, true, posRaw.x()+(sizeRaw.x()/2), posRaw.y()+(sizeRaw.y()/2), texSize.x(), texSize.y(), 1);
    }
}
