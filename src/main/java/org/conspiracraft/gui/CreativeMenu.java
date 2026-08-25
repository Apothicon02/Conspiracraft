package org.conspiracraft.gui;

import org.conspiracraft.items.Item;
import org.conspiracraft.items.types.ItemType;
import org.conspiracraft.items.types.ItemTypes;
import org.joml.Vector2i;

import static org.conspiracraft.gui.GUI.slotSize;

public class CreativeMenu extends Menu {
    public int slotsPerRow = 14;
    public int rows = 4;
    public int slotAmt = slotsPerRow*rows;
    public CreativeMenu() {
        this.menuSizeRaw = new Vector2i(slotSize*slotsPerRow, slotSize*rows);
        for (int i = 0; i < slotAmt; i++) {
            addSlot(new Slot().setPos(GUI.slotSize*(i%slotsPerRow), (GUI.slotSize*(i/slotsPerRow))));
        }
        this.items = new Item[slotAmt];
        maxScroll = (ItemTypes.itemTypeMap.size()/slotsPerRow);
    }

    public void tick() {
        if (GUI.inventoryOpen) {
            for (int i = 0; i < items.length; i++) {
                ItemType type = ItemTypes.itemTypeMap.get(1+i+(scroll*slotsPerRow));
                if (type != null) {
                    setItem(i, new Item().type(type).amount(type.maxStackSize));
                } else {
                    setItem(i, null);
                }
            }
            baseTick();
        }
    }
}