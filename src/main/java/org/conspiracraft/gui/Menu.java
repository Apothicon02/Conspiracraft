package org.conspiracraft.gui;

import kotlin.Pair;
import org.conspiracraft.Main;
import org.conspiracraft.graphics.Renderer;
import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.Recipes;
import org.conspiracraft.items.types.ItemType;
import org.conspiracraft.items.types.ItemTypes;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

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
    public Item[] items = null;
    public Menu() {}

    public Menu setName(String name) {this.name = name; return this;}
    public Menu setPos(float posX, float posY) {menuPosRaw.set(posX, posY); return this;}
    public Menu setCentered(boolean centeredMenuX, boolean centeredMenuY) {this.centeredMenuX = centeredMenuX; this.centeredMenuY = centeredMenuY; return this;}

    public void menuColor(Vector4f col) {col.set(1, 0.95f, 0.875f, 1);}

    public ArrayList<GUIElement> elements = new ArrayList<>();
    public Vector2i getPos() {return new Vector2i(menuPos);}
    public void update() {
        menuSize.set(menuSizeRaw);//.div(guiScale).mul(1, aspectRatio);
        menuPos.set((int)(menuPosRaw.x()*width), (int)(menuPosRaw.y()*height));
        if (centeredMenuX) {menuPos.sub((int)(width*((menuSize.x()*0.5f)/guiScale)), 0);}
        if (centeredMenuY) {menuPos.sub(0, (int)(height*(((menuSize.y()*0.5f)/guiScale)*aspectRatio)));}
        for (GUIElement element : elements) {element.update(this);}
    }
    public ArrayList<Slot> slots = new ArrayList<>();
    public void addSlot(GUIElement slot) {addSlot((Slot)slot);}
    public void addSlot(Slot slot) {
        slot.id(slots.size());
        slot.setSize(slotSize, slotSizeY);
        slots.add(slot);
        elements.add(slot);
    }
    public void tick() {
        baseTick();
    }
    public void baseTick() {
        if (!Main.player.inputHandler.leftButtonPressed) {
            if (Main.player.inv.cursorItem != null && selectedSlotsL.size() > 1) {
                int amtPerSlot = Main.player.inv.cursorItem.amount / selectedSlotsL.size();
                int amtPut = 0;
                for (Slot slot : selectedSlotsL) {
                    Item item = items[slot.id];
                    if (item == null) {
                        item = Main.player.inv.cursorItem.clone().amount(amtPerSlot);
                        amtPut += amtPerSlot;
                    } else {
                        int prevAmount = item.amount;
                        item.amount = Math.min(item.type.maxStackSize, item.amount + amtPerSlot);
                        amtPut += item.amount - prevAmount;
                    }
                    items[slot.id] = item;
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
                    Item item = items[slot.id];
                    if (item == null) {
                        item = Main.player.inv.cursorItem.clone().amount(1);
                        amtPut++;
                    } else {
                        int prevAmount = item.amount;
                        item.amount = Math.min(item.type.maxStackSize, item.amount+1);
                        amtPut += item.amount - prevAmount;
                    }
                    items[slot.id] = item;
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
        for (GUIElement element : elements) {element.tick(this);}
        GUI.canInteract = true;
    }

    public void draw() {
        drawBase();
    }
    public void drawBase() {
        pushUBO.updateTex(null);
        color.set(0, 0, 0, 1);
        drawQuad(-1, -1, menuSizeRaw.x()+2, menuSizeRaw.y()+1, 1);
        for (Slot slot : slots) {
            pushUBO.updateTex(Textures.gui);
            pushUBO.updateLayer(1); //inventory
            pushUBO.updateAtlasOffset(new Vector2i(0));
            menuColor(color);
            drawSlot(0, 0, slot.posRaw.x(), slot.posRaw.y() - 0.5f, 0, 0, slotSize, slotSize, 1);
            color.set(1);
            if (items != null && slot.id < items.length) {
                drawItem(items[slot.id], slot.posRaw.x() + 2, slot.posRaw.y() + 2);
            }
        }
        pushUBO.updateTex(Textures.gui);
        pushUBO.updateLayer(2); //selector
        pushUBO.updateAtlasOffset(new Vector2i());
        for (Slot slot : selectedSlotsL) {
            drawSlot(0, 0, slot.posRaw.x()-1, slot.posRaw.y()-1, 0, 0, enlargedSlotSize, enlargedSlotSize, 1);
        }
        for (Slot slot : selectedSlotsR) {
            drawSlot(0, 0, slot.posRaw.x()-1, slot.posRaw.y()-1, 0, 0, enlargedSlotSize, enlargedSlotSize, 1);
        }
        if (selectedSlot != null) {
            pushUBO.updateTex(Textures.gui);
            pushUBO.updateLayer(2); //selector
            pushUBO.updateAtlasOffset(new Vector2i());
            drawSlot(0, 0, selectedSlot.posRaw.x()-1, selectedSlot.posRaw.y()-1, 0, 0, enlargedSlotSize, enlargedSlotSize, 1);
            if (selectedSlot.id < items.length) {
                Item item = items[selectedSlot.id];
                if (item != null && item.type != ItemTypes.AIR) {
                    drawItemHoverDetails(selectedSlot.posRaw.x() + (GUI.slotSize/2), selectedSlot.posRaw.y() + (GUI.slotSizeY-4), item);
                }
            }
        }
    }

    public void drawItemHoverDetails(int offX, int offY, Item item) {
        pushUBO.updateTex(null); //use no texture
        color.set(0.015f, 0.023f, 0.027f, 1.f);
        List<Pair<ItemType, ItemType>> uses = Recipes.getUses(item.type);
        if (!uses.isEmpty()) {
            drawSlot(offX, offY, (charWidth * 2) - 1, (charHeight * -2.5f) + 1 - (GUI.enlargedSlotSize * (uses.size() - 1)), 0, 0, (GUI.enlargedSlotSize * 3) + 2, (GUI.enlargedSlotSize * uses.size()) + 2, 1);
            int offPxY = 0;
            for (int i = 0; i < uses.size(); i++) {
                Pair<ItemType, ItemType> recipe = uses.get(i);
                drawRecipe(offX, offY, offPxY, recipe.getFirst(), recipe.getSecond());
                offPxY-=GUI.enlargedSlotSize;
            }
        }

        char[] chars = Languages.translate("item/"+item.type.name).toCharArray();
        color.set(1.f);
        pushUBO.updateTex(Textures.gui); //use gui atlas
        pushUBO.updateLayer(2); //selector
        pushUBO.updateAtlasOffset(new Vector2i(0, 22));
        drawSlot(offX, offY, (charWidth*2) - 1, charHeight - 4, 0, 0, 10, 16, 1);
        int centerWidth = Math.max(0, (charWidth*chars.length)-12);
        if (centerWidth > 0) {
            pushUBO.updateAtlasOffset(new Vector2i(10, 22));
            drawSlot(offX, offY, ((charWidth * 2) - 1) + 10, charHeight - 4, 0, 0, centerWidth, 16, 1);
        }
        pushUBO.updateAtlasOffset(new Vector2i(201, 22));
        drawSlot(offX, offY, ((charWidth*2) - 1)+10+centerWidth, charHeight - 4, 0, 0, 10, 16, 1);
        pushUBO.updateLayer(0); //text
        drawText(false, offX, offY, charWidth*2, charHeight, chars, 1);
    }
    public void drawRecipe(int offX, int offY, int offPxY, ItemType ingredient, ItemType product) {
        pushUBO.updateTex(Textures.gui); //use gui atlas
        pushUBO.updateLayer(2); //selector
        color.set(1, 1, 1, 0.85f);
        pushUBO.updateAtlasOffset(new Vector2i(44, 0));
        drawSlot(offX, offY, (charWidth*2)+ GUI.enlargedSlotSize, ((charHeight*-2.5f)+2)+offPxY, 0, 0, GUI.enlargedSlotSize, GUI.enlargedSlotSize, 1);
        pushUBO.updateAtlasOffset(new Vector2i(22, 0));
        drawSlot(offX, offY, charWidth*2, ((charHeight*-2.5f)+2)+offPxY, 0, 0, GUI.enlargedSlotSize, GUI.enlargedSlotSize, 1);
        drawSlot(offX, offY, (charWidth*2)+(GUI.enlargedSlotSize *2), ((charHeight*-2.5f)+2)+offPxY, 0, 0, GUI.enlargedSlotSize, GUI.enlargedSlotSize, 1);
        color.set(1.f);
        pushUBO.updateTex(Textures.items); //items
        pushUBO.updateAtlasOffset(ingredient.atlasOffset);
        drawSlot(offX, offY, (charWidth*2)+3, ((charHeight*-2.5f)+5)+offPxY, 0, 0, ItemTypes.itemTexSize, ItemTypes.itemTexSize, 1);
        pushUBO.updateAtlasOffset(product.atlasOffset);
        drawSlot(offX, offY, (charWidth*2)+(GUI.enlargedSlotSize *2)+3, ((charHeight*-2.5f)+5)+offPxY, 0, 0, ItemTypes.itemTexSize, ItemTypes.itemTexSize, 1);
        pushUBO.updateTex(Textures.gui); //use gui atlas
        pushUBO.updateLayer(0); //text
        drawText(true, offX, offY, (charWidth*2)+(GUI.enlargedSlotSize *1.5f)+1, ((charHeight*-1.5f)+1)+offPxY, "-->".toCharArray(), 1);
    }
    public void drawItem(Item item, int x, int y) {
        if (item != null) {
            pushUBO.updateTex(Textures.items);
            pushUBO.updateAtlasOffset(item.type.atlasOffset);
            drawSlot(0, 0, x, y, 0, 0, ItemTypes.itemTexSize, ItemTypes.itemTexSize, 1);
            if (item.amount > 1) {
                pushUBO.updateTex(Textures.gui); //use gui atlas
                char[] chars = item.amountString().toCharArray();
                float startOffset = 16 - (chars.length * (charWidth/2.f));
                drawText(false, x, y, startOffset, 1, chars, 2);
            }
        }
    }
    public void drawText(boolean centered, int offsetX, int offsetY, float offsetPX, float offsetPY, char[] chars, int iScale) {
        int scaledCharWidth = charWidth/iScale;
        float size = chars.length * scaledCharWidth;
        float centeredOffset = centered ? size*0.5f : 0.f;
        float offset = 0;
        for (char character : chars) {
            int charAtlasOffset = getCharAtlasOffset(character);
            if (charAtlasOffset >= 0) {
                pushUBO.updateAtlasOffset(new Vector2i(charAtlasOffset, 0));
                drawSlot(offsetX, offsetY, (offsetPX + offset - centeredOffset) + (centered ? 0 : scaledCharWidth*0.5f), offsetPY, 0, 0, charWidth, charHeight, iScale);
            }
            offset += scaledCharWidth;
        }
    }
    public void drawSlot(float offsetX, float offsetY, float offPxX, float offPxY, int x, int y, int sizeX, int sizeY, int iScale) {
        drawSlot(false, false, offsetX, offsetY, offPxX, offPxY, x, y, sizeX, sizeY, iScale);
    }
    public void drawSlot(boolean centeredX, boolean centeredY, float offsetX, float offsetY, float offPxX, float offPxY, int x, int y, int sizeX, int sizeY, int iScale) {
        float selectedPosX = x * (slotSize / guiScale);
        float selectedPosY = y * ((slotSizeY / guiScale) * aspectRatio);
        drawQuad(centeredX, centeredY, (int)(x + offsetX + offPxX), (int)(y + (offsetY - (3.f / height)) + offPxY), sizeX, sizeY, iScale);
    }
    public void drawQuad(int x, int y, int scaleX, int scaleY, int iScale) {
        drawQuad(false, false, x, y, scaleX, scaleY, iScale);
    }
    public void drawQuad(boolean centeredX, boolean centeredY, int x, int y, int scaleX, int scaleY, int iScale) {
        if (iScale > 1) {
            GUI.updateScale(iScale);
        }
        float xScale = (scaleX / guiScale);
        float yScale = (scaleY / guiScale) * aspectRatio;
        if (iScale > 1) {
            GUI.updateScale(1);
        }
        float xOffset = (((((float)menuPos.x()+(x*guiScaleMul))/width)-0.5f)*2) + (centeredX ? 0 : xScale);
        float yOffset = (((((float)menuPos.y()+(y*guiScaleMul))/height)-0.5f)*-2) - (centeredY ? 0 : yScale);
        pushUBO.updateSize(new Vector2i(scaleX, scaleY));
        Renderer.drawQuadCentered(new Matrix4f().translate(xOffset, yOffset, 0.f).scale(xScale, yScale, 1), color);
    }
}