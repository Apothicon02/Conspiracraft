package org.conspiracraft.gui.menus;

import org.conspiracraft.gui.Menu;
import org.conspiracraft.gui.Slot;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.types.ItemTypes;
import org.joml.Vector2i;

public class HealthMenu extends Menu {
    public static final int menuSlotSize = 13;
    public int slotAmt = 10;
    public HealthMenu() {
        this.menuSizeRaw = new Vector2i(menuSlotSize*slotAmt, menuSlotSize);
        for (int i = 0; i < slotAmt; i++) {
            addSlot(new Slot().setPos(menuSlotSize*i, 0));
        }
        this.items = new Item[slotAmt];
        for (int i = 0; i < slotAmt; i++) {
            setItem(i, ItemTypes.HEART.createItem());
        }
        init();
    }
}