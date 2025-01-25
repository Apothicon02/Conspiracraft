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
    private BitBuffer blockData = new BitBuffer(totalBlocks, 0);
    private final List<Integer> cornerPalette = new ArrayList<>(List.of(0));
    private BitBuffer cornerData = new BitBuffer(totalBlocks, 0);

    public int getNeededBitsPerValue(int uniqueValues) {
        return (int) Math.ceil(Math.log(uniqueValues) / Math.log(2));
    }

    //Blocks start
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
        blockData = new BitBuffer(totalBlocks, getNeededBitsPerValue(blockPalette.size()));
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
            int neededBitsPerValue = getNeededBitsPerValue(blockPalette.size());
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
    //Blocks end

    //Corners start
    public void setCornerPalette(int[] data) {
        int i = 0;
        for (int integer : data) {
            if (i == 0) {
                cornerPalette.set(i++, integer);
            } else {
                cornerPalette.add(i++, integer);
            }
        }
    }
    public int[] getCornerPalette() {
        int[] returnObj = new int[cornerPalette.size()];
        int i = 0;
        for (int corner : cornerPalette) {
            returnObj[i++] = corner;
        }
        return returnObj;
    }
    public int getCornerPaletteSize() {
        return cornerPalette.size();
    }
    public void setCornerData(int[] data) {
        cornerData = new BitBuffer(totalBlocks, getNeededBitsPerValue(cornerPalette.size()));
        cornerData.setData(data);
    }
    public int[] getCornerData() {
        return cornerData.getData();
    }
    public void setCornerKey(int pos, int key) {
        cornerData.setValue(pos, key);
    }
    public int getCornerKey(int pos) {
        return cornerData.getValue(pos);
    }
    public int getCorner(int pos) {
        int index = getCornerKey(pos);
        return cornerPalette.get(index);
    }
    public void setCorner(int pos, int corner, Vector3i globalPos) {
        boolean wasInPalette = false;
        int key = -1;
        for (int paletteEntry : cornerPalette) {
            key++;
            if (paletteEntry == corner) {
                setCornerKey(pos, key);
                wasInPalette = true;
                break;
            }
        }
        if (!wasInPalette) {
            cornerPalette.addLast(corner);
            int neededBitsPerValue = getNeededBitsPerValue(cornerPalette.size());
            if (neededBitsPerValue > cornerData.bitsPerValue) {
                BitBuffer newData = new BitBuffer(totalBlocks, neededBitsPerValue);
                for (int i = 0; i < totalBlocks; i++) {
                    newData.setValue(i, cornerData.getValue(i));
                }
                cornerData = newData;
            }
            setCornerKey(pos, cornerPalette.size()-1);
        }
        World.queueColumnUpdate(globalPos);
    }
    //Corners end
}