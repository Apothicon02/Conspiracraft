package org.conspiracraft.game.world;

import org.conspiracraft.game.blocks.Block;
import org.conspiracraft.game.blocks.BlockHelper;

import java.util.*;

import static org.conspiracraft.game.world.World.chunkSize;

public class Chunk {
    private static int totalBlocks = chunkSize*chunkSize*chunkSize;
    private byte[] blocks = new byte[totalBlocks];
    private short[] blocksShorts = null;
    private List<Block> palette = new ArrayList<>();

    public Chunk() {
        Arrays.fill(blocks, (byte) -1);
        palette.add(null);
    }

    public Block getBlock(int pos) {
        int index; //get the palette index from the blocks array
        if (blocks == null) {
            index = blocksShorts[pos];
        } else {
            index = blocks[pos];
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
            if (blocks == null) {
                blocksShorts[pos] = 0;
            } else {
                blocks[pos] = 0;
            }
        } else {
            boolean wasInPalette = false;
            for (int i = 0; i < palette.size(); i++) { //iterate through palette until finding a matching block, and upon doing so set the palette index for that block position to the index of the matching palette entry
                if (block.equals(palette.get(i))) {
                    if (blocks == null) {
                        blocksShorts[pos] = (short) i;
                    } else {
                        blocks[pos] = (byte) i;
                    }
                    wasInPalette = true;
                    break;
                }
            }
            if (!wasInPalette) {
                int size = palette.size();
                if (size == 127) {
                    blocksShorts = new short[totalBlocks];
                    Arrays.fill(blocksShorts, (short) -1);
                    for (int i = 0; i < blocks.length; i++) {
                        blocksShorts[i] = blocks[i];
                    }
                    blocks = null;
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
                if (blocks == null) {
                    palette.set(index, block);
                    blocksShorts[pos] = (short) index;
                } else {
                    palette.set(index, block);
                    blocks[pos] = (byte) index;
                }
            }
        }
        if (update) {
            cleanPalette();
        }
    }
    public void cleanPalette() {
        if (!palette.isEmpty()) {
            if (blocks == null) {
                int[] pointerUses = new int[palette.size()+1];
                for (short block : blocksShorts) {
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
                    blocks = new byte[totalBlocks];
                    Arrays.fill(blocks, (byte) -1);

                    for (int i = 0; i < blocksShorts.length; i++) {
                        byte index = blocks[i];
                        if (index >= 0) {
                            Block block = palette.get(index);
                            for (byte p = 0; p < newPalette.size(); p++) {
                                if (newPalette.get(p).equals(block)) {
                                    blocks[i] = p;
                                }
                            }
                        }
                    }
                    blocksShorts = null;
                } else {
                    for (int i = 0; i < blocksShorts.length; i++) {
                        short index = blocksShorts[i];
                        if (index > 0) {
                            Block block = palette.get(index);
                            for (short p = 1; p < newPalette.size(); p++) {
                                if (newPalette.get(p).equals(block)) {
                                    blocksShorts[i] = p;
                                }
                            }
                        }
                    }
                }
                palette = newPalette;
            } else {
                int[] pointerUses = new int[palette.size()+1];
                for (byte block : blocks) {
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
                for (int i = 0; i < blocks.length; i++) {
                    byte index = blocks[i];
                    if (index > 0) {
                        Block block = palette.get(index);
                        for (byte p = 1; p < newPalette.size(); p++) {
                            if (newPalette.get(p).equals(block)) {
                                blocks[i] = p;
                            }
                        }
                    }
                }
                palette = newPalette;
            }
        }
    }
}