package org.conspiracraft.game.world;

import org.conspiracraft.engine.ConspiracraftMath;
import org.conspiracraft.game.blocks.types.BlockType;
import org.conspiracraft.game.blocks.types.LightBlockProperties;
import org.conspiracraft.game.blocks.types.LightBlockType;
import org.conspiracraft.game.noise.Noises;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.world.cover.Mud;
import org.conspiracraft.game.world.cover.Plains;
import org.conspiracraft.game.world.shapes.Blob;
import org.conspiracraft.game.world.shapes.Pillar;
import org.conspiracraft.game.world.trees.*;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

import static org.conspiracraft.engine.Utils.*;
import static org.conspiracraft.engine.Utils.condenseLocalPos;
import static org.conspiracraft.game.world.World.*;

public class WorldGen {
    public static int maxWorldgenHeight = 256;
    public static boolean areChunksCompressed = false;

    public static Vector2i getBlockWorldgen(int x, int y, int z) {
        return chunks[condenseChunkPos(x >> 4, y >> 4, z >> 4)].getBlockUncompressed(condenseLocalPos(x & 15, y & 15, z & 15));
    }
    public static void setBlockWorldgen(int x, int y, int z, int blockTypeId, int blockSubtypeId) {
        chunks[condenseChunkPos(x >> 4, y >> 4, z >> 4)].setBlockUncompressed(new Vector3i(x & 15, y & 15, z & 15), blockTypeId, blockSubtypeId);
    }
    public static void setBlockWorldgenUpdates(int x, int y, int z, int blockTypeId, int blockSubtypeId) {
        setBlockWorldgen(x, y, z, blockTypeId, blockSubtypeId);
        Vector2i above = getBlockWorldgen(x, y+1, z);
        BlockType type = BlockTypes.blockTypeMap.get(above.x);
        type.onPlace(new Vector3i(x, y, z), above, true);
    }
    public static void setBlockWorldgenInBounds(int x, int y, int z, int blockTypeId, int blockSubtypeId) {
        if (World.inBounds(x, y, z)) {
            setBlockWorldgen(x, y, z, blockTypeId, blockSubtypeId);
        }
    }
    public static void setBlockWorldgenNoReplace(int x, int y, int z, int blockTypeId, int blockSubtypeId) {
        if (getBlockWorldgen(x, y, z).x <= 1) {
            setBlockWorldgen(x, y, z, blockTypeId, blockSubtypeId);
        }
    }
    public static void setBlockWorldgenNoReplaceSolids(int x, int y, int z, int blockTypeId, int blockSubtypeId) {
        if (!BlockTypes.blockTypeMap.get(getBlockWorldgen(x, y, z).x).blockProperties.isSolid) {
            setBlockWorldgen(x, y, z, blockTypeId, blockSubtypeId);
        }
    }
    public static Vector4i getLightWorldgen(int x, int y, int z) {
        return chunks[condenseChunkPos(x >> 4, y >> 4, z >> 4)].getLight(condenseLocalPos(x & 15, y & 15, z & 15));
    }
    public static void setLightWorldgen(int x, int y, int z, Vector4i light) {
        chunks[condenseChunkPos(x >> 4, y >> 4, z >> 4)].setLight(new Vector3i(x & 15, y & 15, z & 15), light, new Vector3i(x, y, z));
    }
    public static void generateHeightmap() {
//        long time = System.currentTimeMillis();
        int startX = currentChunk*chunkSize;
        for (int x = startX; x < startX+chunkSize; x++) {
            for (int z = 0; z < size; z++) {
                if ((x <= halfSize && z <= halfSize) || !quarterWorld) {
                    int lavaX = x*3;
                    int lavaZ = z*3;
                    float lavaNoise = Noises.NOODLE_NOISE.sample(lavaX, lavaZ);
                    float baseCellularNoise = Noises.CELLULAR_NOISE.sample(x, z);
                    float basePerlinNoise = Noises.COHERERENT_NOISE.sample(x, z);
                    float noodleNoise = Noises.NOODLE_NOISE.sample(x, z);
                    int miniX = x*8;
                    int miniZ = z*8;
                    float miniCellularNoise = Noises.CELLULAR_NOISE.sample(miniX, miniZ);
                    double northEastDist = (distance(x, z, size, size)/eighthSize)+(lavaNoise/2);
                    double desertness = northEastDist < 1 ? (Math.max(0.25, (1.5f-northEastDist)*0.25f)-0.3)/9 : 0;

                    double centDist = Math.min(1, (distance(x, z, halfSize, halfSize) / halfSize) * 1.15f);
                    double secondaryVolcanoDist = Math.min(1, (distance(x, z, halfSize*1.25f, halfSize*1.25f) / halfSize)*1.66f);
                    double valley = (noodleNoise > 0.67 ? 184 : (184 * Math.min(1, Math.abs(noodleNoise) + 0.33f)));
                    int volcanoElevation = (int) (Math.max(((Math.min(0.25, secondaryVolcanoDist)-0.25)*-1400)+((Math.min(-0.95, secondaryVolcanoDist-1)+0.95)*10000), ((Math.min(0.25, centDist)-0.25)*-2000)+((Math.min(-0.95, centDist-1)+0.95)*10000))*(1 - Math.abs((miniCellularNoise*miniCellularNoise)/4)));
                    double dunes = (Math.abs(desertness*lavaNoise)*3000);
                    if (dunes > 0) {
                        dunes -= Math.min(4, dunes);
                    }
                    int surface = (int) (dunes+(maxWorldgenHeight - Math.max(centDist * 184, (valley - (Math.max(0, noodleNoise * 320) * Math.abs(basePerlinNoise))))));
                    double oceanDist = Math.min(40, Math.abs(1-Math.min(1, (distance(x, z, eighthSize, eighthSize)/eighthSize)+(lavaNoise/10)))*eighthSize);
                    for (int y = Math.max(surface, volcanoElevation); y >= 0; y--) {
                        double baseGradient = ConspiracraftMath.gradient(y, (int)(surface-oceanDist), (int)(48-oceanDist), 2, -1);
                        double baseDensity = baseCellularNoise + baseGradient;
                        double islands = oceanDist > 1 ? Math.max(0, basePerlinNoise + ConspiracraftMath.gradient(y, seaLevel+24, 20, 2, -1)) : 0;
                        baseDensity += islands;
                        if (baseDensity > 0 || volcanoElevation >= y) {
                            heightmap[condensePos(x, z)] = (short) y;
                            break;
                        }
                    }
                }
            }
        }
//        System.out.print("Took "+(System.currentTimeMillis()-time)+"ms to generate slice of terrain. \n");
//        String progress = String.valueOf(((float) currentChunk / sizeChunks)*100).substring(0, 3);
//        System.out.print("World is " + (progress + "% generated. \n"));
    }

