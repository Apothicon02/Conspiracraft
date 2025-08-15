package org.conspiracraft.game.world;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.conspiracraft.engine.BitBuffer;
import org.conspiracraft.engine.Utils;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

import static org.conspiracraft.engine.Utils.*;
import static org.conspiracraft.game.world.World.*;

public class Chunk {
    public final int condensedChunkPos;
    private static final int totalBlocks = chunkSize*chunkSize*chunkSize;
    public final IntArrayList blockPalette = new IntArrayList();
    private BitBuffer blockData = new BitBuffer(totalBlocks, 0);
    public final IntArrayList lightPalette = new IntArrayList();
    private BitBuffer lightData = new BitBuffer(totalBlocks, 0);
    public final IntArrayList cornerPalette = new IntArrayList();
    private BitBuffer cornerData = new BitBuffer(totalBlocks, 0);
    private BitBuffer subChunks = new BitBuffer(8, 1);

    public Chunk(int compressedChunkPos) {
        condensedChunkPos = compressedChunkPos;
        blockPalette.add(0);
        lightPalette.add(Utils.packColor(new Vector4i(0, 0, 0, 20)));
        cornerPalette.add(0);
    }
    public Chunk(int compressedChunkPos, Vector2i block, int sunLight) {
        condensedChunkPos = compressedChunkPos;
        blockPalette.add(Utils.packInts(block.x, block.y));
        lightPalette.add(Utils.packColor(new Vector4i(0, 0, 0, sunLight)));
        cornerPalette.add(0);
    }

