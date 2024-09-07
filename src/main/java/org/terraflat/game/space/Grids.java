package org.terraflat.game.space;

import java.util.HashMap;
import java.util.Map;

public class Grids {
    private static HashMap<String, Grid> allGrids = new HashMap<>();

    public static Grid createGrid(String name, Grid grid) {
        allGrids.put(name, grid);
        return grid;
    }

    public static Grid getGrid(String name) {
        return allGrids.get(name);
    }

    public static Map<String, Grid> getGrids() {
        return allGrids;
    }
}