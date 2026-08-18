package org.conspiracraft.items;

import kotlin.Pair;
import org.conspiracraft.items.types.ItemType;
import org.conspiracraft.items.types.ItemTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Recipes {
    public static Map<Pair<Object, Object>, Product> recipes = Map.of(
            new Pair<>(ItemTypes.DIRT, ItemTypes.GRASS), new Product(ItemTypes.GRASSY_DIRT, true),
            new Pair<>(ItemTypes.PEBBLE, ItemTypes.PEBBLE), new Product(ItemTypes.STONE, true),
            new Pair<>(ItemTags.log, ItemTypes.PAPER), new Product(ItemTypes.RESEARCH_TABLE, true),
            new Pair<>(ItemTypes.STICK, ItemTypes.PAPER), new Product(ItemTypes.SHOJI, true),
            new Pair<>(ItemTypes.STICK, ItemTypes.PEBBLE), new Product(ItemTypes.STONE_HATCHET, true),
            new Pair<>(ItemTags.axe, ItemTags.log), new Product(ItemTypes.STICK, false),
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
                for (ItemTag selTag : selItem.type.tags) {
                    product = Recipes.recipes.get(new Pair<>(tag, selTag));
                    if (product != null) {return product;}
                    product = Recipes.recipes.get(new Pair<>(selTag, tag));
                    if (product != null) {if (product.consume()) {return product;} else {product = null;}}
                }
            }
            for (ItemTag tag : cursorItem.type.tags) {
                product = Recipes.recipes.get(new Pair<>(tag, selItem.type));
                if (product != null) {return product;}
                product = Recipes.recipes.get(new Pair<>(selItem.type, tag));
                if (product != null) {if (product.consume()) {return product;} else {product = null;}}
            }
            for (ItemTag selTag : selItem.type.tags) {
                product = Recipes.recipes.get(new Pair<>(cursorItem.type, selTag));
                if (product != null) {return product;}
                product = Recipes.recipes.get(new Pair<>(selTag, cursorItem.type));
                if (product != null) {if (product.consume()) {return product;} else {product = null;}}
            }
        }
        return product;
    }
    public static List<Pair<ItemType, ItemType>> getUses(ItemType item) {
        List<Pair<ItemType, ItemType>> uses = new ArrayList<>();
        recipes.forEach((ingredients, product) -> {
            boolean matchFirst = ingredients.component1() == item, matchLast = ingredients.component2() == item;
            ItemTag firstTag = ingredients.component1() instanceof ItemTag ? (ItemTag) ingredients.component1() : null,
                    lastTag = ingredients.component2() instanceof ItemTag ? (ItemTag) ingredients.component2() : null;
            if (matchFirst || (!matchLast && firstTag != null && firstTag.tagged.contains(item))) {
                if (ingredients.component2() instanceof ItemType ingredient) {
                    uses.add(new Pair<>(ingredient, product.itemType));
                } else if (ingredients.component2() instanceof ItemTag tag) {
                    uses.add(new Pair<>(tag.tagged.get(0), product.itemType));
                }
            } else if (matchLast || (lastTag != null && lastTag.tagged.contains(item))) {
                if (ingredients.component1() instanceof ItemType ingredient) {
                    uses.add(new Pair<>(ingredient, product.itemType));
                } else if (ingredients.component1() instanceof ItemTag tag) {
                    uses.add(new Pair<>(tag.tagged.get(0), product.itemType));
                }
            }
        });
        return uses;
    }
}
