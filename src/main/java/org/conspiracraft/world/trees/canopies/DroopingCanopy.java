package org.conspiracraft.world.trees.canopies;

import org.conspiracraft.blocks.BlockTags;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.world.*;
import org.joml.SimplexNoise;
import org.joml.Vector2i;
import org.joml.Vector3i;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Random;

import static org.conspiracraft.world.World.*;

public class DroopingCanopy extends Canopy {
    private static int getLeaves(Random random, long pos, int blockType, int blockSubType) {return Chunk.packInts(blockType, blockSubType);}

    private static void addToMap(Long2IntOpenHashMap map, long pos, Random random, int blockType, int blockSubtype) {
        map.put(pos, getLeaves(random, pos, blockType, blockSubtype));
    }

    private static void addToMap(Long2IntOpenHashMap map, Vector3i pos, Random random, int minY, int blockType, int blockSubtype) {
        if (pos.y() < minY) {
            pos.add(Directions.NORTH).add(Directions.EAST);
            long packedPos = LongPos.pack(pos);
            map.put(packedPos, getLeaves(random, packedPos, blockType, blockSubtype));
        } else {
            long packedPos = LongPos.pack(pos);
            map.put(packedPos, getLeaves(random, packedPos, blockType, blockSubtype));
        }
    }
    
    public static Long2IntOpenHashMap generateCanopy(Random random, Long2IntOpenHashMap blocks, int ogX, int ogY, int ogZ, int blockType, int blockSubType, int trunkHeight, Vector3i treeOrigin, int height, int maxRadius, float droopiness) {
        Vector3i origin = new Vector3i(ogX, ogY, ogZ);

        for (int x = origin.x()-maxRadius; x <= origin.x()+maxRadius; x++) {
            for (int z = origin.z()-maxRadius; z <= origin.z()+maxRadius; z++) {
                int minY = origin.y();
                int maxY = (origin.y()+height);
                boolean set = false;
                for (int y = minY; y <= maxY; y++) {
                    int radius = maxRadius;
                    if ((y == minY && height > 2) || (y == maxY-1 && height >= 4)) {
                        radius--;
                    } else if (y == maxY && height >= 5) {
                        radius-=2;
                    }
                    radius*=radius;
                    int xDist = x-origin.x();
                    int zDist = z-origin.z();
                    int dist = xDist*xDist+zDist*zDist;
                    if (dist <= radius) {
                        int droop = 0;
                        if (dist >= radius-2 && y < minY+(height/2)) {
                            droop += (int) ((1+SimplexNoise.noise(x/20.f, z/20.f))*4);
                        }
                        int xOgDist = x-treeOrigin.x();
                        int zOgDist = z-treeOrigin.z();
                        int originDist = xOgDist*xOgDist+zOgDist*zOgDist;
                        BlockPos pos = new BlockPos(x, (int)(y-(originDist/35.f)-((dist/22.f)*droopiness)), z);
                        for (int i = 0; i <= droop; i++) {
                            if (droop == 2) {
                                addSquare(blocks, pos.below(i), random, 1, true, minY, blockType, blockSubType);
                            } else {
                                addToMap(blocks, pos.below(i), random, minY, blockType, blockSubType);
                            }
                        }
                        set = true;
                    }
                }
                if (set) {
                    if (BlockTags.leaves.tagged.contains(blockType)) {
                        long bPos = LongPos.pack(x, minY-height, z);
                        long aPos = LongPos.pack(x, (1+minY)-height, z);
                        if ((isAir(aPos) && !blocks.containsKey(aPos) &&
                                (BlockTypes.blockTypes[getBlockWorldgen(bPos)>> 16].blockProperties.isSolid || solid(blocks.get(bPos))))) {
                            addToMap(blocks, aPos, random, blockType, (int) Math.abs(random.nextDouble() * 6) + 1);
                        }
                    }
                }
            }
        }

        return blocks;
    }

    private static boolean solid(int block) {
        if (block == 0) {return false;}
        return BlockTypes.blockTypes[block >> 16].blockProperties.isSolid;
    }

    private static void addSquare(Long2IntOpenHashMap map, Vector3i pos, Random random, int radius, boolean corners, int minY, int blockType, int blockSubType) {
        int minX = pos.x()-radius;
        int maxX = pos.x()+radius;
        int minZ = pos.z()-radius;
        int maxZ = pos.z()+radius;
        if (radius == 0) {
            maxX+=1;
            maxZ+=1;
        }
        for (int x = pos.x()-radius; x <= maxX; x++) {
            for (int z = pos.z()-radius; z <= maxZ; z++) {
                if (!((x == minX || x == maxX) && (z == minZ || z == maxZ)) || corners) {
                    addToMap(map, new Vector3i(x, pos.y(), z), random, minY, blockType, blockSubType);
                }
            }
        }
    }
}