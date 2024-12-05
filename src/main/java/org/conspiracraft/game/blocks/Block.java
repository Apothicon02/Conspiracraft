package org.conspiracraft.game.blocks;

import org.conspiracraft.engine.Utils;

public record Block(int id, byte r, byte g, byte b, byte s) {
    public Block {}
    public Block(int type, int subtype, byte red, byte green, byte blue, byte sun) {
        this(Utils.packInts(type, subtype), red, green, blue, sun);
    }
    public Block(int type, int subtype) {
        this(Utils.packInts(type, subtype), Utils.emptyByte, Utils.emptyByte,Utils.emptyByte, Utils.emptyByte);
    }
    public Block(int blockId) {
        this(blockId, Utils.emptyByte, Utils.emptyByte,Utils.emptyByte, Utils.emptyByte);
    }

    public int typeId() {
        return Utils.unpackInt(id).x;
    }
    public int subtypeId() {
        return Utils.unpackInt(id).y;
    }
    public int id() {
        return id;
    }
}
