package org.conspiracraft.gui;

import org.joml.Vector2i;

public class GUIElement {
    public Vector2i posRaw = new Vector2i();
    public Vector2i sizeRaw = new Vector2i();
    public Vector2i pos = new Vector2i();
    public Vector2i size = new Vector2i();
    public Vector2i globalPos = new Vector2i();
    public GUIElement() {}

    public GUIElement setPos(int x, int y) {this.posRaw.set(x, y); return this;}
    public GUIElement setSize(int x, int y) {this.sizeRaw.set(x, y); return this;}

    public void update(Menu menu) {
        pos.set((int) (posRaw.x()*GUI.guiScaleMul), (int) (posRaw.y()*GUI.guiScaleMul));
        globalPos.set(pos).add(menu.menuPos);
        size.set((int) (sizeRaw.x()*GUI.guiScaleMul), (int) (sizeRaw.y()*GUI.guiScaleMul));
    }
    public void tick(Menu menu) {}
}