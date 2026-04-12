package org.conspiracraft.world;

import org.conspiracraft.utils.Utils;
import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.lang.Math;
import java.util.Random;

import static org.conspiracraft.Main.*;
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
        Matrix4f sunMatrix = new Matrix4f().rotateXYZ(0.5f, 0.5f, 0.5f).setTranslation(Utils.getInterpolatedVec(prevSunPos, sunPos)).scale(120);
        Vector4f sunColor = new Vector4f(1.25f, 1.2f, 0, 1);
        //drawCube(sunMatrix, sunColor);
        Matrix4f munMatrix = new Matrix4f().rotateXYZ(0.5f, 0.5f, 0.5f).setTranslation(Utils.getInterpolatedVec(prevMunPos, munPos)).scale(40);
        Vector4f munColor = new Vector4f(0.9f, 0.88f, 1.f, 1);
        //drawCube(munMatrix, munColor);
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
        Random worldgenRandom = new Random(67);
        for (int x = 0; x < sizeChunks; x++) {
            for (int z = 0; z < sizeChunks; z++) {
                for (int y = 0; y < heightChunks; y++) {
                    int packedChunkPos = packChunkPos(x, y, z);
                    chunks[packedChunkPos] = new Chunk(new Vector3i(x, y, z), packedChunkPos);
                }
            }
        }

        for (int cX = 0; cX < World.sizeChunks; cX++) {
            for (int cZ = 0; cZ < World.sizeChunks; cZ++) {
                for (int x = cX*chunkSize; x < (cX*chunkSize)+chunkSize; x++) {
                    for (int z = cZ*chunkSize; z < (cZ*chunkSize)+chunkSize; z++) {
                        int elevation = getElevation(x, z);
                        if (elevation <= 63) {
                            World.setBlock(x, 63, z, 1, 12);
                            for (int y = 62; y > elevation; y--) {
                                World.setBlock(x, y, z, 1, 15);
                            }
                        }
                        if (elevation >= 66 && worldgenRandom.nextBoolean()) {
                            World.setBlock(x, elevation + 1, z, worldgenRandom.nextFloat() < 0.15f ? 5 : 4, 0);//4 + worldgenRandom.nextInt(), worldgenRandom.nextInt(3));
                        }
                        World.setBlock(x, elevation, z, elevation < 66 ? 23 : 2, 0);
                        int cY = elevation/chunkSize; //needs to use the minimum heightmap value for this column of chunks instead of elevation of this specific block to prevent underground gaps.
                        for (int y = elevation-1; y >= cY*chunkSize; y--) {
                            World.setBlock(x, y, z, elevation <= 66 ? 24 : 3, 0);
                        }
                    }
                }
            }
        }
    }
    public int getElevation(int x, int z) {
        double mountainNoise = (1 + Math.max(0, SimplexNoise.noise(x / 500.f, z / 500.f)));
        double elevationNoise = SimplexNoise.noise(x / 1000.f, z / 1000.f) + 0.5f;
        double elevationMul = mountainNoise * elevationNoise;
        double detailNoise = (SimplexNoise.noise(x / 100.f, z / 100.f) * 16);
        int elevation = (int) (detailNoise * Math.max(0.f, elevationMul));
        if (elevation < 0) {
            elevation *= -0.25f;
        }
        elevation += 62;
        return elevation;
    }
}
