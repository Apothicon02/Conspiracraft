package org.conspiracraft.game.blocks;

import org.conspiracraft.engine.Utils;

public class Block {
    public static int blockId;
    public static int brightness;

    public Block(int type, int subtype) {
        blockId = Utils.packInts(type, subtype);
        brightness = -16777216; //0, 0, 0, 256
    }
    public Block(int id) {
        blockId = id;
        brightness = -16777216; //0, 0, 0, 256
    }
}