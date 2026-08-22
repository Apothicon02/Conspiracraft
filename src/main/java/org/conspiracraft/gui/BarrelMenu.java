package org.conspiracraft.gui;

import org.conspiracraft.graphics.textures.Textures;
import org.joml.Vector2i;

import static org.conspiracraft.graphics.Renderer.pushUBO;
import static org.conspiracraft.gui.GUI.hotbarSizeX;
import static org.conspiracraft.gui.GUI.hotbarSizeY;
import static org.conspiracraft.gui.GUI.color;
import static org.conspiracraft.gui.GUI.enlargedSlotSize;

public class BarrelMenu extends Menu {
    public int slotsPerRow = 14;
    public int rows = 4;
    public BarrelMenu() {
        this.menuSizeRaw = new Vector2i(hotbarSizeX, hotbarSizeY*4);
        for (int i = 0; i < slotsPerRow*rows; i++) {
            addSlot(new Slot().setPos(GUI.slotSize*(i%slotsPerRow), 1+(GUI.slotSizeY*(i/slotsPerRow))));
        }
    }

    @Override
    public void tick() {
        selectedSlot = null;
        for (GUIElement element : elements) {element.tick(this);}
    }
    @Override
    public void draw() {
        pushUBO.updateTex(Textures.gui); //use gui atlas
        pushUBO.updateLayer(1); //inventory
        pushUBO.updateAtlasOffset(new Vector2i(0));
        color.set(1);
        drawQuad(0, 0, hotbarSizeX, hotbarSizeY);
        drawQuad(0, hotbarSizeY, hotbarSizeX, hotbarSizeY);
        drawQuad(0, (hotbarSizeY * 2), hotbarSizeX, hotbarSizeY);
        drawQuad(0, (hotbarSizeY * 3), hotbarSizeX, hotbarSizeY);
        if (selectedSlot != null) {
            pushUBO.updateLayer(2); //selector
            pushUBO.updateAtlasOffset(new Vector2i());
            drawSlot(0, 0, selectedSlot.posRaw.x(), selectedSlot.posRaw.y() - 0.5f, 0, 0, enlargedSlotSize, enlargedSlotSize);
        }
        pushUBO.updateLayer(0);
        drawText(false, 0, (hotbarSizeY * 4) + 3, 0, 0, name.toCharArray());
    }
}