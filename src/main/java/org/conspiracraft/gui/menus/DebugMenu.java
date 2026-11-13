package org.conspiracraft.gui.menus;

import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.gui.Menu;
import org.conspiracraft.gui.MenuButtonElement;
import org.conspiracraft.items.types.ItemTypes;
import org.conspiracraft.player.Player;
import org.joml.Vector2i;

import static org.conspiracraft.gui.GUI.slotSize;

public class DebugMenu extends Menu {
    public DebugMenu() {
        elements.add(new MenuButtonElement().setMenu(Player.histogramMenu).setTex(Textures.items).setAtlasOffset(ItemTypes.GRASSY_DIRT.atlasOffset).setTexSize(ItemTypes.itemTexSize).setPos(0, 0).setSize(slotSize, slotSize));
        elements.add(new MenuButtonElement().setMenu(Player.histogramMenu).setTex(Textures.items).setAtlasOffset(ItemTypes.TORCH.atlasOffset).setTexSize(ItemTypes.itemTexSize).setPos(slotSize, 0).setSize(slotSize, slotSize));
        elements.add(new MenuButtonElement().setMenu(Player.histogramMenu).setTex(Textures.items).setAtlasOffset(ItemTypes.FIREBALL.atlasOffset).setTexSize(ItemTypes.itemTexSize).setPos(slotSize*2, 0).setSize(slotSize, slotSize));
        elements.add(new MenuButtonElement().setMenu(Player.histogramMenu).setTex(Textures.items).setAtlasOffset(ItemTypes.SKULL.atlasOffset).setTexSize(ItemTypes.itemTexSize).setPos(slotSize*3, 0).setSize(slotSize, slotSize));
        elements.add(new MenuButtonElement().setMenu(Player.histogramMenu).setTex(Textures.items).setAtlasOffset(ItemTypes.HEART.atlasOffset).setTexSize(ItemTypes.itemTexSize).setPos(slotSize*4, 0).setSize(slotSize, slotSize));
        this.menuSizeRaw = new Vector2i(slotSize*elements.size(), slotSize+(hideName ? 0 : 12));
    }

    @Override
    public void draw() {
        drawBase();

//        pushUBO.updateTex(null);
//        color.set(0.329f, 0.333f, 0.341f, 0.88f);
//        drawQuad(0, 0, menuSizeRaw.x(), menuSizeRaw.y()-12, 1);
    }

    @Override
    public void tick() {
        baseTick();
    }
}