package org.conspiracraft.gui;

import org.conspiracraft.items.Item;
import org.conspiracraft.items.types.ItemTypes;
import org.joml.Vector2i;

import static org.conspiracraft.gui.GUI.slotSize;

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
        setItem(0, new Item().type(ItemTypes.STICK).amount(64));
        setItem(1, new Item().type(ItemTypes.PAPER).amount(32));
        setItem(2, new Item().type(ItemTypes.STEEL_HATCHET).amount(1));
    }
}