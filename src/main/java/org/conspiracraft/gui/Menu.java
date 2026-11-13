package org.conspiracraft.gui;

import kotlin.Pair;
import org.conspiracraft.Main;
import org.conspiracraft.graphics.Renderer;
import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.items.BlueprintItem;
import org.conspiracraft.items.DurableItem;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.Recipes;
import org.conspiracraft.items.types.ItemType;
import org.conspiracraft.items.types.ItemTypes;
import org.conspiracraft.utils.Utils;
import org.joml.*;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

import static org.conspiracraft.Main.player;
import static org.conspiracraft.Settings.height;
import static org.conspiracraft.Settings.width;
import static org.conspiracraft.graphics.Renderer.pushUBO;
import static org.conspiracraft.gui.GUI.*;
import static org.conspiracraft.gui.GUI.enlargedSlotSize;

public class Menu {
    public String name = "Menu";
    public Vector2f menuPosRaw = new Vector2f();
    public Vector2i menuPos = new Vector2i();
    public Vector2i menuSizeRaw = new Vector2i();
    public Vector2f menuSize = new Vector2f();
    public boolean centeredMenuX = true, centeredMenuY = true;
    public ArrayList<Slot> selectedSlotsL = new ArrayList<>();
    public ArrayList<Slot> selectedSlotsR = new ArrayList<>();
    public Slot selectedSlot = null;
    public int scroll = 0, maxScroll = 0;
    public Item[] items = null;
    public boolean unlocksMouse = false;
    public boolean backround = true;
    public boolean hideSlots = false;
    public boolean hideName = false;
    public Menu parent = null;
    public boolean aboveParent = false;
    public boolean rightOfParent = false;
    public boolean leftOfParent = false;
    public boolean insetY = false;
    public boolean insetX = false;
    public Focuser focuser;
    public Menu() {}

    public void init() {
        focuser = (Focuser)new Focuser().setSize(menuSizeRaw.x(), menuSizeRaw.y());

        if (!hideName) {
            GUIElement dragger = new Dragger().setPos(0, menuSizeRaw.y()-12).setSize(menuSizeRaw.x() - 12, 12);
            elements.add(dragger);
            GUIElement close = new Close().setPos(menuSizeRaw.x() - 10, menuSizeRaw.y()-13).setSize(12, 12);
            elements.add(close);
        }
    }

    public Menu setName(String name) {this.name = name; return this;}
    public Menu setParent(Menu menu) {this.parent = menu; return this;}
    public Menu setPos(float posX, float posY) {menuPosRaw.set(posX, posY); return this;}
    public Menu setCentered(boolean centeredMenuX, boolean centeredMenuY) {this.centeredMenuX = centeredMenuX; this.centeredMenuY = centeredMenuY; return this;}
    public Menu disableBackground() {this.backround = false; return this;}
    public Menu hideSlots() {this.hideSlots = true; return this;}
    public Menu hideName() {this.hideName = true; return this;}
    public Menu aboveParent() {this.aboveParent = true; return this;}
    public Menu rightOfParent() {this.rightOfParent = true; return this;}
    public Menu leftOfParent() {this.leftOfParent = true; return this;}
    public Menu insetY() {this.insetY = true; return this;}
    public Menu insetX() {this.insetX = true; return this;}
    public Menu unlocksMouse() {this.unlocksMouse = true; return this;}

    public void menuColor(Vector4f col) {col.set(1, 0.95f, 0.875f, 1);}

