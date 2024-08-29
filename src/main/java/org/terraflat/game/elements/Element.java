package org.terraflat.game.elements;

import org.terraflat.game.grids.Grid;
import org.terraflat.game.grids.Grids;

public class Element {
    Grid grid = Grids.getGrid("terrain");
    String model;

    public Element() {
        this.model = new String("cube");
    }
    public Element(String model) {
        this.model = model;
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
