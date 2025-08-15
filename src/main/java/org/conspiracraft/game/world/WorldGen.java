package org.conspiracraft.game.world;

import org.conspiracraft.engine.ConspiracraftMath;
import org.conspiracraft.engine.Utils;
import org.conspiracraft.game.blocks.types.BlockType;
import org.conspiracraft.game.blocks.types.LightBlockType;
import org.conspiracraft.game.noise.Noises;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.world.cover.Plains;
import org.conspiracraft.game.world.shapes.Blob;
import org.conspiracraft.game.world.trees.JungleTree;
import org.conspiracraft.game.world.trees.OakTree;
import org.conspiracraft.game.world.trees.PalmTree;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

import static org.conspiracraft.engine.Utils.*;
import static org.conspiracraft.engine.Utils.condenseLocalPos;
import static org.conspiracraft.game.world.World.*;

public class WorldGen {
    public static int maxWorldgenHeight = 256;

    public static Vector2i getBlockWorldgen(int x, int y, int z) {
        return chunks[condenseChunkPos(x >> 4, y >> 4, z >> 4)].getBlock(condenseLocalPos(x & 15, y & 15, z & 15));
    }
    public static void setBlockWorldgen(int x, int y, int z, int blockTypeId, int blockSubtypeId) {
        chunks[condenseChunkPos(x >> 4, y >> 4, z >> 4)].setBlock(new Vector3i(x & 15, y & 15, z & 15), blockTypeId, blockSubtypeId, new Vector3i(x, y, z));
    }
    public static void setBlockWorldgenInBounds(int x, int y, int z, int blockTypeId, int blockSubtypeId) {
        if (World.inBounds(x, y, z)) {
            chunks[condenseChunkPos(x >> 4, y >> 4, z >> 4)].setBlock(new Vector3i(x & 15, y & 15, z & 15), blockTypeId, blockSubtypeId, new Vector3i(x, y, z));
        }
    }
    public static void setBlockWorldgenNoReplace(int x, int y, int z, int blockTypeId, int blockSubtypeId) {
        if (getBlockWorldgen(x, y, z).x <= 1) {
            chunks[condenseChunkPos(x >> 4, y >> 4, z >> 4)].setBlock(new Vector3i(x & 15, y & 15, z & 15), blockTypeId, blockSubtypeId, new Vector3i(x, y, z));
        }
    }
    public static void setBlockWorldgenNoReplaceSolids(int x, int y, int z, int blockTypeId, int blockSubtypeId) {
        if (!BlockTypes.blockTypeMap.get(getBlockWorldgen(x, y, z).x).blockProperties.isSolid) {
            chunks[condenseChunkPos(x >> 4, y >> 4, z >> 4)].setBlock(new Vector3i(x & 15, y & 15, z & 15), blockTypeId, blockSubtypeId, new Vector3i(x, y, z));
        }
    }
    public static Vector4i getLightWorldgen(int x, int y, int z) {
        return chunks[condenseChunkPos(x >> 4, y >> 4, z >> 4)].getLight(condenseLocalPos(x & 15, y & 15, z & 15));
    }
    public static void setLightWorldgen(int x, int y, int z, Vector4i light) {
        chunks[condenseChunkPos(x >> 4, y >> 4, z >> 4)].setLight(new Vector3i(x & 15, y & 15, z & 15), light, new Vector3i(x, y, z));
    }
    public static void blackenLightPaletteWorldgen(int x, int y, int z) {
        chunks[condenseChunkPos(x, y, z)].lightPalette.set(0, 0);
    }
    public static void generateHeightmap() {
//        long time = System.currentTimeMillis();
        int startX = currentChunk*chunkSize;
        for (int x = startX; x < startX+chunkSize; x++) {
            for (int z = 0; z < size; z++) {
                if ((x <= halfSize && z <= halfSize) || !quarterWorld) {
                    float baseCellularNoise = Noises.CELLULAR_NOISE.sample(x, z);
                    float basePerlinNoise = Noises.COHERERENT_NOISE.sample(x, z);
                    float noodleNoise = Noises.NOODLE_NOISE.sample(x, z);
                    int miniX = x*8;
                    int miniZ = z*8;
                    float miniCellularNoise = Noises.CELLULAR_NOISE.sample(miniX, miniZ);

                    double centDist = Math.min(1, (distance(x, z, halfSize, halfSize) / halfSize) * 1.15f);
                    double secondaryVolcanoDist = Math.min(1, (distance(x, z, halfSize*1.25f, halfSize*1.25f) / halfSize)*1.66f);
                    double valley = (noodleNoise > 0.67 ? 184 : (184 * Math.min(1, Math.abs(noodleNoise) + 0.33f)));
                    int volcanoElevation = (int) (Math.max(((Math.min(0.25, secondaryVolcanoDist)-0.25)*-1400)+((Math.min(-0.95, secondaryVolcanoDist-1)+0.95)*10000), ((Math.min(0.25, centDist)-0.25)*-2000)+((Math.min(-0.95, centDist-1)+0.95)*10000))*(1 - Math.abs((miniCellularNoise*miniCellularNoise)/4)));
                    int surface = (int) (maxWorldgenHeight - Math.max(centDist * 184, (valley - (Math.max(0, noodleNoise * 320) * Math.abs(basePerlinNoise)))));
                    double oceanDist = Math.min(40, Math.abs(1-Math.min(1, distance(x, z, eighthSize, eighthSize)/eighthSize))*eighthSize);
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

    public static void generateSurface() {
//        long time = System.currentTimeMillis();
        int startX = currentChunk*chunkSize;
        for (int x = startX; x < startX+chunkSize; x++) {
            int minY = height-1;
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
                    int lavaLevel = (int) (293-(lavaNoise*5));
                    if (centDist < 0.05f && y < lavaLevel) {
                        heightmap[condensedPos] = (short) lavaLevel;
                        for (int newY = lavaLevel; newY >= 0; newY--) {
                            setBlockWorldgen(x, newY, z, 19, 0);
                        }
                    } else if (secondaryVolcanoDist < 0.05f && y < lavaLevel-100) {
                        heightmap[condensedPos] = (short) (lavaLevel-100);
                        int stopY = Math.floorDiv(lavaLevel-100, chunkSize)*chunkSize;
                        for (int newY = lavaLevel-100; newY >= stopY; newY--) {
                            setBlockWorldgen(x, newY, z, 19, 0);
                        }
                    } else if (y < seaLevel) {
                        for (int newY = seaLevel; newY > y; newY--) {
                            setBlockWorldgen(x, newY, z, 1, 15);
                            if (newY == seaLevel) {
                                heightmap[condensedPos] = (short) y;
                            }
                        }
                        for (int newY = y; newY > y-3; newY--) {
                            setBlockWorldgen(x, newY, z, 3, 0);
                        }
                        int stopY = Math.floorDiv(y-3, chunkSize)*chunkSize;
                        for (int newY = y-3; newY >= stopY; newY--) {
                            setBlockWorldgen(x, newY, z, 10, 0);
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
                        int volcanoElevation = (int) Math.max((Math.min(0.25, centDist)-0.25)*-2000, (Math.min(0.25, secondaryVolcanoDist)-0.25)*-1400);
                        double oceanDist = distance(x, z, eighthSize, eighthSize)/eighthSize;
                        if (y >= seaLevel && y < seaLevel+6 && (oceanDist < 0.95f || (flat && oceanDist < 1f))) {
                            heightmap[condensedPos] = (short) (y);
                            for (int newY = y; newY > y-5; newY--) {
                                setBlockWorldgen(x, newY, z, 23, 0);
                            }
                            int stopY = Math.floorDiv(y-6, chunkSize)*chunkSize;
                            for (int newY = y-6; newY >= stopY; newY--) {
                                setBlockWorldgen(x, newY, z, 24, 0);
                            }
                        } else if (flat) {
                            setBlockWorldgen(x, y, z, y <= volcanoElevation ? 3 : 2, 0);
                            setBlockWorldgen(x, y - 1, z, 3, 0);
                            setBlockWorldgen(x, y - 2, z, 3, 0);
                            setBlockWorldgen(x, y - 3, z, 3, 0);
                            int stopY = Math.floorDiv(y-4, chunkSize)*chunkSize;
                            for (int newY = y - 4; newY >= stopY; newY--) {
                                setBlockWorldgen(x, newY, z, newY <= volcanoElevation ? 9 : 10, 0);
                            }
                        } else {
                            int lavaAir = (int)(Math.abs(lavaNoise)*200);
                            short lowestY = (short)(y);
                            for (int newY = y; newY >= 0; newY--) {
                                if (newY <= volcanoElevation) {
                                    int blockTypeId = lavaNoise > -0.1 && lavaNoise < 0.1 ? (newY >= y - lavaAir ? 0 : (lavaAir > 3 ? 19 : 9)) : 9;
                                    setBlockWorldgen(x, newY, z, blockTypeId, 0);
                                    if (blockTypeId == 0) {
                                        lowestY = (short)(newY);
                                    }
                                } else {
                                    setBlockWorldgen(x, newY, z, 10, 0);
                                }
                            }
                            heightmap[condensedPos] = lowestY;
                        }
                    }
                    minY = Math.min(minY, heightmap[condensedPos]);
                    int chunkZ = z >> 4;
                    if (chunkZ != prevChunkZ) {
                        for (int chunkY = (minY / chunkSize) - 1; chunkY >= 0; chunkY--) {
                            //blackenLightPaletteWorldgen(currentChunk, chunkY, prevChunkZ);
                            chunks[condenseChunkPos(currentChunk, chunkY, prevChunkZ)] = new Chunk(new Vector2i(10, 0), 0);
                        }
                        prevChunkZ = chunkZ;
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
                    int y = surfaceHeightmap[condensePos(x, z)];
                    if (y >= seaLevel) {
                        Vector2i blockOn = getBlockWorldgen(x, y, z);
                        float basePerlinNoise = Noises.COHERERENT_NOISE.sample(x, z);
                        float foliageNoise = (basePerlinNoise + 0.5f);
                        float exponentialFoliageNoise = foliageNoise * foliageNoise;
                        double northEastDist = distance(x, z, size, size) / size;
                        double southWestDist = distance(x, z, 0, 0) / size;
                        double oceanDist = distance(x, z, eighthSize, eighthSize) / eighthSize;
                        double centDist = distance(x, z, size/2, size/2) / size;
                        double taiganess = Math.min(0.03f, Math.max(0.7, (1.5f-centDist)*0.7f)-0.8)/9;
                        double jungleness = oceanDist < 1 ? (Math.max(0.25, (1.5f-southWestDist)*0.25f)-0.3)/9 : 0;
                        double forestness = (Math.max(0.34, (1.5f-Math.max(southWestDist-jungleness, northEastDist))*0.34f)-0.4);
                        double plainness = 0.01d-Math.max(0, Math.max(forestness, taiganess));
                        double randomNumber = Math.random();
                        boolean setAnything = false;
                        if (randomNumber > 0.99995d) {
                            if (randomNumber > 0.99997d) {
                                setBlockWorldgen(x, y, z, 7, 0);
                                setBlockWorldgen(x, y + 1, z, 7, 0);
                                setBlockWorldgen(x, y + 2, z, 7, 0);
                            } else {
                                setBlockWorldgen(x, y + 1, z, 14, 0);
                            }
                            setAnything = true;
                        } else if (blockOn.x == 2 || blockOn.x == 3 || blockOn.x == 23) {
                            if (randomNumber < jungleness*Math.max(0.8f, exponentialFoliageNoise)) { //jungle
                                JungleTree.generate(blockOn, x, y, z, (int) (Math.random() * 8) + 28, (int) (3 + (randomNumber + 0.5f)), 20, 0, 21, 0, randomNumber < 0.25f);
                                PalmTree.generate(blockOn, x, y, z, (int) (Math.random() * 14) + 8, 25, 0, 27, 0);
                                setAnything = true;
                            } else if (randomNumber < jungleness*0.2) {
                                if (randomNumber > jungleness*0.05) {
                                    int maxHeight = (int) (Math.random() + 1);
                                    OakTree.generate(blockOn, x, y, z, maxHeight, 3 + (maxHeight * 2), 16, 0, 17, 0);
                                    setAnything = true;
                                } else {
                                    int maxHeight = (int) (Math.random() * 4) + 8;
                                    OakTree.generate(blockOn, x, y, z, maxHeight, (int) (maxHeight + (randomNumber * 100)) - 2, 16, 0, 17, 0);
                                    setAnything = true;
                                }
                            } else if (randomNumber < forestness*(exponentialFoliageNoise*4)) { //forest
                                int maxHeight = (int) (Math.random() * 4) + 8;
                                OakTree.generate(blockOn, x, y, z, maxHeight, (int) (maxHeight + (randomNumber * 100)), 16, 0, 17, 0);
                                setAnything = true;
                            } else if (randomNumber < forestness/2) {
                                int maxHeight = (int) (Math.random() + 1);
                                OakTree.generate(blockOn, x, y, z, maxHeight, 3 + (maxHeight * 2), 16, 0, 17, 0);
                                setAnything = true;
                            } else if (randomNumber*20 < plainness) { //plains
                                int maxHeight = (int) (Math.random() + 1);
                                OakTree.generate(blockOn, x, y, z, maxHeight, 3 + (maxHeight * 2), 16, 0, 17, 0);
                                setAnything = true;
                            } else if (randomNumber*18 < plainness) {
                                int maxHeight = (int) (Math.random() * 4) + 8;
                                OakTree.generate(blockOn, x, y, z, maxHeight, (int) (maxHeight + (randomNumber * 100)), 16, 0, 17, 0);
                                setAnything = true;
                            }
                        }
                        if (!setAnything) {
                            if (getBlockWorldgen(x, y + 1, z).x <= 1) { //only replace air and water
                                Plains.generate(blockOn, x, y, z);
                                if (randomNumber < 0.08f) {
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
                    for (int y = heightmap[condensePos(x, z)] - 1; y > surfaceHeightmap[condensePos(x, z)]; y--) {
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