    public ArrayList<GUIElement> elements = new ArrayList<>();
    public Vector2i getPos() {return new Vector2i(menuPos);}
    public void update() {
        baseUpdate();
    }
    public void baseUpdate() {
        menuSize.set(menuSizeRaw);//.div(guiScale).mul(1, aspectRatio);
        float x = menuPosRaw.x(), y = menuPosRaw.y();
        if (parent != null) {
            x/=guiScale;
            x+=parent.menuPosRaw.x();
            if (parent.centeredMenuX) {
                x-=(parent.menuSizeRaw.x()*0.5f)/guiScale;
            }
            if (parent.centeredMenuY) {
                y-=(parent.menuSizeRaw.y()*0.5f)/guiScale;
            }
            y/=guiScale;
            y*=aspectRatio;
            y+=parent.menuPosRaw.y();
            if (rightOfParent) {
                x+=parent.menuSize.x()/guiScale;
                if (insetX) {
                    x-=menuSize.x()/guiScale;
                }
            } else if (leftOfParent) {
                x-=(menuSize.x()*3)/guiScale;
            }
            if (aboveParent) {
                y+=(parent.menuSize.y()/guiScale)*aspectRatio;
                if (insetY) {
                    y-=menuSize.y()/guiScale;
                }
            }
        }
        menuPos.set((int)(x*width), (int)(y*height));
        if (centeredMenuX) {menuPos.sub((int)(width*((menuSize.x()*0.5f)/guiScale)), 0);}
        if (centeredMenuY) {menuPos.sub(0, (int)(height*(((menuSize.y()*0.5f)/guiScale)*aspectRatio)));}
        focuser.update(this);
        for (GUIElement element : elements) {element.update(this);}
    }
    public ArrayList<Slot> slots = new ArrayList<>();
    public void addSlot(GUIElement slot) {addSlot((Slot)slot);}
    public void addSlot(Slot slot) {
        slot.id(slots.size());
        slots.add(slot);
        elements.add(slot);
    }
    public Item addItem(Item item) {
        for (int i = 0; i < items.length; i++) { //first try merging with existing stacks
            Item slotItem = getItem(i);
            if (slotItem != null && slotItem.type == item.type) {
                item = addToSlot(i, item, item.amount);
                if (item == null) {
                    break;
                }
            }
        }
        if (item != null) {
            for (int i = 0; i < items.length; i++) {//then try adding to an empty slot
                Item slotItem = getItem(i);
                if (slotItem == null || slotItem.type == ItemTypes.AIR) {
                    setItem(i, item.clone());
                    item = null;
                    break;
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
    public void setItem(int slotId, Item item) {setItem(slotId, item, false);}
    public void setItem(int slotId, Item item, boolean silent) {
        if (Renderer.initialized) {
            Vector3f earPos = new Vector3f(Main.player.pos).add(0, Main.player.eyeHeight, 0);
            Item existing = items[slotId];
            if (item != null) {
                if ((existing == null || item.type != existing.type || item.amount != existing.amount) && !silent) {
                    item.playSound(earPos);
                }
                item.prevTickTime(Main.timeMsLong);
            } else if (existing != null && !silent) {
                existing.playSound(earPos);
            }
        }
        items[slotId] = item;
    }
    public Item getItem(int index) {
        return items[index];
    }
    public void tick() {
        if (GUI.inventoryOpen) {baseTick();}
    }
    public void baseTick() {
        if (player.inputHandler.scroll.y > 0) {
            scroll++;
            if (scroll >= maxScroll) {
                scroll = maxScroll-1;//0;
            }
        } else if (player.inputHandler.scroll.y < 0) {
            scroll--;
            if (scroll < 0) {
                scroll = 0;//maxScroll-1;
            }
        }
        if (!Main.player.inputHandler.leftButtonPressed) {
            if (Main.player.inv.cursorItem != null && selectedSlotsL.size() > 1) {
                int amtPerSlot = Main.player.inv.cursorItem.amount / selectedSlotsL.size();
                int amtPut = 0;
                for (Slot slot : selectedSlotsL) {
                    Item item = getItem(slot.id);
                    if (item == null) {
                        item = Main.player.inv.cursorItem.clone().amount(amtPerSlot);
                        amtPut += amtPerSlot;
                    } else {
                        int prevAmount = item.amount;
                        item.amount = Math.min(item.type.maxStackSize, item.amount + amtPerSlot);
                        amtPut += item.amount - prevAmount;
                    }
                    setItem(slot.id, item);
                }
                Main.player.inv.cursorItem.amount -= amtPut;
                if (Main.player.inv.cursorItem.amount <= 0) {
                    Main.player.inv.cursorItem = null;
                }
                GUI.canInteract = false;
            }
            selectedSlotsL.clear();
        }
        if (!Main.player.inputHandler.rightButtonPressed) {
            if (Main.player.inv.cursorItem != null && selectedSlotsR.size() > 1) {
                int amtPut = 0;
                for (Slot slot : selectedSlotsR) {
                    Item item = getItem(slot.id);
                    if (item == null) {
                        item = Main.player.inv.cursorItem.clone().amount(1);
                        amtPut++;
                    } else {
                        int prevAmount = item.amount;
                        item.amount = Math.min(item.type.maxStackSize, item.amount+1);
                        amtPut += item.amount - prevAmount;
                    }
                    setItem(slot.id, item);
                }
                Main.player.inv.cursorItem.amount -= amtPut;
                if (Main.player.inv.cursorItem.amount <= 0) {
                    Main.player.inv.cursorItem = null;
                }
                GUI.canInteract = false;
            }
            selectedSlotsR.clear();
        }
        selectedSlot = null;
        focuser.tick(this);
        for (GUIElement element : elements) {element.tick(this);}
        GUI.canInteract = true;
    }

    public void draw() {
        drawBase();
    }
    public void drawBase() {
        if (backround) {
            drawBackground(false, true, 0, 0, 0, 0, menuSizeRaw.x(), menuSizeRaw.y());
        }
        if (!hideName) {
            pushUBO.updateTex(null);
            menuColor(color);
            color.set(0.807f, 0.8f, 0.768f, 0.88f);
            drawQuad(0, menuSizeRaw.y()-12, menuSizeRaw.x(), 12, 1);

            color.set(1);
            pushUBO.updateTex(Textures.gui);
            pushUBO.updateLayer(0);
            pushUBO.updateAtlasOffset(new Vector2i());
            drawText(false, -1, menuSizeRaw.y() - 9, 0, 0, name.toCharArray(), 1);
        }
        for (GUIElement element : elements) {
            element.draw(this);
        }
    }

    public void drawBackground(boolean selected, boolean filled, float offX, float offY, float offPxX, float offPxY, int sizeX, int sizeY) {
        drawBackground(selected, filled, offX, offY, offPxX, offPxY, sizeX, sizeY, 0.f);
    }
    public void drawBackground(boolean selected, boolean filled, float offX, float offY, float offPxX, float offPxY, int sizeX, int sizeY, float layer) {
        drawBackground(selected, filled, Math.round(offX + offPxX), Math.round(offY + offPxY), sizeX, sizeY, layer);
    }
    public void drawBackground(boolean selected, boolean filled, int offX, int offY, int sizeX, int sizeY, float layer) {
        if (filled) {
            pushUBO.updateTex(null);
            color.set(0.862f, 0.988f, 0.988f, 1);
            drawQuad(offX, offY, sizeX, sizeY, layer, 1);
        }
        menuColor(color);
        pushUBO.updateTex(Textures.gui);
        pushUBO.updateLayer(selected ? 2 : 1); //selector or inventory
        pushUBO.updateAtlasOffset(new Vector2i(0, 2160-13));
        drawQuad(-3+offX, -3+offY, 13, 13, layer, 1);
        pushUBO.updateAtlasOffset(new Vector2i(3840-13, 2160-13));
        drawQuad(sizeX-10+offX, -3+offY, 13, 13, layer, 1);
        pushUBO.updateAtlasOffset(new Vector2i(0, 22));
        drawQuad(-3+offX, sizeY-10+offY, 13, 13, layer, 1);
        pushUBO.updateAtlasOffset(new Vector2i(3840-13, 22));
        drawQuad(sizeX-10+offX, sizeY-10+offY, 13, 13, layer, 1);
        int horizontalLength = sizeX-20;
        if (horizontalLength > 0) {
            pushUBO.updateAtlasOffset(new Vector2i(13, 2160-3));
            drawQuad(10+offX, -3+offY, horizontalLength, 3, layer, 1);
            pushUBO.updateAtlasOffset(new Vector2i(13, 22));
            drawQuad(10+offX, sizeY+offY, horizontalLength, 3, layer, 1);
        }
        int verticalLength = sizeY-20;
        if (verticalLength > 0) {
            pushUBO.updateAtlasOffset(new Vector2i(0, 35));
            drawQuad(-3+offX, 10+offY, 3, verticalLength, layer, 1);
            pushUBO.updateAtlasOffset(new Vector2i(3840-3, 35));
            drawQuad(sizeX+offX, 10+offY, 3, verticalLength, layer, 1);
        }
    }

    public void drawItemHoverDetails(int offX, int offY, Item item) {
//        pushUBO.updateTex(null); //use no texture
//        color.set(0.015f, 0.023f, 0.027f, 1.f);
        char[] chars = Languages.translate("item/"+item.type.name).toCharArray();
        int detailsWidth = 1+((chars.length+1)*charWidth);
        boolean nameOnly = true;
        if (item instanceof BlueprintItem bpItem) {
            nameOnly = false;
            detailsWidth = Math.max(detailsWidth, GUI.enlargedSlotSize * 4);
            drawBackground(true, true, offX+1, offY+1, enlargedSlotSize * 0.5f, 3 - GUI.enlargedSlotSize, detailsWidth, GUI.enlargedSlotSize + 16, 0.9f);
            //drawSlot(offX, offY, enlargedSlotSize * 0.5f, 3 - GUI.enlargedSlotSize, 0, 0, detailsWidth, GUI.enlargedSlotSize + 2, 0.9f, 1);
            drawBlueprint(offX, offY, 0, ItemTypes.FORGE, ItemTypes.STEEL_FRAME, 0.9f);
        } else {
            List<Pair<ItemType, ItemType>> uses = Recipes.getHandcraftingUses(item.type);
            if (!uses.isEmpty()) {
                nameOnly = false;
                detailsWidth = Math.max(detailsWidth, GUI.enlargedSlotSize * 3);
                drawBackground(true, true, offX+1, offY+1, enlargedSlotSize * 0.5f, 3 - (GUI.enlargedSlotSize * (uses.size())), detailsWidth, (GUI.enlargedSlotSize * uses.size()) + 16, 0.9f);
                //drawSlot(offX, offY, enlargedSlotSize * 0.5f, 3 - (GUI.enlargedSlotSize * (uses.size())), 0, 0, detailsWidth, (GUI.enlargedSlotSize * uses.size()) + 2, 0.9f, 1);
                int offPxY = 0;
                for (int i = 0; i < uses.size(); i++) {
                    Pair<ItemType, ItemType> recipe = uses.get(i);
                    drawRecipe(offX, offY, offPxY, recipe.getFirst(), recipe.getSecond(), 0.9f);
                    offPxY -= GUI.enlargedSlotSize;
                }
            }
        }

        offX += 1;
        color.set(1.f);
        pushUBO.updateTex(Textures.gui); //use gui atlas
        pushUBO.updateLayer(2); //selector
        int textBoxAtlasOffsetX = nameOnly ? 321 : 110;
        pushUBO.updateAtlasOffset(new Vector2i(textBoxAtlasOffsetX, 0));
        drawSlot(offX, offY, (charWidth*2) - 1, charHeight - 4, 0, 0, 10, 16, 0.9f, 1);
        int centerWidth = Math.max(detailsWidth-20, (charWidth*chars.length)-12);
        if (centerWidth > 0) {
            pushUBO.updateAtlasOffset(new Vector2i(textBoxAtlasOffsetX+10, 0));
            drawSlot(offX, offY, ((charWidth * 2) - 1) + 10, charHeight - 4, 0, 0, centerWidth, 16, 0.9f, 1);
        }
        pushUBO.updateAtlasOffset(new Vector2i(textBoxAtlasOffsetX+201, 0));
        drawSlot(offX, offY, ((charWidth*2) - 1)+10+centerWidth, charHeight - 4, 0, 0, 10, 16, 0.9f, 1);
        pushUBO.updateLayer(0); //text
        drawText(false, offX, offY, charWidth*2, charHeight, chars, 0.9f, 1, detailsWidth == 0);
    }
    public void drawRecipe(int offX, int offY, int offPxY, ItemType ingredient, ItemType product, float layer) {
        pushUBO.updateTex(Textures.gui); //use gui atlas
        pushUBO.updateLayer(2); //selector
        color.set(1, 1, 1, 0.85f);
        pushUBO.updateAtlasOffset(new Vector2i(44, 0));
        drawSlot(offX, offY, (charWidth*2)+ GUI.enlargedSlotSize, ((charHeight*-2.5f)+2)+offPxY, 0, 0, GUI.enlargedSlotSize, GUI.enlargedSlotSize, layer, 1);
        pushUBO.updateAtlasOffset(new Vector2i(22, 0));
        drawSlot(offX, offY, charWidth*2, ((charHeight*-2.5f)+2)+offPxY, 0, 0, GUI.enlargedSlotSize, GUI.enlargedSlotSize, layer, 1);
        drawSlot(offX, offY, (charWidth*2)+(GUI.enlargedSlotSize *2), ((charHeight*-2.5f)+2)+offPxY, 0, 0, GUI.enlargedSlotSize, GUI.enlargedSlotSize, layer, 1);
        color.set(1.f);
        pushUBO.updateTex(Textures.items); //items
        pushUBO.updateAtlasOffset(ingredient.atlasOffset);
        drawSlot(offX, offY, (charWidth*2)+3, ((charHeight*-2.5f)+5)+offPxY, 0, 0, ItemTypes.itemTexSize, ItemTypes.itemTexSize, layer, 1);
        pushUBO.updateAtlasOffset(product.atlasOffset);
        drawSlot(offX, offY, (charWidth*2)+(GUI.enlargedSlotSize *2)+3, ((charHeight*-2.5f)+5)+offPxY, 0, 0, ItemTypes.itemTexSize, ItemTypes.itemTexSize, layer, 1);
        pushUBO.updateTex(Textures.gui); //use gui atlas
        pushUBO.updateLayer(0); //text
        drawText(true, offX, offY, (charWidth*2)+(GUI.enlargedSlotSize *1.5f)+1, ((charHeight*-1.5f)+1)+offPxY, "-->".toCharArray(), layer, 1, true);
    }
    public void drawBlueprint(int offX, int offY, int offPxY, ItemType station, ItemType product, float layer) {
        pushUBO.updateTex(Textures.gui); //use gui atlas
        pushUBO.updateLayer(2); //selector
        color.set(1, 1, 1, 0.85f);
        pushUBO.updateAtlasOffset(new Vector2i(44, 0));
        drawSlot(offX, offY, charWidth*2, ((charHeight*-2.5f)+2)+offPxY, 0, 0, GUI.enlargedSlotSize, GUI.enlargedSlotSize, layer, 1);
        drawSlot(offX, offY, (charWidth*2)+(GUI.enlargedSlotSize*2), ((charHeight*-2.5f)+2)+offPxY, 0, 0, GUI.enlargedSlotSize, GUI.enlargedSlotSize, layer, 1);
        pushUBO.updateAtlasOffset(new Vector2i(22, 0));
        drawSlot(offX, offY, (charWidth*2)+GUI.enlargedSlotSize, ((charHeight*-2.5f)+2)+offPxY, 0, 0, GUI.enlargedSlotSize, GUI.enlargedSlotSize, layer, 1);
        drawSlot(offX, offY, (charWidth*2)+(GUI.enlargedSlotSize*3), ((charHeight*-2.5f)+2)+offPxY, 0, 0, GUI.enlargedSlotSize, GUI.enlargedSlotSize, layer, 1);
        color.set(1.f);
        pushUBO.updateTex(Textures.items); //items
        pushUBO.updateAtlasOffset(station.atlasOffset);
        drawSlot(offX, offY, (charWidth*2)+GUI.enlargedSlotSize+3, ((charHeight*-2.5f)+5)+offPxY, 0, 0, ItemTypes.itemTexSize, ItemTypes.itemTexSize, layer, 1);
        pushUBO.updateAtlasOffset(product.atlasOffset);
        drawSlot(offX, offY, (charWidth*2)+(GUI.enlargedSlotSize*3)+3, ((charHeight*-2.5f)+5)+offPxY, 0, 0, ItemTypes.itemTexSize, ItemTypes.itemTexSize, layer, 1);
        pushUBO.updateTex(Textures.gui); //use gui atlas
        pushUBO.updateLayer(0); //text
        drawText(true, offX, offY, (charWidth*2)+(GUI.enlargedSlotSize*0.5f)+1, ((charHeight*-1.5f)+1)+offPxY, "-->".toCharArray(), layer, 1, true);
        drawText(true, offX, offY, (charWidth*2)+(GUI.enlargedSlotSize*2.5f)+1, ((charHeight*-1.5f)+1)+offPxY, "-->".toCharArray(), layer, 1, true);
    }
    public void drawItem(Item item, int x, int y) {
        drawItem(item, x, y, false);
    }
    public void drawItem(Item item, int x, int y, boolean hideDurability) {
        if (item != null) {
            pushUBO.updateTex(Textures.items);
            pushUBO.updateAtlasOffset(item.type.atlasOffset);
            drawQuad(x, y, ItemTypes.itemTexSize, ItemTypes.itemTexSize, 1);
            if (!hideDurability && item instanceof DurableItem durableItem && durableItem.durability < item.type.maxDurability() && !durableItem.type().hideDurability) {
                pushUBO.updateTex(null); //use no texture
                float durabilityPercent = (float) durableItem.durability / item.type.maxDurability();
                color.set(Utils.mix(1, 0, durabilityPercent), Utils.mix(0, 1, durabilityPercent), 0, 1);
                drawQuad(x, y, (int)(ItemTypes.itemTexSize*durabilityPercent), 2, 1);
                color.set(1);
            }
            if (item.amount > 1) {
                pushUBO.updateTex(Textures.gui); //use gui atlas
                char[] chars = item.amountString().toCharArray();
                float startOffset = 13 - (chars.length * (charWidth/2.f));
                drawText(false, x, y, startOffset, 1, chars, 2);
            }
        }
    }
    public void drawText(boolean centered, int offsetX, int offsetY, float offsetPX, float offsetPY, char[] chars, int iScale) {
        drawText(centered, offsetX, offsetY, offsetPX, offsetPY, chars, iScale, false);
    }
    public void drawText(boolean centered, int offsetX, int offsetY, float offsetPX, float offsetPY, char[] chars, int iScale, boolean inverted) {
        drawText(centered, offsetX, offsetY, offsetPX, offsetPY, chars, 0.f, iScale, inverted);
    }
    public void drawText(boolean centered, int offsetX, int offsetY, float offsetPX, float offsetPY, char[] chars, float layer, int iScale, boolean inverted) {
        int scaledCharWidth = charWidth/iScale;
        float size = chars.length * scaledCharWidth;
        float centeredOffset = centered ? size*0.5f : 0.f;
        float offset = 0;
        for (char character : chars) {
            int charAtlasOffset = getCharAtlasOffset(character);
            if (charAtlasOffset >= 0) {
                pushUBO.updateAtlasOffset(new Vector2i(charAtlasOffset, inverted ? charHeight : 0));
                drawSlot(offsetX, offsetY, (offsetPX + offset - centeredOffset) + (centered ? 0 : scaledCharWidth*0.5f), offsetPY, 0, 0, charWidth, charHeight, layer, iScale);
            }
            offset += scaledCharWidth;
        }
    }
    public void drawSlot(float offsetX, float offsetY, float offPxX, float offPxY, int x, int y, int sizeX, int sizeY, int iScale) {
        drawSlot(false, false, offsetX, offsetY, offPxX, offPxY, x, y, sizeX, sizeY, iScale);
    }
    public void drawSlot(boolean centeredX, boolean centeredY, float offsetX, float offsetY, float offPxX, float offPxY, int x, int y, int sizeX, int sizeY, int iScale) {
        drawQuad(centeredX, centeredY, Math.round(x + offsetX + offPxX), Math.round(y + offsetY + offPxY), sizeX, sizeY, iScale);
    }
    public void drawSlot(float offsetX, float offsetY, float offPxX, float offPxY, int x, int y, int sizeX, int sizeY, float layer, int iScale) {
        drawSlot(false, false, offsetX, offsetY, offPxX, offPxY, x, y, sizeX, sizeY, layer, iScale);
    }
    public void drawSlot(boolean centeredX, boolean centeredY, float offsetX, float offsetY, float offPxX, float offPxY, int x, int y, int sizeX, int sizeY, float layer, int iScale) {
        drawQuad(centeredX, centeredY, Math.round(x + offsetX + offPxX), Math.round(y + offsetY + offPxY), sizeX, sizeY, layer, iScale);
    }
    public void drawQuad(int x, int y, int scaleX, int scaleY, int iScale) {
        drawQuad(false, false, x, y, scaleX, scaleY, iScale);
    }
    public void drawQuad(boolean centeredX, boolean centeredY, int x, int y, int scaleX, int scaleY, int iScale) {
        drawQuad(centeredX, centeredY, x, y, scaleX, scaleY, 0.f, iScale);
    }
    public void drawQuad(int x, int y, int scaleX, int scaleY, float layer, int iScale) {
        drawQuad(false, false, x, y, scaleX, scaleY, layer, iScale);
    }
    public void drawQuad(boolean centeredX, boolean centeredY, int x, int y, int scaleX, int scaleY, float layer, int iScale) {
        if (iScale > 1) {
            GUI.updateScale(iScale);
        }
        float xScale = (scaleX / guiScale);
        float yScale = (scaleY / guiScale) * aspectRatio;
        if (iScale > 1) {
            GUI.updateScale(1);
        }
        float xOffset = ((((menuPos.x()+(x*guiScaleMul))*2)-width)/width) + (centeredX ? 0 : xScale);
        float yOffset = ((((menuPos.y()+(y*guiScaleMul))*-2)+height)/height) - (centeredY ? 0 : yScale);
        pushUBO.updateSize(new Vector2i(scaleX, scaleY));
        Renderer.drawQuadCentered(new Matrix4f().translate(xOffset, yOffset, layer).scale(xScale, yScale, 1), color);
    }
}