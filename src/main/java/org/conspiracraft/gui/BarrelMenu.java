package org.conspiracraft.gui;

import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.types.ItemTypes;
import org.joml.Vector2i;

import static org.conspiracraft.graphics.Renderer.pushUBO;
import static org.conspiracraft.gui.GUI.hotbarSizeX;
import static org.conspiracraft.gui.GUI.hotbarSizeY;
import static org.conspiracraft.gui.GUI.color;
import static org.conspiracraft.gui.GUI.enlargedSlotSize;

public class BarrelMenu extends Menu {
    public int slotsPerRow = 14;
    public int rows = 4;
    public int slotAmt = slotsPerRow*rows;
    public BarrelMenu() {
        this.menuSizeRaw = new Vector2i(hotbarSizeX, hotbarSizeY*4);
        for (int i = 0; i < slotAmt; i++) {
            addSlot(new Slot().setPos(GUI.slotSize*(i%slotsPerRow), 1+(GUI.slotSizeY*(i/slotsPerRow))));
        }
        this.items = new Item[slotAmt];
        this.items[0] = new Item().type(ItemTypes.STICK).amount(64);
        this.items[1] = new Item().type(ItemTypes.PAPER).amount(32);
        this.items[2] = new Item().type(ItemTypes.STEEL_HATCHET).amount(1);
    }

    @Override
    public void draw() {
        pushUBO.updateTex(Textures.gui);
        pushUBO.updateLayer(1); //inventory
        pushUBO.updateAtlasOffset(new Vector2i(0));
        menuColor(color);
        drawQuad(0, 0, hotbarSizeX, hotbarSizeY, 1);
        drawQuad(0, hotbarSizeY, hotbarSizeX, hotbarSizeY, 1);
        drawQuad(0, (hotbarSizeY * 2), hotbarSizeX, hotbarSizeY, 1);
        drawQuad(0, (hotbarSizeY * 3), hotbarSizeX, hotbarSizeY, 1);
        color.set(1);
        for (Slot slot : slots) {
            if (items != null && slot.id < items.length) {
                drawItem(items[slot.id], slot.posRaw.x() + 3, slot.posRaw.y() + 2);
            }
            if (selectedSlotsL.contains(slot) || selectedSlotsR.contains(slot)) {
                pushUBO.updateTex(Textures.gui);
                pushUBO.updateLayer(2); //selector
                pushUBO.updateAtlasOffset(new Vector2i());
                drawSlot(0, 0, slot.posRaw.x(), slot.posRaw.y() - 0.5f, 0, 0, enlargedSlotSize, enlargedSlotSize, 1);
            }
        }
        pushUBO.updateTex(Textures.gui);
        pushUBO.updateLayer(2); //selector
        pushUBO.updateAtlasOffset(new Vector2i());
        if (selectedSlot != null) {
            drawSlot(0, 0, selectedSlot.posRaw.x(), selectedSlot.posRaw.y() - 0.5f, 0, 0, enlargedSlotSize, enlargedSlotSize, 1);
            if (selectedSlot.id < items.length) {
                Item item = items[selectedSlot.id];
                if (item != null && item.type != ItemTypes.AIR) {
                    drawItemHoverDetails(selectedSlot.posRaw.x() + (GUI.slotSize/2), selectedSlot.posRaw.y() + (GUI.slotSizeY-4), item);
                }
            }
        }
        color.set(1);
        pushUBO.updateTex(Textures.gui);
        pushUBO.updateLayer(0);
        pushUBO.updateAtlasOffset(new Vector2i());
        drawText(false, 0, (hotbarSizeY * 4) + 3, 0, 0, name.toCharArray(), 1);
    }
}