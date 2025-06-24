package org.conspiracraft.game.blocks.types;

import org.conspiracraft.game.blocks.Tag;
import org.conspiracraft.game.blocks.Tags;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockTypes {
    public static Map<Integer, BlockType> blockTypeMap = new HashMap<>(Map.of());

    public static int getId(BlockType type) {
        int id = 0;
        for (BlockType mapBlocKType : blockTypeMap.values()) {
            if (mapBlocKType.equals(type)) {
                return id;
            }
            id++;
        }
        return 0;
    }

    public static BlockType AIR = create(new BlockType(false, false, false, true, false, false));
    public static BlockType WATER = create(new BlockType(false, false, false, false, true, true));
    public static BlockType GRASS = create(new BlockType());
    public static BlockType DIRT = create(new BlockType());
    public static BlockType TALL_GRASS = create(new BlockType(false, false, false, true, false, true, true));
    public static BlockType ROSE = create(List.of(Tags.flowers), new BlockType(false, false, false, true, false, true, true));
    public static BlockType TORCH = create(new LightBlockType(false, false, false, true, false, false, true, 20, 15, 0));
    public static BlockType KYANITE = create(List.of(Tags.rocks), new LightBlockType(0, 2, 6));
    public static BlockType WHITE_STONE = create(List.of(Tags.rocks), new BlockType());
    public static BlockType IGNEOUS = create(List.of(Tags.rocks), new BlockType());
    public static BlockType STONE = create(List.of(Tags.rocks), new BlockType());
    public static BlockType PURPLE_STAINED_GLASS = create(new BlockType(true, false));
    public static BlockType LIME_STAINED_GLASS = create(new BlockType(true, false));
    public static BlockType GLASS = create(new BlockType(true, false));
    public static BlockType PORECAP = create(new LightBlockType(false, false, false, true, false, false, true, 0, 12, 6));
    public static BlockType WILLOW_PLANKS = create(new BlockType());
    public static BlockType WILLOW_LOG = create(new BlockType());
    public static BlockType WILLOW_LEAVES = create(List.of(Tags.leaves), new LeafBlockType(false, false, false));
    public static BlockType HYDRANGEA = create(List.of(Tags.flowers), new BlockType(false, false, false, true, false, false, true));
    public static BlockType MAGMA = create(new LightBlockType(8, 3, 0));
    public static BlockType MAHOGANY_LOG = create(new BlockType());
    public static BlockType MAHOGANY_LEAVES = create(List.of(Tags.leaves), new LeafBlockType(false, false, false));

    private static BlockType create(List<Tag> tags, BlockType type) {
        for (Tag tag : tags) {
            tag.tagged.add(blockTypeMap.size());
        }
        blockTypeMap.put(blockTypeMap.size(), type);
        return type;
    }
    private static BlockType create(BlockType type) {
        blockTypeMap.put(blockTypeMap.size(), type);
        return type;
    }
}
