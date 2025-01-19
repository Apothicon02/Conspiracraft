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
    private BitSet blockBools = new BitSet(totalBlocks);
    private byte[] blockBytes = null;
    private short[] blockShorts = null;
    private BitSet lightBools = new BitSet(totalBlocks);
    private byte[] lightBytes = null;
    private short[] lightShorts = null;
    private List<Vector2i> blockPalette = new ArrayList<>();
    private List<Vector4i> lightPalette = new ArrayList<>();

    public Chunk() {
        Arrays.fill(corners, Utils.convertBoolArrayToByte(new boolean[]{true, true, false, true, true, true, true, true}));
    }

    public byte getCorners(int pos) {
        return corners[pos];
    }
    public void setLights(List<Integer> lights) {
        if (lightPalette.size() < 2) {
            if (lightBools == null) {
                lightBools = new BitSet(totalBlocks);
            }
            lightBytes = null;
            lightShorts = null;
            for (int i = 0; i < totalBlocks; i++) {
                lightBools.set(i, lights.get(i) >= 0);
            }
        } else if (lightPalette.size() < 127) {
            lightBools = null;
            lightBytes = new byte[totalBlocks];
            lightShorts = null;
            for (int i = 0; i < totalBlocks; i++) {
                lightBytes[i] = lights.get(i).byteValue();
            }
        } else {
            lightBools = null;
            lightBytes = null;
            lightShorts = new short[totalBlocks];
            for (int i = 0; i < totalBlocks; i++) {
                lightShorts[i] = lights.get(i).shortValue();
            }
        }
    }
    public int[] getAllLights() {
        int[] returnObj = new int[totalBlocks];
        for (int i = 0; i < totalBlocks/2; i++) {
            returnObj[i] = Utils.packInts(getLightKey((i*2)), getLightKey((i*2)+1));
        }
        return returnObj;
    }
    public int[] getAllLightsUncompressed() {
        int[] returnObj = new int[totalBlocks];
        for (int i = 0; i < totalBlocks; i++) {
            returnObj[i] = getLightKey(i);
        }
        return returnObj;
    }
    public void setBlocks(List<Integer> blocks) {
        if (blockPalette.size() < 2) {
            if (blockBools == null) {
                blockBools = new BitSet(totalBlocks);
            }
            blockBytes = null;
            blockShorts = null;
            for (int i = 0; i < totalBlocks; i++) {
                blockBools.set(i, blocks.get(i) >= 0);
            }
        } else if (blockPalette.size() < 127) {
            blockBools = null;
            blockBytes = new byte[totalBlocks];
            blockShorts = null;
            for (int i = 0; i < totalBlocks; i++) {
                blockBytes[i] = blocks.get(i).byteValue();
            }
        } else {
            blockBools = null;
            blockBytes = null;
            blockShorts = new short[totalBlocks];
            for (int i = 0; i < totalBlocks; i++) {
                blockShorts[i] = blocks.get(i).shortValue();
            }
        }
    }
    public int[] getAllBlocks() {
        int[] returnObj = new int[totalBlocks/2];
        for (int i = 0; i < totalBlocks/2; i++) {
            returnObj[i] = Utils.packInts(getBlockKey((i*2)), getBlockKey((i*2)+1));
        }
        return returnObj;
    }
    public int[] getAllBlocksUncompressed() {
        int[] returnObj = new int[totalBlocks];
        for (int i = 0; i < totalBlocks; i++) {
            returnObj[i] = getBlockKey(i);
        }
        return returnObj;
    }
    public void setPalette(List<Integer> palette) {
        for (Integer integer : palette) {
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
    public void setLightPalette(List<Integer> palette) {
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
        if (blockShorts != null) {
            return blockShorts[pos];
        } else if (blockBytes != null) {
            return blockBytes[pos];
        } else {
            return blockBools.get(pos) ? 0 : -1;
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
        int index; //get the blockPalette index from the blocks array
        if (blockShorts != null) {
            index = blockShorts[pos];
        } else if (blockBytes != null) {
            index = blockBytes[pos];
        } else {
            index = blockBools.get(pos) ? 0 : -1;
        }
        Vector2i id;
        if (index >= 0) {
            id = blockPalette.get(index);
        } else {
            id = air;
        }
        int lightIndex; //get the blockPalette index from the blocks array
        if (lightShorts != null) {
            lightIndex = lightShorts[pos];
        } else if (lightBytes != null) {
            lightIndex = lightBytes[pos];
        } else {
            lightIndex = lightBools.get(pos) ? 0 : -1;
        }
        Vector4i light;
        if (lightIndex >= 0) {
            light = lightPalette.get(lightIndex);
        } else {
            light = fullSunLight;
        }
        return new Block(id.x, id.y, (byte) light.x, (byte) light.y, (byte) light.z, (byte) light.w);
    }
    public void setBlock(int pos, Block block, Vector3i globalPos) {
        boolean wasInPalette = false;
        for (int i = 0; i < blockPalette.size(); i++) { //iterate through blockPalette until finding a matching block, and upon doing so set the blockPalette index for that block position to the index of the matching blockPalette entry
            if (block.idVec().equals(blockPalette.get(i))) {
                if (blockShorts != null) {
                    blockShorts[pos] = (short) i;
                } else if (blockBytes != null) {
                    blockBytes[pos] = (byte) i;
                } else {
                    blockBools.set(pos, true);
                }
                wasInPalette = true;
                break;
            }
        }
        if (!wasInPalette) {
            int size = blockPalette.size();
            if (size == 127 && blockBytes != null) {
                blockShorts = new short[totalBlocks];
                Arrays.fill(blockShorts, (short) -1);
                for (int i = 0; i < blockBytes.length; i++) {
                    blockShorts[i] = blockBytes[i];
                }
                blockBytes = null;
            } else if (size == 1 && blockBools != null) {
                blockBytes = new byte[totalBlocks];
                Arrays.fill(blockBytes, (byte) -1);
                for (int i = 0; i < blockBools.length(); i++) {
                    blockBytes[i] = (byte) (blockBools.get(i) ? 0 : -1);
                }
                blockBools = null;
            }

            int index = blockPalette.size();
            for (int i = 1; i < blockPalette.size(); i++) {
                if (blockPalette.get(i) == null) {
                    index = i;
                    break;
                }
            }
            if (index == size) {
                blockPalette.addLast(null);
            }
            blockPalette.set(index, block.idVec());
            if (blockShorts != null) {
                blockShorts[pos] = (short) index;
            } else if (blockBytes != null) {
                blockBytes[pos] = (byte) index;
            } else {
                blockBools.set(pos, true);
            }
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
    }
    public void cleanPalette() {
        if (!blockPalette.isEmpty()) {
            int[] pointerUses = new int[blockPalette.size()+1];
            if (blockShorts != null) {
                for (short block : blockShorts) {
                    if (block >= 0) {
                        pointerUses[block] += 1;
                    }
                }
                List<Vector2i> newPalette = new ArrayList<>();
                for (short entry = 0; entry < blockPalette.size(); entry++) {
                    int pointer = pointerUses[entry];
                    if (pointer >= 0) {
                        newPalette.addLast(blockPalette.get(entry));
                    }
                }
                if (newPalette.size() < 2) {
                    blockBools = new BitSet(totalBlocks);
                    for (int i = 0; i < totalBlocks; i++) {
                        byte index = (byte) blockShorts[i];
                        if (index >= 0) {
                            Vector2i block = blockPalette.get(index);
                            if (newPalette.contains(block)) {
                                blockBools.set(i, true);
                                break;
                            }
                        }
                    }
                    blockShorts = null;
                } else if (newPalette.size() < 127) {
                    blockBytes = new byte[totalBlocks];
                    Arrays.fill(blockBytes, (byte) -1);
                    for (int i = 0; i < totalBlocks; i++) {
                        byte index = (byte) blockShorts[i];
                        if (index >= 0) {
                            Vector2i block = blockPalette.get(index);
                            for (byte p = 0; p < newPalette.size(); p++) {
                                if (newPalette.get(p).equals(block)) {
                                    blockBytes[i] = p;
                                    break;
                                }
                            }
                        }
                    }
                    blockShorts = null;
                } else {
                    for (int i = 0; i < totalBlocks; i++) {
                        short index = blockShorts[i];
                        if (index >= 0) {
                            Vector2i block = blockPalette.get(index);
                            for (short p = 0; p < newPalette.size(); p++) {
                                if (newPalette.get(p).equals(block)) {
                                    blockShorts[i] = p;
                                    break;
                                }
                            }
                        }
                    }
                }
                blockPalette = newPalette;
            } else if (blockBytes != null) {
                for (byte block : blockBytes) {
                    if (block >= 0) {
                        pointerUses[block] += 1;
                    }
                }
                List<Vector2i> newPalette = new ArrayList<>();
                for (byte entry = 0; entry < blockPalette.size(); entry++) {
                    int pointer = pointerUses[entry];
                    if (pointer >= 0) {
                        newPalette.addLast(blockPalette.get(entry));
                    }
                }
                if (newPalette.size() < 2) {
                    blockBools = new BitSet(totalBlocks);
                    for (int i = 0; i < totalBlocks; i++) {
                        byte index = blockBytes[i];
                        if (index >= 0) {
                            Vector2i block = blockPalette.get(index);
                            if (newPalette.contains(block)) {
                                blockBools.set(i, true);
                                break;
                            }
                        }
                    }
                    blockBytes = null;
                } else {
                    for (int i = 0; i < totalBlocks; i++) {
                        byte index = blockBytes[i];
                        if (index >= 0) {
                            Vector2i block = blockPalette.get(index);
                            for (byte p = 0; p < newPalette.size(); p++) {
                                if (newPalette.get(p).equals(block)) {
                                    blockBytes[i] = p;
                                }
                            }
                        }
                    }
                }
                blockPalette = newPalette;
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