package org.conspiracraft.gui;

import org.conspiracraft.graphics.Renderer;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.ArrayList;

import static org.conspiracraft.Settings.height;
import static org.conspiracraft.Settings.width;
import static org.conspiracraft.graphics.Renderer.pushUBO;
import static org.conspiracraft.gui.GUI.guiScale;
import static org.conspiracraft.gui.GUI.aspectRatio;
import static org.conspiracraft.gui.GUI.charWidth;
import static org.conspiracraft.gui.GUI.charHeight;
import static org.conspiracraft.gui.GUI.getCharAtlasOffset;
import static org.conspiracraft.gui.GUI.guiScaleMul;
import static org.conspiracraft.gui.GUI.color;
import static org.conspiracraft.gui.GUI.slotSize;
import static org.conspiracraft.gui.GUI.slotSizeY;

public class Menu {
    public String name = "Menu";
    public Vector2f menuPosRaw = new Vector2f();
    public Vector2i menuPos = new Vector2i();
    public Vector2i menuSizeRaw = new Vector2i();
    public Vector2f menuSize = new Vector2f();
    public boolean centeredMenuX = true, centeredMenuY = true;
    public Slot selectedSlot = null;
    public Menu() {}

    public Menu setName(String name) {this.name = name; return this;}
    public Menu setPos(float posX, float posY) {menuPosRaw.set(posX, posY); return this;}
    public Menu setCentered(boolean centeredMenuX, boolean centeredMenuY) {this.centeredMenuX = centeredMenuX; this.centeredMenuY = centeredMenuY; return this;}

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
        slot.setSize(GUI.enlargedSlotSize, GUI.enlargedSlotSize);
        slots.add(slot);
        elements.add(slot);
    }
    public void tick() {
        selectedSlot = null;
        for (GUIElement element : elements) {element.tick(this);}
    }

    public void draw() {}

    public void drawText(boolean centered, int offsetX, int offsetY, float offsetPX, float offsetPY, char[] chars) {
        int scaledCharWidth = charWidth;
        float size = chars.length * scaledCharWidth;
        float centeredOffset = centered ? size*0.5f : 0.f;
        float offset = 0;
        for (char character : chars) {
            int charAtlasOffset = getCharAtlasOffset(character);
            if (charAtlasOffset >= 0) {
                pushUBO.updateAtlasOffset(new Vector2i(charAtlasOffset, 0));
                drawSlot(offsetX, offsetY, (offsetPX + offset - centeredOffset) + (centered ? 0 : scaledCharWidth*0.5f), offsetPY, 0, 0, charWidth, charHeight);
            }
            offset += scaledCharWidth;
        }
    }
    public void drawSlot(float offsetX, float offsetY, float offPxX, float offPxY, int x, int y, int sizeX, int sizeY) {
        drawSlot(false, false, offsetX, offsetY, offPxX, offPxY, x, y, sizeX, sizeY);
    }
    public void drawSlot(boolean centeredX, boolean centeredY, float offsetX, float offsetY, float offPxX, float offPxY, int x, int y, int sizeX, int sizeY) {
        float selectedPosX = x * (slotSize / guiScale);
        float selectedPosY = y * ((slotSizeY / guiScale) * aspectRatio);
        drawQuad(centeredX, centeredY, (int)(x + offsetX + offPxX), (int)(y + (offsetY - (3.f / height)) + offPxY), sizeX, sizeY);
    }
    public void drawQuad(int x, int y, int scaleX, int scaleY) {
        drawQuad(false, false, x, y, scaleX, scaleY);
    }
    public void drawQuad(boolean centeredX, boolean centeredY, int x, int y, int scaleX, int scaleY) {
        float xScale = (scaleX / guiScale);
        float yScale = (scaleY / guiScale) * aspectRatio;
        float xOffset = (((((float)menuPos.x()+(x*guiScaleMul))/width)-0.5f)*2) + (centeredX ? 0 : xScale);
        float yOffset = (((((float)menuPos.y()+(y*guiScaleMul))/height)-0.5f)*-2) - (centeredY ? 0 : yScale);
        pushUBO.updateSize(new Vector2i(scaleX, scaleY));
        Renderer.drawQuadCentered(new Matrix4f().translate(xOffset, yOffset, 0.f).scale(xScale, yScale, 1), color);
    }
}