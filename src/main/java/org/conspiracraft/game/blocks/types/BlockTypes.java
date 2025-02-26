package org.conspiracraft.game.blocks.types;

import java.util.HashMap;
import java.util.Map;

public class BlockTypes {
    public static Map<Integer, BlockType> blockTypeMap = new HashMap<>(Map.of());

    public static BlockType AIR = create(new BlockType(false, false, false, true));
    public static BlockType WATER = create(new BlockType(false, false, false, false, true));
    public static BlockType GRASS = create(new BlockType());
    public static BlockType DIRT = create(new BlockType());
    public static BlockType TALL_GRASS = create(new BlockType(false, false, false, true));
    public static BlockType ROSE = create(new BlockType(false, false, false, true));
    public static BlockType TORCH = create(new LightBlockType(false, false, false, true, 20, 15, 0));
    public static BlockType KYANITE = create(new LightBlockType(0, 2, 6));
    public static BlockType WHITE_STONE = create(new BlockType());
    public static BlockType BLACK_STONE = create(new BlockType());
    public static BlockType STONE = create(new BlockType());
    public static BlockType PURPLE_STAINED_GLASS = create(new BlockType(true, false));
    public static BlockType LIME_STAINED_GLASS = create(new BlockType(true, false));
    public static BlockType GLASS = create(new BlockType(true, false));
    public static BlockType PORECAP = create(new LightBlockType(false, false, false, true, false, 0, 12, 6));
    public static BlockType WILLOW_PLANKS = create(new BlockType());
    public static BlockType WILLOW_LOG = create(new BlockType());
    public static BlockType WILLOW_LEAVES = create(new BlockType(false, true, false));
    public static BlockType HYDRANGEA = create(new BlockType(false, false, false, true));

    private static BlockType create(BlockType type) {
        blockTypeMap.put(blockTypeMap.size(), type);
        return type;
    }
}
