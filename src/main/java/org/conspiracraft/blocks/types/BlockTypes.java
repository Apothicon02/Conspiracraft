package org.conspiracraft.blocks.types;

import org.conspiracraft.audio.SFX;
import org.conspiracraft.audio.Sounds;
import org.conspiracraft.blocks.BlockTag;
import org.conspiracraft.blocks.BlockTags;
import org.conspiracraft.graphics.buffers.Buffer;
import org.conspiracraft.graphics.textures.ImageHelper;
import org.conspiracraft.graphics.textures.Texture3D;
import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.utils.Utils;
import org.lwjgl.system.MemoryStack;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class BlockTypes {
    public static int blockTexWidth = 8;
    public static long blockTexWidthL = blockTexWidth;
    public static int blockTexHeight = blockTexWidth * blockTexWidth;
    private static Map<Integer, BlockType> blockTypeMap = new HashMap<>(Map.of());

    public static BlockType
            AIR = create(new BlockType(blockTypeMap.size(), "misc/texture/air", new BlockProperties().blockSFX(
                    new SFX[]{Sounds.CLOUD}, 0.75f, 0.75f, new SFX[]{Sounds.CLOUD}, 0.75f, 0.75f)
                    .isSolid(false).blocksLight(false).isCollidable(false).isFluidReplaceable(true).obstructsHeightmap(false))),
            WATER = create(new BlockType(blockTypeMap.size(), "natural/texture/water", new BlockProperties().isSolid(false).blocksLight(false).isCollidable(false).isFluid(true).obstructsHeightmap(false).blockSFX(
                    new SFX[]{Sounds.SPLASH1}, 1f, 1.25f, new SFX[]{Sounds.SPLASH1}, 0f, 1f))),
            GRASS = create(List.of(BlockTags.sediment, BlockTags.grass, BlockTags.spadeEfficient), new BlockType(blockTypeMap.size(), "plant/texture/grass",  new BlockProperties().ttb(200).blockSFX(
                    new SFX[]{Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 1, 1, new SFX[]{Sounds.GRASS_STEP1, Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 1, 1))),
            DIRT = create(List.of(BlockTags.sediment, BlockTags.dirt, BlockTags.spadeEfficient), new BlockType(blockTypeMap.size(), "natural/texture/dirt",  new BlockProperties().ttb(200).blockSFX(
                    new SFX[]{Sounds.DIRT_STEP1, Sounds.DIRT_STEP2, Sounds.DIRT_STEP3}, 1, 1, new SFX[]{Sounds.DIRT_STEP1, Sounds.DIRT_STEP2, Sounds.DIRT_STEP3}, 1, 1))),
            TALL_GRASS = create(List.of(BlockTags.survivesOnGrass), new PlantBlockType(blockTypeMap.size(), "plant/texture/tall_grass",  GRASS.blockProperties.copy().ttb(50).obstructsHeightmap(false).isSolid(false).blocksLight(false).isCollidable(false).isFluidReplaceable(true)
                    .needsSupport(true))),
            ROSE = create(List.of(BlockTags.shortFlowers, BlockTags.flowers, BlockTags.survivesOnGrass), new PlantBlockType(blockTypeMap.size(), "plant/texture/rose",  TALL_GRASS.blockProperties)), //5
            TORCH = create(List.of(BlockTags.smallBlock), new LightBlockType(blockTypeMap.size(), "crafted/texture/torch",  (LightBlockProperties) new LightBlockProperties().r(40).g(38).b(30).ttb(100).obstructsHeightmap(false).isSolid(false).blocksLight(false)
                    .isCollidable(false).isFluidReplaceable(true).needsSupport(true).blockSFX(
                            new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 1, 1, new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 1, 1))),
            KYANITE = create(List.of(BlockTags.rocks, BlockTags.crystals, BlockTags.blunt), new LightBlockType(blockTypeMap.size(), "geological/texture/kyanite",  (LightBlockProperties) (new LightBlockProperties().r(4).g(20).b(40).blockSFX(
                    new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 1, 1, new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 1, 1)))).altTexLoad(true),
            MARBLE = create(List.of(BlockTags.rocks, BlockTags.blunt), new BlockType(blockTypeMap.size(), "geological/texture/marble",  new BlockProperties().blockSFX(
                    new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 1f, 0.6f, new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 1f, 0.5f))),
            IGNEOUS = create(List.of(BlockTags.rocks, BlockTags.blunt), new BlockType(blockTypeMap.size(), "geological/texture/igneous",  new BlockProperties())),
            STONE = create(List.of(BlockTags.rocks, BlockTags.blunt), new BlockType(blockTypeMap.size(), "geological/texture/stone",  new BlockProperties())), //10
            GLASS = create(List.of(BlockTags.blunt), new BlockType(blockTypeMap.size(), "crafted/texture/glass",  new BlockProperties().blockSFX(
                    new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 1, 1, new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 1, 1).blocksLight(false).obstructsHeightmap(false))),
            MAGENTA_STAINED_GLASS = create(List.of(BlockTags.blunt), new BlockType(blockTypeMap.size(), "crafted/texture/magenta_stained_glass",  GLASS.blockProperties)),
            LIME_STAINED_GLASS = create(List.of(BlockTags.blunt), new BlockType(blockTypeMap.size(), "crafted/texture/lime_stained_glass",  GLASS.blockProperties)),
            PORECAP = create(List.of(BlockTags.sediment), new PlantLightBlockType(blockTypeMap.size(), "plant/texture/porecap",  ((LightBlockProperties)TORCH.blockProperties.copy().ttb(50)).r(0).g(12).b(6))),
            OAK_PLANK = create(List.of(BlockTags.planks), new BlockType(blockTypeMap.size(), "tree/texture/oak_planks",  new BlockProperties().ttb(200).blockSFX(
                    new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 1, 1, new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 1, 1))), //15
            OAK_LOG = create(new BlockType(blockTypeMap.size(), "tree/texture/oak_log",  new BlockProperties().ttb(200).blockSFX(
                    new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 1, 1, new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 1, 1))),
            OAK_LEAVES = create(List.of(BlockTags.leaves), new LeafBlockType(blockTypeMap.size(), "tree/texture/oak_leaves",  new BlockProperties().ttb(100).blockSFX(
                    new SFX[]{Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 1, 1, new SFX[]{Sounds.GRASS_STEP1, Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 1, 1).isSolid(false).blocksLight(false).isCollidable(false).isFluidReplaceable(true))),
            HYDRANGEA = create(List.of(BlockTags.shortFlowers, BlockTags.flowers, BlockTags.survivesOnGrass), new PlantBlockType(blockTypeMap.size(), "plant/texture/hydrangea",  ROSE.blockProperties)),
            MAGMA = create(List.of(BlockTags.blunt), new LightBlockType(blockTypeMap.size(), "geological/texture/magma",  ((LightBlockProperties)(KYANITE.blockProperties.copy().blockSFX(
                    new SFX[]{Sounds.SIZZLE1, Sounds.SIZZLE2}, 1, 1, new SFX[]{Sounds.SIZZLE1, Sounds.SIZZLE2}, 1, 1))).r(16).g(6).b(0))),
            MAHOGANY_LOG = create(new BlockType(blockTypeMap.size(), "tree/texture/mahogany_log",  OAK_LOG.blockProperties)), //20
            MAHOGANY_LEAVES = create(List.of(BlockTags.leaves), new LeafBlockType(blockTypeMap.size(), "tree/texture/mahogany_leaves",  OAK_LEAVES.blockProperties)),
            BUCKET = create(List.of(BlockTags.buckets, BlockTags.cantBreakBlocks), new BlockType(blockTypeMap.size(), "crafted/texture/bucket",  new BlockProperties().ttb(0).isSolid(false).blocksLight(false).obstructsHeightmap(false))),
            SAND = create(List.of(BlockTags.sediment, BlockTags.sand, BlockTags.spadeEfficient), new PowderBlockType(blockTypeMap.size(), "natural/texture/sand",  new BlockProperties().ttb(200).blockSFX(
                    new SFX[]{Sounds.SAND_STEP1, Sounds.SAND_STEP2}, 0.45f, 1.33f, new SFX[]{Sounds.SAND_STEP1, Sounds.SAND_STEP2}, 0.45f, 1.33f).needsSupport(true).blocksLight(true).obstructsHeightmap(true))),
            SANDSTONE = create(List.of(BlockTags.blunt), new BlockType(blockTypeMap.size(), "geological/texture/sandstone",  new BlockProperties())),
            PALM_LOG = create(new BlockType(blockTypeMap.size(), "tree/texture/palm_log",  OAK_LOG.blockProperties)), //25
            PALM_PLANK = create(List.of(BlockTags.planks), new BlockType(blockTypeMap.size(), "tree/texture/palm_planks",  OAK_PLANK.blockProperties)),
            PALM_LEAVES = create(List.of(BlockTags.leaves), new LeafBlockType(blockTypeMap.size(), "tree/texture/palm_leaves",  OAK_LEAVES.blockProperties)),
            MAHOGANY_PLANK = create(List.of(BlockTags.planks), new BlockType(blockTypeMap.size(), "tree/texture/mahogany_planks",  OAK_PLANK.blockProperties)),
            CACTUS = create(List.of(BlockTags.survivesOnSand), new PlantBlockType(blockTypeMap.size(), "plant/texture/cactus",  new BlockProperties().isSolid(false).blocksLight(false).obstructsHeightmap(false).isCollidable(true).isFluidReplaceable(true).needsSupport(true))),
            DEAD_BUSH = create(List.of(BlockTags.survivesOnSediment), new PlantBlockType(blockTypeMap.size(), "plant/texture/dead_bush",  ROSE.blockProperties)), //30
            CLOUD = create(new BlockType(blockTypeMap.size(), "natural/texture/cloud",  new BlockProperties().blockSFX(
                    new SFX[]{Sounds.CLOUD}, 0.75f, 0.75f, new SFX[]{Sounds.CLOUD}, 0.75f, 0.75f)
                    .isSolid(false).isCollidable(false).blocksLight(false).obstructsHeightmap(false))),
            RAIN_CLOUD = create(new CloudBlockType(blockTypeMap.size(), "natural/texture/rain_cloud",  CLOUD.blockProperties)),
            DRY_MUD = create(List.of(BlockTags.soakers, BlockTags.sediment, BlockTags.spadeEfficient), new BlockType(blockTypeMap.size(), "natural/texture/dry_mud",  DIRT.blockProperties)),
            SPRUCE_PLANK = create(List.of(BlockTags.planks), new BlockType(blockTypeMap.size(), "tree/texture/spruce_planks",  OAK_PLANK.blockProperties)),
            SPRUCE_LOG = create(new BlockType(blockTypeMap.size(), "tree/texture/spruce_log",  OAK_LOG.blockProperties)), //35
            SPRUCE_LEAVES = create(List.of(BlockTags.leaves), new LeafBlockType(blockTypeMap.size(), "tree/texture/spruce_leaves",  OAK_LEAVES.blockProperties)),
            CHERRY_PLANK = create(List.of(BlockTags.planks), new BlockType(blockTypeMap.size(), "tree/texture/cherry_planks",  OAK_PLANK.blockProperties)),
            CHERRY_LOG = create(new BlockType(blockTypeMap.size(), "tree/texture/cherry_log",  OAK_LOG.blockProperties)),
            CHERRY_LEAVES = create(List.of(BlockTags.leaves), new LeafBlockType(blockTypeMap.size(), "tree/texture/cherry_leaves",  OAK_LEAVES.blockProperties)),
            BIRCH_PLANK = create(List.of(BlockTags.planks), new BlockType(blockTypeMap.size(), "tree/texture/birch_planks",  OAK_PLANK.blockProperties)), //40
            BIRCH_LOG = create(new BlockType(blockTypeMap.size(), "tree/texture/birch_log",  OAK_LOG.blockProperties)),
            BIRCH_LEAVES = create(List.of(BlockTags.leaves), new LeafBlockType(blockTypeMap.size(), "tree/texture/birch_leaves",  OAK_LEAVES.blockProperties)),
            ACACIA_PLANK = create(List.of(BlockTags.planks), new BlockType(blockTypeMap.size(), "tree/texture/acacia_planks",  OAK_PLANK.blockProperties)),
            ACACIA_LOG = create(new BlockType(blockTypeMap.size(), "tree/texture/acacia_log",  OAK_LOG.blockProperties)),
            ACACIA_LEAVES = create(List.of(BlockTags.leaves), new LeafBlockType(blockTypeMap.size(), "tree/texture/acacia_leaves",  OAK_LEAVES.blockProperties)), //45
            WILLOW_PLANK = create(List.of(BlockTags.planks), new BlockType(blockTypeMap.size(), "tree/texture/willow_planks",  OAK_PLANK.blockProperties)),
            WILLOW_LOG = create(new BlockType(blockTypeMap.size(), "tree/texture/willow_log",  OAK_LOG.blockProperties)),
            WILLOW_LEAVES = create(List.of(BlockTags.leaves), new LeafBlockType(blockTypeMap.size(), "tree/texture/willow_leaves",  OAK_LEAVES.blockProperties)),
            REDWOOD_PLANK = create(List.of(BlockTags.planks), new BlockType(blockTypeMap.size(), "tree/texture/redwood_planks",  OAK_PLANK.blockProperties)),
            REDWOOD_LOG = create(new BlockType(blockTypeMap.size(), "tree/texture/redwood_log",  OAK_LOG.blockProperties)), //50
            REDWOOD_LEAVES = create(List.of(BlockTags.leaves), new LeafBlockType(blockTypeMap.size(), "tree/texture/redwood_leaves",  OAK_LEAVES.blockProperties)),
            HIBISCUS = create(List.of(BlockTags.flowers, BlockTags.survivesOnGrass), new PlantLightBlockType(blockTypeMap.size(), "plant/texture/hibiscus",  ((LightBlockProperties)(PORECAP.blockProperties)).copy().r(17).g(1).b(17))),
            BLUE_HIBISCUS = create(List.of(BlockTags.flowers, BlockTags.survivesOnGrass), new PlantLightBlockType(blockTypeMap.size(), "plant/texture/blue_hibiscus",  ((LightBlockProperties)(PORECAP.blockProperties)).copy().r(1).g(10).b(17))),
            SNOW = create(List.of(BlockTags.spadeEfficient), new BlockType(blockTypeMap.size(), "natural/texture/snow",  new BlockProperties().ttb(200).blockSFX(
                    new SFX[]{Sounds.GRAVEL_STEP1, Sounds.GRAVEL_STEP2}, 0.5f, 0.8f, new SFX[]{Sounds.GRAVEL_STEP1, Sounds.GRAVEL_STEP2}, 0.5f, 0.8f))),
            GRAVEL = create(List.of(BlockTags.sediment, BlockTags.spadeEfficient), new PowderBlockType(blockTypeMap.size(), "natural/texture/gravel",  SAND.blockProperties.copy().blockSFX(
                    new SFX[]{Sounds.GRAVEL_STEP1, Sounds.GRAVEL_STEP2}, 0.4f, 1, new SFX[]{Sounds.GRAVEL_STEP1, Sounds.GRAVEL_STEP2}, 0.4f, 1))), //55
            FLINT = create(List.of(BlockTags.blunt), new BlockType(blockTypeMap.size(), "geological/texture/flint",  new BlockProperties())),
            MUD = create(List.of(BlockTags.sediment, BlockTags.spadeEfficient), new BlockType(blockTypeMap.size(), "natural/texture/mud",  new BlockProperties().ttb(200).blockSFX(
                    new SFX[]{Sounds.MUD_STEP1, Sounds.MUD_STEP2}, 0.66f, 0.66f, new SFX[]{Sounds.MUD_STEP1, Sounds.MUD_STEP2}, 0.66f, 0.66f))),
            CLAY = create(List.of(BlockTags.sediment, BlockTags.spadeEfficient), new BlockType(blockTypeMap.size(), "natural/texture/clay",  MUD.blockProperties)),
            OBSIDIAN = create(List.of(BlockTags.blunt), new BlockType(blockTypeMap.size(), "geological/texture/obsidian",  GLASS.blockProperties.copy().ttb(2000).blocksLight(true))),
            IRON_ORE = create(new BlockType(blockTypeMap.size(), "geological/texture/iron_ore",  new BlockProperties().blockSFX(
                    new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.66f, 0.66f, new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.66f, 0.66f))), //60
            COPPER_ORE = create(new BlockType(blockTypeMap.size(), "geological/texture/copper_ore",  IRON_ORE.blockProperties.copy())),
            STICK = create(new BlockType(blockTypeMap.size(), "natural/texture/stick",  new BlockProperties().isSolid(false).blocksLight(false).obstructsHeightmap(false).isFluidReplaceable(true).blockSFX(
                    new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 1, 1, new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 1, 1))),
            STICK_PLATFORM = create(new BlockType(blockTypeMap.size(), "crafted/texture/stick_platform",  STICK.blockProperties.copy().isCollidable(true))),
            STEEL_FRAME = create(new BlockType(blockTypeMap.size(), "crafted/texture/steel_frame",  new BlockProperties().blocksLight(false).obstructsHeightmap(false).permeable(true).blockSFX(
                    new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.66f, 0.66f, new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.66f, 0.66f))),
            POWERED_VENT = create(new BlockType(blockTypeMap.size(), "crafted/texture/powered_vent",  new BlockProperties().blockSFX(
                    new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.66f, 0.66f, new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.66f, 0.66f))), //65
            BLUE_STAINED_GLASS = create(List.of(BlockTags.blunt), new BlockType(blockTypeMap.size(), "crafted/texture/blue_stained_glass",  GLASS.blockProperties)),
            RED_STAINED_GLASS = create(List.of(BlockTags.blunt), new BlockType(blockTypeMap.size(), "crafted/texture/red_stained_glass",  GLASS.blockProperties)),
            ORANGE_SAND = create(List.of(BlockTags.sediment, BlockTags.spadeEfficient), new PowderBlockType(blockTypeMap.size(), "natural/texture/orange_sand",  SAND.blockProperties.copy())),
            ORANGE_SANDSTONE = create(List.of(BlockTags.blunt), new BlockType(blockTypeMap.size(), "geological/texture/orange_sandstone",  SANDSTONE.blockProperties.copy())),
            RED_SAND = create(List.of(BlockTags.sediment, BlockTags.sand, BlockTags.spadeEfficient), new PowderBlockType(blockTypeMap.size(), "natural/texture/red_sand",  SAND.blockProperties.copy())), //70
            RED_SANDSTONE = create(List.of(BlockTags.blunt), new BlockType(blockTypeMap.size(), "geological/texture/red_sandstone",  SANDSTONE.blockProperties.copy())),
            RED_GRAVEL = create(List.of(BlockTags.sediment, BlockTags.spadeEfficient), new PowderBlockType(blockTypeMap.size(), "natural/texture/red_gravel",  GRAVEL.blockProperties.copy())),
            WET_SAND = create(List.of(BlockTags.sediment, BlockTags.sand, BlockTags.spadeEfficient), new PowderBlockType(blockTypeMap.size(), "natural/texture/wet_sand",  SAND.blockProperties.copy())),
            ICE = create(List.of(BlockTags.blunt), new BlockType(blockTypeMap.size(), "natural/texture/ice",  GLASS.blockProperties.copy().blocksLight(true))),
            BASALT = create(List.of(BlockTags.blunt), new BlockType(blockTypeMap.size(), "geological/texture/basalt",  STONE.blockProperties.copy())), //75
            GRANITE = create(List.of(BlockTags.rocks, BlockTags.blunt), new BlockType(blockTypeMap.size(), "geological/texture/granite",  new BlockProperties())),
            CINNABAR = create(List.of(BlockTags.rocks, BlockTags.blunt), new BlockType(blockTypeMap.size(), "geological/texture/cinnabar",  new BlockProperties())),
            OBSIDIAN_DUST = create(List.of(BlockTags.sediment, BlockTags.spadeEfficient), new PowderBlockType(blockTypeMap.size(), "natural/texture/obsidian_dust",  GRAVEL.blockProperties.copy().blockSFX(
                    new SFX[]{Sounds.GRAVEL_STEP1, Sounds.GRAVEL_STEP2}, 0.4f, 1, new SFX[]{Sounds.GRAVEL_STEP1, Sounds.GRAVEL_STEP2}, 0.4f, 1))),
            STEEL_PLATING = create(new BlockType(blockTypeMap.size(), "crafted/texture/steel_plating",  new BlockProperties().blockSFX(
                    new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.66f, 0.66f, new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.66f, 0.66f))),
            HAZARD = create(new BlockType(blockTypeMap.size(), "crafted/texture/hazard",  new BlockProperties().blockSFX(
                    new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.66f, 0.66f, new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.66f, 0.66f))).altTexLoad(true), //80
            ROSE_QUARTZ = create(List.of(BlockTags.blunt), new BlockType(blockTypeMap.size(), "geological/texture/rose_quartz",  ICE.blockProperties.copy().blocksLight(true))).altTexLoad(true),
            CINNABAR_SPIKES = create(new PlantBlockType(blockTypeMap.size(), "geological/texture/cinnabar_spikes",  CINNABAR.blockProperties.copy().obstructsHeightmap(false).isSolid(false).blocksLight(false).isCollidable(false).isFluidReplaceable(true).needsSupport(true))),
            TURQUOISE = create(List.of(BlockTags.blunt), new BlockType(blockTypeMap.size(), "geological/texture/turquoise",  ROSE_QUARTZ.blockProperties.copy())),
            LAPIS = create(List.of(BlockTags.blunt), new BlockType(blockTypeMap.size(), "geological/texture/lapis",  TURQUOISE.blockProperties.copy())),
            AZURINE = create(List.of(BlockTags.blunt), new BlockType(blockTypeMap.size(), "geological/texture/azurine",  LAPIS.blockProperties.copy())); //85

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
    public static final BlockType[] blockTypes = createArr();
    public static BlockType[] createArr() {
        BlockType[] arr = new BlockType[blockTypeMap.size()];
        for (int i = 0; i < blockTypeMap.size(); i++) {
            arr[i] = blockTypeMap.get(i);
        }
        blockTypeMap = null;
        return arr;
    }
    public static boolean reloading = false;
    public static Buffer atlasBuffer;
    public static void fillTexture(MemoryStack stack) throws IOException {
        int sliceSize = Textures.atlas.width*Textures.atlas.height;
        int texSize = sliceSize*((Texture3D)Textures.atlas).depth;
        if (atlasBuffer == null) {
            atlasBuffer = new Buffer(stack, texSize * 4, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, true);
        }
        for (int i = 0; i < blockTypes.length; i++) {
            BlockType type = blockTypes[i];
            BufferedImage image = Utils.loadImage("block/"+type.name);
            int height = image.getHeight();
            ByteBuffer blockBuf = Utils.imageToBuffer(image);
            if (height > blockTexWidth) {
                copyTexture(blockBuf, i, height);
            } else {
                copyPartialTexture(blockBuf, i, type.altTexLoad);
            }
            memFree(blockBuf);
        }
        ImageHelper.fillImage(stack, Textures.atlas, atlasBuffer, reloading);
        reloading = true;
    }
    public static void copyTexture(ByteBuffer buf, int i, int height) {
        for (long row = 0; row < height; row++) {
            memCopy(memAddress(buf) + row * blockTexWidth * 4L,
                    atlasBuffer.pointer.get(0) + ((row * Textures.atlas.width + (i * blockTexWidthL)) * 4L),
                    blockTexWidth * 4L);
        }
    }
    public static void copyPartialTexture(ByteBuffer buf, int i, boolean alt) {
        for (long row = 0; row < blockTexWidth; row++) {
            for (long layer = 0; layer < blockTexHeight/2; layer+=blockTexWidth) {
                memCopy(memAddress(buf) + row * blockTexWidth * 4L,
                        atlasBuffer.pointer.get(0) + (((row+layer) * Textures.atlas.width + (i * blockTexWidthL)) * 4L),
                        blockTexWidth * 4L);
                for (long col = 0; col < blockTexWidth; col++) {
                    if (alt) {
                        memCopy(memAddress(buf) + ((((blockTexWidth - 1) - (row - 1 < 0 ? blockTexWidth - 1 : row - 1)) * blockTexWidth) + ((blockTexWidth - 1) - col)) * 4L,
                                atlasBuffer.pointer.get(0) + ((((row + (layer + (blockTexHeight / 2))) * Textures.atlas.width) + col + (i * blockTexWidthL)) * 4L),
                                4L);
                    } else {
                        memCopy(memAddress(buf) + ((((blockTexWidth - 1) - row) * blockTexWidth) + ((blockTexWidth - 1) - col)) * 4L,
                                atlasBuffer.pointer.get(0) + ((((row + (layer + (blockTexHeight / 2))) * Textures.atlas.width) + col + (i * blockTexWidthL)) * 4L),
                                4L);
                    }
                }
            }
        }
        for (long row = 1; row < blockTexWidth; row++) {
            for (long col = 0; col < blockTexWidth; col++) {
                memCopy(memAddress(buf) + ((row * blockTexWidth) + col) * 4L,
                        atlasBuffer.pointer.get(0) + ((((((row - 1) * blockTexWidth) + blockTexWidth) * Textures.atlas.width) + col + (i * blockTexWidthL)) * 4L),
                        4L);
                if (alt) {
                    if (col+1 < blockTexWidth) {
                        memCopy(memAddress(buf) + ((row * blockTexWidth) + col) * 4L,
                                atlasBuffer.pointer.get(0) + ((((((row - 1) * blockTexWidth) + blockTexWidth + (blockTexWidth - 1)) * Textures.atlas.width) + col + 1 + (i * blockTexWidthL)) * 4L),
                                4L);
                    }
                } else {
                    if (row + 1 < blockTexWidth) {
                        memCopy(memAddress(buf) + ((((blockTexWidth - 1) - row) * blockTexWidth) + ((blockTexWidth - 1) - col)) * 4L,
                                atlasBuffer.pointer.get(0) + ((((((row - 1) * blockTexWidth) + blockTexWidth + (blockTexWidth - 1)) * Textures.atlas.width) + col + (i * blockTexWidthL)) * 4L),
                                4L);
                    }
                }
                memCopy(memAddress(buf) + ((row * blockTexWidth) + col) * 4L,
                        atlasBuffer.pointer.get(0) + (((((((row - 1) * blockTexWidth) + blockTexWidth) + col) * Textures.atlas.width) + (i * blockTexWidthL)) * 4L),
                        4L);
                if (alt) {
                    if (col+1 < blockTexWidth) {
                        memCopy(memAddress(buf) + ((row * blockTexWidth) + col) * 4L,
                                atlasBuffer.pointer.get(0) + (((((((row - 1) * blockTexWidth) + blockTexWidth) + col + 1) * Textures.atlas.width) + (blockTexWidth - 1) + (i * blockTexWidthL)) * 4L),
                                4L);
                    }
                } else {
                    if (row + 1 < blockTexWidth) {
                        memCopy(memAddress(buf) + ((((blockTexWidth - 1) - row) * blockTexWidth) + ((blockTexWidth - 1) - col)) * 4L,
                                atlasBuffer.pointer.get(0) + (((((((row - 1) * blockTexWidth) + blockTexWidth) + col) * Textures.atlas.width) + (blockTexWidth - 1) + (i * blockTexWidthL)) * 4L),
                                4L);
                    }
                }
            }
        }
    }
}
