package org.conspiracraft.game.world;

import org.conspiracraft.game.blocks.Block;

public class Chunk {
    private final Block[] blocks = new Block[4096];

    public Block getBlock(int pos) {
        Block block = blocks[pos];
        if (block == null) {
            return new Block(0);
        }
        return block;
    }
    public void setBlock(int pos, Block block) {
        blocks[pos] = block;
    }
}