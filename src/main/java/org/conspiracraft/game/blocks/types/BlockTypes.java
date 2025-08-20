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

    public static BlockType
            AIR = create(new BlockType(new BlockProperties().isSolid(false).blocksLight(false).isCollidable(false).isFluidReplaceable(true).obstructsHeightmap(false))), 
            WATER = create(new BlockType(new BlockProperties().isSolid(false).blocksLight(false).isCollidable(false).isFluid(true).obstructsHeightmap(false).blockSFX(new int[]{8}, 1f, 1.25f))),
            GRASS = create(new BlockType(new BlockProperties())),
            DIRT = create(new BlockType(new BlockProperties())),
            TALL_GRASS = create(new BlockType(new BlockProperties().isSolid(false).blocksLight(false).isCollidable(false).isFluidReplaceable(true).needsSupport(true))),
            ROSE = create(List.of(Tags.flowers), new BlockType(TALL_GRASS.blockProperties)), //5
            TORCH = create(new LightBlockType((LightBlockProperties) new LightBlockProperties().r(20).g(15).isSolid(false).blocksLight(false).isCollidable(false).isFluidReplaceable(true).needsSupport(true))),
            KYANITE = create(List.of(Tags.rocks), new LightBlockType(new LightBlockProperties().g(2).b(6))),
            WHITE_STONE = create(List.of(Tags.rocks), new BlockType(new BlockProperties())),
            IGNEOUS = create(List.of(Tags.rocks), new BlockType(new BlockProperties())),
            STONE = create(List.of(Tags.rocks), new BlockType(new BlockProperties())), //10
            GLASS = create(new BlockType(new BlockProperties().blocksLight(false))),
            PURPLE_STAINED_GLASS = create(new BlockType(GLASS.blockProperties)),
            LIME_STAINED_GLASS = create(new BlockType(GLASS.blockProperties)),
            PORECAP = create(new LightBlockType(((LightBlockProperties)TORCH.blockProperties).copy().r(0).g(12).b(6))),
            WILLOW_PLANKS = create(new BlockType(new BlockProperties())), //15
            WILLOW_LOG = create(new BlockType(new BlockProperties())),
            WILLOW_LEAVES = create(List.of(Tags.leaves), new LeafBlockType(new BlockProperties().isSolid(false).blocksLight(false).isCollidable(false).isFluidReplaceable(true))),
            HYDRANGEA = create(List.of(Tags.flowers), new BlockType(ROSE.blockProperties)),
            MAGMA = create(new LightBlockType(((LightBlockProperties)KYANITE.blockProperties).copy().r(16).g(6).b(0))),
            MAHOGANY_LOG = create(new BlockType(new BlockProperties())), //20
            MAHOGANY_LEAVES = create(List.of(Tags.leaves), new LeafBlockType(WILLOW_LEAVES.blockProperties)),
            BUCKET = create(new BlockType(new BlockProperties().blocksLight(false).needsSupport(true))),
            SAND = create(new BlockType(new BlockProperties())),
            SANDSTONE = create(new BlockType(new BlockProperties())),
            PALM_LOG = create(new BlockType(new BlockProperties())), //25
            PALM_PLANKS = create(new BlockType(new BlockProperties())),
            PALM_LEAVES = create(List.of(Tags.leaves), new LeafBlockType(WILLOW_LEAVES.blockProperties)),
            MAHOGANY_PLANKS = create(new BlockType(new BlockProperties())),
            CACTUS = create(new BlockType(new BlockProperties().isSolid(false).blocksLight(false).isCollidable(true).isFluidReplaceable(true).needsSupport(true))),
            DEAD_BUSH = create(List.of(Tags.flowers), new BlockType(ROSE.blockProperties));

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
