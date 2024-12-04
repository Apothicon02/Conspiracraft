package org.conspiracraft.game.world;

import org.conspiracraft.game.blocks.Block;

import java.util.Arrays;

public class Chunk {
    public Block[] blocks = new Block[4096];

    public Chunk() {
        Arrays.fill(blocks, new Block(0));
    }
}