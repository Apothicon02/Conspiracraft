package org.conspiracraft.items;

import kotlin.Pair;

import java.util.Map;

public class Recipes {
    public static Map<Pair<ItemType, ItemType>, ItemType> recipes = Map.of(
            new Pair<>(ItemTypes.DIRT, ItemTypes.GRASS), ItemTypes.GRASSY_DIRT
    );
    public static Map<Pair<ItemTag, ItemTag>, ItemType> tagRecipes = Map.of(
            new Pair<>(ItemTags.axe, ItemTags.log), ItemTypes.STICK
    );
}
