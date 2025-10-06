package org.conspiracraft.game.blocks;

import org.conspiracraft.game.blocks.types.BlockTypes;

import java.util.Map;

public class Fluids {
    public static Map<Integer, Integer> liquidGasMap = Map.of(
            BlockTypes.getId(BlockTypes.WATER), BlockTypes.getId(BlockTypes.STEAM),
            BlockTypes.getId(BlockTypes.STEAM), BlockTypes.getId(BlockTypes.WATER)
    );

    public static Map<Integer, Integer> fluidBucketMap = Map.of(
            BlockTypes.getId(BlockTypes.WATER), BlockTypes.getId(BlockTypes.WATER_BUCKET),
            BlockTypes.getId(BlockTypes.WATER_BUCKET), BlockTypes.getId(BlockTypes.WATER),
            BlockTypes.getId(BlockTypes.STEAM), BlockTypes.getId(BlockTypes.STEAM_BUCKET),
            BlockTypes.getId(BlockTypes.STEAM_BUCKET), BlockTypes.getId(BlockTypes.STEAM)
    );
}
