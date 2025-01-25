package org.conspiracraft.game.world;

import org.conspiracraft.engine.BitBuffer;
import org.conspiracraft.engine.Utils;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;

import static org.conspiracraft.game.world.World.chunkSize;

public class Chunk {
    private static final int totalBlocks = chunkSize*chunkSize*chunkSize;
    private final List<Vector2i> blockPalette = new ArrayList<>(List.of(new Vector2i(0, 0)));
    private BitBuffer blockData = new BitBuffer(totalBlocks, 1);

    public int getNeededBitsPerValue() {
        return Math.max(1, (int) Math.ceil(Math.log(blockPalette.size()) / Math.log(2)));
    }
    public void setBlockPalette(int[] data) {
        int i = 0;
        for (int integer : data) {
            if (i == 0) {
                blockPalette.set(i++, Utils.unpackInt(integer));
            } else {
                blockPalette.add(i++, Utils.unpackInt(integer));
            }
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
        blockData = new BitBuffer(totalBlocks, getNeededBitsPerValue());
        blockData.setData(data);
    }
    public int[] getBlockData() {
        return blockData.getData();
    }
    public void setBlockKey(int pos, int key) {
        blockData.setValue(pos, key);
    }
    public int getBlockKey(int pos) {
        return blockData.getValue(pos);
    }
    public Vector2i getBlock(int pos) {
        int index = getBlockKey(pos);
        return blockPalette.get(index);
    }
    public void setBlock(int pos, Vector2i block, Vector3i globalPos) {
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
            int neededBitsPerValue = getNeededBitsPerValue();
            if (neededBitsPerValue > blockData.bitsPerValue) {
                BitBuffer newData = new BitBuffer(totalBlocks, neededBitsPerValue);
                for (int i = 0; i < totalBlocks; i++) {
                    newData.setValue(i, blockData.getValue(i));
                }
                blockData = newData;
            }
            setBlockKey(pos, blockPalette.size()-1);
        }
        World.queueColumnUpdate(globalPos);
    }
    public void setBlock(int pos, int type, int subType, Vector3i globalPos) {
        setBlock(pos, new Vector2i(type, subType), globalPos);
    }
}