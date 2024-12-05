package org.conspiracraft.game.world;

import org.conspiracraft.game.blocks.Block;

import java.util.ArrayList;
import java.util.List;

public class Chunk {
    private final short[] blocks = new short[4096];
    private final List<Block> palette = new ArrayList<>(List.of());

    public Block getBlock(int pos) {
        short index = blocks[pos];
        if (palette.size() > index) {
            Block block = palette.get(index);
            if (block == null) {
                return new Block(0);
            }
            return block;
        }
        return new Block(0);
    }
    public void setBlock(int pos, Block block) {
        boolean wasInPalette = false;
        for (short i = 0; i < palette.size(); i++) {
            if (palette.get(i).equals(block)) {
                blocks[pos] = i;
                wasInPalette = true;
                break;
            }
        }
        if (!wasInPalette) {
            blocks[pos] = (short) palette.size();
            palette.add(block);
        }
    }
}