    public int getNeededBitsPerValue(int uniqueValues) {
        return 32-Integer.numberOfLeadingZeros(uniqueValues);
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
    public void setSubChunks(int[] subChunksData) {
        subChunks.setData(subChunksData);
    }
    public int[] getSubChunks() {
        return subChunks.getData();
    }
    public void setBlockKey(Vector3i pos, int key, Vector3i globalPos) {
        int neededBitsPerValue = getNeededBitsPerValue(blockPalette.size());
        if (neededBitsPerValue != blockData.bitsPerValue) {
            BitBuffer newData = new BitBuffer(totalBlocks, neededBitsPerValue);
            for (int i = 0; i < totalBlocks; i++) {
                newData.setValue(i, blockData.getValue(i));
            }
            blockData = newData;
        }
        blockData.setValue(condenseLocalPos(pos), key);
        if (blockPalette.get(key) != 0) {
            subChunks.setValue(condenseSubchunkPos(pos.x >= World.subChunkSize ? 1 : 0, pos.y >= World.subChunkSize ? 1 : 0, pos.z >= World.subChunkSize ? 1 : 0), 1);
        } else {
            boolean isEmpty = true;
            for (int x = 0; x < chunkSize && isEmpty; x++) {
                for (int z = 0; z < chunkSize && isEmpty; z++) {
                    for (int y = 0; y < chunkSize && isEmpty; y++) {
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
                int chunkDataIndex = condensedChunkPos / 32;
                int bit = (condensedChunkPos - (chunkDataIndex * 32)) - 1;
                int prev = World.chunkEmptiness[chunkDataIndex];
                World.chunkEmptiness[chunkDataIndex] &= ~(1 << bit);
                if (prev != World.chunkEmptiness[chunkDataIndex]) {
                    chunkEmptinessChanged = true;
                }
            }
        }
    }
    public int getBlockKey(int pos) {
        return blockData.getValue(pos);
    }
    public Vector2i getBlock(int pos) {
        int index = getBlockKey(pos);
        return Utils.unpackInt(blockPalette.get(index));
    }
    public void setBlock(Vector3i pos, int type, int subType, Vector3i globalPos) {
        int block = Utils.packInts(type, subType);
        int key = blockPalette.indexOf(block);
        if (key > -1) {
            setBlockKey(pos, key, globalPos);
        } else {
            int chunkDataIndex = condensedChunkPos/32;
            int bit = (condensedChunkPos-(chunkDataIndex*32)) - 1;
            int prev = World.chunkEmptiness[chunkDataIndex];
            World.chunkEmptiness[chunkDataIndex] |= 1 << bit;
            if (prev != World.chunkEmptiness[chunkDataIndex]) {
                chunkEmptinessChanged = true;
            }
            blockPalette.addLast(block);
            setBlockKey(pos, blockPalette.size()-1, globalPos);
        }
    }
    //Blocks end

    //Corners start
    public int bitsPerCorner() {
        return cornerData.bitsPerValue;
    }
    public int cornerValueMask() {
        return cornerData.valueMask;
    }
    public int cornerValuesPerInt() {
        return cornerData.valuesPerInt;
    }
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
    public void setCornerKey(Vector3i pos, int key) {
        int neededBitsPerValue = getNeededBitsPerValue(cornerPalette.size());
        if (neededBitsPerValue != cornerData.bitsPerValue) {
            BitBuffer newData = new BitBuffer(totalBlocks, neededBitsPerValue);
            for (int i = 0; i < totalBlocks; i++) {
                newData.setValue(i, cornerData.getValue(i));
            }
            cornerData = newData;
        }
        cornerData.setValue(condenseLocalPos(pos), key);
        if (cornerPalette.get(key) != 0) {
            subChunks.setValue(condenseSubchunkPos(pos.x >= World.subChunkSize ? 1 : 0, pos.y >= World.subChunkSize ? 1 : 0, pos.z >= World.subChunkSize ? 1 : 0), 1);
        } else {
            boolean isEmpty = true;
            for (int x = 0; x < chunkSize && isEmpty; x++) {
                for (int z = 0; z < chunkSize && isEmpty; z++) {
                    for (int y = 0; y < chunkSize && isEmpty; y++) {
                        if (cornerPalette.get(getCornerKey(condenseLocalPos(x, y, z))) != 0) {
                            isEmpty = false;
                            break;
                        }
                    }
                }
            }
            if (isEmpty) {
                cornerPalette.clear();
                cornerPalette.add(0);
            }
        }
    }
    public int getCornerKey(int pos) {
        return cornerData.getValue(pos);
    }
    public int getCorner(int pos) {
        int index = getCornerKey(pos);
        return cornerPalette.get(index);
    }
    public void setCorner(Vector3i pos, int corner, Vector3i globalPos) {
        int key = cornerPalette.indexOf(corner);
        if (key > -1) {
            setCornerKey(pos, key);
        } else {
            cornerPalette.addLast(corner);
            setCornerKey(pos, cornerPalette.size()-1);
        }
    }
    //Corners end

    //Lights start
    public int bitsPerLight() {
        return lightData.bitsPerValue;
    }
    public int lightValueMask() {
        return lightData.valueMask;
    }
    public int lightValuesPerInt() {
        return lightData.valuesPerInt;
    }
    public void setLightPalette(int[] data) {
        int i = 0;
        for (int integer : data) {
            if (i == 0) {
                lightPalette.set(i++, integer);
            } else {
                lightPalette.add(i++, integer);
            }
        }
    }
    public int[] getLightPalette() {
        int[] returnObj = new int[lightPalette.size()];
        int i = 0;
        for (int light : lightPalette) {
            returnObj[i++] = light;
        }
        return returnObj;
    }
    public int getLightPaletteSize() {
        return lightPalette.size();
    }
    public void setLightData(int[] data) {
        lightData = new BitBuffer(totalBlocks, getNeededBitsPerValue(lightPalette.size()));
        lightData.setData(data);
    }
    public int[] getLightData() {
        return lightData.getData();
    }
    public static Vector4i fullSunlight = new Vector4i(0, 0, 0, 20);
    public void setLightKey(Vector3i pos, int key) {
        int neededBitsPerValue = getNeededBitsPerValue(lightPalette.size());
        if (neededBitsPerValue != lightData.bitsPerValue) {
            BitBuffer newData = new BitBuffer(totalBlocks, neededBitsPerValue);
            for (int i = 0; i < totalBlocks; i++) {
                newData.setValue(i, lightData.getValue(i));
            }
            lightData = newData;
        }
        lightData.setValue(Utils.condenseLocalPos(pos), key);
        if (!Utils.unpackColor(lightPalette.get(key)).equals(fullSunlight)) {
            subChunks.setValue(condenseSubchunkPos(pos.x >= World.subChunkSize ? 1 : 0, pos.y >= World.subChunkSize ? 1 : 0, pos.z >= World.subChunkSize ? 1 : 0), 1);
        } else {
            boolean isEmpty = true;
            for (int x = 0; x < chunkSize && isEmpty; x++) {
                for (int z = 0; z < chunkSize && isEmpty; z++) {
                    for (int y = 0; y < chunkSize && isEmpty; y++) {
                        if (lightPalette.get(getLightKey(condenseLocalPos(x, y, z))) != 0) {
                            isEmpty = false;
                            break;
                        }
                    }
                }
            }
            if (isEmpty) {
                lightPalette.clear();
                lightPalette.add(0);
            }
        }
    }
    public int getLightKey(int pos) {
        return lightData.getValue(pos);
    }
    public Vector4i getLight(int pos) {
        int index = getLightKey(pos);
        return Utils.unpackColor(lightPalette.get(index));
    }
    public void setLight(Vector3i pos, Vector4i unpackedLight, Vector3i globalPos) {
        int light = Utils.packColor(unpackedLight);
        int key = lightPalette.indexOf(light);
        if (key > -1) {
            setLightKey(pos, key);
        } else {
            lightPalette.addLast(light);
            setLightKey(pos, lightPalette.size()-1);
        }
    }
    public void setLight(Vector3i pos, int r, int g, int b, int s, Vector3i globalPos) {
        setLight(pos, new Vector4i(r, g, b, s), globalPos);
    }
    //Lights end
}