package org.conspiracraft.world;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.conspiracraft.utils.BitBuffer;
import org.joml.Vector2i;
import org.joml.Vector3i;

import static org.conspiracraft.world.World.chunkSize;

public class Chunk {
    public final Vector3i chunkPos;
    public final int condensedChunkPos;
    private static final int totalBlocks = chunkSize*chunkSize*chunkSize;
    public final IntArrayList blockPalette;
    public BitBuffer blockData;

    public Chunk(Vector3i chunkPos, int compressedChunkPos) {
        this.chunkPos = chunkPos;
        condensedChunkPos = compressedChunkPos;
        blockPalette = new IntArrayList(new int[]{0});
        blockData = new BitBuffer(totalBlocks, 0);
    }

    public static int condenseLocalPos(int x, int y, int z) {
        return (((x*World.chunkSize)+z)*World.chunkSize)+y;
    }
    public static int condenseLocalPos(Vector3i pos) {
        return (((pos.x*World.chunkSize)+pos.z)*World.chunkSize)+pos.y;
    }
    public static int packInts(int first4, int last4) {
        return (first4 << 16) | last4;
    }
    public static Vector2i unpackInt(int all8) {
        return new Vector2i(all8 >> 16, all8 & 0xFFFF);
    }

    public int getNeededBitsPerValue(int uniqueValues) {
        return 32-Integer.numberOfLeadingZeros(uniqueValues);
    }
    public void setChunkNonEmpty() {
        int chunkDataIndex = condensedChunkPos/32;
        int bit = (condensedChunkPos-(chunkDataIndex*32)) - 1;
        int prev = World.chunkEmptiness[chunkDataIndex];
        World.chunkEmptiness[chunkDataIndex] |= 1 << bit;
        if (prev != World.chunkEmptiness[chunkDataIndex]) {
            World.chunkEmptinessChanged = true;
        }
    }
    //Blocks start

    public int bitsPerBlock() {
        return blockData.bitsPerValue;
    }
    public int blockValueMask() {
        return blockData.valueMask;
    }
    public int blockValuesPerInt() {
        return blockData.valuesPerInt;
    }
    public void setBlockPalette(int[] data) {
        int i = 0;
        for (int integer : data) {
            if (i == 0) {
                blockPalette.set(i++, integer);
            } else {
                blockPalette.add(i++, integer);
            }
        }
    }
    public int[] getBlockPalette() {
        int[] returnObj = new int[blockPalette.size()];
        int i = 0;
        for (int block : blockPalette) {
            returnObj[i++] = block;
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
    public void updateBlockPaletteKeySize() {
        int neededBitsPerValue = getNeededBitsPerValue(blockPalette.size());
        if (neededBitsPerValue != blockData.bitsPerValue) {
            BitBuffer newData = new BitBuffer(totalBlocks, neededBitsPerValue);
            for (int i = 0; i < totalBlocks; i++) {
                newData.setValue(i, blockData.getValue(i));
            }
            blockData = newData;
        }
    }
    public void setBlockKey(int pos, int key) {
        updateBlockPaletteKeySize();
        blockData.setValue(pos, key);
        if (blockPalette.get(key) == 0) {
            boolean isEmpty = true;
            for (int x = 0; x < chunkSize && isEmpty; x++) {
                for (int z = 0; z < chunkSize && isEmpty; z++) {
                    for (int y = 0; y < chunkSize; y++) {
                        if (blockPalette.get(getBlockKey(condenseLocalPos(x, y, z))) != 0) {
                            isEmpty = false;
                            break;
                        }
                    }
                }
            }
            if (isEmpty) {
                blockPalette.clear();
                blockPalette.add(0);
                setChunkNonEmpty();
            }
        }
    }
    public void setBlockKey(int x, int y, int z, int keys) {
        setBlockKey(condenseLocalPos(x, y, z), keys);
    }
    public int getBlockKey(int pos) {
        return blockData.getValue(pos);
    }
    public Vector2i getBlock(int pos) {
        int index = getBlockKey(pos);
        return unpackInt(blockPalette.get(index));
    }
    public void setBlock(int x, int y, int z, int type, int subType) {
        int block = packInts(type, subType);
        int key = blockPalette.indexOf(block);
        if (key > -1) {
            setBlockKey(x, y, z, key);
        } else {
            setChunkNonEmpty();
            blockPalette.addLast(block);
            setBlockKey(x, y, z, blockPalette.size() - 1);
        }
    }
    //Blocks end
}
