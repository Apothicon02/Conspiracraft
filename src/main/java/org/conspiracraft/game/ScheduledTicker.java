package org.conspiracraft.game;

import org.conspiracraft.Main;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.world.World;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;

public class ScheduledTicker {
    public static HashMap<Long, Vector3i> schedule = new HashMap<>(Map.of());

    public static void tick() {
        schedule.forEach((scheduledTick, pos) -> {
            if (Main.currentTick > scheduledTick) {
                BlockTypes.blockTypeMap.get(World.getBlock(pos).x).tick(pos);
            }
        });
    }
}
