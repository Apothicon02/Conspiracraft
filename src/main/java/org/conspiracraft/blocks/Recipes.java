package org.conspiracraft.blocks;

import org.joml.Vector3f;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.types.ItemTypes;
import org.conspiracraft.world.World;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class Recipes {
    public static Map<Integer, Integer> strippingRecipes = Map.of(
            BlockTypes.OAK_FENCE.id, BlockTypes.OAK_FENCE_STRIPPED.id,
            BlockTypes.BIRCH_FENCE.id, BlockTypes.BIRCH_FENCE_STRIPPED.id,
            BlockTypes.SPRUCE_FENCE.id, BlockTypes.SPRUCE_FENCE_STRIPPED.id,
            BlockTypes.MAHOGANY_FENCE.id, BlockTypes.MAHOGANY_FENCE_STRIPPED.id,
            BlockTypes.ACACIA_FENCE.id, BlockTypes.ACACIA_FENCE_STRIPPED.id,
            BlockTypes.CHERRY_FENCE.id, BlockTypes.CHERRY_FENCE_STRIPPED.id,
            BlockTypes.PALM_FENCE.id, BlockTypes.PALM_FENCE_STRIPPED.id,
            BlockTypes.REDWOOD_FENCE.id, BlockTypes.REDWOOD_FENCE_STRIPPED.id,
            BlockTypes.WILLOW_FENCE.id, BlockTypes.WILLOW_FENCE_STRIPPED.id);

    public static Map<Integer, Map<Item, Float>> siftingRecipes = Map.of( //must have any kind of "guaranteed" drop last.
            BlockTypes.SAND.id, Map.of(ItemTypes.SAND.createItem(), 1.f),
            BlockTypes.GRAVEL.id, Map.of(ItemTypes.FLINT.createItem(), 0.05f, ItemTypes.GRAVEL.createItem(), 1.f),
            BlockTypes.RED_SAND.id, Map.of(ItemTypes.COPPER_ORE.createItem(), 0.15f, ItemTypes.IRON_ORE.createItem(), 0.67f),
            BlockTypes.ORANGE_SAND.id, Map.of(ItemTypes.IRON_ORE.createItem(), 0.05f, ItemTypes.COPPER_ORE.createItem(), 0.05f));

    public static void drop(int blockType, Vector3f pos) {
        AtomicBoolean droppedAnything = new AtomicBoolean(false);
        Map<Item, Float> recipe = Recipes.siftingRecipes.get(blockType);
        recipe.forEach((Item item, Float chance) -> {
            if ((!(chance >= 1.f && droppedAnything.get())) && Math.random() < chance) {
                droppedAnything.set(true);
                World.items.add(item.clone().moveTo(pos));
            }
        });
    }
}
