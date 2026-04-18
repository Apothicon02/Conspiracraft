package org.conspiracraft.blocks.types;

import org.conspiracraft.audio.SFX;
import org.conspiracraft.audio.Sounds;
import org.conspiracraft.blocks.BlockTag;
import org.conspiracraft.blocks.BlockTags;

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
            AIR = create(new BlockType(new BlockProperties().blockSFX(new SFX[]{Sounds.CLOUD}, 0.75f, 0.75f, new SFX[]{Sounds.CLOUD}, 0.75f, 0.75f)
                    .isSolid(false).blocksLight(false).isCollidable(false).isFluidReplaceable(true).obstructsHeightmap(false))),
            WATER = create(new BlockType(new BlockProperties().isSolid(false).blocksLight(false).isCollidable(false).isFluid(true).obstructsHeightmap(false).blockSFX(
                    new SFX[]{Sounds.SPLASH1}, 1f, 1.25f, new SFX[]{Sounds.SPLASH1}, 0f, 1f))),
            GRASS = create(List.of(BlockTags.sediment, BlockTags.grass), new BlockType(new BlockProperties().ttb(200).blockSFX(new SFX[]{Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 1, 1,
                    new SFX[]{Sounds.GRASS_STEP1, Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 1, 1))),
            DIRT = create(List.of(BlockTags.sediment, BlockTags.dirt), new BlockType(new BlockProperties().ttb(200).blockSFX(new SFX[]{Sounds.DIRT_STEP1, Sounds.DIRT_STEP2, Sounds.DIRT_STEP3}, 1, 1,
                    new SFX[]{Sounds.DIRT_STEP1, Sounds.DIRT_STEP2, Sounds.DIRT_STEP3}, 1, 1))),
            TALL_GRASS = create(List.of(BlockTags.survivesOnGrass), new PlantBlockType(GRASS.blockProperties.copy().ttb(50).obstructsHeightmap(false).isSolid(false).blocksLight(false).isCollidable(false).isFluidReplaceable(true)
                    .needsSupport(true))),
            ROSE = create(List.of(BlockTags.shortFlowers, BlockTags.flowers, BlockTags.survivesOnGrass), new PlantBlockType(TALL_GRASS.blockProperties)), //5
            TORCH = create(List.of(BlockTags.smallBlock), new LightBlockType((LightBlockProperties) new LightBlockProperties().r(40).g(38).b(30).ttb(100).obstructsHeightmap(false).isSolid(false).blocksLight(false)
                    .isCollidable(false).isFluidReplaceable(true).needsSupport(true).blockSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 1, 1,
                            new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 1, 1))),
            KYANITE = create(List.of(BlockTags.rocks, BlockTags.crystals, BlockTags.blunt), new LightBlockType((LightBlockProperties) (new LightBlockProperties().r(4).g(20).b(40)
                    .blockSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 1, 1, new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 1, 1)))),
            MARBLE = create(List.of(BlockTags.rocks, BlockTags.blunt), new BlockType(new BlockProperties()
                    .blockSFX(new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 1f, 0.6f, new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 1f, 0.5f))),
            IGNEOUS = create(List.of(BlockTags.rocks, BlockTags.blunt), new BlockType(new BlockProperties())),
            STONE = create(List.of(BlockTags.rocks, BlockTags.blunt), new BlockType(new BlockProperties())), //10
            GLASS = create(List.of(BlockTags.blunt), new BlockType(new BlockProperties().blockSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 1, 1,
                    new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 1, 1).blocksLight(false).obstructsHeightmap(false))),
            MAGENTA_STAINED_GLASS = create(List.of(BlockTags.blunt), new BlockType(GLASS.blockProperties)),
            LIME_STAINED_GLASS = create(List.of(BlockTags.blunt), new BlockType(GLASS.blockProperties)),
            PORECAP = create(List.of(BlockTags.sediment), new PlantLightBlockType(((LightBlockProperties)TORCH.blockProperties.copy().ttb(50)).r(0).g(12).b(6))),
            OAK_PLANK = create(List.of(BlockTags.planks), new BlockType(new BlockProperties().ttb(200).blockSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 1, 1,
                    new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 1, 1))), //15
            OAK_LOG = create(new BlockType(new BlockProperties().ttb(200).blockSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 1, 1,
                    new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 1, 1))),
            OAK_LEAVES = create(List.of(BlockTags.leaves), new LeafBlockType(new BlockProperties().ttb(100).blockSFX(new SFX[]{Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 1, 1,
                    new SFX[]{Sounds.GRASS_STEP1, Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 1, 1).isSolid(false).blocksLight(true).isCollidable(false).isFluidReplaceable(true))),
            HYDRANGEA = create(List.of(BlockTags.shortFlowers, BlockTags.flowers, BlockTags.survivesOnGrass), new PlantBlockType(ROSE.blockProperties)),
            MAGMA = create(List.of(BlockTags.blunt), new LightBlockType(((LightBlockProperties)(KYANITE.blockProperties.copy().blockSFX(
                    new SFX[]{Sounds.SIZZLE1, Sounds.SIZZLE2}, 1, 1, new SFX[]{Sounds.SIZZLE1, Sounds.SIZZLE2}, 1, 1))).r(16).g(6).b(0))),
            MAHOGANY_LOG = create(new BlockType(OAK_LOG.blockProperties)), //20
            MAHOGANY_LEAVES = create(List.of(BlockTags.leaves), new LeafBlockType(OAK_LEAVES.blockProperties)),
            BUCKET = create(List.of(BlockTags.buckets, BlockTags.cantBreakBlocks), new BlockType(new BlockProperties().ttb(0).isSolid(false).blocksLight(false).obstructsHeightmap(false))),
            SAND = create(List.of(BlockTags.sediment, BlockTags.sand), new PowderBlockType(new BlockProperties().ttb(200).blockSFX(new SFX[]{Sounds.SAND_STEP1, Sounds.SAND_STEP2}, 0.45f, 1.33f,
                    new SFX[]{Sounds.SAND_STEP1, Sounds.SAND_STEP2}, 0.45f, 1.33f).needsSupport(true).blocksLight(true).obstructsHeightmap(true))),
            SANDSTONE = create(List.of(BlockTags.blunt), new BlockType(new BlockProperties())),
            PALM_LOG = create(new BlockType(OAK_LOG.blockProperties)), //25
            PALM_PLANK = create(List.of(BlockTags.planks), new BlockType(OAK_PLANK.blockProperties)),
            PALM_LEAVES = create(List.of(BlockTags.leaves), new LeafBlockType(OAK_LEAVES.blockProperties)),
            MAHOGANY_PLANK = create(List.of(BlockTags.planks), new BlockType(OAK_PLANK.blockProperties)),
            CACTUS = create(List.of(BlockTags.survivesOnSand), new PlantBlockType(new BlockProperties().isSolid(false).blocksLight(false).obstructsHeightmap(false).isCollidable(true).isFluidReplaceable(true).needsSupport(true))),
            DEAD_BUSH = create(List.of(BlockTags.survivesOnSediment), new PlantBlockType(ROSE.blockProperties)), //30
            CLOUD = create(new BlockType(new BlockProperties().blockSFX(new SFX[]{Sounds.CLOUD}, 0.75f, 0.75f, new SFX[]{Sounds.CLOUD}, 0.75f, 0.75f)
                    .isSolid(false).isCollidable(false).blocksLight(false).obstructsHeightmap(false))),
            RAIN_CLOUD = create(new CloudBlockType(CLOUD.blockProperties)),
            DRY_MUD = create(List.of(BlockTags.soakers, BlockTags.sediment), new BlockType(DIRT.blockProperties)),
            SPRUCE_PLANK = create(List.of(BlockTags.planks), new BlockType(OAK_PLANK.blockProperties)),
            SPRUCE_LOG = create(new BlockType(OAK_LOG.blockProperties)), //35
            SPRUCE_LEAVES = create(List.of(BlockTags.leaves), new LeafBlockType(OAK_LEAVES.blockProperties)),
            CHERRY_PLANK = create(List.of(BlockTags.planks), new BlockType(OAK_PLANK.blockProperties)),
            CHERRY_LOG = create(new BlockType(OAK_LOG.blockProperties)),
            CHERRY_LEAVES = create(List.of(BlockTags.leaves), new LeafBlockType(OAK_LEAVES.blockProperties)),
            BIRCH_PLANK = create(List.of(BlockTags.planks), new BlockType(OAK_PLANK.blockProperties)), //40
            BIRCH_LOG = create(new BlockType(OAK_LOG.blockProperties)),
            BIRCH_LEAVES = create(List.of(BlockTags.leaves), new LeafBlockType(OAK_LEAVES.blockProperties)),
            ACACIA_PLANK = create(List.of(BlockTags.planks), new BlockType(OAK_PLANK.blockProperties)),
            ACACIA_LOG = create(new BlockType(OAK_LOG.blockProperties)),
            ACACIA_LEAVES = create(List.of(BlockTags.leaves), new LeafBlockType(OAK_LEAVES.blockProperties)), //45
            WILLOW_PLANK = create(List.of(BlockTags.planks), new BlockType(OAK_PLANK.blockProperties)),
            WILLOW_LOG = create(new BlockType(OAK_LOG.blockProperties)),
            WILLOW_LEAVES = create(List.of(BlockTags.leaves), new LeafBlockType(OAK_LEAVES.blockProperties)),
            REDWOOD_PLANK = create(List.of(BlockTags.planks), new BlockType(OAK_PLANK.blockProperties)),
            REDWOOD_LOG = create(new BlockType(OAK_LOG.blockProperties)), //50
            REDWOOD_LEAVES = create(List.of(BlockTags.leaves), new LeafBlockType(OAK_LEAVES.blockProperties)),
            HIBISCUS = create(List.of(BlockTags.flowers, BlockTags.survivesOnGrass), new PlantLightBlockType(((LightBlockProperties)(PORECAP.blockProperties)).copy().r(17).g(1).b(17))),
            BLUE_HIBISCUS = create(List.of(BlockTags.flowers, BlockTags.survivesOnGrass), new PlantLightBlockType(((LightBlockProperties)(PORECAP.blockProperties)).copy().r(1).g(10).b(17))),
            SNOW = create(new BlockType(new BlockProperties().ttb(200).blockSFX(new SFX[]{Sounds.GRAVEL_STEP1, Sounds.GRAVEL_STEP2}, 0.5f, 0.8f,
                    new SFX[]{Sounds.GRAVEL_STEP1, Sounds.GRAVEL_STEP2}, 0.5f, 0.8f))),
            GRAVEL = create(List.of(BlockTags.sediment), new PowderBlockType(SAND.blockProperties.copy().blockSFX(new SFX[]{Sounds.GRAVEL_STEP1, Sounds.GRAVEL_STEP2}, 0.4f, 1,
                    new SFX[]{Sounds.GRAVEL_STEP1, Sounds.GRAVEL_STEP2}, 0.4f, 1))), //55
            FLINT = create(List.of(BlockTags.blunt), new BlockType(new BlockProperties())),
            MUD = create(List.of(BlockTags.sediment), new BlockType(new BlockProperties().ttb(200).blockSFX(new SFX[]{Sounds.MUD_STEP1, Sounds.MUD_STEP2}, 0.66f, 0.66f,
                    new SFX[]{Sounds.MUD_STEP1, Sounds.MUD_STEP2}, 0.66f, 0.66f))),
            CLAY = create(List.of(BlockTags.sediment), new BlockType(MUD.blockProperties)),
            OBSIDIAN = create(List.of(BlockTags.blunt), new BlockType(GLASS.blockProperties.copy().ttb(2000).blocksLight(true))),
            IRON_ORE = create(new BlockType(new BlockProperties().blockSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.66f, 0.66f,
                    new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.66f, 0.66f))), //60
            COPPER_ORE = create(new BlockType(IRON_ORE.blockProperties.copy())),
            STICK = create(new BlockType(new BlockProperties().isSolid(false).blocksLight(false).obstructsHeightmap(false).isFluidReplaceable(true).blockSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 1, 1,
                    new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 1, 1))),
            STICK_PLATFORM = create(new BlockType(STICK.blockProperties.copy().isCollidable(true))),
            STEEL_FRAME = create(new BlockType(new BlockProperties().blocksLight(false).obstructsHeightmap(false).permeable(true).blockSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.66f, 0.66f,
                    new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.66f, 0.66f))),
            POWERED_VENT = create(new BlockType(new BlockProperties().blockSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.66f, 0.66f,
                    new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.66f, 0.66f))), //65
            BLUE_STAINED_GLASS = create(List.of(BlockTags.blunt), new BlockType(GLASS.blockProperties)),
            RED_STAINED_GLASS = create(List.of(BlockTags.blunt), new BlockType(GLASS.blockProperties)),
            REGOLITH = create(List.of(BlockTags.sediment), new PowderBlockType(GRAVEL.blockProperties.copy().blockSFX(new SFX[]{Sounds.GRAVEL_STEP1, Sounds.GRAVEL_STEP2}, 0.4f, 1,
                    new SFX[]{Sounds.GRAVEL_STEP1, Sounds.GRAVEL_STEP2}, 0.4f, 1))),
            MARTIAN_REGOLITH = create(List.of(BlockTags.sediment), new PowderBlockType(REGOLITH.blockProperties.copy())),
            RED_SAND = create(List.of(BlockTags.sediment, BlockTags.sand), new PowderBlockType(SAND.blockProperties.copy())),
            RED_SANDSTONE = create(List.of(BlockTags.blunt), new BlockType(SANDSTONE.blockProperties.copy())),
            RED_GRAVEL = create(List.of(BlockTags.sediment), new PowderBlockType(GRAVEL.blockProperties.copy()));

    private static BlockType create(List<BlockTag> tags, BlockType type) {
        for (BlockTag tag : tags) {
            tag.tagged.add(blockTypeMap.size());
        }
        type.tags = tags;
        blockTypeMap.put(blockTypeMap.size(), type);
        return type;
    }
    private static BlockType create(BlockType type) {
        blockTypeMap.put(blockTypeMap.size(), type);
        return type;
    }
}
