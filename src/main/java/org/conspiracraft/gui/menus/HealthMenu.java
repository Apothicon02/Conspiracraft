package org.conspiracraft.gui.menus;

import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.gui.Menu;
import org.conspiracraft.gui.Slot;
import org.conspiracraft.items.DurableItem;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.ItemTags;
import org.conspiracraft.items.types.ItemTypes;
import org.joml.Vector2i;

import static org.conspiracraft.graphics.Renderer.pushUBO;
import static org.conspiracraft.gui.GUI.color;

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
    }

    @Override
    public void draw() {
        pushUBO.updateLayer(0); //items
        for (Slot slot : slots) {
            color.set(1);
            if (items != null) {
                Item item = getItem(slot.id);
                if (item instanceof DurableItem durableItem && ItemTags.armor.tagged.contains(durableItem.type())) {
                    int howMany = durableItem.durability/100;
                    for (int i = howMany; i > 0; i--) {
                        drawItem(item, slot.posRaw.x() + 2, slot.posRaw.y()+2+(i*3), true);
                    }
                }
            }
        }
        drawBase();
    }
}