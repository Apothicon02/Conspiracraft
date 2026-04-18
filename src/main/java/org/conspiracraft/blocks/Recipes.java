package org.conspiracraft.blocks;

import org.joml.Vector3f;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.ItemTypes;
import org.conspiracraft.world.World;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class Recipes {
    public static Map<Integer, Map<Item, Float>> siftingRecipes = Map.of( //must have any kind of "guaranteed" drop last.
            BlockTypes.getId(BlockTypes.SAND), Map.of(new Item().type(ItemTypes.SAND), 1.f),
            BlockTypes.getId(BlockTypes.GRAVEL), Map.of(new Item().type(ItemTypes.FLINT), 0.05f, new Item().type(ItemTypes.GRAVEL), 1.f),
            BlockTypes.getId(BlockTypes.MARTIAN_REGOLITH), Map.of(new Item().type(ItemTypes.COPPER_ORE), 0.15f, new Item().type(ItemTypes.IRON_ORE), 0.67f),
            BlockTypes.getId(BlockTypes.REGOLITH), Map.of(new Item().type(ItemTypes.IRON_ORE), 0.05f, new Item().type(ItemTypes.COPPER_ORE), 0.05f));

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
