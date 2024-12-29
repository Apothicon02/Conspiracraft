package org.conspiracraft.game.world;

import org.conspiracraft.engine.Utils;
import org.conspiracraft.game.blocks.Block;
import org.conspiracraft.game.blocks.BlockHelper;

import java.util.*;

import static org.conspiracraft.game.world.World.chunkSize;

public class Chunk {
    private static final int totalBlocks = chunkSize*chunkSize*chunkSize;
    private byte[] corners = new byte[totalBlocks];
    private byte[] blockBytes = new byte[totalBlocks];
    private short[] blockShorts = null;
    private List<Block> palette = new ArrayList<>();

    public Chunk() {
        Arrays.fill(blockBytes, (byte) -1);
        Arrays.fill(corners, Utils.convertBoolArrayToByte(new boolean[]{true, true, false, true, true, true, true, true}));
        palette.add(null);
    }

    public byte getCorners(int pos) {
        return corners[pos];
    }
    public int[] getAllBlocks() {
        int[] returnObj = new int[totalBlocks];
        for (int x = 0; x < chunkSize; x++) {
            for (int z = 0; z < chunkSize; z++) {
                for (int y = 0; y < chunkSize; y++) {
                    int pos = World.condenseLocalPos(x, y, z);
                    Block block = getBlock(pos);
                    returnObj[pos] = Utils.packInts(block.typeId(), block.subtypeId());
                }
            }
        }
        return returnObj;
    }
    public Block getBlock(int pos) {
        int index; //get the palette index from the blocks array
        if (blockShorts != null) {
            index = blockShorts[pos];
        } else {
            index = blockBytes[pos];
        }
        if (index <= 0) {  //optimization for fully sun-lit air blocks to not require their own instance in each chunk.
            return BlockHelper.litAirBlock;
        }
        return palette.get(index);
    }
    public void setBlock(int pos, Block block) {
        setBlock(pos, block, false);
    }
    public void setBlock(int pos, Block block, boolean update) {
        if (block.equals(BlockHelper.litAirBlock)) { //optimization for fully sun-lit air blocks to not require their own instance in each chunk.
            if (blockShorts != null) {
                blockShorts[pos] = 0;
            } else {
                blockBytes[pos] = 0;
            }
        } else {
            boolean wasInPalette = false;
            for (int i = 0; i < palette.size(); i++) { //iterate through palette until finding a matching block, and upon doing so set the palette index for that block position to the index of the matching palette entry
                if (block.equals(palette.get(i))) {
                    if (blockShorts != null) {
                        blockShorts[pos] = (short) i;
                    } else {
                        blockBytes[pos] = (byte) i;
                    }
                    wasInPalette = true;
                    break;
                }
            }
            if (!wasInPalette) {
                int size = palette.size();
                if (size == 127 && blockBytes != null) {
                    blockShorts = new short[totalBlocks];
                    Arrays.fill(blockShorts, (short) -1);
                    for (int i = 0; i < blockBytes.length; i++) {
                        blockShorts[i] = blockBytes[i];
                    }
                    blockBytes = null;
                }

                int index = palette.size();
                for (int i = 1; i < palette.size(); i++) {
                    if (palette.get(i) == null) {
                        index = i;
                        break;
                    }
                }
                if (index == size) {
                    palette.addLast(null);
                }
                if (blockShorts != null) {
                    palette.set(index, block);
                    blockShorts[pos] = (short) index;
                } else {
                    palette.set(index, block);
                    blockBytes[pos] = (byte) index;
                }
            }
        }
        if (update) {
            cleanPalette();
        }
    }
    public void cleanPalette() {
        if (!palette.isEmpty()) {
            int[] pointerUses = new int[palette.size()+1];
            if (blockShorts != null) {
                for (short block : blockShorts) {
                    if (block >= 0) {
                        pointerUses[block] += 1;
                    }
                }
                List<Block> newPalette = new ArrayList<>();
                newPalette.add(null);
                for (short entry = 1; entry < palette.size(); entry++) {
                    int pointer = pointerUses[entry];
                    if (pointer > 0) {
                        newPalette.addLast(palette.get(entry));
                    }
                }
                if (newPalette.size() < 127) {
                    blockBytes = new byte[totalBlocks];
                    Arrays.fill(blockBytes, (byte) -1);
                    for (int i = 0; i < totalBlocks; i++) {
                        byte index = (byte) blockShorts[i];
                        if (index >= 0) {
                            Block block = palette.get(index);
                            for (byte p = 1; p < newPalette.size(); p++) {
                                if (newPalette.get(p).equals(block)) {
                                    blockBytes[i] = p;
                                }
                            }
                        }
                    }
                    blockShorts = null;
                } else {
                    for (int i = 0; i < totalBlocks; i++) {
                        short index = blockShorts[i];
                        if (index > 0) {
                            Block block = palette.get(index);
                            for (short p = 1; p < newPalette.size(); p++) {
                                if (newPalette.get(p).equals(block)) {
                                    blockShorts[i] = p;
                                }
                            }
                        }
                    }
                }
                palette = newPalette;
            } else {
                for (byte block : blockBytes) {
                    if (block >= 0) {
                        pointerUses[block] += 1;
                    }
                }
                List<Block> newPalette = new ArrayList<>();
                newPalette.add(null);
                for (byte entry = 1; entry < palette.size(); entry++) {
                    int pointer = pointerUses[entry];
                    if (pointer > 0) {
                        newPalette.addLast(palette.get(entry));
                    }
                }
                for (int i = 0; i < totalBlocks; i++) {
                    byte index = blockBytes[i];
                    if (index > 0) {
                        Block block = palette.get(index);
                        for (byte p = 1; p < newPalette.size(); p++) {
                            if (newPalette.get(p).equals(block)) {
                                blockBytes[i] = p;
                            }
                        }
                    }
                }
                palette = newPalette;
            }
        }
    }
}