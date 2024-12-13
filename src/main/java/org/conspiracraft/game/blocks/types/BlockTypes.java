package org.conspiracraft.game.blocks.types;

import java.util.HashMap;
import java.util.Map;

public class BlockTypes {
    public static Map<Integer, BlockType> blockTypeMap = new HashMap<>(Map.of());

    private static int key = 0;

    public static BlockType AIR = create(new BlockType(true, false));
    public static BlockType WATER = create(new BlockType(true, false));
    public static BlockType GRASS = create(new BlockType(false));
    public static BlockType DIRT = create(new BlockType(false));
    public static BlockType TALL_GRASS = create(new BlockType(true, false));
    public static BlockType ROSE = create(new BlockType(true, false));
    public static BlockType TORCH = create(new LightBlockType(true, false, 20, 15, 0));
    public static BlockType TEAL_TORCH = create(new LightBlockType(true, false, 0, 12, 20));
    public static BlockType YELLOW_STAR = create(new LightBlockType(false, 8, 7, 2));
    public static BlockType WHITE_STAR = create(new LightBlockType(false, 10, 9, 8));
    public static BlockType STONE = create(new BlockType(false));
    public static BlockType PURPLE_STAINED_GLASS = create(new BlockType(true));
    public static BlockType LIME_STAINED_GLASS = create(new BlockType(true));
    public static BlockType GLASS = create(new BlockType(true));
    public static BlockType WILLOW_PLANKS = create(new BlockType(false));
    public static BlockType WILLOW_LOG = create(new BlockType(false));

    private static BlockType create(BlockType type) {
        blockTypeMap.put(key, type);
        key++;
        return type;
    }
}
