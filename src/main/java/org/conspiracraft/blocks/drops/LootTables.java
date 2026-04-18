package org.conspiracraft.blocks.drops;

import kotlin.Pair;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.ItemTypes;

import java.util.Map;

public class LootTables {
    public static Map<Pair<Float, Integer>[], Item>  //must have any kind of "guaranteed" drop last.
            STONE = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.STONE)),
            MARBLE = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.MARBLE)),
            GRASS = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.GRASS)),
            ROSE = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.ROSE)),
            HYDRANGEA = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.HYDRANGEA)),
            PORECAP = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.PORECAP)),
            GRASSY_DIRT = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.GRASSY_DIRT)),
            DIRT = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.DIRT)),
            MARTIAN_REGOLITH = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.MARTIAN_REGOLITH)),
            REGOLITH = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.REGOLITH)),
            SAND = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.SAND)),
            SANDSTONE = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.SANDSTONE)),
            CLAY = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.CLAY)),
            MUD = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.MUD)),
            GRAVEL = Map.of(
                    new Pair[]{new Pair<>(0.05f, 1), new Pair<>(0.2f, 2)}, new Item().type(ItemTypes.FLINT),
                    new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.GRAVEL)),
            KYANITE = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.KYANITE)),
            FLINT = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.FLINT)),
            IRON_ORE = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.IRON_ORE)),
            COPPER_ORE = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.COPPER_ORE)),
            GLASS = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.GLASS)),
            LIME_STAINED_GLASS = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.LIME_STAINED_GLASS)),
            MAGENTA_STAINED_GLASS = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.MAGENTA_STAINED_GLASS)),
            MAGMA = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.MAGMA)),
            OAK_LOG = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.OAK_LOG)),
            BIRCH_LOG = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.BIRCH_LOG)),
            CHERRY_LOG = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.CHERRY_LOG)),
            MAHOGANY_LOG = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.MAHOGANY_LOG)),
            ACACIA_LOG = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.ACACIA_LOG)),
            PALM_LOG = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.PALM_LOG)),
            SPRUCE_LOG = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.SPRUCE_LOG)),
            WILLOW_LOG = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.WILLOW_LOG)),
            REDWOOD_LOG = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.REDWOOD_LOG)),
            OAK_PLANK = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.OAK_PLANK)),
            BIRCH_PLANK = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.BIRCH_PLANK)),
            CHERRY_PLANK = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.CHERRY_PLANK)),
            MAHOGANY_PLANK = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.MAHOGANY_PLANK)),
            ACACIA_PLANK = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.ACACIA_PLANK)),
            PALM_PLANK = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.PALM_PLANK)),
            SPRUCE_PLANK = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.SPRUCE_PLANK)),
            WILLOW_PLANK = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.WILLOW_PLANK)),
            REDWOOD_PLANK = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.REDWOOD_PLANK)),
            LEAVES = Map.of(
                    new Pair[]{new Pair<>(0.05f, 2), new Pair<>(0.2f, 1)}, new Item().type(ItemTypes.STICK)),
            APPLE_LEAVES = Map.of(
                    new Pair[]{new Pair<>(0.05f, 2), new Pair<>(0.2f, 1)}, new Item().type(ItemTypes.STICK),
                    new Pair[]{new Pair<>(0.01f, 2), new Pair<>(0.04f, 1)}, new Item().type(ItemTypes.APPLE)),
            ORANGE_LEAVES = Map.of(
                    new Pair[]{new Pair<>(0.05f, 2), new Pair<>(0.2f, 1)}, new Item().type(ItemTypes.STICK),
                    new Pair[]{new Pair<>(0.01f, 2), new Pair<>(0.04f, 1)}, new Item().type(ItemTypes.ORANGE)),
            CHERRY_LEAVES = Map.of(
                    new Pair[]{new Pair<>(0.05f, 2), new Pair<>(0.2f, 1)}, new Item().type(ItemTypes.STICK),
                    new Pair[]{new Pair<>(0.02f, 2), new Pair<>(0.06f, 1)}, new Item().type(ItemTypes.CHERRY));
}
