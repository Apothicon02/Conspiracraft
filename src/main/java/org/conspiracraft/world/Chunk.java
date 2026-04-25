package org.conspiracraft.world;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.conspiracraft.utils.BitBuffer;
import org.joml.Vector2i;
import org.joml.Vector3i;

import static org.conspiracraft.world.World.chunkSize;
import static org.conspiracraft.world.World.worldgeneratedBlocks;

public class Chunk {
    public final Vector3i chunkPos;
    public final int condensedChunkPos;
    private static final int totalBlocks = chunkSize*chunkSize*chunkSize;
    public int[] uncompressedBlocks;
    public IntArrayList blockPalette;
    public BitBuffer blockData;
    public static boolean unedited = true;

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
    public void setBlockUncompressed(Vector3i pos, int type, int subType) {
        if (uncompressedBlocks == null) {
            uncompressedBlocks = new int[totalBlocks];
            setChunkNonEmpty();
        }
        uncompressedBlocks[condenseLocalPos(pos)] = packInts(type, subType);
    }
    public Vector2i getBlockUncompressed(int pos) {
        return uncompressedBlocks == null ? new Vector2i(0) : unpackInt(uncompressedBlocks[pos]);
    }

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
    public void setBlockKey(Vector3i pos, int key, Vector3i globalPos) {
        updateBlockPaletteKeySize();
        blockData.setValue(condenseLocalPos(pos), key);
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
    public int getBlockKey(int pos) {
        return blockData.getValue(pos);
    }
    public Vector2i getBlock(int pos) {
        int index = getBlockKey(pos);
        return unpackInt(blockPalette.get(index));
    }
    public static final IntArrayList defaultBlockPalette = new IntArrayList(new int[]{0, 65550, 65551, 3604480, 1507328, 1572864, 262145, 131072, 196608, 262144, 262146, 1179650, 327680, 327681, 327682, 1179648, 1179649, 3538944, 131073, 262149, 262150, 262148, 655360, 3670016, 3145728, 1048576, 1114112, 2031616, 2097152});
    public void setBlock(Vector3i pos, int type, int subType, Vector3i globalPos) {
        int block = packInts(type, subType);
//        if (World.generating) {
//            if (!defaultBlockPalette.contains(block)) {
//                worldgeneratedBlocks.addIfAbsent(block);
//            }
//        }
        if (unedited) {
            unedited = false;
            if (World.generating) {
                blockPalette = new IntArrayList(defaultBlockPalette);
            }
        }
        int key = blockPalette.indexOf(block);
        if (key > -1) {
            setBlockKey(pos, key, globalPos);
        } else {
            setChunkNonEmpty();
            blockPalette.addLast(block);
            setBlockKey(pos, blockPalette.size()-1, globalPos);
        }
    }
    //Blocks end
}
