package org.conspiracraft.gui;

import org.conspiracraft.items.Item;
import org.conspiracraft.items.types.ItemTypes;
import org.joml.Vector2i;

import static org.conspiracraft.gui.GUI.slotSize;

public class InvMenu extends Menu {
    public int slotAmt = 14;
    public InvMenu() {
        this.menuSizeRaw = new Vector2i(slotSize*slotAmt, slotSize);
        for (int i = 0; i < slotAmt; i++) {
            addSlot(new Slot().setPos(GUI.slotSize*i, 0));
        }
        this.items = new Item[slotAmt];
        setItem(0, ItemTypes.STEEL_SCYTHE.createItem().amount(1));
        setItem(1, ItemTypes.STEEL_PICK.createItem().amount(1));
        setItem(2, ItemTypes.STEEL_HATCHET.createItem().amount(1));
        setItem(3, ItemTypes.STEEL_SPADE.createItem().amount(1));
    }
}