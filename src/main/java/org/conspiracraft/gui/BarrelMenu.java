package org.conspiracraft.gui;

import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.types.ItemTypes;
import org.joml.Vector2i;

import static org.conspiracraft.graphics.Renderer.pushUBO;
import static org.conspiracraft.gui.GUI.slotSize;
import static org.conspiracraft.gui.GUI.color;

public class BarrelMenu extends Menu {
    public int slotsPerRow = 14;
    public int rows = 4;
    public int slotAmt = slotsPerRow*rows;
    public BarrelMenu() {
        this.menuSizeRaw = new Vector2i(slotSize*slotsPerRow, slotSize*rows);
        for (int i = 0; i < slotAmt; i++) {
            addSlot(new Slot().setPos(GUI.slotSize*(i%slotsPerRow), (GUI.slotSize*(i/slotsPerRow))));
        }
        this.items = new Item[slotAmt];
        this.items[0] = new Item().type(ItemTypes.STICK).amount(64);
        this.items[1] = new Item().type(ItemTypes.PAPER).amount(32);
        this.items[2] = new Item().type(ItemTypes.STEEL_HATCHET).amount(1);
    }

    @Override
    public void draw() {
        drawBase();
        color.set(1);
        pushUBO.updateTex(Textures.gui);
        pushUBO.updateLayer(0);
        pushUBO.updateAtlasOffset(new Vector2i());
        drawText(false, 0, menuSizeRaw.y() + 3, 0, 0, name.toCharArray(), 1);
    }
}