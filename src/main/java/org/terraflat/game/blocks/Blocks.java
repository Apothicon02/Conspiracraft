package org.terraflat.game.blocks;

import org.terraflat.game.space.Voxel;

import java.util.HashMap;
import java.util.Map;

public class Blocks {
    public static Map<Integer, Block> allBlocks = new HashMap<>(Map.of());

    public static float AIR = createBlock(allBlocks.size(), new Block());
    public static float SUN = createBlock(allBlocks.size(), new Block());
    public static float GRASS = createBlock(allBlocks.size(), new Block());

    private static float createBlock(int id, Block block) {
        block.setDefaultVoxel(new Voxel(id));
        allBlocks.put(id, block);
        return id;
    }
}