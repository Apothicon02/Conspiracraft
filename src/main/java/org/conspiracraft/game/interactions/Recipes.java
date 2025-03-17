package org.conspiracraft.game.interactions;

import kotlin.Pair;
import org.conspiracraft.game.blocks.Tag;
import org.conspiracraft.game.blocks.Tags;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.joml.Vector2i;

import java.util.HashMap;
import java.util.Map;

public class Recipes {
    public static Map<Pair<Tag, Integer>, Pair<Vector2i, Integer>> recipes = new HashMap<>(Map.of(
            new Pair<>(Tags.rocks, BlockTypes.getId(BlockTypes.WILLOW_LOG)), new Pair<>(new Vector2i(BlockTypes.getId(BlockTypes.WILLOW_PLANKS), 0), 1)
    ));
}
