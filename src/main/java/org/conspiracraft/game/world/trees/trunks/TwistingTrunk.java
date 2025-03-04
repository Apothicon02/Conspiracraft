package org.conspiracraft.game.world.trees.trunks;

import org.joml.Vector3i;

import java.util.HashSet;
import java.util.Set;

import static org.conspiracraft.game.world.WorldGen.setBlockWorldgenInBounds;

public class TwistingTrunk extends Trunk {
    public static Set<Vector3i> generateTrunk(int oX, int oY, int oZ, int trunkHeight, int blockType, int blockSubType, boolean overgrown, int minBranchHeight) {
        Set<Vector3i> canopies = new HashSet<>();

        int minBranch = minBranchHeight+oY;
        int prevXPositive = 0;
        int prevZPositive = 0;
        int twistable = 0;
        int maxHeight = trunkHeight+oY;
        Vector3i pos = new Vector3i(oX, oY, oZ);
        for (int height = oY-1; height <= maxHeight; height++) {
            twistable--;
            boolean branch = false;
            Vector3i dir = new Vector3i(0, 0, 0);
            if (height > oY+2 && twistable <= 0 && Math.random()*10 < 4) {
                int xOff = (int) ((Math.random()*20)-10);
                int zOff = (int) ((Math.random()*20)-10);
                boolean xPositive = xOff >= prevXPositive;
                boolean zPositive = zOff >= prevZPositive;
                if (xPositive) {
                    prevXPositive = 5;
                    dir.x += 1;
                } else {
                    prevXPositive = -5;
                    dir.x -= 1;
                }
                if (zPositive) {
                    prevZPositive = 5;
                    dir.z += 1;
                } else {
                    prevZPositive = -5;
                    dir.z -= 1;
                }
                branch = true;
                twistable = 2;
            }
            pos.add(dir);
            pos.y = height;
            makeSquare(new Vector3i(pos.x, pos.y-1, pos.z), blockType, blockSubType);
            makeSquare(pos, blockType, blockSubType);
            if (branch && pos.y >= minBranch) {
                canopies.add(makeBranch(pos, dir, blockType, blockSubType));
                if (overgrown) {
                    canopies.add(makeBranch(pos, new Vector3i(dir.x * (Math.random() >= 0.5f ? 1 : 0), +2, dir.z * (Math.random() >= 0.5f ? 1 : 0)), blockType, blockSubType));
                }
            }
            if (pos.y == maxHeight) {
                canopies.add(new Vector3i(pos.x, pos.y+1, pos.z));
                if (overgrown) {
                    canopies.add(new Vector3i(pos.x, pos.y-1, pos.z+3));
                    canopies.add(new Vector3i(pos.x+3, pos.y, pos.z));
                    canopies.add(new Vector3i(pos.x, pos.y-1, pos.z-3));
                    canopies.add(new Vector3i(pos.x-3, pos.y, pos.z));
                }
            }
        }

        return canopies;
    }

    private static void makeSquare(Vector3i pos, int blockType, int blockSubType) {
        setBlockWorldgenInBounds(pos.x, pos.y, pos.z, blockType, blockSubType);
        setBlockWorldgenInBounds(pos.x, pos.y, pos.z+1, blockType, blockSubType);
        setBlockWorldgenInBounds(pos.x+1, pos.y, pos.z, blockType, blockSubType);
        setBlockWorldgenInBounds(pos.x+1, pos.y, pos.z+1, blockType, blockSubType);
    }

    private static Vector3i makeBranch(Vector3i pos, Vector3i dir, int blockType, int blockSubType) {
        makeSquare(new Vector3i(pos.x+dir.x, pos.y, pos.z+dir.z), blockType, blockSubType);
        makeSquare(new Vector3i(pos.x+(dir.x*2), pos.y-1, pos.z+(dir.z*2)), blockType, blockSubType);
        makeSquare(new Vector3i(pos.x+(dir.x*3), pos.y-1, pos.z+(dir.z*3)), blockType, blockSubType);
        setBlockWorldgenInBounds(pos.x+(dir.x*4), pos.y, pos.z+(dir.z*4), blockType, blockSubType);
        setBlockWorldgenInBounds(pos.x+(dir.x*5), pos.y, pos.z+(dir.z*5), blockType, blockSubType);
        return new Vector3i(pos.x+(dir.x*5), pos.y+1, pos.z+(dir.z*5));
    }
}