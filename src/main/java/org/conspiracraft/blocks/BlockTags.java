package org.conspiracraft.blocks;

import kotlin.Pair;

import java.util.List;

public class BlockTags {
    public static BlockTag rocks = new BlockTag();
    public static BlockTag leaves = new BlockTag();
    public static BlockTag shortFlowers = new BlockTag();
    public static BlockTag flowers = new BlockTag();
    public static BlockTag survivesOnGrass = new BlockTag();
    public static BlockTag grass = new BlockTag();
    public static BlockTag survivesOnDirt = new BlockTag();
    public static BlockTag dirt = new BlockTag();
    public static BlockTag survivesOnSand = new BlockTag();
    public static BlockTag sand = new BlockTag();
    public static BlockTag survivesOnSediment = new BlockTag();
    public static BlockTag sediment = new BlockTag();
    public static BlockTag soakers = new BlockTag();
    public static BlockTag crystals = new BlockTag();
    public static BlockTag planks = new BlockTag();
    public static BlockTag buckets = new BlockTag();
    public static BlockTag cantBreakBlocks = new BlockTag();
    public static BlockTag blunt = new BlockTag();
    public static BlockTag chipping = new BlockTag();
    public static BlockTag smallBlock = new BlockTag();
    public static List<BlockTag> tags = List.of(rocks, leaves, shortFlowers, flowers, soakers, crystals, planks, buckets, cantBreakBlocks, blunt, chipping, smallBlock);
    public static List<Pair<BlockTag, BlockTag>> survivalTags = List.of(
            new Pair<>(survivesOnGrass, grass),
            new Pair<>(survivesOnDirt, dirt),
            new Pair<>(survivesOnSand, sand),
            new Pair<>(survivesOnSediment, sediment)
    );
}