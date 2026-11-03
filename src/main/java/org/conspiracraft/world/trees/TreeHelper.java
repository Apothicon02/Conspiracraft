package org.conspiracraft.world.trees;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.world.Bounds;
import org.conspiracraft.world.LongPos;
import org.joml.Vector2i;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import static org.conspiracraft.world.World.*;

public class TreeHelper {
    public static void set(Long2IntOpenHashMap blocks) {
        ObjectIterator<Long2IntMap.Entry> iterator = blocks.long2IntEntrySet().fastIterator();
        while (iterator.hasNext()) {
            Long2IntMap.Entry entry = iterator.next();
            int block = entry.getIntValue();
            setBlockWorldgen(entry.getLongKey(), block >> 16, block & 0xFFFF);
        }
    }
    public static boolean collides(Long2IntOpenHashMap blocks, Bounds bounds, int minCollisionY, boolean ignore) {
        if (ignore) {return false;}
        LongIterator iterator = blocks.keySet().longIterator();
        while (iterator.hasNext()) {
            long pos = iterator.nextLong();
            if (bounds.out(pos) || (LongPos.y(pos) > minCollisionY && !isAirUnchecked(LongPos.x(pos), LongPos.y(pos), LongPos.z(pos)))) {
                return true;
            }
        }
        return false;
    }
//    public static boolean integrateCanopy(Bounds bounds, Long2IntOpenHashMap canopy, Long2IntOpenHashMap blocks, int minCollisionY) {
//        return integrateCanopy(bounds, canopy, blocks, minCollisionY, false);
//    }
//    public static boolean integrateCanopy(Bounds bounds, Long2IntOpenHashMap canopy, Long2IntOpenHashMap blocks, int minCollisionY, boolean ignoreWater) {
//        for (long pos : canopy.keySet()) {
//            if (bounds.out(pos)) {return false;}
//            if (!blocks.containsKey(pos)) {
//                if (isAirUnchecked(LongPos.x(pos), LongPos.y(pos), LongPos.z(pos))) {
//                    blocks.put(pos, canopy.get(pos));
//                } else if (LongPos.y(pos) > minCollisionY) {return false;}
//            }
//        }
//        return true;
//    }
    public static boolean oldIntegrateCanopy(Long2IntOpenHashMap canopy, Long2IntOpenHashMap blocks, int minCollisionY) {
        for (long pos : canopy.keySet()) {
            int existingBlock = getBlockWorldgen(LongPos.x(pos), LongPos.y(pos), LongPos.z(pos));
            if (!blocks.containsKey(pos)) {
                if (BlockTypes.blockTypes[existingBlock >> 16].blockProperties.isFluidReplaceable) {
                    blocks.put(pos, canopy.get(pos));
                }
                if (LongPos.y(pos) > minCollisionY && existingBlock >> 16 != 0) {
                    return false;
                }
            }
        }
        return true;
    }
}
