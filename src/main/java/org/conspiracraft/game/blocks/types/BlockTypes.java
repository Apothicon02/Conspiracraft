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

    public static BlockType AIR = create(new BlockType(new BlockProperties().isSolid(false).blocksLight(false).isCollidable(false).isFluidReplaceable(true).obstructsHeightmap(false)));
    public static BlockType WATER = create(new BlockType(new BlockProperties().isSolid(false).blocksLight(false).isCollidable(false).isFluid(true)
            .blockSFX(new int[]{8}, 1f, 1.25f)));
    public static BlockType GRASS = create(new BlockType(new BlockProperties()));
    public static BlockType DIRT = create(new BlockType(new BlockProperties()));
    public static BlockType TALL_GRASS = create(new BlockType(new BlockProperties().isSolid(false).blocksLight(false).isCollidable(false).isFluidReplaceable(true).needsSupport(true)));
    public static BlockType ROSE = create(List.of(Tags.flowers), new BlockType(TALL_GRASS.blockProperties));
    public static BlockType TORCH = create(new LightBlockType((LightBlockProperties) new LightBlockProperties().r(20).g(15).isSolid(false).blocksLight(false).isCollidable(false).isFluidReplaceable(true).needsSupport(true)));
    public static BlockType KYANITE = create(List.of(Tags.rocks), new LightBlockType(new LightBlockProperties().g(2).b(6)));
    public static BlockType WHITE_STONE = create(List.of(Tags.rocks), new BlockType(new BlockProperties()));
    public static BlockType IGNEOUS = create(List.of(Tags.rocks), new BlockType(new BlockProperties()));
    public static BlockType STONE = create(List.of(Tags.rocks), new BlockType(new BlockProperties()));
    public static BlockType GLASS = create(new BlockType(new BlockProperties().blocksLight(false)));
    public static BlockType PURPLE_STAINED_GLASS = create(new BlockType(GLASS.blockProperties));
    public static BlockType LIME_STAINED_GLASS = create(new BlockType(GLASS.blockProperties));
    public static BlockType PORECAP = create(new LightBlockType(((LightBlockProperties)TORCH.blockProperties).copy().r(0).g(12).b(6)));
    public static BlockType WILLOW_PLANKS = create(new BlockType(new BlockProperties()));
    public static BlockType WILLOW_LOG = create(new BlockType(new BlockProperties()));
    public static BlockType WILLOW_LEAVES = create(List.of(Tags.leaves), new LeafBlockType(new BlockProperties().isSolid(false).blocksLight(false).isCollidable(false).isFluidReplaceable(true)));
    public static BlockType HYDRANGEA = create(List.of(Tags.flowers), new BlockType(ROSE.blockProperties));
    public static BlockType MAGMA = create(new LightBlockType(((LightBlockProperties)KYANITE.blockProperties).copy().r(8).g(3).b(0)));
    public static BlockType MAHOGANY_LOG = create(new BlockType(new BlockProperties()));
    public static BlockType MAHOGANY_LEAVES = create(List.of(Tags.leaves), new LeafBlockType(new BlockProperties().isSolid(false).blocksLight(false).isCollidable(false).isFluidReplaceable(true)));
    public static BlockType BUCKET = create(new BlockType(new BlockProperties().blocksLight(false).needsSupport(true)));

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
