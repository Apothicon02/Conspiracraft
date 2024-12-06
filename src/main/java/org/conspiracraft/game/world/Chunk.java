package org.conspiracraft.game.world;

import org.conspiracraft.game.blocks.Block;
import org.conspiracraft.game.blocks.BlockHelper;

import java.util.ArrayList;
import java.util.List;

public class Chunk {
    private final short[] blocks = new short[4096];
    private final List<Block> palette = new ArrayList<>();

    public Chunk() {
        palette.add(null);
    }

    public Block getBlock(int pos) {
        short index = blocks[pos]; //get the short / palette index from the blocks array
        if (index == -1) {  //optimization for fully sun-lit air blocks to not require their own instance in each chunk.
            return BlockHelper.litAirBlock;
        } else if (index == 0) {  //optimization for unlit air blocks to not require their own instance in each chunk.
            return BlockHelper.unlitAirBlock;
        }
        if (!palette.isEmpty()) { //if palette is empty / chunk has no blocks return null
            return palette.get(index);
        }
        return null;
    }
    public void setBlock(int pos, Block block) {
        if (block.equals(BlockHelper.litAirBlock)) { //optimization for fully sun-lit air blocks to not require their own instance in each chunk.
            blocks[pos] = -1;
        } else if (block.equals(BlockHelper.unlitAirBlock)) { //optimization for unlit air blocks to not require their own instance in each chunk.
            blocks[pos] = 0;
        } else {
            boolean wasInPalette = false;
            for (short i = 0; i < palette.size(); i++) { //iterate through palette until finding a matching block, and upon doing so set the palette index for that block position to the index of the matching palette entry
                if (block.equals(palette.get(i))) {
                    blocks[pos] = i;
                    wasInPalette = true;
                    break;
                }
            }
            if (!wasInPalette) {
                blocks[pos] = (short) palette.size(); //palette.size() is where the new palette entry will be
                palette.addLast(block);
            }
        }
    }
}