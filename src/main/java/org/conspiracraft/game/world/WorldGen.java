package org.conspiracraft.game.world;

import org.conspiracraft.engine.ConspiracraftMath;
import org.conspiracraft.game.Noise;
import org.conspiracraft.game.world.trees.canopies.BlobCanopy;
import org.conspiracraft.game.world.trees.trunks.StraightTrunk;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

import java.util.List;

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
    public static Vector4i getLightWorldgen(int x, int y, int z) {
        return chunks[condenseChunkPos(x >> 4, y >> 4, z >> 4)].getLight(condenseLocalPos(x & 15, y & 15, z & 15));
    }
    public static void setLightWorldgen(int x, int y, int z, Vector4i light) {
        chunks[condenseChunkPos(x >> 4, y >> 4, z >> 4)].setLight(new Vector3i(x & 15, y & 15, z & 15), light, new Vector3i(x, y, z));
    }
    public static void generateTerrain() {
//        long time = System.currentTimeMillis();
        int startX = currentChunk*chunkSize;
        for (int x = startX; x < startX+chunkSize; x++) {
            for (int z = 0; z < size; z++) {
                if ((x <= halfSize && z <= halfSize) || !quarterWorld) {
                    float baseCellularNoise = (Noise.blue(Noise.CELLULAR_NOISE.getRGB(x - (((int) (x / Noise.CELLULAR_NOISE.getWidth())) * Noise.CELLULAR_NOISE.getWidth()), z - (((int) (z / Noise.CELLULAR_NOISE.getHeight())) * Noise.CELLULAR_NOISE.getHeight()))) / 128) - 1;
                    float basePerlinNoise = (Noise.blue(Noise.COHERERENT_NOISE.getRGB(x - (((int) (x / Noise.COHERERENT_NOISE.getWidth())) * Noise.COHERERENT_NOISE.getWidth()), z - (((int) (z / Noise.COHERERENT_NOISE.getHeight())) * Noise.COHERERENT_NOISE.getHeight()))) / 128) - 1;
                    float noodleNoise = (Noise.blue(Noise.NOODLE_NOISE.getRGB((x - (((int) (x / Noise.NOODLE_NOISE.getWidth())) * Noise.NOODLE_NOISE.getWidth())), (z - (((int) (z / Noise.NOODLE_NOISE.getHeight())) * Noise.NOODLE_NOISE.getHeight())))) / 128) - 1;
                    int miniX = x*8;
                    int miniZ = z*8;
                    float miniCellularNoise = (Noise.blue(Noise.CELLULAR_NOISE.getRGB(miniX - (((int) (miniX / Noise.CELLULAR_NOISE.getWidth())) * Noise.CELLULAR_NOISE.getWidth()), miniZ - (((int) (miniZ / Noise.CELLULAR_NOISE.getHeight())) * Noise.CELLULAR_NOISE.getHeight()))) / 128) - 1;

                    double centDist = Math.min(1, (distance(x, z, halfSize, halfSize) / halfSize) * 1.15f);
                    double secondaryVolcanoDist = Math.min(1, (distance(x, z, halfSize*1.25f, halfSize*1.25f) / halfSize)*1.66f);
                    double valley = (noodleNoise > 0.67 ? 184 : (184 * Math.min(1, Math.abs(noodleNoise) + 0.33f)));
                    int volcanoElevation = (int) (Math.max(((Math.min(0.25, secondaryVolcanoDist)-0.25)*-1400)+((Math.min(-0.95, secondaryVolcanoDist-1)+0.95)*10000), ((Math.min(0.25, centDist)-0.25)*-2000)+((Math.min(-0.95, centDist-1)+0.95)*10000))*(1 - Math.abs((miniCellularNoise*miniCellularNoise)/4)));
                    int surface = (int) (maxWorldgenHeight - Math.max(centDist * 184, (valley - (Math.max(0, noodleNoise * 320) * Math.abs(basePerlinNoise)))));
                    for (int y = Math.max(surface, volcanoElevation); y >= 0; y--) {
                        double baseGradient = ConspiracraftMath.gradient(y, surface, 48, 2, -1);
                        double baseDensity = baseCellularNoise + baseGradient;
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
            for (int z = 0; z < size; z++) {
                if ((x <= halfSize && z <= halfSize) || !quarterWorld) {
                    int lavaX = x*3;
                    int lavaZ = z*3;
                    float lavaNoise = (Noise.blue(Noise.NOODLE_NOISE.getRGB((lavaX - (((int) (lavaX / Noise.NOODLE_NOISE.getWidth())) * Noise.NOODLE_NOISE.getWidth())), (lavaZ - (((int) (lavaZ / Noise.NOODLE_NOISE.getHeight())) * Noise.NOODLE_NOISE.getHeight())))) / 128) - 1;
                    int y = surfaceHeightmap[condensePos(x, z)];
                    double centDist = Math.min(1, (distance(x, z, halfSize, halfSize) / halfSize) * 1.15f);
                    double secondaryVolcanoDist = Math.min(1, (distance(x, z, halfSize*1.25f, halfSize*1.25f) / halfSize)*1.66f);
                    int lavaLevel = (int) (293-(lavaNoise*5));
                    if (centDist < 0.05f && y < lavaLevel) {
                        heightmap[condensePos(x, z)] = (short) lavaLevel;
                        for (int newY = lavaLevel; newY > 0; newY--) {
                            setBlockWorldgen(x, newY, z, 19, 0);
                            setLightWorldgen(x, newY, z, new Vector4i(0, 0, 0, 0));
                        }
                    } else if (secondaryVolcanoDist < 0.05f && y < lavaLevel-100) {
                        heightmap[condensePos(x, z)] = (short) (lavaLevel-100);
                        for (int newY = lavaLevel-100; newY > 0; newY--) {
                            setBlockWorldgen(x, newY, z, 19, 0);
                            setLightWorldgen(x, newY, z, new Vector4i(0, 0, 0, 0));
                        }
                    } else if (y < seaLevel) {
                        for (int newY = seaLevel; newY > y; newY--) {
                            setBlockWorldgen(x, newY, z, 1, 20);
                            setLightWorldgen(x, newY, z, new Vector4i(0, 0, 0, 0));
                            if (newY == seaLevel) {
                                heightmap[condensePos(x, z)] = (short) y;
                            }
                        }
                        for (int newY = y; newY > y-3; newY--) {
                            setBlockWorldgen(x, newY, z, 3, 0);
                        }
                        for (int newY = y-3; newY >= 0; newY--) {
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
//                    if (flat) {
//                        maxSteepness = 0;
//                        for (int pos : new int[]{condensePos(Math.min(size-1, x+5), z), condensePos(Math.max(0, x-5), z), condensePos(x, Math.min(size-1, z+5)), condensePos(x, Math.max(0, z-5))}) {
//                            int nY = surfaceHeightmap[pos];
//                            int steepness = Math.abs(y-nY);
//                            maxSteepness = Math.max(maxSteepness, steepness);
//                        }
//                        flat = maxSteepness < 5;
//                    }

                        int volcanoElevation = (int) Math.max((Math.min(0.25, centDist)-0.25)*-2000, (Math.min(0.25, secondaryVolcanoDist)-0.25)*-1400);
                        if (flat) {
                            setBlockWorldgen(x, y, z, y <= volcanoElevation ? 3 : 2, 0);
                            setBlockWorldgen(x, y - 1, z, 3, 0);
                            setBlockWorldgen(x, y - 2, z, 3, 0);
                            setBlockWorldgen(x, y - 3, z, 3, 0);
                            for (int newY = y - 4; newY >= 0; newY--) {
                                setBlockWorldgen(x, newY, z, newY <= volcanoElevation ? 9 : 10, 0);
                            }
                            float basePerlinNoise = (Noise.blue(Noise.COHERERENT_NOISE.getRGB(x - (((int) (x / Noise.COHERERENT_NOISE.getWidth())) * Noise.COHERERENT_NOISE.getWidth()), z - (((int) (z / Noise.COHERERENT_NOISE.getHeight())) * Noise.COHERERENT_NOISE.getHeight()))) / 128) - 1;
                            float foliageNoise = (basePerlinNoise + 0.5f);
                            float exponentialFoliageNoise = foliageNoise * foliageNoise;
                            double northEastDist = distance(x, z, size, size)/size;
                            double torchChance = Math.random();
                            if (torchChance > 0.99995d) {
                                if (torchChance > 0.99997d) {
                                    setBlockWorldgen(x, y, z, 7, 0);
                                    setBlockWorldgen(x, y + 1, z, 7, 0);
                                    setBlockWorldgen(x, y + 2, z, 7, 0);
                                } else {
                                    setBlockWorldgen(x, y + 1, z, 14, 0);
                                }
                            } else if (torchChance < ((exponentialFoliageNoise-(northEastDist > 0.75 ? (northEastDist*2) : (northEastDist/2))) * 0.015f)) { // oak tree
                                int maxHeight = (int) (Math.random() * 4) + 8;
                                List<Vector3i> canopies = StraightTrunk.generateTrunk(x, y, z, maxHeight, 16, 0);
                                for (Vector3i canopyPos : canopies) {
                                    BlobCanopy.generateCanopy(canopyPos.x, canopyPos.y, canopyPos.z, 17, 0, (int) (maxHeight + (torchChance * 100)));
                                }
                            } else {
                                double flowerChance = Math.random();
                                setBlockWorldgen(x, y + 1, z, 4 + (flowerChance > 0.98f ? (flowerChance > 0.99f ? 14 : 1) : 0), (int) (Math.random() * 3));
                            }
                        } else {
                            int lavaAir = (int)(Math.abs(lavaNoise)*200);
                            for (int newY = y; newY >= 0; newY--) {
                                if (newY <= volcanoElevation) {
                                    setBlockWorldgen(x, newY, z, lavaNoise > -0.1 && lavaNoise < 0.1 ? (newY >= y-lavaAir ? 0 : (lavaAir > 3 ? 19 : 9)) : 9, 0);
                                } else {
                                    setBlockWorldgen(x, newY, z, newY >= minNeighborY ? 8 : 10, 0);
                                }
                            }
                        }
                    }
                }
            }
        }
//        System.out.print("Took "+(System.currentTimeMillis()-time)+"ms to generate slice of surface. \n");
//        String progress = String.valueOf(((float) currentChunk / sizeChunks)*100).substring(0, 3);
//        System.out.print("World is " + (progress + "% generated. \n"));
    }
}
