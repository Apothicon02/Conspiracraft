package org.conspiracraft.player;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.conspiracraft.gui.*;
import org.conspiracraft.items.*;
import org.conspiracraft.items.types.ItemTypes;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.Main;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class Inventory {
    public Item cursorItem = null;
    public CreativeMenu creativeMenu = null;
    public InvMenu menu = null;
    public Menu containerMenu = null;
    public void tick() {
        if (!GUI.inventoryOpen) {
            menu.selectedSlot = menu.slots.get(HandManager.hotbarSlot);
            GUI.menus.remove(containerMenu);
        } else if (!GUI.menus.contains(containerMenu)) {
            GUI.menus.add(containerMenu);
        }
        if (!Main.player.creative || !GUI.inventoryOpen) {
            GUI.menus.remove(creativeMenu);
        } else if (!GUI.menus.contains(creativeMenu)) {
            GUI.menus.add(creativeMenu);
        }
    }

    public void init() {
        creativeMenu = new CreativeMenu();
        creativeMenu.setName("Creative").setPos(0.5f, 0.65f);
        //containerMenu = new BarrelMenu().setName("Barrel").setPos(0.5f, 0.65f);
        menu = new InvMenu();
        menu.setName("Inventory").setPos(0.5f, 0.0075f).setCentered(true, false);
        GUI.menus.add(menu);
    }

    public static Path invPath = Path.of(Main.mainFolder + "player_inv.data");

    public void load() throws IOException {
        FileChannel in = FileChannel.open(invPath, StandardOpenOption.READ);
        MappedByteBuffer data = in.map(FileChannel.MapMode.READ_ONLY, 0, in.size());
        data.order(ByteOrder.BIG_ENDIAN);
        IntBuffer itemsData = data.asIntBuffer();
        int slot = 0;
        while (itemsData.position() < itemsData.capacity()) {
            int itemDataLength = itemsData.get();
            if (itemDataLength > 0) {
                menu.setItem(slot++, ItemTypes.loadItem(itemsData));
            } else {
                slot++;
            }
        }
        Utils.unmap(data);
        in.close();
    }

    public void save() throws IOException {
        IntArrayList itemsData = new IntArrayList();
        int i = 0;
        for (Item item : menu.items) {
            if (item == null) {
                itemsData.add(i, 0);
            } else {
                int[] itemData = item.getData();
                itemsData.addElements(i, itemData);
                i += itemData[0];
            }
            i++;
        }

        FileChannel out = FileChannel.open(invPath, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        MappedByteBuffer data = out.map(FileChannel.MapMode.READ_WRITE, 0, itemsData.size() * 4L);
        data.order(ByteOrder.BIG_ENDIAN);
        data.asIntBuffer().put(itemsData.toIntArray());
        Utils.unmap(data);
        out.close();
    }

    public Item getSelectedItem(boolean ignoreCursorItem) {
        if (!ignoreCursorItem && cursorItem != null) {
            return cursorItem;
        } else {
            return menu.getItem(HandManager.hotbarSlot);
        }
    }
}
