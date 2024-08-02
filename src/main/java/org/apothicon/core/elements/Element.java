package org.apothicon.core.elements;

import org.apothicon.core.grids.Grid;
import org.apothicon.core.grids.Grids;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class Element {
    Grid grid = Grids.getGrid("terrain");
    Vector3i pos;
    Vector3f rot;
    Mesh mesh;

    public Element() {
        this.pos =  new Vector3i(0, 0, 0);
        this.rot =  new Vector3f(0, 0, 0);
        this.mesh = new Mesh(Models.CUBE);
    }

    public Element(Mesh mesh) {
        this.pos =  new Vector3i(0, 0, 0);
        this.rot =  new Vector3f(0, 0, 0);
        this.mesh = mesh;
    }

    public Element(Vector3i pos) {
        this.pos = pos;
        this.rot = new Vector3f(0, 0, 0);
        this.mesh = new Mesh(Models.CUBE);
    }

    public Element(Vector3i pos, Mesh mesh) {
        this.pos = pos;
        this.rot = new Vector3f(0, 0, 0);
        this.mesh = mesh;
    }

    public Element(Vector3i pos, Vector3f rot) {
        this.pos = pos;
        this.rot = rot;
        this.mesh = new Mesh(Models.CUBE);
    }

    public Element(Vector3i pos, Vector3f rot, Mesh mesh) {
        this.pos = pos;
        this.rot = rot;
        this.mesh = mesh;
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
    
    public Mesh getMesh() {
        return mesh;
    }
}