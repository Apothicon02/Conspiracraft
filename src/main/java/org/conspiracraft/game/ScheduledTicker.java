package org.conspiracraft.game;

import org.conspiracraft.Main;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.world.World;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ScheduledTicker {
    public static HashMap<Long, ArrayList<Vector3i>> schedule = new HashMap<>(Map.of());
    public static long lastTick = 0;

    public static void scheduleTick(long tickTime, Vector3i pos) {
        schedule.putIfAbsent(tickTime, new ArrayList<>());
        schedule.get(tickTime).add(pos);
    }

    public static void tick() {
        for (long scheduledTick = lastTick+1; scheduledTick <= Main.currentTick; scheduledTick++) {
            ArrayList<Vector3i> positions = schedule.get(scheduledTick);
            if (positions != null) {
                positions.forEach((pos) -> {
                    BlockTypes.blockTypeMap.get(World.getBlock(pos).x).tick(pos);
                });
            }
            schedule.remove(scheduledTick);
        }
    }
}
