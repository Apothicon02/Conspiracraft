package org.conspiracraft.game;

import org.conspiracraft.Main;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.world.World;
import org.joml.Vector3i;
import org.joml.Vector4i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ScheduledTicker {
    public static HashMap<Long, ArrayList<Vector4i>> schedule = new HashMap<>(Map.of());
    public static long lastTick = 0;

    public static void scheduleTick(long tickTime, Vector3i pos, int tickType) {
        while (schedule.containsKey(tickTime)) {
            if (schedule.get(tickTime).size() > 50000) {
                tickTime++;
            } else {
                break;
            }
        }
        schedule.putIfAbsent(tickTime, new ArrayList<>());
        Vector4i value = new Vector4i(pos.x, pos.y, pos.z, tickType);
        if (!schedule.get(tickTime).contains(value)) {
            schedule.get(tickTime).add(value);
        }
    }

    public static void tick() {
        for (long scheduledTick = lastTick+1; scheduledTick <= Main.currentTick; scheduledTick++) {
            ArrayList<Vector4i> positions = schedule.get(scheduledTick);
            if (positions != null) {
                positions.forEach((pos) -> {
                    BlockTypes.blockTypeMap.get(World.getBlock(pos.xyz(new Vector3i())).x).tick(pos);
                });
            }
            schedule.remove(scheduledTick);
        }
        lastTick = Main.currentTick;
    }
}
