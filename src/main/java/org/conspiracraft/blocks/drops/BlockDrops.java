package org.conspiracraft.blocks.drops;

import kotlin.Pair;
import org.joml.Vector2i;
import org.conspiracraft.blocks.types.BlockType;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.items.Item;

import java.util.ArrayList;
import java.util.Map;

public class BlockDrops {
    public static Map<BlockType, Map<Pair<Float, Integer>[], Item>> blockTypeDropTable = Map.ofEntries(
            Map.entry(BlockTypes.STONE, LootTables.STONE),
            Map.entry(BlockTypes.MARBLE, LootTables.MARBLE),
            Map.entry(BlockTypes.TALL_GRASS, LootTables.GRASS),
            Map.entry(BlockTypes.ROSE, LootTables.ROSE),
            Map.entry(BlockTypes.HYDRANGEA, LootTables.HYDRANGEA),
            Map.entry(BlockTypes.PORECAP, LootTables.PORECAP),
            Map.entry(BlockTypes.GRASS, LootTables.GRASSY_DIRT),
            Map.entry(BlockTypes.DIRT, LootTables.DIRT),
            Map.entry(BlockTypes.MARTIAN_REGOLITH, LootTables.MARTIAN_REGOLITH),
            Map.entry(BlockTypes.REGOLITH, LootTables.REGOLITH),
            Map.entry(BlockTypes.SAND, LootTables.SAND),
            Map.entry(BlockTypes.SANDSTONE, LootTables.SANDSTONE),
            Map.entry(BlockTypes.CLAY, LootTables.CLAY),
            Map.entry(BlockTypes.MUD, LootTables.MUD),
            Map.entry(BlockTypes.GRAVEL, LootTables.GRAVEL),
            Map.entry(BlockTypes.FLINT, LootTables.FLINT),
            Map.entry(BlockTypes.KYANITE, LootTables.KYANITE),
            Map.entry(BlockTypes.IRON_ORE, LootTables.IRON_ORE),
            Map.entry(BlockTypes.COPPER_ORE, LootTables.COPPER_ORE),
            Map.entry(BlockTypes.GLASS, LootTables.GLASS),
            Map.entry(BlockTypes.LIME_STAINED_GLASS, LootTables.LIME_STAINED_GLASS),
            Map.entry(BlockTypes.MAGENTA_STAINED_GLASS, LootTables.MAGENTA_STAINED_GLASS),
            Map.entry(BlockTypes.MAGMA, LootTables.MAGMA),
            Map.entry(BlockTypes.OAK_LOG, LootTables.OAK_LOG),
            Map.entry(BlockTypes.BIRCH_LOG, LootTables.BIRCH_LOG),
            Map.entry(BlockTypes.CHERRY_LOG, LootTables.CHERRY_LOG),
            Map.entry(BlockTypes.MAHOGANY_LOG, LootTables.MAHOGANY_LOG),
            Map.entry(BlockTypes.ACACIA_LOG, LootTables.ACACIA_LOG),
            Map.entry(BlockTypes.PALM_LOG, LootTables.PALM_LOG),
            Map.entry(BlockTypes.SPRUCE_LOG, LootTables.SPRUCE_LOG),
            Map.entry(BlockTypes.WILLOW_LOG, LootTables.WILLOW_LOG),
            Map.entry(BlockTypes.REDWOOD_LOG, LootTables.REDWOOD_LOG),
            Map.entry(BlockTypes.OAK_PLANK, LootTables.OAK_PLANK),
            Map.entry(BlockTypes.BIRCH_PLANK, LootTables.BIRCH_PLANK),
            Map.entry(BlockTypes.CHERRY_PLANK, LootTables.CHERRY_PLANK),
            Map.entry(BlockTypes.MAHOGANY_PLANK, LootTables.MAHOGANY_PLANK),
            Map.entry(BlockTypes.ACACIA_PLANK, LootTables.ACACIA_PLANK),
            Map.entry(BlockTypes.PALM_PLANK, LootTables.PALM_PLANK),
            Map.entry(BlockTypes.SPRUCE_PLANK, LootTables.SPRUCE_PLANK),
            Map.entry(BlockTypes.WILLOW_PLANK, LootTables.WILLOW_PLANK),
            Map.entry(BlockTypes.REDWOOD_PLANK, LootTables.REDWOOD_PLANK),
            Map.entry(BlockTypes.SPRUCE_LEAVES, LootTables.LEAVES), Map.entry(BlockTypes.REDWOOD_LEAVES, LootTables.LEAVES),
            Map.entry(BlockTypes.OAK_LEAVES, LootTables.APPLE_LEAVES), Map.entry(BlockTypes.BIRCH_LEAVES, LootTables.APPLE_LEAVES), Map.entry(BlockTypes.WILLOW_LEAVES, LootTables.APPLE_LEAVES),
            Map.entry(BlockTypes.PALM_LEAVES, LootTables.ORANGE_LEAVES), Map.entry(BlockTypes.MAHOGANY_LEAVES, LootTables.ORANGE_LEAVES), Map.entry(BlockTypes.ACACIA_LEAVES, LootTables.ORANGE_LEAVES),
            Map.entry(BlockTypes.CHERRY_LEAVES, LootTables.CHERRY_LEAVES)
    );

    public static ArrayList<Item> getDrops(Vector2i block) {
        ArrayList<Item> drops = new ArrayList<>();
        Map<Pair<Float, Integer>[], Item> items = blockTypeDropTable.get(BlockTypes.blockTypeMap.get(block.x));
        if (items != null) {
            boolean droppedAnything = false;
            for (Pair<Float, Integer>[] drop : items.keySet()) {
                for (Pair<Float, Integer> chance : drop) {
                    if ((!(chance.component1() >= 1.f && droppedAnything)) && Math.random() < chance.component1()) {
                        droppedAnything = true;
                        drops.add(items.get(drop).clone());
                    }
                }
            }
        }
        return drops;
    }
}