    public static int getSurfaceBlock(int x, int y, int z, double desertDist) {
        int snowLevel = (int) (((distance(x, z, halfSize, halfSize)/halfSize)*100)+72);
        return desertDist > 1 ? (snowLevel <= y ? 54 : 2) : 23;
    }
    public static int getCrustBlock(int x, int y, int z, double desertDist) {
        return desertDist > 1 ?  3 : 23;
    }
    public static int getRockBlock(int x, int y, int z, double desertDist, int volcanoElevation) {
        return volcanoElevation >= y ? 19 : (desertDist > 1 ?  10 : 24);
    }

    public static void generateSurface() {
//        long time = System.currentTimeMillis();
        int startX = currentChunk*chunkSize;
        for (int x = startX; x < startX+chunkSize; x++) {
            int prevChunkZ = 0;
            for (int z = 0; z < size; z++) {
                int condensedPos = condensePos(x, z);
                if ((x <= halfSize && z <= halfSize) || !quarterWorld) {
                    int lavaX = x*3;
                    int lavaZ = z*3;
                    float lavaNoise = Noises.NOODLE_NOISE.sample(lavaX, lavaZ);
                    int y = surfaceHeightmap[condensedPos];
                    double centDist = Math.min(1, (distance(x, z, halfSize, halfSize) / halfSize) * 1.15f);
                    double secondaryVolcanoDist = Math.min(1, (distance(x, z, halfSize*1.25f, halfSize*1.25f) / halfSize)*1.66f);
                    int volcanoElevation = (int) Math.max((Math.min(0.25, centDist)-0.25)*-2000, (Math.min(0.25, secondaryVolcanoDist)-0.25)*-1400);
                    double desertDist = (distance(x, z, size, size)/eighthSize)+(lavaNoise/2);
                    int lavaLevel = (int) (293-(lavaNoise*5));
                    if (centDist < 0.05f && y < lavaLevel) {
                        heightmap[condensedPos] = (short) lavaLevel;
                        for (int newY = lavaLevel; newY >= 0; newY--) {
                            setBlockWorldgen(x, newY, z, 19, 0);
                        }
                    } else if (secondaryVolcanoDist < 0.05f && y < lavaLevel-100) {
                        heightmap[condensedPos] = (short) (lavaLevel-100);
                        for (int newY = lavaLevel-100; newY >= seaLevel; newY--) {
                            setBlockWorldgen(x, newY, z, 19, 0);
                        }
                    } else if (y < seaLevel) {
                        for (int newY = seaLevel; newY > y; newY--) {
                            setBlockWorldgen(x, newY, z, 1, 15);
                            if (newY == seaLevel) {
                                heightmap[condensedPos] = (short) y;
                            }
                        }
                        int surface = 57;
                        int subSurface = 10;
                        if (lavaNoise > -0.15f && lavaNoise < 0.15f) {
                            surface = 58;
                        } else if (lavaNoise > 0.7f) {
                            surface = 55;
                        } else if (lavaNoise < -0.7f) {
                            surface = 23;
                            subSurface = 24;
                        }
                        for (int newY = y; newY > y-3; newY--) {
                            setBlockWorldgen(x, newY, z, surface, 0);
                        }
                        for (int newY = y-3; newY >= seaLevel; newY--) {
                            setBlockWorldgen(x, newY, z, subSurface, 0);
                        }
                    } else {
                        int maxSteepness = 0;
                        int minNeighborY = height - 1;
                        for (int pos : new int[]{condensePos(Math.min(size - 1, x + 3), z), condensePos(Math.max(0, x - 3), z), condensePos(x, Math.min(size - 1, z + 3)), condensePos(x, Math.max(0, z - 3)),
                                condensePos(Math.max(0, x - 3), Math.max(0, z - 3)), condensePos(Math.min(size - 1, x + 3), Math.max(0, z - 3)), condensePos(Math.max(0, x - 3), Math.min(size - 1, z + 3)), condensePos(Math.min(size - 1, x + 3), Math.min(size - 1, z + 3))}) {
                            int nY = surfaceHeightmap[pos];
                            minNeighborY = Math.min(minNeighborY, nY);
                            int steepness = Math.abs(y - nY);
                            maxSteepness = Math.max(maxSteepness, steepness);
                        }
                        boolean flat = maxSteepness < 3;
                        double oceanDist = (distance(x, z, eighthSize, eighthSize)/eighthSize)+(lavaNoise/8);
                        if (y >= seaLevel && y < seaLevel+6 && (oceanDist < 0.95f || (flat && oceanDist < 1f))) {
                            for (int newY = y; newY > y-5; newY--) {
                                setBlockWorldgen(x, newY, z, 23, 0);
                            }
                            for (int newY = y-6; newY >= seaLevel; newY--) {
                                setBlockWorldgen(x, newY, z, 24, 0);
                            }
                        } else if (flat) {
                            int surface = getSurfaceBlock(x, y, z, desertDist);
                            int crust = getCrustBlock(x, y, z, desertDist);
                            int rock = getRockBlock(x, y, z, desertDist, volcanoElevation);
                            setBlockWorldgen(x, y, z, y <= volcanoElevation ? crust : surface, 0);
                            setBlockWorldgen(x, y - 1, z, crust, 0);
                            setBlockWorldgen(x, y - 2, z, crust, 0);
                            setBlockWorldgen(x, y - 3, z, crust, 0);
                            for (int newY = y - 4; newY >= seaLevel; newY--) {
                                setBlockWorldgen(x, newY, z, newY <= volcanoElevation ? 9 : rock, 0);
                            }
                        } else {
                            int lavaAir = (int)(Math.abs(lavaNoise)*200);
                            int lowestY = y;
                            for (int newY = y; newY >= seaLevel; newY--) {
                                if (newY <= volcanoElevation) {
                                    int blockTypeId = lavaNoise > -0.1 && lavaNoise < 0.1 ? (newY >= y - lavaAir ? 0 : (lavaAir > 3 ? 19 : 9)) : 9;
                                    setBlockWorldgen(x, newY, z, blockTypeId, 0);
                                    if (blockTypeId != 0 && lowestY == y) {
                                        lowestY = newY;
                                    }
                                } else {
                                    setBlockWorldgen(x, newY, z, 10, 0);
                                }
                            }
                            heightmap[condensedPos] = (short)(lowestY);
                        }
                    }
                }
            }
        }
//        System.out.print("Took "+(System.currentTimeMillis()-time)+"ms to generate slice of surface. \n");
//        String progress = String.valueOf(((float) currentChunk / sizeChunks)*100).substring(0, 3);
//        System.out.print("World is " + (progress + "% generated. \n"));
    }

