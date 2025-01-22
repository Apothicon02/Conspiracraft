package org.conspiracraft.game.world;

import org.conspiracraft.engine.Utils;
import org.conspiracraft.game.blocks.Block;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

import java.util.*;

import static org.conspiracraft.game.world.World.chunkSize;

public class Chunk {
    private static final int totalBlocks = chunkSize*chunkSize*chunkSize;
    public static final Vector2i air = new Vector2i(0, 0);
    public static final Vector4i fullSunLight = new Vector4i(0, 0, 0, 20);
    private byte[] corners = new byte[totalBlocks];
    int[] blockData = new int[totalBlocks/32];
    private BitSet lightBools = new BitSet(totalBlocks);
    private byte[] lightBytes = null;
    private short[] lightShorts = null;
    private List<Vector2i> blockPalette = new ArrayList<>(List.of(new Vector2i(0, 0)));
    private List<Vector4i> lightPalette = new ArrayList<>();

    public Chunk() {
        Arrays.fill(corners, Utils.convertBoolArrayToByte(new boolean[]{true, true, false, true, true, true, true, true}));
    }

    public byte getCorners(int pos) {
        return corners[pos];
    }
    public void setLights(int[] lights) {
        if (lightPalette.size() < 2) {
            if (lightBools == null) {
                lightBools = new BitSet(totalBlocks);
            }
            lightBytes = null;
            lightShorts = null;
            for (int i = 0; i < totalBlocks/32; i++) {
                int bits = lights[i];
                int offset = i*32;
                for (int e = 0; e <= 31; e++) {
                    lightBools.set(offset+e, (bits & (1 << e)) != 0);
                }
            }
        } else if (lightPalette.size() < 127) {
            lightBools = null;
            lightBytes = new byte[totalBlocks];
            lightShorts = null;
            int i = 0;
            if (lightPalette.size() <= 3) {
                for (int light : lights) {
                    int[] values = Utils.unpackPacked16Ints(light);
                    for (int value : values) {
                        lightBytes[i++] = (byte) (value);
                    }
                }
            } else {
                for (int light : lights) {
                    Vector4i vec = Utils.unpackPacked4Ints(light);
                    lightBytes[i++] = (byte) (vec.x);
                    lightBytes[i++] = (byte) (vec.y);
                    lightBytes[i++] = (byte) (vec.z);
                    lightBytes[i++] = (byte) (vec.w);
                }
            }
        } else {
            lightBools = null;
            lightBytes = null;
            lightShorts = new short[totalBlocks];
            int i = 0;
            for (int light : lights) {
                Vector2i vec = Utils.unpackInt(light);
                lightShorts[i++] = (short) (vec.x);
                lightShorts[i++] = (short) (vec.y);
            }
        }
    }
    public int[] getAllLights() {
        if (lightBools != null) {
            int[] returnObj = new int[totalBlocks/32];
            for (int i = 0; i < totalBlocks/32; i++) {
                returnObj[i] = Utils.packBools(lightBools.get(i*32, (i*32)+32));
            }
            return returnObj;
        } else if (lightBytes != null) {
            if (lightPalette.size() <= 3) {
                int[] returnObj = new int[totalBlocks/16];
                for (int i = 0; i < totalBlocks/16; i++) {
                    returnObj[i] = Utils.pack16Ints(new int[]{getLightKey(i*16), getLightKey((i*16)+1), getLightKey((i*16)+2), getLightKey((i*16)+3),
                            getLightKey((i*16)+4), getLightKey((i*16)+5), getLightKey((i*16)+6), getLightKey((i*16)+7),
                            getLightKey((i*16)+8), getLightKey((i*16)+9), getLightKey((i*16)+10), getLightKey((i*16)+11),
                            getLightKey((i*16)+12), getLightKey((i*16)+13), getLightKey((i*16)+14), getLightKey((i*16)+15)});
                }
                return returnObj;
            } else {
                int[] returnObj = new int[totalBlocks / 4];
                for (int i = 0; i < totalBlocks / 4; i++) {
                    returnObj[i] = Utils.pack4Ints(getLightKey(i * 4), getLightKey((i * 4) + 1), getLightKey((i * 4) + 2), getLightKey((i * 4) + 3));
                }
                return returnObj;
            }
        } else {
            int[] returnObj = new int[totalBlocks/2];
            for (int i = 0; i < totalBlocks/2; i++) {
                returnObj[i] = Utils.packInts(getLightKey(i*2), getLightKey((i*2)+1));
            }
            return returnObj;
        }
    }
    public void setBlocks(int[] blocks) {
        blockData = blocks;
    }
    public int[] getAllBlocks() {
        return blockData;
    }
    public void setPalette(int[] palette) {
        for (int integer : palette) {
            blockPalette.addLast(Utils.unpackInt(integer));
        }
    }
    public int[] getPalette() {
        int[] returnObj = new int[blockPalette.size()];
        for (int i = 0; i < returnObj.length; i++) {
            Vector2i block = blockPalette.get(i);
            returnObj[i] = Utils.packInts(block.x, block.y);
        }
        return returnObj;
    }
    public void setLightPalette(int[] palette) {
        for (int color : palette) {
            lightPalette.addLast(new Vector4i(0xFF & color >> 16, 0xFF & color >> 8, 0xFF & color, 0xFF & color >> 24));
        }
    }
    public int[] getLightPalette() {
        int[] returnObj = new int[lightPalette.size()];
        for (int i = 0; i < returnObj.length; i++) {
            Vector4i light = lightPalette.get(i);
            returnObj[i] = light.x() << 16 | light.y() << 8 | light.z() | light.w() << 24;
        }
        return returnObj;
    }
    public int getPaletteSize() {
        return blockPalette.size();
    }
    public int getLightPaletteSize() {
        return lightPalette.size();
    }
    public int getBlockKey(int pos) {
        if (blockPalette.size() < 3) {
            int intPos = pos/32;
            BitSet bits = Utils.unpackBools(blockData[intPos]);
            return bits.get(pos-(intPos*32)) ? 1 : 0;
        } else if (blockPalette.size() < 128) {
            if (blockPalette.size() <= 4) {
                int intPos = pos/16;
                int[] bits = Utils.unpackPacked16Ints(blockData[intPos]);
                return bits[(pos-(intPos*16))];
            } else {
                int intPos = pos/4;
                Vector4i bits = Utils.unpackPacked4Ints(blockData[intPos]);
                int bit = pos-(intPos*4);
                return bit == 0 ? bits.x : (bit == 1 ? bits.y : (bit == 2 ? bits.z : bits.w));
            }
        } else {
            int intPos = pos/2;
            Vector2i bits = Utils.unpackInt(blockData[intPos]);
            int bit = pos-(intPos*2);
            return bit == 0 ? bits.x : bits.y;
        }
    }
    public void setBlockKey(int pos, int key) {
        if (blockPalette.size() < 3) {
            int intPos = pos/32;
            BitSet bits = Utils.unpackBools(blockData[intPos]);
            bits.set(pos-(intPos*32), key);
            blockData[intPos] = Utils.packBools(bits);
        } else if (blockPalette.size() < 128) {
            if (blockPalette.size() <= 4) {
                int intPos = pos/16;
                int[] bits = Utils.unpackPacked16Ints(blockData[intPos]);
                bits[(pos-(intPos*16))] = key;
                blockData[intPos] = Utils.pack16Ints(bits);
            } else {
                int intPos = pos/4;
                Vector4i bits = Utils.unpackPacked4Ints(blockData[intPos]);
                int bit = pos-(intPos*4);
                if (bit == 0) {
                    bits.x = key;
                } else if (bit == 1) {
                    bits.y = key;
                } else if (bit == 2) {
                    bits.z = key;
                } else {
                    bits.w = key;
                }
                blockData[intPos] = Utils.pack4Ints(bits.x, bits.y, bits.z, bits.z);
            }
        } else {
            int intPos = pos/2;
            Vector2i bits = Utils.unpackInt(blockData[intPos]);
            int bit = pos-(intPos*2);
            if (bit == 0) {
                bits.x = key;
            } else {
                bits.y = key;
            }
            blockData[intPos] = Utils.packInts(bits.x, bits.y);
        }
    }
    public int getLightKey(int pos) {
        if (lightShorts != null) {
            return lightShorts[pos];
        } else if (lightBytes != null) {
            return lightBytes[pos];
        } else {
            return lightBools.get(pos) ? 0 : -1;
        }
    }
    public Block getBlock(int pos) {
        int index = getBlockKey(pos);
        Vector2i id = blockPalette.get(index);
        int lightIndex = getLightKey(pos); //get the blockPalette index from the blocks array
        Vector4i light;
        if (lightIndex >= 0) {
            light = lightPalette.get(lightIndex);
        } else {
            light = fullSunLight;
        }
        return new Block(id.x, id.y, (byte) light.x, (byte) light.y, (byte) light.z, (byte) light.w);
    }
    public void setBlockPaletteId(int pos, int id) {
        if (blockPalette.size() < 3) {
            int intPos = pos/32;
            BitSet bits = Utils.unpackBools(blockData[intPos]);
            bits.set(pos-(intPos*32), id == 1);
            blockData[intPos] = Utils.packBools(bits);
        } else if (blockPalette.size() < 128) {
            if (blockPalette.size() <= 4) {
                int intPos = pos/16;
                int[] bits = Utils.unpackPacked16Ints(blockData[intPos]);
                bits[pos-(intPos*16)] = id;
                blockData[intPos] = Utils.pack16Ints(bits);
            } else {
                int intPos = pos/4;
                Vector4i bits = Utils.unpackPacked4Ints(blockData[intPos]);
                int bit = pos-(intPos*4);
                if (bit == 0) {
                    bits.x = id;
                } else if (bit == 1) {
                    bits.y = id;
                } else if (bit == 2) {
                    bits.z = id;
                } else {
                    bits.w = id;
                }
                blockData[intPos] = Utils.pack4Ints(bits.x, bits.y, bits.z, bits.w);
            }
        } else {
            int intPos = pos/2;
            Vector2i bits = Utils.unpackInt(blockData[intPos]);
            int bit = pos-(intPos*2);
            if (bit == 0) {
                bits.x = id;
            } else {
                bits.y = id;
            }
            blockData[intPos] = Utils.packInts(bits.x, bits.y);
        }
    }
    public void setBlock(int pos, Block block, Vector3i globalPos) {
        boolean wasInPalette = false;
        for (int i = 0; i < blockPalette.size(); i++) { //iterate through blockPalette until finding a matching block, and upon doing so set the blockPalette index for that block position to the index of the matching blockPalette entry
            if (block.idVec().equals(blockPalette.get(i))) {
                setBlockPaletteId(pos, i);
                wasInPalette = true;
                break;
            }
        }
        if (!wasInPalette) {
            int oldSize = blockPalette.size();
            //add to palette
            blockPalette.addLast(block.idVec());
            //update compressed data to be in proper format for the new palette size if necessary
            if (oldSize == 2) {
                int[] newBlocks = new int[totalBlocks/16];
                int i = 0;
                for (int packed : blockData) {
                    BitSet oldBlocks = Utils.unpackBools(packed);
                    for (int section = 0; section < 2; section++) {
                        int[] sectionData = new int[16];
                        int e = 0;
                        for (int oldBlock = section*16; oldBlock < (section+1)*16; oldBlock++) {
                            sectionData[e] = oldBlocks.get(oldBlock) ? 1 : 0;
                            e++;
                        }
                        newBlocks[i] = Utils.pack16Ints(sectionData);
                        i++;
                    }
                }
                blockData = newBlocks;
            } else if (oldSize == 4) {
                int[] newBlocks = new int[totalBlocks/4];
                int i = 0;
                for (int packed : blockData) {
                    int[] oldBlocks = Utils.unpackPacked16Ints(packed);
                    for (int section = 0; section < 4; section++) {
                        int[] sectionData = new int[4];
                        int e = 0;
                        for (int oldBlock = section*4; oldBlock < (section+1)*4; oldBlock++) {
                            sectionData[e] = oldBlocks[oldBlock];
                            e++;
                        }
                        newBlocks[i] = Utils.pack4Ints(sectionData[0], sectionData[1], sectionData[2], sectionData[3]);
                        i++;
                    }
                }
                blockData = newBlocks;
            } else if (oldSize == 127) {
                int[] newBlocks = new int[totalBlocks/2];
                int i = 0;
                for (int packed : blockData) {
                    Vector4i oldVec = Utils.unpackPacked4Ints(packed);
                    int[] oldBlocks = new int[]{oldVec.x, oldVec.y, oldVec.z, oldVec.w};
                    for (int section = 0; section < 2; section++) {
                        int[] sectionData = new int[2];
                        int e = 0;
                        for (int oldBlock = section*2; oldBlock < (section+1)*2; oldBlock++) {
                            sectionData[e] = oldBlocks[oldBlock];
                            e++;
                        }
                        newBlocks[i] = Utils.packInts(sectionData[0], sectionData[1]);
                        i++;
                    }
                }
                blockData = newBlocks;
            }
            //finally, set the block
            setBlockPaletteId(pos, blockPalette.size());
        }
        wasInPalette = false;
        for (int i = 0; i < lightPalette.size(); i++) { //iterate through blockPalette until finding a matching block, and upon doing so set the blockPalette index for that block position to the index of the matching blockPalette entry
            if (block.light().equals(lightPalette.get(i))) {
                if (lightShorts != null) {
                    lightShorts[pos] = (short) i;
                } else if (lightBytes != null) {
                    lightBytes[pos] = (byte) i;
                } else {
                    lightBools.set(pos, true);
                }
                wasInPalette = true;
                break;
            }
        }
        if (!wasInPalette) {
            int size = lightPalette.size();
            if (size == 127 && lightBytes != null) {
                lightShorts = new short[totalBlocks];
                Arrays.fill(lightShorts, (short) -1);
                for (int i = 0; i < lightBytes.length; i++) {
                    lightShorts[i] = lightBytes[i];
                }
                lightBytes = null;
            } else if (size == 1 && lightBools != null) {
                lightBytes = new byte[totalBlocks];
                Arrays.fill(lightBytes, (byte) -1);
                for (int i = 0; i < lightBools.length(); i++) {
                    lightBytes[i] = (byte) (lightBools.get(i) ? 0 : -1);
                }
                lightBools = null;
            }

            int index = lightPalette.size();
            for (int i = 1; i < lightPalette.size(); i++) {
                if (lightPalette.get(i) == null) {
                    index = i;
                    break;
                }
            }
            if (index == size) {
                lightPalette.addLast(null);
            }
            lightPalette.set(index, block.light());
            if (lightShorts != null) {
                lightShorts[pos] = (short) index;
            } else if (lightBytes != null) {
                lightBytes[pos] = (byte) index;
            } else {
                lightBools.set(pos, true);

            }
        }
        if (World.worldGenerated) {
            World.queueCleaning(globalPos);
        }
        World.queueColumnUpdate(globalPos);
    }
    public void cleanPalette() {
        if (!blockPalette.isEmpty()) {
            //count usages of each palette entry
            int oldSize = blockPalette.size();
            int[] pointerUses = new int[oldSize];
            for (int pos = 0; pos < totalBlocks; pos++) {
                pointerUses[getBlockKey(pos)]++;
            }
            //create new palette leaving out unused entries from the old palette
            List<Vector2i> newPalette = new ArrayList<>();
            int entryIndex = 0;
            for (Vector2i entry : blockPalette) {
                if (pointerUses[entryIndex] >= 0) {
                    newPalette.addLast(entry);
                }
                entryIndex++;
            }
            int size = newPalette.size();
            //update compression format for block data and clean palette indexes if new palette size is different enough
            int oldBitsPerBlock = oldSize < 3 ? 1 : (oldSize < 128 ? (oldSize <= 4 ? 2 : 8) : 16);
            int bitsPerBlock = size < 3 ? 1 : (size < 128 ? (size <= 4 ? 2 : 8) : 16);
            if (oldBitsPerBlock != bitsPerBlock) {
                int[] uncompressedBlocks = new int[totalBlocks];
                for (int pos = 0; pos < totalBlocks; pos++) {
                    uncompressedBlocks[pos] = getBlockKey(pos);
                }
                blockPalette = newPalette;
                int pos = 0;
                for (int block : uncompressedBlocks) {
                    setBlockKey(pos, block);
                    pos++;
                }
            }
        }

        if (!lightPalette.isEmpty()) {
            int[] pointerUses = new int[lightPalette.size()+1];
            if (lightShorts != null) {
                for (short light : lightShorts) {
                    if (light >= 0) {
                        pointerUses[light] += 1;
                    }
                }
                List<Vector4i> newPalette = new ArrayList<>();
                for (short entry = 0; entry < lightPalette.size(); entry++) {
                    int pointer = pointerUses[entry];
                    if (pointer >= 0) {
                        newPalette.addLast(lightPalette.get(entry));
                    }
                }
                if (newPalette.size() < 2) {
                    lightBools = new BitSet(totalBlocks);
                    for (int i = 0; i < totalBlocks; i++) {
                        byte index = (byte) lightShorts[i];
                        if (index >= 0) {
                            Vector4i light = lightPalette.get(index);
                            if (newPalette.contains(light)) {
                                lightBools.set(i, true);
                                break;
                            }
                        }
                    }
                    lightShorts = null;
                } else if (newPalette.size() < 127) {
                    lightBytes = new byte[totalBlocks];
                    Arrays.fill(lightBytes, (byte) -1);
                    for (int i = 0; i < totalBlocks; i++) {
                        byte index = (byte) lightShorts[i];
                        if (index >= 0) {
                            Vector4i light = lightPalette.get(index);
                            for (byte p = 0; p < newPalette.size(); p++) {
                                if (newPalette.get(p).equals(light)) {
                                    lightBytes[i] = p;
                                    break;
                                }
                            }
                        }
                    }
                    lightShorts = null;
                } else {
                    for (int i = 0; i < totalBlocks; i++) {
                        short index = lightShorts[i];
                        if (index >= 0) {
                            Vector4i light = lightPalette.get(index);
                            for (short p = 0; p < newPalette.size(); p++) {
                                if (newPalette.get(p).equals(light)) {
                                    lightShorts[i] = p;
                                    break;
                                }
                            }
                        }
                    }
                }
                lightPalette = newPalette;
            } else if (lightBytes != null) {
                for (byte light : lightBytes) {
                    if (light >= 0) {
                        pointerUses[light] += 1;
                    }
                }
                List<Vector4i> newPalette = new ArrayList<>();
                for (byte entry = 0; entry < lightPalette.size(); entry++) {
                    int pointer = pointerUses[entry];
                    if (pointer >= 0) {
                        newPalette.addLast(lightPalette.get(entry));
                    }
                }
                if (newPalette.size() < 2) {
                    lightBools = new BitSet(totalBlocks);
                    for (int i = 0; i < totalBlocks; i++) {
                        byte index = lightBytes[i];
                        if (index >= 0) {
                            Vector4i light = lightPalette.get(index);
                            if (newPalette.contains(light)) {
                                lightBools.set(i, true);
                                break;
                            }
                        }
                    }
                    lightBytes = null;
                } else {
                    for (int i = 0; i < totalBlocks; i++) {
                        byte index = lightBytes[i];
                        if (index >= 0) {
                            Vector4i light = lightPalette.get(index);
                            for (byte p = 0; p < newPalette.size(); p++) {
                                if (newPalette.get(p).equals(light)) {
                                    lightBytes[i] = p;
                                }
                            }
                        }
                    }
                }
                lightPalette = newPalette;
            }
        }
    }
}