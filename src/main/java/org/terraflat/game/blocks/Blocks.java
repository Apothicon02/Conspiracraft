package org.terraflat.game.blocks;

import org.terraflat.game.space.Voxel;

import java.util.HashMap;
import java.util.Map;

public class Blocks {
    public static Map<Short, Block> allBlocks = new HashMap<>(Map.of());

    private static short nextAvailableID = -32768; //Minimum short value.
    public static short AIR = createBlock(getID(), new Block());
    public static short SUN = createBlock(getID(), new Block());
    public static short GRASS = createBlock(getID(), new Block());

    private static short createBlock(short id, Block block) {
        block.setDefaultVoxel(new Voxel(id));
        allBlocks.put(id, block);
        return id;
    }

    private static short getID() {
        nextAvailableID++;
        return nextAvailableID;
    }
}