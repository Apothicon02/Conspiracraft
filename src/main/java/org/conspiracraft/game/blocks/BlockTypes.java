package org.conspiracraft.game.blocks;

import java.util.HashMap;
import java.util.Map;

public class BlockTypes {
    public static Map<Integer, BlockType> blockTypeMap = new HashMap<>(Map.of());

    private static int key = 0;

    public static BlockType AIR = create(new BlockType());
    public static BlockType WATER = create(new BlockType());
    public static BlockType GRASS = create(new BlockType());
    public static BlockType DIRT = create(new BlockType());
    public static BlockType TALL_GRASS = create(new BlockType());
    public static BlockType ROSE = create(new BlockType());
    public static BlockType TORCH = create(new LightBlockType(20, 15, 0));

    private static BlockType create(BlockType type) {
        blockTypeMap.put(key, type);
        key++;
        return type;
    }
}
