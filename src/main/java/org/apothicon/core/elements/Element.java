package org.apothicon.core.elements;

import org.apothicon.core.grids.Grid;
import org.apothicon.core.grids.Grids;
import org.joml.Vector3f;

public class Element {
    Grid grid;
    Vector3f pos;
    Vector3f rot;
    Mesh mesh;

    public Element() {
        this.grid = Grids.TERRAIN;
        this.pos =  new Vector3f(0, 0, 0);
        this.rot =  new Vector3f(0, 0, 0);
        this.mesh = new Mesh(Models.CUBE);
    }

    public Element(Vector3f pos) {
        this.grid = Grids.TERRAIN;
        this.pos = pos;
        this.rot = new Vector3f(0, 0, 0);
        this.mesh = new Mesh(Models.CUBE);
    }

    public Element(Vector3f pos, Mesh mesh) {
        this.grid = Grids.TERRAIN;
        this.pos = pos;
        this.rot = new Vector3f(0, 0, 0);
        this.mesh = mesh;
    }

    public Element(Vector3f pos, Vector3f rot) {
        this.grid = Grids.TERRAIN;
        this.pos = pos;
        this.rot = rot;
        this.mesh = new Mesh(Models.CUBE);
    }

    public Element(Vector3f pos, Vector3f rot, Mesh mesh) {
        this.grid = Grids.TERRAIN;
        this.pos = pos;
        this.rot = rot;
        this.mesh = mesh;
    }

    public Element(Grid grid, Vector3f pos) {
        this.grid = grid;
        this.pos = pos;
        this.rot = new Vector3f(0, 0, 0);
        this.mesh = new Mesh(Models.CUBE);
    }

    public Element(Grid grid, Vector3f pos, Mesh mesh) {
        this.grid = grid;
        this.pos = pos;
        this.rot = new Vector3f(0, 0, 0);
        this.mesh = mesh;
    }

    public Element(Grid grid, Vector3f pos, Vector3f rot) {
        this.grid = grid;
        this.pos = pos;
        this.rot = rot;
        this.mesh = new Mesh(Models.CUBE);
    }

    public Element(Grid grid, Vector3f pos, Vector3f rot, Mesh mesh) {
        this.grid = grid;
        this.pos = pos;
        this.rot = rot;
        this.mesh = mesh;
    }

    public Vector3f getPos() {
        return pos;
    }

    public Vector3f getRot() {
        return rot;
    }
    
    public Grid getGrid() {
        return grid;
    }
    
    public Mesh getMesh() {
        return mesh;
    }
}