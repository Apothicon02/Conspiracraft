package org.terraflat.game.elements;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.terraflat.game.grids.Grid;
import org.terraflat.game.grids.Grids;

public class Element {
    Grid grid = Grids.getGrid("terrain");
    Vector3i pos;
    Vector3f rot;
    String model;

    public Element() {
        this.pos =  new Vector3i(0, 0, 0);
        this.rot =  new Vector3f(0, 0, 0);
        this.model = new String("cube");
    }

    public Element(String model) {
        this.pos =  new Vector3i(0, 0, 0);
        this.rot =  new Vector3f(0, 0, 0);
        this.model = model;
    }

    public Element(Vector3i pos) {
        this.pos = pos;
        this.rot = new Vector3f(0, 0, 0);
        this.model = new String("cube");
    }

    public Element(Vector3i pos, String model) {
        this.pos = pos;
        this.rot = new Vector3f(0, 0, 0);
        this.model = model;
    }

    public Element(Vector3i pos, Vector3f rot) {
        this.pos = pos;
        this.rot = rot;
        this.model = new String("cube");
    }

    public Element(Vector3i pos, Vector3f rot, String model) {
        this.pos = pos;
        this.rot = rot;
        this.model = model;
    }

    public Vector3i getPos() {
        return pos;
    }

    public void setPos(Vector3i pos) {
        this.pos = pos;
    }

    public Vector3f getRot() {
        return rot;
    }

    public void setRot(Vector3f rot) {
        this.rot = rot;
    }

    public Grid getGrid() {
        return grid;
    }

    public void setGrid(Grid grid) {
        this.grid = grid;
    }

    public String getModel() {
        return model;
    }
}
