package org.conspiracraft.items;

import kotlin.Pair;
import org.conspiracraft.items.types.ItemType;
import org.conspiracraft.items.types.ItemTypes;

import java.util.Map;

public class Recipes {
    public static Map<Pair<Object, Object>, Product> recipes = Map.of(
            new Pair<>(ItemTypes.DIRT, ItemTypes.GRASS), new Product(ItemTypes.GRASSY_DIRT, true),
            new Pair<>(ItemTypes.PEBBLE, ItemTypes.PEBBLE), new Product(ItemTypes.STONE, true),
            new Pair<>(ItemTags.log, ItemTypes.PAPER), new Product(ItemTypes.RESEARCH_TABLE, true),
            new Pair<>(ItemTypes.STICK, ItemTypes.PEBBLE), new Product(ItemTypes.STONE_HATCHET, true),
            new Pair<>(ItemTags.axe, ItemTypes.BAMBOO), new Product(ItemTypes.PAPER, false)
    );
    public record Product(ItemType itemType, boolean consume) {}
    public static Product getProduct(Item cursorItem, Item selItem) {
        Recipes.Product product = Recipes.recipes.get(new Pair<>(cursorItem.type, selItem.type));
        if (product == null) {
            product = Recipes.recipes.get(new Pair<>(selItem.type, cursorItem.type));
        }
        if (product == null) {
            for (ItemTag tag : cursorItem.type.tags) {
                if (tag.tagged.contains(cursorItem.type)) {
                    for (ItemTag selTag : selItem.type.tags) {
                        if (selTag.tagged.contains(selItem.type)) {
                            product = Recipes.recipes.get(new Pair<>(tag, selTag));
                            if (product != null) {
                                return product;
                            }
                            product = Recipes.recipes.get(new Pair<>(selTag, tag));
                            if (product != null) {
                                return product;
                            }
                        }
                    }
                }
            }
            for (ItemTag tag : cursorItem.type.tags) {
                if (tag.tagged.contains(cursorItem.type)) {
                    product = Recipes.recipes.get(new Pair<>(tag, selItem.type));
                    if (product != null) {
                        return product;
                    }
                    product = Recipes.recipes.get(new Pair<>(selItem.type, tag));
                    if (product != null) {
                        return product;
                    }
                }
            }
            for (ItemTag selTag : selItem.type.tags) {
                if (selTag.tagged.contains(selItem.type)) {
                    product = Recipes.recipes.get(new Pair<>(cursorItem.type, selTag));
                    if (product != null) {
                        return product;
                    }
                    product = Recipes.recipes.get(new Pair<>(selTag, cursorItem.type));
                    if (product != null) {
                        return product;
                    }
                }
            }
        }
        return product;
    }
}
