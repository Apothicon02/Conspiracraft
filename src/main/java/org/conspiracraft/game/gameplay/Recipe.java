package org.conspiracraft.game.gameplay;

import org.joml.Vector2i;

public class Recipe {
    public Ingredient ingredient;
    public Ingredient handIngredient;
    public Vector2i product;
    public Recipe(Ingredient ingredient, Ingredient handIngredient, Vector2i product) {
        this.ingredient = ingredient;
        this.handIngredient = handIngredient;
        this.product = product;
    }
    public Vector2i craftRecipe(Vector2i block, Vector2i hand) {
        if (ingredient.matchesIngredient(block) && handIngredient.matchesIngredient(hand)) {
            return product;
        }
        return null;
    }
}
