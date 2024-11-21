package org.conspiracraft.game.blocks.types;

import java.util.HashMap;
import java.util.Map;

public class BlockTypes {
    public static Map<Integer, BlockType> blockTypeMap = new HashMap<>(Map.of());

    private static int key = 0;

    public static BlockType AIR = create(new BlockType(true));
    public static BlockType WATER = create(new BlockType(true));
    public static BlockType GRASS = create(new BlockType(false));
    public static BlockType DIRT = create(new BlockType(false));
    public static BlockType TALL_GRASS = create(new BlockType(true));
    public static BlockType ROSE = create(new BlockType(true));
    public static BlockType TORCH = create(new LightBlockType(true, 20, 12, 0));
    public static BlockType TEAL_TORCH = create(new LightBlockType(true, 0, 12, 20));
    public static BlockType YELLOW_STAR = create(new LightBlockType(true, 3, 3, 0));
    public static BlockType WHITE_STAR = create(new LightBlockType(true, 4, 4, 3));

    private static BlockType create(BlockType type) {
        blockTypeMap.put(key, type);
        key++;
        return type;
    }
}
