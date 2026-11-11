package org.conspiracraft.gui.menus;

import org.conspiracraft.gui.Menu;
import org.conspiracraft.gui.Slot;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.types.ItemTypes;
import org.joml.Vector2i;

public class ManaMenu extends Menu {
    public static final int menuSlotSize = 6;
    public int slotAmt = 5;
    public ManaMenu() {
        this.menuSizeRaw = new Vector2i(menuSlotSize, menuSlotSize*slotAmt);
        for (int i = 0; i < slotAmt; i++) {
            addSlot(new Slot().setPos(0, menuSlotSize*i));
        }
        this.items = new Item[slotAmt];
        for (int i = 0; i < slotAmt; i++) {
            setItem(i, ItemTypes.ARGON_ICE.createItem());
        }
        init();
    }
}