    public static void generateFeatures() {
//        long time = System.currentTimeMillis();
        int startX = currentChunk*chunkSize;
        for (int x = startX; x < startX+chunkSize; x++) {
            for (int z = 0; z < size; z++) {
                if ((x <= halfSize && z <= halfSize) || !quarterWorld) {
                    int condensedPos = condensePos(x, z);
                    int y = surfaceHeightmap[condensedPos];
                    float basePerlinNoise = Noises.COHERERENT_NOISE.sample(x, z);
                    int cloudScale = (int)(4+(basePerlinNoise*4));
                    if (Noises.CLOUD_NOISE.sample(x*cloudScale, z*cloudScale) > 0.1f && Math.random() > 0.95f && heightmap[condensedPos] < 166) {
                        boolean isRainCloud = (distance(x, z, size, 0) / quarterSize < 1 && (Math.random() < 0.0005f));
                        Blob.generate(new Vector2i(0), x, 216+(int)Math.abs(Noises.CELLULAR_NOISE.sample(x, z)*32), z, isRainCloud ? 32 : 31, 0, (isRainCloud ? 10 : 0) +(int)(2+(Math.random()*8)));
                        if (isRainCloud) {
                            Mud.generate(new Vector2i(0), x, y, z, 33, 0, 16 + (int) (2 + (Math.random() * 8)), true);
                        }
                    }
                    if (y >= seaLevel) {
                        Vector2i blockOn = getBlockWorldgen(x, y, z);
                        float foliageNoise = (basePerlinNoise + 0.5f);
                        float exponentialFoliageNoise = foliageNoise * foliageNoise;
                        double northEastDist = distance(x, z, size, size) / size;
                        double southDist = (double) z / halfSize;
                        double southWestDist = distance(x, z, 0, 0) / size;
                        double oceanDist = distance(x, z, eighthSize, eighthSize) / eighthSize;
                        double centDist = distance(x, z, size/2, size/2) / size;
                        double taiganess = Math.min(0.03f, Math.max(0.7, (1.5f-centDist)*0.7f)-0.8)/9;
                        double oceanness = oceanDist < 1.1 ? (Math.max(0.25, (1.5f-southWestDist)*0.25)-0.3)/9 : 0;
                        double jungleness = oceanDist < 1 ? (Math.max(0.25, (1.5f-southWestDist)*0.25f)-0.3)/9 : 0;
                        double desertness = northEastDist < 1 ? (Math.max(0.25, (1.5f-northEastDist)*0.25f)-0.3)/9 : 0;
                        double forestness = southDist < 1 ? (Math.max(0.5, (1.5f-(southDist-oceanness))*0.5)-0.6)/9 : 0;
                        double plainness = 0.01d-Math.max(0, Math.max(forestness, taiganess));
                        double randomNumber = Math.random();
                        boolean setAnything = false;
                        if (randomNumber > 0.999d) {
                            if (randomNumber > 0.99997d) {
                                setBlockWorldgen(x, y, z, 7, 0);
                                setBlockWorldgen(x, y + 1, z, 7, 0);
                                setBlockWorldgen(x, y + 2, z, 7, 0);
                            } else if (blockOn.x == 2) {
                                if (randomNumber > 0.9992d) {
                                    if (jungleness > 0) {
                                        int type = 52 + (Math.random() > 0.5f ? 1 : 0);
                                        setBlockWorldgen(x, y + 1, z, type, 2);
                                        setBlockWorldgen(x, y + 2, z, type, 1);
                                        setBlockWorldgen(x, y + 3, z, type, 0);
                                    }
                                } else {
                                    setBlockWorldgen(x, y + 1, z, 14, 0);
                                }
                            }
                            setAnything = true;
                        } else if (blockOn.x == 2 || blockOn.x == 3 || blockOn.x == 23 || blockOn.x == 54) {
                            double jungDes = Math.max(desertness, oceanness);
                            if (randomNumber/10 < jungDes) { //jungle & desert
                                if (blockOn.x == 23 && randomNumber < jungDes*Math.max(0.8f, exponentialFoliageNoise)) {
                                    if (desertness <= jungleness || (desertness > jungleness && y <= seaLevel + 1)) {
                                        PalmTree.generate(blockOn, x, y, z, (int) (Math.random() * 14) + 8, 25, 0, 27, 0);
                                    } else {
                                        double deadBushChance = Math.random();
                                         if (deadBushChance < 0.03) {
                                             int variant = deadBushChance < 0.015 ? 0 : 1;
                                             Blob.generate(blockOn, x, y - 3 + variant, z + variant, 24, 0, 3 + variant);
                                             Blob.generate(blockOn, x, y, z + variant, 24, 0, 2 + variant);
                                             Blob.generate(blockOn, x, y + 2 + variant, z + variant, 24, 0, 1);
                                             Blob.generate(blockOn, x, y + 4 + variant, z + 1 + variant, 24, 0, 1);
                                             variant *= 2;
                                             Blob.generate(blockOn, x, y + 7 + variant, z + 1 + variant, 24, 0, 2 + variant);
                                             Blob.generate(blockOn, x - 1 - variant, y + 7 + variant, z + variant, 24, 0, 2 + variant);
                                             Blob.generate(blockOn, x + 1 + variant, y + 7 + variant, z + variant, 24, 0, 2 + variant);
                                        } else if (deadBushChance < 0.2) {
                                             if (deadBushChance > 0.19) {
                                                 int maxHeight = (int) (Math.random() * 6) + 12;
                                                 DeadOakTree.generate(blockOn, x, y, z, maxHeight, 16, 0);
                                             } else {
                                                 setBlockWorldgenNoReplace(x, y + 1, z, 30, deadBushChance < 0.1 ? 0 : 1);
                                             }
                                        } else {
                                            Pillar.generate(blockOn, x, y + 1, z, (int) (Math.random() * 6) + 2, 29, 0);
                                        }
                                    }
                                } else if (jungleness > desertness) {
                                    if (!JungleTree.generate(blockOn, x, y, z, (int) (Math.random() * 8) + 28, (int) (3 + (randomNumber + 0.5f)), 20, 0, 21, 0, randomNumber < 0.25f)) {
                                        // attempt to place an oak tree if jungle tree fails.
                                        int maxHeight = (int) (Math.random() * 6) + 12;
                                        int leavesHeight = (int) (Math.random()*3) + 3;
                                        int radius = (int) (Math.random()*4) + 6;
                                        if (!OakTree.generate(blockOn, x, y, z, maxHeight, radius, leavesHeight, 16, 0, 17, 0)) {
                                            //attempts to place a willow shrub if oak tree fails.
                                            PalmShrub.generate(blockOn, x, y, z, (int) (Math.random() * 3) + 1, 25, 0, 27, 0);
                                        }
                                    }
                                }
                                setAnything = true;
                            } else if (randomNumber < jungleness*0.2) {
                                if (randomNumber > jungleness*0.05) {
                                    int maxHeight = (int) (Math.random() + 1);
                                    OakShrub.generate(blockOn, x, y, z, maxHeight, 3 + (maxHeight * 2), 16, 0, 17, 0);
                                    setAnything = true;
                                } else {
                                    int maxHeight = (int) (Math.random() * 6) + 12;
                                    int leavesHeight = (int) (Math.random()*3) + 3;
                                    int radius = (int) (Math.random()*4) + 6;
                                    OakTree.generate(blockOn, x, y, z, maxHeight, radius, leavesHeight, 16, 0, 17, 0);
                                    setAnything = true;
                                }
                            } else if (randomNumber < taiganess*(exponentialFoliageNoise+0.4)) { //taiga
                                int maxHeight = (int) (Math.random() * 19) + 5;
                                PineTree.generate(blockOn, x, y, z, maxHeight, 35, 0, 36, 0);
                                setAnything = true;
                            } else if (randomNumber < forestness*(exponentialFoliageNoise-0.2)) { //forest
                                int maxHeight = (int) (Math.random() * 6) + 12;
                                int leavesHeight = (int) (Math.random()*3) + 3;
                                int radius = (int) (Math.random()*4) + 6;
                                if (Math.random() < 0.015f) { //1.5% chance the tree is dead
                                    DeadOakTree.generate(blockOn, x, y, z, maxHeight, 47, 0);
                                } else {
                                    OakTree.generate(blockOn, x, y, z, maxHeight, radius, leavesHeight, 16, 0, 17, 0);
                                }
                                setAnything = true;
                            } else if (randomNumber < forestness/2) {
                                int maxHeight = (int) (Math.random() + 1);
                                OakShrub.generate(blockOn, x, y, z, maxHeight, 3 + (maxHeight * 2), 16, 0, 17, 0);
                                setAnything = true;
                            } else if (randomNumber*20 < plainness) { //plains
                                int maxHeight = (int) (Math.random() + 1);
                                OakShrub.generate(blockOn, x, y, z, maxHeight, 3 + (maxHeight * 2), 16, 0, 17, 0);
                                setAnything = true;
                            } else if (randomNumber*18 < plainness) {
                                int maxHeight = (int) (Math.random() * 6) + 12;
                                int leavesHeight = (int) (Math.random()*3) + 3;
                                int radius = (int) (Math.random()*4) + 6;
                                OakTree.generate(blockOn, x, y, z, maxHeight, radius, leavesHeight, 16, 0, 17, 0);
                                setAnything = true;
                            }
                        }
                        if (!setAnything) {
                            if (getBlockWorldgen(x, y + 1, z).x <= 1) { //only replace air and water
                                Plains.generate(blockOn, x, y, z);
                                if (randomNumber < 0.08f && blockOn.x == 10) {
                                    Blob.generate(blockOn, x, y, z, 8, 0, (int) (2 + ((Math.random() + 1) * 3)));
                                }
                            }
                        }
                    }
                }
            }
        }
//        System.out.print("Took "+(System.currentTimeMillis()-time)+"ms to generate slice of features. \n");
//        String progress = String.valueOf(((float) currentChunk / sizeChunks)*100).substring(0, 3);
//        System.out.print("World is " + (progress + "% generated. \n"));
    }

