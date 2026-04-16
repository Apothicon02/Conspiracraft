package org.conspiracraft.world;

import org.conspiracraft.utils.Utils;
import org.conspiracraft.world.shapes.Blob;
import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.lang.Math;
import java.util.Random;

import static org.conspiracraft.Main.*;
import static org.conspiracraft.graphics.Renderer.drawCube;
import static org.conspiracraft.world.World.*;

public class Earth extends WorldType {
    public static Random seededRand = new Random(35311350L);
    public Random rand() {
        return seededRand;
    }
    public static Vector3f prevSunPos = new Vector3f(0, World.height*2, 0);
    public static Vector3f sunPos = new Vector3f(0, World.height*2, 0);
    public static Vector3f prevMunPos = new Vector3f(0, World.height*-2, 0);
    public static Vector3f munPos = new Vector3f(0, World.height*-2, 0);
    @Override
    public void renderCelestialBodies(MemoryStack stack) {
        Matrix4f sunMatrix = new Matrix4f().rotateXYZ(0.5f, 0.5f, 0.5f).setTranslation(Utils.getInterpolatedVec(prevSunPos, sunPos)).scale(500);
        Vector4f sunColor = new Vector4f(1.25f, 1.2f, 0, 1);
        drawCube(sunMatrix, sunColor);
        Matrix4f munMatrix = new Matrix4f().rotateXYZ(0.5f, 0.5f, 0.5f).setTranslation(Utils.getInterpolatedVec(prevMunPos, munPos)).scale(300);
        Vector4f munColor = new Vector4f(0.9f, 0.88f, 1.f, 1);
        drawCube(munMatrix, munColor);
    }
    @Override
    public Vector4f getSkylight() {return sunPos.y() < 0 && sunPos.y() < munPos.y() ? new Vector4f(munPos, 0.33f) : new Vector4f(sunPos, 1.f);}
    @Override
    public Vector3f getSun() {return sunPos;}
    @Override
    public void tick() {
        prevSunPos.set(sunPos);
        sunPos.set(0, World.size*2, 0);
        sunPos.rotateZ(timeNs/100000000000.f);
        sunPos.rotateX(0.5f);
        sunPos.set(sunPos.x+(World.size/2f), sunPos.y, sunPos.z+(World.size/2f)+128);
        prevMunPos.set(munPos);
        munPos.set(0, World.size*-2, 0);
        munPos.rotateZ(timeNs/100000000000.f);
        sunPos.rotateX(-0.2f);
        munPos.set(munPos.x+(World.size/2f), munPos.y, munPos.z+(World.size/2f)+128);
    }
    @Override
    public void generate() {
        for (int x = 0; x < sizeChunks; x++) {
            for (int z = 0; z < sizeChunks; z++) {
                for (int y = 0; y < heightChunks; y++) {
                    int packedChunkPos = packChunkPos(x, y, z);
                    chunks[packedChunkPos] = new Chunk(new Vector3i(x, y, z), packedChunkPos);
                }
            }
        }
        short[] chunksMinElevations = new short[sizeChunks*sizeChunks];
        for (int cX = 0; cX < World.sizeChunks; cX++) {
            for (int cZ = 0; cZ < World.sizeChunks; cZ++) {
                short minElevation = (short) (height-1);
                for (int x = cX * chunkSize; x < (cX * chunkSize) + chunkSize; x++) {
                    for (int z = cZ * chunkSize; z < (cZ * chunkSize) + chunkSize; z++) {
                        short elevation = getElevation(x, z);
                        heightmap[packPos(x, z)] = elevation;
                        minElevation = (short) Math.min(minElevation, elevation);
                    }
                }
                chunksMinElevations[packChunkPos(cX, cZ)] = minElevation;
            }
        }
        for (int cX = 0; cX < World.sizeChunks; cX++) {
            for (int cZ = 0; cZ < World.sizeChunks; cZ++) {
                int cY = chunksMinElevations[packChunkPos(cX, cZ)]/chunkSize;
                for (int x = cX*chunkSize; x < (cX*chunkSize)+chunkSize; x++) {
                    for (int z = cZ*chunkSize; z < (cZ*chunkSize)+chunkSize; z++) {
                        int elevation = heightmap[packPos(x, z)];
                        int maxSteepness = 0;
                        int minNeighborY = height - 1;
                        for (int pos : new int[]{packPos(Math.min(size - 1, x + 3), z), packPos(Math.max(0, x - 3), z), packPos(x, Math.min(size - 1, z + 3)), packPos(x, Math.max(0, z - 3)),
                                packPos(Math.max(0, x - 3), Math.max(0, z - 3)), packPos(Math.min(size - 1, x + 3), Math.max(0, z - 3)), packPos(Math.max(0, x - 3), Math.min(size - 1, z + 3)), packPos(Math.min(size - 1, x + 3), Math.min(size - 1, z + 3))}) {
                            int nY = heightmap[pos];
                            minNeighborY = Math.min(minNeighborY, nY);
                            int steepness = Math.abs(elevation - nY);
                            maxSteepness = Math.max(maxSteepness, steepness);
                        }
                        boolean flat = maxSteepness < 4;
                        if (elevation <= 63) {
                            World.setBlock(x, 63, z, 1, 12);
                            for (int y = 62; y > elevation; y--) {
                                World.setBlock(x, y, z, 1, 15);
                            }
                        }
                        if (flat) {
                            if (elevation >= 66) {
                                if (seededRand.nextBoolean() && seededRand.nextFloat() < SimplexNoise.noise(x/100.f, z/100.f)-0.2f) {
                                    World.setBlock(x, elevation + 1, z, 5, seededRand.nextInt(3));
                                } else if (seededRand.nextFloat() < 0.003f) {
                                    World.setBlock(x, elevation + 1, z, 18, seededRand.nextInt(3));
                                }
                            }
                            World.setBlock(x, elevation, z, elevation < 66 ? 23 : 2, 0);
                            for (int y = elevation - 1; y >= cY * chunkSize; y--) {
                                World.setBlock(x, y, z, elevation <= 66 ? 24 : 3, 0);
                            }
                        } else {
                            for (int y = elevation; y >= cY * chunkSize; y--) {
                                World.setBlock(x, y, z, 55, 0);
                            }
                        }
                    }
                }
            }
        }


        for (int cX = 0; cX < World.sizeChunks; cX++) {
            for (int cZ = 0; cZ < World.sizeChunks; cZ++) {
                for (int x = cX * chunkSize; x < (cX * chunkSize) + chunkSize; x++) {
                    for (int z = cZ * chunkSize; z < (cZ * chunkSize) + chunkSize; z++) {
                        int elevation = heightmap[(x * size) + z];
                        Vector2i blockOn = getBlock(x, elevation, z);
                        float randomNumber = seededRand.nextFloat();
                        if (blockOn.x == 55) {
                            if (randomNumber < 0.05f) {
                                Blob.generate(blockOn, x, elevation, z, (Math.abs(SimplexNoise.noise(x / 150.f, z / 150.f)) < 0.05f ? 56 : 10), 0, (int) (2 + (seededRand.nextFloat() * 16)));
                            }
                        } else if (blockOn.x == 2) {
                            if (randomNumber < 0.0004f) {
                                Blob.generate(blockOn, x, elevation, z, 48, 0, (int) (2 + (seededRand.nextFloat() * 7)));
                            } else if (randomNumber < 0.0008f || randomNumber < SimplexNoise.noise(x / 200.f, z / 200.f)/50) {
                                for (int y = elevation - 1; y < elevation+10; y++) {
                                    World.setBlock(x, y, z, 16, 0);
                                }
                                Blob.generate(blockOn, x, elevation+10, z, 17, 0, (int) (2 + (seededRand.nextFloat() * 7)));
                            }
                        }
                    }
                }
            }
        }

//        for (int x = 950; x < 1050; x++) {
//            for (int z = 950; z < 1050; z++) {
//                for (int y = 150; y < 250; y++) {
//                    World.setBlock(x, y, z, 1, 15);
//                }
//            }
//        }
//        for (int x = 8; x < size-8; x++) {
//            for (int z = 8; z < size-8; z++) {
//                double cloudNoise = Math.abs(SimplexNoise.noise(x / 400.f, z / 400.f));
//                double cloudSecondaryNoise = Math.abs(SimplexNoise.noise(x / 600.f, z / 600.f));
//                if (cloudNoise < 0.4f && cloudSecondaryNoise > 0.5f && seededRand.nextFloat() > 0.95f && heightmap[packPos(x, z)] < 166) {
//                    int cloudHeight = 216 + (int) Math.abs(SimplexNoise.noise(x/800.f, z/800.f) * 84);
//                    boolean isRainCloud = (new Vector2i(x, z).distance(size, size) / World.quarterSize < 1 && (seededRand.nextFloat() < 0.0005f));
//                    int radius = (int) (((isRainCloud ? 6 : 0) + seededRand.nextInt(2, 6)) * (1+(150*Math.pow(0.4f-Math.min(0.4f, cloudNoise), 2))));
//                    Blob.generate(new Vector2i(0), x, cloudHeight, z, isRainCloud ? 32 : 31, 0, radius, true);
//                }
//            }
//        }
    }
    public short getElevation(int x, int z) {
        double mountainNoise = Math.max(0, SimplexNoise.noise(x / 250.f, z / 250.f));
        double elevationNoise = SimplexNoise.noise(x / 500.f, z / 500.f) + 0.5f;
        double elevationMul = (mountainNoise * elevationNoise)+0.25F;
        double detailNoise = ((SimplexNoise.noise(x / 75.f, z / 75.f)+elevationNoise) * 16);
        double elevation = detailNoise * Math.max(-0.5f, elevationMul*2);
        if (elevation < 0) {
            elevation *= -0.25f;
        }
        double continentNoise = -(Math.abs(elevationNoise-0.5f)-0.7f);
        elevation += 66*Math.min(0, continentNoise);
        elevation += 66;
        return (short)elevation;
    }
//
//    @Override
//    public void generate() {
//        for (int x = 0; x < sizeChunks; x++) {
//            for (int z = 0; z < sizeChunks; z++) {
//                for (int y = 0; y < heightChunks; y++) {
//                    int packedChunkPos = packChunkPos(x, y, z);
//                    chunks[packedChunkPos] = new Chunk(new Vector3i(x, y, z), packedChunkPos);
//                }
//            }
//        }
//        short[] chunksMinElevations = new short[sizeChunks*sizeChunks];
//        for (int cX = 0; cX < World.sizeChunks; cX++) {
//            for (int cZ = 0; cZ < World.sizeChunks; cZ++) {
//                short minElevation = (short) (height-1);
//                for (int x = cX * chunkSize; x < (cX * chunkSize) + chunkSize; x++) {
//                    for (int z = cZ * chunkSize; z < (cZ * chunkSize) + chunkSize; z++) {
//                        short elevation = getElevation(x, z);
//                        heightmap[packPos(x, z)] = elevation;
//                        minElevation = (short) Math.min(minElevation, elevation);
//                    }
//                }
//                chunksMinElevations[packChunkPos(cX, cZ)] = minElevation;
//            }
//        }
//
//        for (int cX = 0; cX < World.sizeChunks; cX++) {
//            for (int cZ = 0; cZ < World.sizeChunks; cZ++) {
//                for (int x = cX * chunkSize; x < (cX * chunkSize) + chunkSize; x++) {
//                    for (int z = cZ * chunkSize; z < (cZ * chunkSize) + chunkSize; z++) {
//                        int topBlock = 0;
//                        for (int y = heightmap[packPos(x, z)]; y >= 0; y--) {
//                            if (canPlace(x, y, z)) {
//                                if (topBlock == 0) {
//                                    topBlock = y;
//                                    if (topBlock >= 66 && seededRand.nextFloat() < 0.1f) {
//                                        World.setBlock(x, y + 1, z, seededRand.nextFloat() < 0.15f ? 5 : 4, seededRand.nextInt(3));
//                                    }
//                                    World.setBlock(x, y, z, topBlock < 66 ? 23 : 2, 0);
//                                } else {
//                                    World.setBlock(x, y, z, topBlock <= 66 ? 24 : 3, 0);
//                                }
//                            } else if (y == 63) {
//                                World.setBlock(x, y, z, 1, 13);
//                            } else if (y < 63) {
//                                World.setBlock(x, y, z, 1, 15);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//    public boolean canPlace(int x, int y, int z) {
//        int yFactor = (int) Math.pow((Math.max(66, y)-66)/2.f, 1.1f);
//        return getUnelevation(x+yFactor, z+yFactor) > y;
//    }
//    public short getElevation(int x, int z) {
//        double elevation = 62+getContinentElevation(x, z);//Math.max(getContinentElevation(x, z), getIslandsElevation(x, z));
//        //elevation = Math.min(elevation, getUnelevation(x, z));
//        return (short)elevation;
//    }
//    public double getUnelevation(int x, int z) {
//        double unelevationNoise = SimplexNoise.noise(x/350.f, z/350.f);
//        double unelevation = (Math.clamp(unelevationNoise, 0.25f, 0.35f)-0.25f)*10;
//        if (unelevation >= 0.9f) {
//            unelevation += (1-Math.max(0.35f, unelevationNoise))*-0.035f;
//        }
//        unelevation *= 250;
//        return height-unelevation;
//    }
//    public double getContinentElevation(int x, int z) {
//        double elevationNoise = SimplexNoise.noise(x/500.f, z/500.f)+0.75f;
//        elevationNoise *= 30;
//        return elevationNoise;
//    }
//    public double getIslandsElevation(int x, int z) {
//        double mountainNoise = (1 + Math.max(0, SimplexNoise.noise(x / 500.f, z / 500.f)));
//        double elevationNoise = SimplexNoise.noise(x / 1000.f, z / 1000.f) + 0.5f;
//        double elevationMul = mountainNoise * elevationNoise;
//        double detailNoise = (SimplexNoise.noise(x / 100.f, z / 100.f) * 16);
//        double elevation = detailNoise * Math.max(0.f, elevationMul);
//        if (elevation < 0) {
//            elevation *= -0.25f;
//        }
//        return elevation;
//    }
}
