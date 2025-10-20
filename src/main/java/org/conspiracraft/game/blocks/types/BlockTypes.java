package org.conspiracraft.game.blocks.types;

import org.conspiracraft.game.audio.SFX;
import org.conspiracraft.game.audio.Sounds;
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
            AIR = create(new BlockType(new BlockProperties().blockSFX(new SFX[]{Sounds.CLOUD}, 0.75f, 0.75f, new SFX[]{Sounds.CLOUD}, 0.75f, 0.75f)
                    .isSolid(false).blocksLight(false).isCollidable(false).isFluidReplaceable(true).obstructsHeightmap(false))),
            WATER = create(new WaterBlockType(new BlockProperties().isSolid(false).blocksLight(false).isCollidable(false).isFluid(true).obstructsHeightmap(false).blockSFX(
                    new SFX[]{Sounds.SPLASH1}, 1f, 1.25f, new SFX[]{Sounds.SPLASH1}, 0f, 1f))),
            GRASS = create(List.of(Tags.sediment, Tags.grass), new BlockType(new BlockProperties().ttb(200).blockSFX(new SFX[]{Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 1, 1,
                    new SFX[]{Sounds.GRASS_STEP1, Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 1, 1))),
            DIRT = create(List.of(Tags.sediment, Tags.dirt), new BlockType(new BlockProperties().ttb(200).blockSFX(new SFX[]{Sounds.DIRT_STEP1, Sounds.DIRT_STEP2, Sounds.DIRT_STEP3}, 1, 1,
                    new SFX[]{Sounds.DIRT_STEP1, Sounds.DIRT_STEP2, Sounds.DIRT_STEP3}, 1, 1))),
            TALL_GRASS = create(List.of(Tags.survivesOnGrass), new PlantBlockType(GRASS.blockProperties.copy().ttb(50).obstructsHeightmap(false).isSolid(false).blocksLight(false).isCollidable(false).isFluidReplaceable(true)
                    .needsSupport(true))),
            ROSE = create(List.of(Tags.shortFlowers, Tags.flowers, Tags.survivesOnGrass), new PlantBlockType(TALL_GRASS.blockProperties)), //5
            TORCH = create(new LightBlockType((LightBlockProperties) new LightBlockProperties().r(20).g(15).ttb(100).obstructsHeightmap(false).isSolid(false).blocksLight(false)
                    .isCollidable(false).isFluidReplaceable(true).needsSupport(true))),
            KYANITE = create(List.of(Tags.rocks, Tags.crystals, Tags.blunt), new LightBlockType((LightBlockProperties) (new LightBlockProperties().g(2).b(6)
                    .blockSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 1, 1, new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 1, 1)))),
            WHITE_STONE = create(List.of(Tags.rocks, Tags.blunt), new BlockType(new BlockProperties())),
            IGNEOUS = create(List.of(Tags.rocks, Tags.blunt), new BlockType(new BlockProperties())),
            STONE = create(List.of(Tags.rocks, Tags.blunt), new BlockType(new BlockProperties())), //10
            GLASS = create(List.of(Tags.blunt), new BlockType(new BlockProperties().blockSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 1, 1,
                    new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 1, 1).blocksLight(false))),
            PURPLE_STAINED_GLASS = create(List.of(Tags.blunt), new BlockType(GLASS.blockProperties)),
            LIME_STAINED_GLASS = create(List.of(Tags.blunt), new BlockType(GLASS.blockProperties)),
            PORECAP = create(List.of(Tags.sediment), new PlantLightBlockType(((LightBlockProperties)TORCH.blockProperties.copy().ttb(50)).r(0).g(12).b(6))),
            OAK_PLANKS = create(List.of(Tags.planks), new BlockType(new BlockProperties().ttb(200).blockSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 1, 1,
                    new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 1, 1))), //15
            OAK_LOG = create(new BlockType(new BlockProperties().ttb(200).blockSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 1, 1,
                    new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 1, 1))),
            OAK_LEAVES = create(List.of(Tags.leaves), new LeafBlockType(new BlockProperties().ttb(100).blockSFX(new SFX[]{Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 1, 1,
                    new SFX[]{Sounds.GRASS_STEP1, Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 1, 1).isSolid(false).blocksLight(false).isCollidable(false).isFluidReplaceable(true))),
            HYDRANGEA = create(List.of(Tags.shortFlowers, Tags.flowers, Tags.survivesOnGrass), new PlantBlockType(ROSE.blockProperties)),
            MAGMA = create(List.of(Tags.blunt), new LightBlockType(((LightBlockProperties)(KYANITE.blockProperties.copy().blockSFX(
                    new SFX[]{Sounds.SIZZLE1, Sounds.SIZZLE2}, 1, 1, new SFX[]{Sounds.SIZZLE1, Sounds.SIZZLE2}, 1, 1))).r(16).g(6).b(0))),
            MAHOGANY_LOG = create(new BlockType(OAK_LOG.blockProperties)), //20
            MAHOGANY_LEAVES = create(List.of(Tags.leaves), new LeafBlockType(OAK_LEAVES.blockProperties)),
            BUCKET = create(List.of(Tags.buckets, Tags.cantBreakBlocks), new BlockType(new BlockProperties().ttb(0).isSolid(false).blocksLight(false).obstructsHeightmap(false))),
            SAND = create(List.of(Tags.sediment, Tags.sand), new PowderBlockType(new BlockProperties().ttb(200).blockSFX(new SFX[]{Sounds.SAND_STEP1, Sounds.SAND_STEP2}, 0.45f, 1.33f,
                    new SFX[]{Sounds.SAND_STEP1, Sounds.SAND_STEP2}, 0.45f, 1.33f).needsSupport(true).blocksLight(false).obstructsHeightmap(false))),
            SANDSTONE = create(List.of(Tags.blunt), new BlockType(new BlockProperties())),
            PALM_LOG = create(new BlockType(OAK_LOG.blockProperties)), //25
            PALM_PLANKS = create(List.of(Tags.planks), new BlockType(OAK_PLANKS.blockProperties)),
            PALM_LEAVES = create(List.of(Tags.leaves), new LeafBlockType(OAK_LEAVES.blockProperties)),
            MAHOGANY_PLANKS = create(List.of(Tags.planks), new BlockType(OAK_PLANKS.blockProperties)),
            CACTUS = create(List.of(Tags.survivesOnSand), new PlantBlockType(new BlockProperties().isSolid(false).blocksLight(false).obstructsHeightmap(false).isCollidable(true).isFluidReplaceable(true).needsSupport(true))),
            DEAD_BUSH = create(List.of(Tags.survivesOnSediment), new PlantBlockType(ROSE.blockProperties)), //30
            CLOUD = create(new BlockType(new BlockProperties().blockSFX(new SFX[]{Sounds.CLOUD}, 0.75f, 0.75f, new SFX[]{Sounds.CLOUD}, 0.75f, 0.75f)
                    .isSolid(false).isCollidable(false).blocksLight(false).obstructsHeightmap(false))),
            RAIN_CLOUD = create(new CloudBlockType(CLOUD.blockProperties)),
            DRY_MUD = create(List.of(Tags.soakers, Tags.sediment), new BlockType(DIRT.blockProperties)),
            SPRUCE_PLANKS = create(List.of(Tags.planks), new BlockType(OAK_PLANKS.blockProperties)),
            SPRUCE_LOG = create(new BlockType(OAK_LOG.blockProperties)), //35
            SPRUCE_LEAVES = create(List.of(Tags.leaves), new LeafBlockType(OAK_LEAVES.blockProperties)),
            CHERRY_PLANKS = create(List.of(Tags.planks), new BlockType(OAK_PLANKS.blockProperties)),
            CHERRY_LOG = create(new BlockType(OAK_LOG.blockProperties)),
            CHERRY_LEAVES = create(List.of(Tags.leaves), new LeafBlockType(OAK_LEAVES.blockProperties)),
            BIRCH_PLANKS = create(List.of(Tags.planks), new BlockType(OAK_PLANKS.blockProperties)), //40
            BIRCH_LOG = create(new BlockType(OAK_LOG.blockProperties)),
            BIRCH_LEAVES = create(List.of(Tags.leaves), new LeafBlockType(OAK_LEAVES.blockProperties)),
            ACACIA_PLANKS = create(List.of(Tags.planks), new BlockType(OAK_PLANKS.blockProperties)),
            ACACIA_LOG = create(new BlockType(OAK_LOG.blockProperties)),
            ACACIA_LEAVES = create(List.of(Tags.leaves), new LeafBlockType(OAK_LEAVES.blockProperties)), //45
            WILLOW_PLANKS = create(List.of(Tags.planks), new BlockType(OAK_PLANKS.blockProperties)),
            WILLOW_LOG = create(new BlockType(OAK_LOG.blockProperties)),
            WILLOW_LEAVES = create(List.of(Tags.leaves), new LeafBlockType(OAK_LEAVES.blockProperties)),
            REDWOOD_PLANKS = create(List.of(Tags.planks), new BlockType(OAK_PLANKS.blockProperties)),
            REDWOOD_LOG = create(new BlockType(OAK_LOG.blockProperties)), //50
            REDWOOD_LEAVES = create(List.of(Tags.leaves), new LeafBlockType(OAK_LEAVES.blockProperties)),
            HIBISCUS = create(List.of(Tags.flowers, Tags.survivesOnGrass), new PlantLightBlockType(((LightBlockProperties)(PORECAP.blockProperties)).copy().r(17).g(1).b(17))),
            BLUE_HIBISCUS = create(List.of(Tags.flowers, Tags.survivesOnGrass), new PlantLightBlockType(((LightBlockProperties)(PORECAP.blockProperties)).copy().r(1).g(10).b(17))),
            SNOW = create(new BlockType(new BlockProperties().ttb(200).blockSFX(new SFX[]{Sounds.GRAVEL_STEP1, Sounds.GRAVEL_STEP2}, 0.5f, 0.8f,
                    new SFX[]{Sounds.GRAVEL_STEP1, Sounds.GRAVEL_STEP2}, 0.5f, 0.8f))),
            GRAVEL = create(List.of(Tags.sediment), new PowderBlockType(SAND.blockProperties.copy().blockSFX(new SFX[]{Sounds.GRAVEL_STEP1, Sounds.GRAVEL_STEP2}, 0.4f, 1,
                    new SFX[]{Sounds.GRAVEL_STEP1, Sounds.GRAVEL_STEP2}, 0.4f, 1))), //55
            FLINT = create(List.of(Tags.blunt), new BlockType(new BlockProperties())),
            MUD = create(List.of(Tags.sediment), new BlockType(new BlockProperties().ttb(200).blockSFX(new SFX[]{Sounds.MUD_STEP1, Sounds.MUD_STEP2}, 0.66f, 0.66f,
                    new SFX[]{Sounds.MUD_STEP1, Sounds.MUD_STEP2}, 0.66f, 0.66f))),
            CLAY = create(List.of(Tags.sediment), new BlockType(MUD.blockProperties)),
            OBSIDIAN = create(List.of(Tags.blunt), new BlockType(GLASS.blockProperties.copy().ttb(2000).blocksLight(true))),
            STEAM = create(new BlockType(new BlockProperties().isSolid(false).blocksLight(false).isCollidable(false).obstructsHeightmap(false).isGas(true))), //60
            STEAM_BUCKET = create(List.of(Tags.buckets, Tags.cantBreakBlocks), new FullBucketBlockType(BUCKET.blockProperties)),
            WATER_BUCKET = create(List.of(Tags.buckets, Tags.cantBreakBlocks), new FullBucketBlockType(BUCKET.blockProperties)),
            FLINT_BATON = create(new BlockType(new BlockProperties().proficiencies(Map.of(Tags.blunt, 0.75f)).isSolid(false).isCollidable(false).blocksLight(false).obstructsHeightmap(false))),
            OBSIDIAN_MACE = create(new BlockType(FLINT_BATON.blockProperties.copy().proficiencies(Map.of(Tags.chipping, 0.75f)).blockSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 1, 1,
                    new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 1, 1))),
            STICK = create(new BlockType(FLINT_BATON.blockProperties.copy().isFluidReplaceable(true).blockSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 1, 1,
                    new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 1, 1))),
            STICK_PLATFORM = create(new BlockType(STICK.blockProperties.copy().isCollidable(true))),
            TUNGSTEN_HAMMER = create(new BlockType(FLINT_BATON.blockProperties.copy().proficiencies(Map.of(Tags.blunt, 0.5f))));

    private static BlockType create(List<Tag> tags, BlockType type) {
        for (Tag tag : tags) {
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
