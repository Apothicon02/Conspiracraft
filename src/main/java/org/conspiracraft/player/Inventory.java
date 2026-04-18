package org.conspiracraft.player;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import kotlin.Pair;
import org.conspiracraft.Window;
import org.conspiracraft.items.*;
import org.conspiracraft.utils.Utils;
import org.joml.Vector2i;
import org.conspiracraft.Main;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.lwjgl.sdl.SDLScancode.*;

public class Inventory {
    public static int invWidth = 14;

    public Item[] items = new Item[invWidth*4];
    public Item cursorItem = null;
    public Vector2i selectedSlot = new Vector2i(0);
    public Vector2i selectedContainerSlot = new Vector2i(0);
    public int prevRMBDeposit = -1;
    public int interactCD = 0;

    public void tick() {
        if (interactCD > 0) {
            interactCD--;
        }
        if (!Main.player.inputHandler.rightButtonPressed) {
            prevRMBDeposit = -1;
        }
        if (interactCD <= 0) {
            Integer selSlotId = selectedSlot == null || selectedSlot.x() < 0 || selectedSlot.y() < 0 ? null : selectedSlot.x+(selectedSlot.y*invWidth);
            Integer containerSlotId = selectedContainerSlot == null || selectedContainerSlot.x() < 0 || selectedContainerSlot.y() < 0 ? null : selectedContainerSlot.x+(selectedContainerSlot.y*invWidth);
            Item selItem = getSelectedItem(true);
            if (cursorItem == null) {
                if (selItem != null) {
                    Item newSelItem = selItem.clone();
                    if (Main.player.inputHandler.leftButtonClick) {
                        cursorItem = newSelItem.clone();
                        newSelItem = null;
                        interactCD = 5;
                    } else if (Main.player.inputHandler.rightButtonClick) {
                        cursorItem = newSelItem.clone();
                        float splitAmt = cursorItem.amount / 2.f;
                        int existAmt = (int) Math.floor(splitAmt);
                        if (existAmt <= 0) {
                            newSelItem = null;
                        } else {
                            newSelItem.amount = existAmt;
                        }
                        cursorItem.amount = (int) Math.ceil(splitAmt);
                        interactCD = 5;
                    }
                    if (selSlotId != null) {
                        setItem(selSlotId, newSelItem);
                        if (Main.player.inputHandler.isKeyDown(SDL_SCANCODE_LEFT) && cursorItem != null) {
                            if (!Main.player.creative || !Main.player.inputHandler.isKeyDown(SDL_SCANCODE_LCTRL)) {
                                addToInventory(cursorItem, selectedSlot.y() > 0);
                            }
                            cursorItem = null;
                        }
                    }
                    if (containerSlotId != null) {
                        if (Main.player.inputHandler.isKeyDown(SDL_SCANCODE_LSHIFT) && cursorItem != null) {
                            addToInventory(cursorItem, true);
                            cursorItem = null;
                        }
                    }
                }
            } else if (Main.player.inputHandler.leftButtonClick) {
                if (selItem != null) {
                    if (cursorItem.type != selItem.type) { //swap item with slot
                        Item oldCursorItem = cursorItem.clone();
                        cursorItem = selItem.clone();
                        if (selSlotId != null) {
                            setItem(selSlotId, oldCursorItem);
                        }
                    } else if (selSlotId != null) { //dump contents into slot
                        if (addToSlot(selSlotId, cursorItem, cursorItem.amount) == null) {
                            cursorItem = null;
                        }
                    } else {
                        //World.dropItem(cursorItem);
                        cursorItem = null;
                    }
                } else if (selSlotId != null) { //dump contents into slot
                    if (addToSlot(selSlotId, cursorItem, cursorItem.amount) == null) {
                        cursorItem = null;
                    }
                } else {
                    if (containerSlotId == null) { //only drop if not over container
                        //World.dropItem(cursorItem);
                    }
                    cursorItem = null;
                }
                interactCD = 5;
            } else if (Main.player.inputHandler.middleButtonClick) {
                ItemType product = Recipes.recipes.get(new Pair<>(cursorItem.type, selItem.type));
                if (product == null) {
                    product = Recipes.recipes.get(new Pair<>(selItem.type, cursorItem.type));
                }
                boolean useCursorItem = true;
                if (product == null) {
                    for (ItemTag tag : cursorItem.type.tags) {
                        if (tag.tagged.contains(cursorItem.type)) {
                            for (ItemTag selTag : selItem.type.tags) {
                                if (selTag.tagged.contains(selItem.type)) {
                                    product = Recipes.tagRecipes.get(new Pair<>(tag, selTag));
                                    if (product != null) {
                                        useCursorItem = false;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                if (product != null) {
                    if (!useCursorItem || selItem.amount <= cursorItem.amount) {
                        selItem.type(product);
                        if (useCursorItem) {
                            cursorItem.amount(cursorItem.amount - selItem.amount);
                            if (cursorItem.amount <= 0) {
                                cursorItem = null;
                            }
                        }
                        selItem.playSound(Main.player.pos);
                    } else {
                        cursorItem.type(product);
                        selItem.amount(selItem.amount-cursorItem.amount);
                        if (selItem.amount <= 0) {
                            selItem = null;
                        }
                        cursorItem.playSound(Main.player.pos);
                    }
                }
                interactCD = 5;
            }
        }
        if (cursorItem != null) {
            if (cursorItem == null || cursorItem.amount <= 0 || cursorItem.type == ItemTypes.AIR) {
                cursorItem = null;
            } else if (interactCD <= 0 && selectedSlot != null) {
                int slotId = selectedSlot.x+(selectedSlot.y*invWidth);
                if (Main.player.inputHandler.leftButtonPressed) { //split evenly across several slots
//                    if (addToSlot(slotId, cursorItem, cursorItem.amount, false) == null) {
//                        cursorItem = null;
//                    }
//                    interactCD = 20;
                } else if (Main.player.inputHandler.rightButtonPressed && prevRMBDeposit != slotId) {
                    if (addToSlot(slotId, cursorItem, 1) == null) {
                        cursorItem = null;
                    }
                    interactCD = 20;
                }
            }
        }
        if (Main.player.inputHandler.scroll.y > 0) {
            scrollUp();
        } else if (Main.player.inputHandler.scroll.y < 0) {
            scrollDown();
        }
    }

    public void init() {
        setItem(0, 0, new Item().type(ItemTypes.STEEL_SCYTHE));
        setItem(1, 0, new Item().type(ItemTypes.STEEL_PICK));
        setItem(2, 0, new Item().type(ItemTypes.STEEL_HATCHET));
        setItem(3, 0, new Item().type(ItemTypes.STEEL_SPADE));
        setItem(4, 0, new Item().type(ItemTypes.STEEL_HOE));
        setItem(12, 0, new Item().type(ItemTypes.APPLE).amount(1));
        setItem(13, 0, new Item().type(ItemTypes.ORANGE).amount(2));
        setItem(13, 1, new Item().type(ItemTypes.ORANGE).amount(1));
        setItem(13, 2, new Item().type(ItemTypes.CHERRY).amount(2));
        setItem(3, 3, new Item().type(ItemTypes.GLASS).amount(37));
        setItem(3, 2, new Item().type(ItemTypes.GLASS).amount(1));
        setItem(2, 3, new Item().type(ItemTypes.STICK).amount(60));
        setItem(1, 3, new Item().type(ItemTypes.OAK_LOG).amount(54));
        setItem(0, 3, new Item().type(ItemTypes.STONE).amount(64));
        setItem(0, 2, new Item().type(ItemTypes.MARBLE).amount(64));
    }

    public static Path invPath = Path.of(Main.mainFolder+"world0/inv.data");

    public void load() throws IOException {
        int[] data = Utils.flipIntArray(Utils.byteArrayToIntArray(new FileInputStream(invPath.toFile()).readAllBytes()));
        int slot = 0;
        for (int i = 0; i < data.length;) {
            int itemDataLength = data[i++];
            if (itemDataLength > 0) {
                items[slot++] = Item.load(data, i);
                i += itemDataLength;
            } else {
                slot++;
            }
        }
    }
    public void save() throws IOException {
        IntArrayList data = new IntArrayList();
        int i = 0;
        for (Item item : items) {
            if (item == null) {
                data.add(i, 0);
            } else {
                int[] itemData = item.getData();
                data.addElements(i, itemData);
                i += itemData[0];
            }
            i++;
        }

        FileOutputStream out = new FileOutputStream(invPath.toFile());
        out.write(Utils.intArrayToByteArray(data.toIntArray()));
        out.close();
    }

    public Item getSelectedItem(boolean ignoreCursorItem) {
        if (!ignoreCursorItem && cursorItem != null) {
            return cursorItem;
        } else if (selectedSlot != null) {
            return getItem(selectedSlot);
        } else if (selectedContainerSlot != null) {
            return getContainerItem(selectedContainerSlot);
        }
        return null;
    }

    public Item getContainerItem(Vector2i xy) {
        return xy == null ? null : getContainerItem((xy.y*invWidth)+xy.x);
    }
    public Item getContainerItem(int index) {
        ItemType type = ItemTypes.itemTypeMap.get(index);
        return type == null ? null : type.createItem().amount(type.maxStackSize);
    }
    public Item getItem(int index) {
        return items[index];
    }
    public Item getItem(int x, int y) {
        return getItem((y*invWidth)+x);
    }
    public Item getItem(Vector2i xy) {
        return xy == null ? null : getItem((xy.y*invWidth)+xy.x);
    }
    public void setItem(int slotId, Item item) {
        Item existing = items[slotId];
        if (item != null) {
            if (existing == null || item.type != existing.type || item.amount != existing.amount) {
                item.playSound(Main.player.pos);
            }
            item.prevTickTime(Main.timeMsLong);
        } else if (existing != null) {
            existing.playSound(Main.player.pos);
        }
        items[slotId] = item;
    }
    public void setItem(Vector2i xy, Item item) {
        setItem((xy.y*invWidth)+xy.x, item);
    }
    public void setItem(int x, int y, Item item) {
        setItem((y*invWidth)+x, item);
    }

    public void addToInventory(ArrayList<Item> items) {
        if (items != null && !items.isEmpty()) {
            for (Item item : items) {
                addToInventory(item, false);
            }
        }
    }

    public Item addToInventory(Item item, boolean hotbarFirst) {
        loop:
        for (int y = hotbarFirst ? 0 : 3; hotbarFirst ? (y < 4) : (y >= 0); y += (hotbarFirst ? 1 : -1)) { //first try merging with existing stacks
            for (int x = 0; x < invWidth; x++) {
                int i = (y*invWidth)+x;
                Item slotItem = getItem(i);
                if (slotItem != null && slotItem.type == item.type) {
                    item = addToSlot(i, item, item.amount);
                    if (item == null) {
                        break loop;
                    }
                }
            }
        }
        if (item != null) {
            loop:
            for (int y = hotbarFirst ? 0 : 3; hotbarFirst ? (y < 4) : (y >= 0); y += (hotbarFirst ? 1 : -1)) { //then try adding to an empty slot
                for (int x = 0; x < invWidth; x++) {
                    int i = (y*invWidth)+x;
                    Item slotItem = getItem(i);
                    if (slotItem == null || slotItem.type == ItemTypes.AIR) {
                        setItem(i, item.clone());
                        item = null;
                        break loop;
                    }
                }
            }
        }
        return item;
    }

    public Item addToSlot(int existingId, Item item, int amount) {
        Item existing = getItem(existingId);
        if (existing == null || existing.amount <= 0 || existing.type == ItemTypes.AIR) {
            existing = item.clone();
            existing.amount = amount;
            item.amount -= amount;
        } else if (existing.type == item.type && existing.amount < existing.type.maxStackSize) {
            existing = existing.clone();
            int space = Math.min(amount, Math.min(item.amount, existing.type.maxStackSize - existing.amount));
            if (space > 0) {
                existing.amount += space;
                item.amount -= space;
            }
        }
        if (item.amount <= 0) {
            item = null;
        }
        setItem(existingId, existing);
        return item;
    }

    public void scrollUp() {
        Item[] newItems = new Item[items.length];
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < invWidth; x++) {
                int row = y+1;
                if (row >= 4) {
                    row = 0;
                }
                newItems[(y*invWidth)+x] = getItem(x, row);
            }
        }
        items = newItems;
    }
    public void scrollDown() {
        Item[] newItems = new Item[items.length];
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < invWidth; x++) {
                int row = y-1;
                if (row < 0) {
                    row = 3;
                }
                newItems[(y*invWidth)+x] = getItem(x, row);
            }
        }
        items = newItems;
    }
}
