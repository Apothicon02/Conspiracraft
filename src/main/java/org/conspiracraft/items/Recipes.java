package org.conspiracraft.items;

import kotlin.Pair;
import org.conspiracraft.items.types.ItemType;
import org.conspiracraft.items.types.ItemTypes;

import java.util.Map;

public class Recipes {
    public static Map<Pair<ItemType, ItemType>, ItemType> recipes = Map.of(
            new Pair<>(ItemTypes.DIRT, ItemTypes.GRASS), ItemTypes.GRASSY_DIRT,
            new Pair<>(ItemTypes.STICK, ItemTypes.DRIFTWOOD), ItemTypes.WOOD_SPADE
    );
    public static Map<Pair<ItemTag, ItemTag>, ItemType> tagRecipes = Map.of(
            new Pair<>(ItemTags.axe, ItemTags.log), ItemTypes.STICK
    );
}
