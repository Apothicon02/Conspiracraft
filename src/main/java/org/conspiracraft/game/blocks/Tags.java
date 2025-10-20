package org.conspiracraft.game.blocks;

import kotlin.Pair;

import java.util.List;

public class Tags {
    public static Tag rocks = new Tag();
    public static Tag leaves = new Tag();
    public static Tag shortFlowers = new Tag();
    public static Tag flowers = new Tag();
    public static Tag survivesOnGrass = new Tag();
    public static Tag grass = new Tag();
    public static Tag survivesOnDirt = new Tag();
    public static Tag dirt = new Tag();
    public static Tag survivesOnSand = new Tag();
    public static Tag sand = new Tag();
    public static Tag survivesOnSediment = new Tag();
    public static Tag sediment = new Tag();
    public static Tag soakers = new Tag();
    public static Tag crystals = new Tag();
    public static Tag planks = new Tag();
    public static Tag buckets = new Tag();
    public static Tag cantBreakBlocks = new Tag();
    public static Tag blunt = new Tag();
    public static Tag chipping = new Tag();
    public static List<Tag> tags = List.of(rocks, leaves, shortFlowers, flowers, soakers, crystals, planks, buckets, cantBreakBlocks, blunt, chipping);
    public static List<Pair<Tag, Tag>> survivalTags = List.of(
            new Pair<>(survivesOnGrass, grass),
            new Pair<>(survivesOnDirt, dirt),
            new Pair<>(survivesOnSand, sand),
            new Pair<>(survivesOnSediment, sediment)
    );
}