    public static void fillLight() {
        int startX = currentChunk*chunkSize;
        for (int x = Math.max(1, startX); x < Math.min(size - 1, startX+chunkSize); x++) {
//            long time = System.currentTimeMillis();
            for (int z = 1; z < size - 1; z++) {
                if ((x <= halfSize && z <= halfSize) || !quarterWorld) {
                    for (int y = heightmap[condensePos(x, z)]; y >= surfaceHeightmap[condensePos(x, z)]; y--) {
                        Vector2i block = getBlockWorldgen(x, y, z);
                        Vector4i light = getLightWorldgen(x, y, z);
                        BlockType blockType = BlockTypes.blockTypeMap.get(block.x);
                        if (blockType instanceof LightBlockType) {
                            LightHelper.updateLight(new Vector3i(x, y, z), block, light);
                        } else {
                            if (light.w() < 20 && !blockType.blockProperties.blocksLight) {
                                //check if any neighbors are a higher brightness
                                if (getLightWorldgen(x, y, z + 1).w() > light.w() || getLightWorldgen(x + 1, y, z).w() > light.w() || getLightWorldgen(x, y, z - 1).w() > light.w() ||
                                        getLightWorldgen(x - 1, y, z).w() > light.w() || getLightWorldgen(x, y + 1, z).w() > light.w() || getLightWorldgen(x, y - 1, z).w() > light.w()) {
                                    LightHelper.updateLight(new Vector3i(x, y, z), block, light);
                                }
                            }
                        }
                    }
                }
            }
//            String progress = String.valueOf(((float) x / size)*100).substring(0, 3);
//            System.out.print("Sunlight is " + (progress + "% filled. \nSlice took " + (System.currentTimeMillis()-time) + "ms to fill. \n"));
        }
    }
}