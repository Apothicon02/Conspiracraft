package org.conspiracraft.game.interactions;

import kotlin.Pair;
import org.conspiracraft.game.blocks.Tag;
import org.conspiracraft.game.blocks.Tags;
import org.joml.Vector2i;
import org.joml.Vector3i;

public class Handcrafting {

    public static Vector3i interact(Vector3i hand, Vector2i target) {
        for (Tag tag : Tags.tags) {
            if (tag.tagged.contains(target.x)) {
                Pair<Vector2i, Integer> result = Recipes.recipes.get(new Pair<>(tag, hand.x));
                if (result != null) {
                    int amount = hand.z*result.getSecond();
                    if (amount < 999) {
                        return new Vector3i(result.getFirst().x, result.getFirst().y, amount);
                    }
                }
            }
        }
        return hand;
    }
}