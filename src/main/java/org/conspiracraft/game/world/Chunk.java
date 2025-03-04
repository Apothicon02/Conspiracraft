package org.conspiracraft.game.world;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.conspiracraft.engine.BitBuffer;
import org.conspiracraft.engine.Utils;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

import static org.conspiracraft.engine.Utils.condenseLocalPos;
import static org.conspiracraft.engine.Utils.condenseSubchunkPos;
import static org.conspiracraft.game.world.World.chunkSize;

public class Chunk {
    private static final int totalBlocks = chunkSize*chunkSize*chunkSize;
    private final IntArrayList blockPalette = new IntArrayList();
    private BitBuffer blockData = new BitBuffer(totalBlocks, 0);
    private final IntArrayList lightPalette = new IntArrayList();
    private BitBuffer lightData = new BitBuffer(totalBlocks, 0);
    private final IntArrayList cornerPalette = new IntArrayList();
    private BitBuffer cornerData = new BitBuffer(totalBlocks, 0);
    private BitBuffer subChunks = new BitBuffer(8, 1);

    public Chunk() {
        blockPalette.add(0);
        lightPalette.add(Utils.packColor(new Vector4i(0, 0, 0, 20)));
        cornerPalette.add(0);
    }

    public int getNeededBitsPerValue(int uniqueValues) {
        return (int) Math.ceil(Math.log(uniqueValues) / Math.log(2));
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
    public void setBlockKey(Vector3i pos, int key) {
        if (blockPalette.get(key) != 0) {
            subChunks.setValue(condenseSubchunkPos(pos.x >= World.subChunkSize ? 1 : 0, pos.y >= World.subChunkSize ? 1 : 0, pos.z >= World.subChunkSize ? 1 : 0), 1);
        }
        blockData.setValue(condenseLocalPos(pos), key);
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
        boolean wasInPalette = false;
        int key = -1;
        for (int paletteEntry : blockPalette) {
            key++;
            if (paletteEntry == block) {
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
        if (!Utils.unpackColor(lightPalette.get(key)).equals(fullSunlight)) {
            subChunks.setValue(condenseSubchunkPos(pos.x >= World.subChunkSize ? 1 : 0, pos.y >= World.subChunkSize ? 1 : 0, pos.z >= World.subChunkSize ? 1 : 0), 1);
        }
        lightData.setValue(Utils.condenseLocalPos(pos), key);
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
        boolean wasInPalette = false;
        int key = -1;
        for (int paletteEntry : lightPalette) {
            key++;
            if (paletteEntry == light) {
                setLightKey(pos, key);
                wasInPalette = true;
                break;
            }
        }
        if (!wasInPalette) {
            lightPalette.addLast(light);
            int neededBitsPerValue = getNeededBitsPerValue(lightPalette.size());
            if (neededBitsPerValue > lightData.bitsPerValue) {
                BitBuffer newData = new BitBuffer(totalBlocks, neededBitsPerValue);
                for (int i = 0; i < totalBlocks; i++) {
                    newData.setValue(i, lightData.getValue(i));
                }
                lightData = newData;
            }
            setLightKey(pos, lightPalette.size()-1);
        }
        World.queueColumnUpdate(globalPos);
    }
    public void setLight(Vector3i pos, int r, int g, int b, int s, Vector3i globalPos) {
        setLight(pos, new Vector4i(r, g, b, s), globalPos);
    }
    //Lights end
}