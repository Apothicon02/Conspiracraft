package org.conspiracraft.game.world;

import org.conspiracraft.game.blocks.Block;
import org.conspiracraft.game.blocks.BlockHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.conspiracraft.game.world.World.chunkSize;

public class Chunk {
    private static int totalBlocks = chunkSize*chunkSize*chunkSize;
    private byte[] blocks = new byte[totalBlocks];
    private short[] blocksShorts = null;
    private final List<Block> palette = new ArrayList<>();

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
        if (block.equals(BlockHelper.litAirBlock)) { //optimization for fully sun-lit air blocks to not require their own instance in each chunk.
            if (blocks == null) {
                blocksShorts[pos] = 0;
            } else {
                blocks[pos] = 0;
            }
        } else {
            int oldIndex;
            byte existing = 0;
            if (blocks == null) {
                oldIndex = blocksShorts[pos];
                if (oldIndex >= 1) {
                    for (int i = 0; i < blocksShorts.length && existing <= 1; i++) {
                        if (blocksShorts[i] == oldIndex) {
                            existing++;
                        }
                    }
                } else {
                    existing = 2;
                }
            } else {
                oldIndex = blocks[pos];
                if (oldIndex >= 1) {
                    for (int i = 0; i < blocks.length && existing <= 1; i++) {
                        if (blocks[i] == oldIndex) {
                            existing++;
                        }
                    }
                } else {
                    existing = 2;
                }
            }
            if (existing < 1) {
                palette.set(oldIndex, null);
            }
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
                    for (byte i = 0; i < blocks.length; i++) {
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
    }
}