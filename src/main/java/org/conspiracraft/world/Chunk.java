package org.conspiracraft.world;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.conspiracraft.utils.BitBuffer;
import org.joml.Vector2i;
import org.joml.Vector3i;

import static org.conspiracraft.world.World.chunkSize;

public class Chunk {
    public final int condensedChunkPos;
    public static final int totalVoxels = chunkSize*chunkSize*chunkSize;
    public final IntArrayList blockPalette;
    public BitBuffer blockData;
    public final IntArrayList lightPalette;
    public BitBuffer lightData;
    public boolean[] lightUpdateArr;
    public boolean[] lightUpdateArr() {
        if (lightUpdateArr == null) {
            lightUpdateArr = new boolean[totalVoxels];
            LightHelper.dirtyChunks.add(this);
        }
        return lightUpdateArr;
    }

    public Chunk(int compressedChunkPos) {
        condensedChunkPos = compressedChunkPos;
        blockPalette = new IntArrayList(new int[]{0});
        blockData = new BitBuffer(totalVoxels, 0);
        lightPalette = new IntArrayList(new int[]{fullSunlight});
        lightData = new BitBuffer(totalVoxels, 0);
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
        blockData = new BitBuffer(totalVoxels, getNeededBitsPerValue(blockPalette.size()));
        blockData.setData(data);
    }
    public int[] getBlockData() {
        return blockData.getData();
    }
    public void updateBlockPaletteKeySize() {
        int neededBitsPerValue = getNeededBitsPerValue(blockPalette.size());
        if (neededBitsPerValue != blockData.bitsPerValue) {
            BitBuffer newData = new BitBuffer(totalVoxels, neededBitsPerValue);
            for (int i = 0; i < totalVoxels; i++) {
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
            blockPalette.addLast(block);
            setBlockKey(x, y, z, blockPalette.size() - 1);
        }
    }
    //Blocks end
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
        lightData = new BitBuffer(totalVoxels, getNeededBitsPerValue(lightPalette.size()));
        lightData.setData(data);
    }
    public int[] getLightData() {
        return lightData.getData();
    }
    public void updateLightPaletteKeySize() {
        int neededBitsPerValue = getNeededBitsPerValue(lightPalette.size());
        if (neededBitsPerValue != lightData.bitsPerValue) {
            BitBuffer newData = new BitBuffer(totalVoxels, neededBitsPerValue);
            for (int i = 0; i < totalVoxels; i++) {
                newData.setValue(i, lightData.getValue(i));
            }
            lightData = newData;
        }
    }
    public static int fullSunlight = packLight(0, 0, 0, 32);
    public void setLightKey(int pos, int key) {
        updateLightPaletteKeySize();
        lightData.setValue(pos, key);
        if (lightPalette.get(key) == fullSunlight) {
            boolean isEmpty = true;
            for (int x = 0; x < chunkSize && isEmpty; x++) {
                for (int z = 0; z < chunkSize && isEmpty; z++) {
                    for (int y = 0; y < chunkSize; y++) {
                        if (lightPalette.get(getLightKey(condenseLocalPos(x, y, z))) != fullSunlight) {
                            isEmpty = false;
                            break;
                        }
                    }
                }
            }
            if (isEmpty) {
                lightPalette.clear();
                lightPalette.add(fullSunlight);
            }
        }
    }
    public void setLightKey(int x, int y, int z, int keys) {
        setLightKey(condenseLocalPos(x, y, z), keys);
    }
    public int getLightKey(int pos) {
        return lightData.getValue(pos);
    }
    public Light getLight(int pos) {
        int index = getLightKey(pos);
        return unpackLight(lightPalette.get(index));
    }
    public void setLight(int x, int y, int z, byte r, byte g, byte b, byte s) {
        setLight(x, y, z, packLight(r, g, b, s));
    }
    public void setLight(int x, int y, int z, Light light) {
        setLight(x, y, z, packLight(light));
    }
    public void setLight(int x, int y, int z, int light) {
        int key = lightPalette.indexOf(light);
        if (key > -1) {
            setLightKey(x, y, z, key);
        } else {
            lightPalette.addLast(light);
            setLightKey(x, y, z, lightPalette.size() - 1);
        }
    }
    public static int packLight(int r, int g, int b, int s) {
        return r << 16 | g << 8 | b | s << 24;
    }
    public static int packLight(byte r, byte g, byte b, byte s) {
        return r << 16 | g << 8 | b | s << 24;
    }
    public static int packLight(Light light) {
        return light.r() << 16 | light.g() << 8 | light.b() | light.s() << 24;
    }
    public static Light unpackLight(int color) {
        return new Light(0xFF & color >> 16, 0xFF & color >> 8, 0xFF & color, 0xFF & color >> 24);
    }
    //Lights end
}
