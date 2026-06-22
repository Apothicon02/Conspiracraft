package org.conspiracraft.rooms;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class Room {
    public Int2ObjectOpenHashMap<Cell> cells;
    public Room() {
        cells = new Int2ObjectOpenHashMap<>();
    }
    public Room(Int2ObjectOpenHashMap<Cell> cells) {
        this.cells = cells;
    }
}
