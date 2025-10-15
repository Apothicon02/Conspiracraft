package org.conspiracraft.game.blocks;

import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.gameplay.Ingredient;
import org.conspiracraft.game.gameplay.Recipe;
import org.joml.Vector2i;

import static org.conspiracraft.Main.player;

public class BlockBreaking {
    public static Recipe[] recipes = new Recipe[]{
            new Recipe(new Ingredient(Tags.leaves), new Ingredient(0), new Vector2i(BlockTypes.getId(BlockTypes.STICK), 0)),
            new Recipe(new Ingredient(BlockTypes.getId(BlockTypes.FLINT)), new Ingredient(BlockTypes.getId(BlockTypes.STICK)), new Vector2i(BlockTypes.getId(BlockTypes.FLINT_HAMMER), 0)),
            new Recipe(new Ingredient(BlockTypes.getId(BlockTypes.OBSIDIAN)), new Ingredient(BlockTypes.getId(BlockTypes.STICK)), new Vector2i(BlockTypes.getId(BlockTypes.OBSIDIAN_MACE), 0)),
    };

    public static void blockBroken(Vector2i blockBreaking, Vector2i handBlock) {
        if (!player.creative) {
            for (Recipe recipe : recipes) {
                Vector2i product = recipe.craftRecipe(blockBreaking, handBlock);
                if (product != null) {
                    player.stack[0] = product.x;
                    player.stack[1] = product.y;
                    break;
                }
            }
        }
    }
}
