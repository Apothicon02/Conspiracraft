package org.conspiracraft.game.world;

import org.conspiracraft.game.blocks.Block;
import org.conspiracraft.game.blocks.BlockHelper;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.*;

import static org.conspiracraft.game.world.World.chunkSize;

public class Chunk {
    private static final int totalBlocks = chunkSize*chunkSize*chunkSize;
    private static byte[] defaultBytes = new byte[totalBlocks];
    private static byte[] defaultShortsBytes = new byte[totalBlocks*2];
    private ByteBuffer blocksBytes = ByteBuffer.allocateDirect(totalBlocks);
    private ByteBuffer blocksShortsBytes = null;
    private ShortBuffer blocksShorts = null;
    private List<Block> palette = new ArrayList<>();

    public Chunk() {
        Arrays.fill(defaultBytes, (byte) -1);
        blocksBytes.put(defaultBytes);
        palette.add(null);
    }

    public Block getBlock(int pos) {
        int index; //get the palette index from the blocks array
        if (blocksBytes == null) {
            index = blocksShorts.get(pos);
        } else {
            index = blocksBytes.get(pos);
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
            if (blocksBytes == null) {
                blocksShorts.put(pos, (short) 0);
            } else {
                blocksBytes.put(pos, (byte) 0);
            }
        } else {
            boolean wasInPalette = false;
            for (int i = 0; i < palette.size(); i++) { //iterate through palette until finding a matching block, and upon doing so set the palette index for that block position to the index of the matching palette entry
                if (block.equals(palette.get(i))) {
                    if (blocksBytes == null) {
                        blocksShorts.put(pos, (short) i);
                    } else {
                        blocksBytes.put(pos, (byte) i);
                    }
                    wasInPalette = true;
                    break;
                }
            }
            if (!wasInPalette) {
                int size = palette.size();
                if (size == 127) {
                    blocksShortsBytes = ByteBuffer.allocateDirect(totalBlocks*2);
                    blocksShortsBytes.put(ByteBuffer.wrap(defaultShortsBytes));
                    blocksShorts = blocksShortsBytes.asShortBuffer();
                    for (int i = 0; i < totalBlocks; i++) {
                        blocksShorts.put(i, blocksBytes.get(i));
                    }
                    blocksBytes = null;
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
                if (blocksBytes == null) {
                    palette.set(index, block);
                    blocksShorts.put(pos, (short) index);
                } else {
                    palette.set(index, block);
                    blocksBytes.put(pos, (byte) index);
                }
            }
        }
        if (update) {
            cleanPalette();
        }
    }
    public void cleanPalette() {
        if (!palette.isEmpty()) {
            if (blocksBytes == null) {
                int[] pointerUses = new int[palette.size()+1];
                for (int i = 0; i < totalBlocks; i++) {
                    short block = blocksShorts.get(i);
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
                    blocksBytes = ByteBuffer.allocateDirect(totalBlocks);
                    blocksBytes.put(ByteBuffer.wrap(defaultBytes));
                    for (int i = 0; i < totalBlocks; i++) {
                        byte index = blocksBytes.get(i);
                        if (index >= 0) {
                            Block block = palette.get(index);
                            for (byte p = 0; p < newPalette.size(); p++) {
                                if (newPalette.get(p).equals(block)) {
                                    blocksBytes.put(i, p);
                                }
                            }
                        }
                    }
                    blocksShorts = null;
                    blocksShortsBytes = null;
                } else {
                    for (int i = 0; i < totalBlocks; i++) {
                        short index = blocksShorts.get(i);
                        if (index > 0) {
                            Block block = palette.get(index);
                            for (short p = 1; p < newPalette.size(); p++) {
                                if (newPalette.get(p).equals(block)) {
                                    blocksShorts.put(i, p);
                                }
                            }
                        }
                    }
                }
                palette = newPalette;
            } else {
                int[] pointerUses = new int[palette.size()+1];
                for (int i = 0; i < totalBlocks; i++) {
                    byte block = blocksBytes.get(i);
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
                    byte index = blocksBytes.get(i);
                    if (index > 0) {
                        Block block = palette.get(index);
                        for (byte p = 1; p < newPalette.size(); p++) {
                            if (newPalette.get(p).equals(block)) {
                                blocksBytes.put(i, p);
                            }
                        }
                    }
                }
                palette = newPalette;
            }
        }
    }
}