package org.conspiracraft.gui;

import kotlin.Pair;
import org.conspiracraft.Main;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.types.ItemTypes;
import org.conspiracraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_LSHIFT;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_Q;

public class Slot extends GUIElement {
    int id = 0;
    long prevLClick = 0;
    public Slot() {}

    public void id(int id) {this.id = id;}

    @Override
    public void tick(Menu menu) {
        if (GUI.cursorPos.x() > globalPos.x() && GUI.cursorPos.y() > globalPos.y() && GUI.cursorPos.x() < globalPos.x()+size.x() && GUI.cursorPos.y() < globalPos.y()+size.y()) {
            if (GUI.canInteract) {
                menu.selectedSlot = this;
                boolean slotReal = menu.items != null && id < menu.items.length;
                if (Main.player.inputHandler.keyRelease(SDL_SCANCODE_Q)) {
                    if (menu.getItem(id) != null) {
                        World.dropItem(menu.getItem(id));
                        menu.setItem(id, null);
                    }
                } else if (Main.player.inputHandler.leftButtonPressed) {
                    if (!menu.selectedSlotsL.contains(this) && Main.player.inv.cursorItem != null && Main.player.inv.cursorItem.amount >= menu.selectedSlotsL.size()) {
                        menu.selectedSlotsL.add(this);
                    }
                } else if (Main.player.inputHandler.leftButtonClick) {
                    if (slotReal) {
                        if (Main.timeMsLong - prevLClick < 200) { //double click
                            if (Main.player.inv.cursorItem == null) { //pickup item hovering over if none is being carried by cursor
                                Main.player.inv.cursorItem = menu.getItem(id).clone();
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
                        } else if (Main.player.inputHandler.isKeyDown(SDL_SCANCODE_LSHIFT) && menu.getItem(id) != null) {
                            if (menu == Main.player.inv.menu) {
                                if (Main.player.inv.containerMenu != null) {
                                    menu.setItem(id, Main.player.inv.containerMenu.addItem(menu.getItem(id)));
                                }
                            } else {
                                menu.setItem(id, Main.player.inv.menu.addItem(menu.getItem(id)));
                            }
                        } else if (Main.player.inv.cursorItem == null) {
                            if (menu.getItem(id) != null) {
                                Main.player.inv.cursorItem = menu.getItem(id).clone();
                                menu.setItem(id, null);
                            }
                        } else if (menu.getItem(id) == null || menu.getItem(id).amount <= 0) {
                            menu.setItem(id, Main.player.inv.cursorItem.clone());
                            Main.player.inv.cursorItem = null;
                        } else if (menu.getItem(id).type == Main.player.inv.cursorItem.type) {
                            int transferAmt = Math.min(Main.player.inv.cursorItem.amount, menu.getItem(id).type.maxStackSize - menu.getItem(id).amount);
                            Main.player.inv.cursorItem.amount -= transferAmt;
                            menu.getItem(id).amount += transferAmt;
                            if (Main.player.inv.cursorItem.amount <= 0) {
                                Main.player.inv.cursorItem = null;
                            }
                        } else {
                            Item newCursorItem = menu.getItem(id).clone();
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
                            if (menu.getItem(id) != null) {
                                int transferAmt = menu.getItem(id).amount / 2;
                                Main.player.inv.cursorItem = menu.getItem(id).clone();
                                Main.player.inv.cursorItem.amount = transferAmt;
                                menu.getItem(id).amount -= transferAmt;
                                if (menu.getItem(id).amount <= 0) {
                                    menu.setItem(id, null);
                                }
                            }
                        } else if (menu.getItem(id) == null || menu.getItem(id).amount <= 0) {
                            menu.setItem(id, Main.player.inv.cursorItem.clone());
                            menu.getItem(id).amount = 1;
                            Main.player.inv.cursorItem.amount -= 1;
                            if (Main.player.inv.cursorItem.amount <= 0) {
                                Main.player.inv.cursorItem = null;
                            }
                        } else if (menu.getItem(id).type == Main.player.inv.cursorItem.type) {
                            int transferAmt = Math.min(1, menu.getItem(id).type.maxStackSize - menu.getItem(id).amount);
                            Main.player.inv.cursorItem.amount -= transferAmt;
                            menu.getItem(id).amount += transferAmt;
                            if (Main.player.inv.cursorItem.amount <= 0) {
                                Main.player.inv.cursorItem = null;
                            }
                        } else {
                            Item newCursorItem = menu.getItem(id).clone();
                            menu.setItem(id, Main.player.inv.cursorItem.clone());
                            Main.player.inv.cursorItem = newCursorItem;
                        }
                    }
                }
            }
        }
    }
}