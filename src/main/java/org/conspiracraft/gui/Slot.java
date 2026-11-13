package org.conspiracraft.gui;

import kotlin.Pair;
import org.conspiracraft.Main;
import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.ItemTag;
import org.conspiracraft.items.types.ItemTypes;
import org.conspiracraft.world.World;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.conspiracraft.graphics.Renderer.pushUBO;
import static org.conspiracraft.gui.GUI.*;
import static org.conspiracraft.gui.GUI.guiScaleMul;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_LSHIFT;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_Q;

public class Slot extends GUIElement {
    public ItemTag[] tags = null;
    public int offsetX = 0, offsetY = 0;
    public int id = 0;
    long prevLClick = 0;
    public Slot() {}

    public void id(int id) {this.id = id;}
    public Slot setTags(ItemTag[] tags) {this.tags = tags; return this;}
    public Slot setOffset(int x, int y) {this.offsetX = x; this.offsetY = y; return this;}

    @Override
    public void tick(Menu menu) {
        float offX = GUI.cursorPos.x()-(offsetX*GUI.guiScaleMul);
        float offY = GUI.cursorPos.y()-(offsetY*GUI.guiScaleMul);
        if (offX > globalPos.x() && offY > globalPos.y() && offX < globalPos.x()+size.x() && offY < globalPos.y()+size.y()) {
            if (GUI.canInteract) {
                menu.selectedSlot = this;
                if (GUI.menus.getLast() != menu) {return;}
                success:
                if (Main.player.inv.cursorItem != null && tags != null) {
                    for (ItemTag tag : tags) {
                        if (tag.tagged.contains(Main.player.inv.cursorItem.type())) {
                            break success;
                        }
                    }
                    return;
                }
                Item slotItem = menu.getItem(id);
                if (slotItem != null && slotItem.type.immovable) {
                    return;
                }
                boolean slotReal = menu.items != null && id < menu.items.length;
                if (Main.player.inputHandler.keyRelease(SDL_SCANCODE_Q)) {
                    if (slotItem != null) {
                        World.dropItem(slotItem);
                        menu.setItem(id, null);
                    }
                } else if (Main.player.inputHandler.leftButtonPressed) {
                    if (!menu.selectedSlotsL.contains(this) && Main.player.inv.cursorItem != null && Main.player.inv.cursorItem.amount >= menu.selectedSlotsL.size()) {
                        menu.selectedSlotsL.add(this);
                    }
                } else if (Main.player.inputHandler.leftButtonClick) {
                    if (slotReal) {
                        if (Main.timeMsLong - prevLClick < 200) { //double click
                            if (Main.player.inv.cursorItem == null && slotItem != null) { //pickup item hovering over if none is being carried by cursor
                                Main.player.inv.cursorItem = slotItem.clone();
                                menu.setItem(id, null);
                            }
                            if (Main.player.inv.cursorItem != null) {
                                List<Pair<Integer, Item>> sortedItems = new ArrayList<>();
                                for (int i = 0; i < menu.items.length; i++) {
                                    Item item = menu.getItem(i);
                                    if (item != null && item.type == Main.player.inv.cursorItem.type) {
                                        sortedItems.addLast(new Pair<>(i, item));
                                    }
                                }
                                sortedItems.sort(Comparator.comparingInt(pair -> pair.component2().amount));
                                for (Pair<Integer, Item> pair : sortedItems) {
                                    Item item = pair.component2();
                                    if (Main.player.inv.cursorItem.amount >= Main.player.inv.cursorItem.type.maxStackSize) {
                                        break;
                                    }
                                    int space = Main.player.inv.cursorItem.type.maxStackSize - Main.player.inv.cursorItem.amount;
                                    int move = Math.min(space, item.amount);
                                    item.amount -= move;
                                    Main.player.inv.cursorItem.amount += move;
                                    if (item.amount <= 0) {
                                        menu.setItem(pair.component1(), null);
                                    }
                                }
                            }
                        } else if (Main.player.inputHandler.isKeyDown(SDL_SCANCODE_LSHIFT) && slotItem != null) {
                            if (menu == Main.player.inv.menu) {
                                if (Main.player.inv.containerMenu != null) {
                                    menu.setItem(id, Main.player.inv.containerMenu.addItem(slotItem));
                                }
                            } else {
                                menu.setItem(id, Main.player.inv.menu.addItem(slotItem));
                            }
                        } else if (Main.player.inv.cursorItem == null) {
                            if (slotItem != null) {
                                Main.player.inv.cursorItem = slotItem.clone();
                                menu.setItem(id, null);
                            }
                        } else if (slotItem == null || slotItem.amount <= 0) {
                            menu.setItem(id, Main.player.inv.cursorItem.clone());
                            Main.player.inv.cursorItem = null;
                        } else if (slotItem.type == Main.player.inv.cursorItem.type) {
                            int transferAmt = Math.min(Main.player.inv.cursorItem.amount, slotItem.type.maxStackSize - slotItem.amount);
                            Main.player.inv.cursorItem.amount -= transferAmt;
                            slotItem.amount += transferAmt;
                            if (Main.player.inv.cursorItem.amount <= 0) {
                                Main.player.inv.cursorItem = null;
                            }
                        } else {
                            Item newCursorItem = slotItem.clone();
                            menu.setItem(id, Main.player.inv.cursorItem.clone());
                            Main.player.inv.cursorItem = newCursorItem;
                        }
                        prevLClick = Main.timeMsLong;
                    }
                } else if (Main.player.inputHandler.rightButtonPressed) {
                    if (!menu.selectedSlotsR.contains(this) && Main.player.inv.cursorItem != null && Main.player.inv.cursorItem.amount >= menu.selectedSlotsR.size()) {
                        menu.selectedSlotsR.add(this);
                    }
                } else if (Main.player.inputHandler.rightButtonClick) {
                    if (slotReal) {
                        if (Main.player.inv.cursorItem == null) {
                            if (slotItem != null) {
                                int transferAmt = slotItem.amount / 2;
                                Main.player.inv.cursorItem = slotItem.clone();
                                Main.player.inv.cursorItem.amount = transferAmt;
                                slotItem.amount -= transferAmt;
                                if (slotItem.amount <= 0) {
                                    menu.setItem(id, null);
                                }
                            }
                        } else if (slotItem == null || slotItem.amount <= 0) {
                            slotItem = Main.player.inv.cursorItem.clone();
                            menu.setItem(id, slotItem);
                            slotItem.amount = 1;
                            Main.player.inv.cursorItem.amount -= 1;
                            if (Main.player.inv.cursorItem.amount <= 0) {
                                Main.player.inv.cursorItem = null;
                            }
                        } else if (slotItem.type == Main.player.inv.cursorItem.type) {
                            int transferAmt = Math.min(1, slotItem.type.maxStackSize - slotItem.amount);
                            Main.player.inv.cursorItem.amount -= transferAmt;
                            slotItem.amount += transferAmt;
                            if (Main.player.inv.cursorItem.amount <= 0) {
                                Main.player.inv.cursorItem = null;
                            }
                        } else {
                            Item newCursorItem = slotItem.clone();
                            menu.setItem(id, Main.player.inv.cursorItem.clone());
                            Main.player.inv.cursorItem = newCursorItem;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void draw(Menu menu) {
        pushUBO.updateTex(Textures.gui);
        pushUBO.updateLayer(1); //inventory
        pushUBO.updateAtlasOffset(new Vector2i(0, 0));
        if (!menu.hideSlots) {
            menu.menuColor(color);
            menu.drawQuad(posRaw.x(), posRaw.y(), slotSize, slotSize, 1);
        }
        color.set(1);
        if (menu.items != null && id < menu.items.length) {
            menu.drawItem(menu.getItem(id), posRaw.x() + 2, posRaw.y()+2);
        }

        if (!dragging) {
            pushUBO.updateTex(Textures.gui);
            pushUBO.updateLayer(2); //selector
            pushUBO.updateAtlasOffset(new Vector2i());
            if (menu.selectedSlot == this) {
                menu.drawQuad(posRaw.x() - 1, posRaw.y() - 1, enlargedSlotSize, enlargedSlotSize, 0.9f, 1);
                if (id < menu.items.length && inventoryOpen) {
                    Item item = menu.getItem(id);
                    if (item != null && item.type != ItemTypes.AIR) {
                        int x = (int) ((cursorPxX() - menu.menuPos.x()) / guiScaleMul), y = (int) ((cursorPxY() - menu.menuPos.y() + (enlargedSlotSize * 0.5f)) / guiScaleMul);
                        menu.drawItemHoverDetails(x, y, item);
                    }
                }
            } else if (menu.selectedSlotsL.contains(this) || menu.selectedSlotsR.contains(this)) {
                menu.drawQuad(posRaw.x() - 1, posRaw.y() - 2, enlargedSlotSize, enlargedSlotSize, 0.9f, 1);
            }
        }
    }
}