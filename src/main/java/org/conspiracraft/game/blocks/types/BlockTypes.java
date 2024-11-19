package org.conspiracraft.game.blocks.types;

import java.util.HashMap;
import java.util.Map;

public class BlockTypes {
    public static Map<Short, BlockType> blockTypeMap = new HashMap<>(Map.of());

    private static short key = 0;

    public static BlockType AIR = create(new BlockType(true));
    public static BlockType WATER = create(new BlockType(true));
    public static BlockType GRASS = create(new BlockType(false));
    public static BlockType DIRT = create(new BlockType(false));
    public static BlockType TALL_GRASS = create(new BlockType(true));
    public static BlockType ROSE = create(new BlockType(true));
    public static BlockType TORCH = create(new LightBlockType(true, 20, 10, 0));
    public static BlockType TEAL_TORCH = create(new LightBlockType(true, 0, 10, 20));

    private static BlockType create(BlockType type) {
        blockTypeMap.put(key, type);
        key++;
        return type;
    }
}
