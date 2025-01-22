package org.conspiracraft.game.world;

import org.conspiracraft.engine.Utils;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;

import static org.conspiracraft.game.world.World.chunkSize;

public class Chunk {
    private static final int totalBlocks = chunkSize*chunkSize*chunkSize;
    private int[] blockData = new int[totalBlocks/32];
    private final List<Vector2i> blockPalette = new ArrayList<>(List.of(new Vector2i(0, 0), new Vector2i(2, 0)));

    public void setBlockPalette(int[] data) {
        int i = 0;
        for (int integer : data) {
            blockPalette.set(i++, Utils.unpackInt(integer));
        }
    }
    public int[] getBlockPalette() {
        int[] returnObj = new int[blockPalette.size()];
        int i = 0;
        for (Vector2i block : blockPalette) {
            returnObj[i++] = Utils.packInts(block.x, block.y);
        }
        return returnObj;
    }
    public int getBlockPaletteSize() {
        return blockPalette.size();
    }
    public void setBlockData(int[] data) {
        blockData = data;
    }
    public int[] getBlockData() {
        return blockData;
    }
    public void setBlockKey(int pos, int key) {
        int intPos = pos/32;
        int values = blockData[intPos];
        int whereInInt = pos-(intPos*32);
        blockData[intPos] = values | (key << whereInInt);
    }
    public int getBlockKey(int pos) {
        int intPos = pos/32;
        int values = blockData[intPos];
        int whereInInt = pos-(intPos*32);
        return (values >> whereInInt) & 1;
    }
    public Vector2i getBlock(int pos) {
        int index = getBlockKey(pos);
        return blockPalette.get(index);
    }
    public void setBlock(int pos, Vector2i block, Vector3i globalPos) {
        block = new Vector2i(block.x > 0 ? 2 : 0, 0);
        boolean wasInPalette = false;
        int key = -1;
        for (Vector2i paletteEntry : blockPalette) {
            key++;
            if (paletteEntry.equals(block)) {
                setBlockKey(pos, key);
                wasInPalette = true;
                break;
            }
        }
        if (!wasInPalette) {
            blockPalette.addLast(block);
            setBlockKey(pos, blockPalette.size());
        }
        World.queueColumnUpdate(globalPos);
    }
    public void setBlock(int pos, int type, int subType, Vector3i globalPos) {
        setBlock(pos, new Vector2i(type, subType), globalPos);
    }
}