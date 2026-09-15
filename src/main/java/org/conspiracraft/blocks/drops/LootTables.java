package org.conspiracraft.blocks.drops;

import kotlin.Pair;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.types.ItemTypes;

import java.util.Map;

public class LootTables {
    public static Map<Pair<Float, Integer>[], Item>  //must have any kind of "guaranteed" drop last.
            STICK = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.STICK.createItem()),
            STONE = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.STONE.createItem()),
            MARBLE = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.MARBLE.createItem()),
            GRASS = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.GRASS.createItem()),
            ROSE = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.ROSE.createItem()),
            HYDRANGEA = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.HYDRANGEA.createItem()),
            PORECAP = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.PORECAP.createItem()),
            GRASSY_DIRT = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.GRASSY_DIRT.createItem()),
            DIRT = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.DIRT.createItem()),
            MARTIAN_REGOLITH = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.MARTIAN_REGOLITH.createItem()),
            REGOLITH = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.REGOLITH.createItem()),
            WET_SAND = Map.of(
                    new Pair[]{new Pair<>(0.5f, 1)}, ItemTypes.DRIFTWOOD.createItem(),
                    new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.SAND.createItem()),
            SAND = Map.of(
                    new Pair[]{new Pair<>(0.05f, 1)}, ItemTypes.DRIFTWOOD.createItem(),
                    new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.SAND.createItem()),
            SANDSTONE = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.SANDSTONE.createItem()),
            CLAY = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.CLAY.createItem()),
            MUD = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.MUD.createItem()),
            GRAVEL = Map.of(
                    new Pair[]{new Pair<>(0.05f, 1), new Pair<>(0.2f, 2)}, ItemTypes.FLINT.createItem(),
                    new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.GRAVEL.createItem()),
            KYANITE = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.KYANITE.createItem()),
            FLINT = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.FLINT.createItem()),
            PEBBLE = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.PEBBLE.createItem()),
            IRON_ORE = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.IRON_ORE.createItem()),
            COPPER_ORE = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.COPPER_ORE.createItem()),
            GLASS = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.GLASS.createItem()),
            LIME_STAINED_GLASS = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.LIME_STAINED_GLASS.createItem()),
            MAGENTA_STAINED_GLASS = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.MAGENTA_STAINED_GLASS.createItem()),
            MAGMA = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.MAGMA.createItem()),
            OAK_LOG = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.OAK_LOG.createItem()),
            BIRCH_LOG = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.BIRCH_LOG.createItem()),
            CHERRY_LOG = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.CHERRY_LOG.createItem()),
            MAHOGANY_LOG = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.MAHOGANY_LOG.createItem()),
            ACACIA_LOG = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.ACACIA_LOG.createItem()),
            PALM_LOG = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.PALM_LOG.createItem()),
            SPRUCE_LOG = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.SPRUCE_LOG.createItem()),
            WILLOW_LOG = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.WILLOW_LOG.createItem()),
            REDWOOD_LOG = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.REDWOOD_LOG.createItem()),
            OAK_PLANK = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.OAK_PLANK.createItem()),
            BIRCH_PLANK = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.BIRCH_PLANK.createItem()),
            CHERRY_PLANK = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.CHERRY_PLANK.createItem()),
            MAHOGANY_PLANK = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.MAHOGANY_PLANK.createItem()),
            ACACIA_PLANK = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.ACACIA_PLANK.createItem()),
            PALM_PLANK = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.PALM_PLANK.createItem()),
            SPRUCE_PLANK = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.SPRUCE_PLANK.createItem()),
            WILLOW_PLANK = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.WILLOW_PLANK.createItem()),
            REDWOOD_PLANK = Map.of(new Pair[]{new Pair<>(1.f, 1)}, ItemTypes.REDWOOD_PLANK.createItem()),
            LEAVES = Map.of(new Pair[]{new Pair<>(0.05f, 2), new Pair<>(0.2f, 1)}, ItemTypes.STICK.createItem()),
            APPLE_LEAVES = Map.of(
                    new Pair[]{new Pair<>(0.05f, 2), new Pair<>(0.2f, 1)}, ItemTypes.STICK.createItem(),
                    new Pair[]{new Pair<>(0.01f, 2), new Pair<>(0.04f, 1)}, ItemTypes.APPLE.createItem()),
            ORANGE_LEAVES = Map.of(
                    new Pair[]{new Pair<>(0.05f, 2), new Pair<>(0.2f, 1)}, ItemTypes.STICK.createItem(),
                    new Pair[]{new Pair<>(0.01f, 2), new Pair<>(0.04f, 1)}, ItemTypes.ORANGE.createItem()),
            CHERRY_LEAVES = Map.of(
                    new Pair[]{new Pair<>(0.05f, 2), new Pair<>(0.2f, 1)}, ItemTypes.STICK.createItem(),
                    new Pair[]{new Pair<>(0.02f, 2), new Pair<>(0.06f, 1)}, ItemTypes.CHERRY.createItem());
}
