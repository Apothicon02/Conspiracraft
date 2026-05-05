package org.conspiracraft.world;

import de.articdive.jnoise.generators.noise_parameters.simplex_variants.Simplex2DVariant;
import de.articdive.jnoise.generators.noise_parameters.simplex_variants.Simplex3DVariant;
import de.articdive.jnoise.generators.noise_parameters.simplex_variants.Simplex4DVariant;
import de.articdive.jnoise.modules.octavation.fractal_functions.FractalFunction;
import de.articdive.jnoise.pipeline.JNoise;
import org.conspiracraft.blocks.types.BlockType;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.entities.EntityTypes;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.world.shapes.*;
import org.conspiracraft.world.trees.*;
import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.lang.Math;
import java.lang.Runtime;
import java.util.BitSet;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.conspiracraft.Main.*;
import static org.conspiracraft.graphics.Renderer.drawCube;
import static org.conspiracraft.graphics.Renderer.pushUBO;
import static org.conspiracraft.world.World.*;

public class Earth extends WorldType {
    public static Vector3f prevSunPos = new Vector3f(0, World.height*2, 0);
    public static Vector3f sunPos = new Vector3f(0, World.height*2, 0);
    public static Vector3f prevMunPos = new Vector3f(0, World.height*-2, 0);
    public static Vector3f munPos = new Vector3f(0, World.height*-2, 0);
    @Override
    public void renderCelestialBodies(MemoryStack stack) {
        pushUBO.updateLayer(-1);
        Matrix4f sunMatrix = new Matrix4f().rotateXYZ(0.5f, 0.5f, 0.5f).setTranslation(Utils.getInterpolatedVec(prevSunPos, sunPos)).scale(500);
        Vector4f sunColor = new Vector4f(1.25f, 1.2f, 0, 1);
        drawCube(sunMatrix, sunColor);
        pushUBO.updateLayer(0);
        pushUBO.updateAtlasOffset(EntityTypes.MUN.atlasOffset);
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
        sunPos.set(0, size*2, 0);
        sunPos.rotateZ(timeNs/100000000000.f);
        sunPos.rotateX(0.5f);
        sunPos.rotateY(2.f);
        sunPos.set(sunPos.x+(size/2f), sunPos.y, sunPos.z+(size/2f)+128);
        prevMunPos.set(munPos);
        munPos.set(0, size*-2, 0);
        munPos.rotateZ(timeNs/100000000000.f);
        munPos.rotateX(-0.2f);
        munPos.rotateY(-1.5f);
        munPos.set(munPos.x+(size/2f), munPos.y, munPos.z+(size/2f)+128);
    }
    public JNoise noisePipeline = JNoise.newBuilder().fastSimplex(3301, Simplex2DVariant.IMPROVE_X, Simplex3DVariant.IMPROVE_XY, Simplex4DVariant.IMPROVE_XYZ_IMPROVE_XZ)
            .octavate(4,1,1.25f, FractalFunction.RIDGED_MULTI,false).build();

    public static boolean fillLake(int x, int y, int z, Lake lake) {
        int packedPos = packPos(x, z);
        if (!inBounds(6, x, y, z) || lake.pos.distance(x, y, z) > 300) {
            return false;
        }
        if (!lake.visited.get(packedPos)) {
            lake.visited.set(packedPos,  true);
            if (heightmap[packPos(x + 1, z)] < y) {if (!fillLake(x + 1, y, z, lake)) {return false;}}
            if (heightmap[packPos(x - 1, z)] < y) {if (!fillLake(x - 1, y, z, lake)) {return false;}}
            if (heightmap[packPos(x, z + 1)] < y) {if (!fillLake(x, y, z + 1, lake)) {return false;}}
            if (heightmap[packPos(x, z - 1)] < y) {if (!fillLake(x, y, z - 1, lake)) {return false;}}
        }
        return true;
    }

    static final int[] xOffset = { 3, -3, 0, 0, 3, -3, -3, 3 };
    static final int[] zOffset = { 0, 0, 3, -3, 3, -3, 3, -3 };
    @Override
    public void generate() throws InterruptedException {
        long start = System.currentTimeMillis();
        generating = true;
        for (int x = 0; x < sizeChunks; x++) {
            for (int z = 0; z < sizeChunks; z++) {
                for (int y = 0; y < heightChunks; y++) {
                    int packedChunkPos = packChunkPos(x, y, z);
                    chunks[packedChunkPos] = new Chunk(new Vector3i(x, y, z), packedChunkPos);
                }
            }
        }
        long startTime = System.currentTimeMillis();
        byte[] biomes = new byte[size*size];
        short[] chunksMinElevations = new short[sizeChunks*sizeChunks];
        Queue<Lake> lakes = new ConcurrentLinkedQueue<>();
        int threads = Math.min(Runtime.getRuntime().availableProcessors(), sizeChunks);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        int heightmapInterval = sizeChunks/threads;
        for (int thread = 0; thread < threads; thread++) {
            int startX = thread * heightmapInterval;
            int endX  = Math.min(startX + heightmapInterval, sizeChunks);
            pool.submit(() -> {
                Random rand = new Random(World.seed);
                for (int cX = startX; cX < endX; cX++) {
                    for (int cZ = 0; cZ < sizeChunks; cZ++) {
                        short minElevation = (short) (height - 1);
                        for (int x = cX * chunkSize; x < (cX * chunkSize) + chunkSize; x++) {
                            for (int z = cZ * chunkSize; z < (cZ * chunkSize) + chunkSize; z++) {
                                double centDist = Math.clamp(Math.max(Math.abs(x - 2048), Math.abs(z - 2048)), 0, 2048) / 2048.f;
                                double desertDist = new Vector2i(0).distance(x, z);
                                double undesertness = (Math.clamp(desertDist, 1000, 1200)-1000) / 200.f;
                                double islandness = 1-((Math.clamp(Math.min(new Vector2i(-750, 1250).distance(x, z), new Vector2i(1250, -750).distance(x, z)), 1000, 1200)-1000) / 200.f);
                                double desertness = 1-undesertness;
                                double ogMoutainness = SimplexNoise.noise(x / 500.f, z / 500.f);
                                //double riverness = Math.min(0, Math.abs(ogMoutainness)-0.25f)*50;
                                double mountainness = ogMoutainness;
                                if (mountainness < 0.f) {
                                    mountainness *= Utils.mix(-0.2f, -0.15f, undesertness);
                                } else {
                                    mountainness *= Math.max(undesertness, 0.4f);
                                }
                                double elevationNoise = ((noisePipeline.evaluateNoise((x - size) / 333.d, (z - size) / 333.d) +
                                        ((noisePipeline.evaluateNoise((x - size) / 125.d, (z - size) / 125.d) * 0.33f)))*undesertness);
                                if (desertness > 0.f) {
                                    elevationNoise += noisePipeline.evaluateNoise((x - size) / 525.d, (z - size) / 525.d)*desertness;
                                }
                                double elevation = elevationNoise * (125 * Math.max((1-undesertness)/3, mountainness));
                                double seaElevation = ((0.2f+(desertness*0.175f)) - (Math.clamp(centDist, 0.75f, 0.95f) - 0.75f)) * 5;
                                double midElevation = (0.15f - (Math.clamp(centDist, 0.2f, 0.35f) - 0.2f)) * 6.667f;
                                double baseHilliness = SimplexNoise.noise((x + size) / 500.f, (z + size) / 500.f);
                                if (baseHilliness < 0.f) {
                                    baseHilliness *= -0.5;
                                }
                                double hilliness = (Math.max(0, baseHilliness) * 35) * seaElevation * midElevation * undesertness;
                                //riverness *= 1-centerElevation;
                                double detailNoise = SimplexNoise.noise(x / 100.f, z / 100.f);
                                double islandsNoise = SimplexNoise.noise(x / 200.f, z / 200.f);
                                double islands = ((60+Math.abs(islandsNoise*10))+Math.max(0, (-islandsNoise)*Math.abs(elevationNoise*20)))*islandness;
                                short finalElevation = (short) Math.max(islands, 10 + (seaElevation * 56) + (midElevation * 40) + Math.max(0, detailNoise) + hilliness + elevation);
                                double snowiness = Utils.gradient(finalElevation, 96, 120, 0, 1);
                                double centBiomeFactor = centDist + (elevationNoise * 0.05f);
                                double redwoodness = 1-Math.clamp(new Vector2f(halfSize+quarterSize, halfSize+quarterSize).distance(x, z)/quarterSize, 0, 1);
                                biomes[x * size + z] = (byte) (Math.max(islandness-(desertDist/1500), desertness)-Math.abs(detailNoise * 0.25f) > 0 ? Biomes.DESERT.id : islandness > 0 ? Biomes.TROPICAL_ISLAND.id : (((elevationNoise * ogMoutainness) + (detailNoise * 0.05f) > snowiness ? Biomes.SNOWY_PEAK.id : (centBiomeFactor < 0.2f ? Biomes.SNOWY_TAIGA.id : (ogMoutainness > 0.1 ? Biomes.CHERRY_GROVE.id : (centBiomeFactor < 0.4f ? (redwoodness > 0.f ? Biomes.REDWOOD_FOREST.id : Biomes.TAIGA.id) : Biomes.TEMPERATE.id))))));
                                heightmap[packPos(x, z)] = finalElevation;
                                minElevation = (short) Math.min(minElevation, finalElevation);
                                if (finalElevation > 66 && rand.nextFloat() < 0.0001f*Math.max(0.1f, undesertness)) {
                                    lakes.add(new Lake(new Vector3i(x, finalElevation+1, z)));
                                }
                            }
                        }
                        chunksMinElevations[packChunkPos(cX, cZ)] = minElevation;
                    }
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        System.out.print("Took "+(System.currentTimeMillis()-startTime)+"ms to generate heightmap from noise. \n");

        startTime = System.currentTimeMillis();
        threads = Math.min(Runtime.getRuntime().availableProcessors(), lakes.size());
        pool = Executors.newFixedThreadPool(threads);
        int lakeInterval = lakes.size()/threads;
        for (int thread = 0; thread < threads; thread++) { //multithreading may break if lakes overlap, but not sure.
            int iterations = Math.min(lakeInterval, lakes.size());
            pool.submit(() -> {
                BitSet threadBitSet = new BitSet(size*size);
                for (int i = 0; i < iterations; i++) {
                    Lake lake = lakes.poll();
                    threadBitSet.clear();
                    lake.visited = threadBitSet;
                    boolean filledLake = fillLake(lake.pos.x(), lake.pos.y(), lake.pos.z(), lake);
                    if (filledLake) {
                        for (int x = 0; x < size; x++) {
                            for (int z = 0; z < size; z++) {
                                int packedPos = packPos(x, z);
                                if (lake.visited.get(packedPos)) {
                                    biomes[packedPos] = Biomes.LAKE.id;
                                    int lakeBed = heightmap[packedPos];
                                    if (lake.pos.y() > lakeBed+1) {
                                        if (getBlock(x, lake.pos.y()-1, z).x() == 0) {
                                            setBlock(x, lake.pos.y()-1, z, 1, 14);
                                            for (int y = lake.pos.y()-2; y > lakeBed; y--) {
                                                setBlock(x, y, z, 1, 15);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        System.out.print("Took "+(System.currentTimeMillis()-startTime)+"ms to fill lakes. \n");

        startTime = System.currentTimeMillis();
        threads = Math.min(Runtime.getRuntime().availableProcessors(), sizeChunks);
        pool = Executors.newFixedThreadPool(threads);
        int surfaceInterval = (sizeChunks + threads - 1) / threads;
        for (int thread = 0; thread < threads; thread++) {
            int startX = thread * surfaceInterval;
            int endX  = Math.min(startX + surfaceInterval, sizeChunks);
            pool.submit(() -> {
                Random rand = new Random(World.seed);
                for (int cX = startX; cX < endX; cX++) {
                    for (int cZ = 0; cZ < sizeChunks; cZ++) {
                        int cY = chunksMinElevations[packChunkPos(cX, cZ)] / chunkSize;
                        for (int x = cX * chunkSize; x < (cX * chunkSize) + chunkSize; x++) {
                            for (int z = cZ * chunkSize; z < (cZ * chunkSize) + chunkSize; z++) {
                                byte biome = biomes[x * size + z];
                                int elevation = heightmap[packPos(x, z)];
                                int maxSteepness = 0;
                                if (biome != Biomes.DESERT.id) {
                                    for (int i = 0; i < xOffset.length; i++) {
                                        int packedPos = packPos(x + xOffset[i], z + zOffset[i]);
                                        if (packedPos >= 0 && packedPos < heightmap.length) {
                                            int nY = heightmap[packedPos];
                                            int steepness = Math.abs(elevation - nY);
                                            maxSteepness = Math.max(maxSteepness, steepness);
                                        }
                                    }
                                }
                                boolean flat = maxSteepness < 4;
                                if (elevation <= 63) {
                                    World.setBlock(x, 63, z, 1, 14);
                                    for (int y = 62; y > elevation; y--) {
                                        World.setBlock(x, y, z, 1, 15);
                                    }
                                }
                                if (flat) {
                                    int sand = (elevation < 64 ? 73 : 23);
                                    int blockType = biome == Biomes.LAKE.id ? 73 : (biome == Biomes.DESERT.id ? sand : (biome == Biomes.SNOWY_PEAK.id || biome == Biomes.SNOWY_TAIGA.id ? 54 : (elevation < 66 ? sand : 2)));
                                    int blockSubtype = elevation >= 66 && (biome == Biomes.REDWOOD_FOREST.id || biome == Biomes.TAIGA.id || biome == Biomes.CHERRY_GROVE.id) ? 1 : 0;
                                    if (blockType == 2) {
                                        float foliageNoise = SimplexNoise.noise(x / 100.f, z / 100.f);
                                        if (rand.nextBoolean() && rand.nextFloat() < foliageNoise - 0.2f) {
                                            World.setBlock(x, elevation + 1, z, 5, rand.nextInt(3));
                                        } else if (rand.nextFloat() < 0.003f) {
                                            World.setBlock(x, elevation + 1, z, 18, rand.nextInt(3));
                                        } else if (rand.nextFloat() < 0.3f && rand.nextFloat() > foliageNoise) {
                                            World.setBlock(x, elevation + 1, z, 4, ((biome == Biomes.REDWOOD_FOREST.id || biome == Biomes.TAIGA.id || biome == Biomes.CHERRY_GROVE.id) ? 4 : 0) + rand.nextInt(3));
                                        }
                                    }
                                    World.setBlock(x, elevation, z, blockType, blockSubtype);
                                    for (int y = elevation - 1; y >= cY * chunkSize; y--) {
                                        World.setBlock(x, y, z, (blockType == 23 || blockType == 73) ? 24 : 3, 0);
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
            });
        }
        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        System.out.print("Took "+(System.currentTimeMillis()-startTime)+"ms to generate surface. \n");

        startTime = System.currentTimeMillis();
        threads = Math.min(Runtime.getRuntime().availableProcessors(), sizeChunks);
        pool = Executors.newFixedThreadPool(threads);
        int featuresInterval = (sizeChunks + threads - 1) / threads;
        for (int thread = 0; thread < threads; thread++) {
            int startX = thread * featuresInterval;
            int endX  = Math.min(startX + featuresInterval, sizeChunks);
            pool.submit(() -> {
                Random rand = new Random(World.seed);
                for (int cX = startX; cX < endX; cX++) {
                    for (int cZ = 0; cZ < sizeChunks; cZ++) {
                        for (int x = cX * chunkSize; x < (cX * chunkSize) + chunkSize; x++) {
                            for (int z = cZ * chunkSize; z < (cZ * chunkSize) + chunkSize; z++) {
                                int elevation = heightmap[(x * size) + z];
                                byte biome = biomes[x * size + z];
                                Vector2i blockOn = getBlock(x, elevation, z);
                                Vector2i blockIn = getBlock(x, elevation+1, z);
                                float randomNumber = rand.nextFloat();
                                float featureNoise = SimplexNoise.noise(x / 300.f, z / 300.f);
                                float featureNoiseSmall = SimplexNoise.noise(x / 100.f, z / 100.f);
                                double rockNoise = Math.abs(SimplexNoise.noise(x / 150.f, z / 150.f));
                                double eleFactor = elevation + (rockNoise * 50);
                                boolean snowy = eleFactor > 136;
                                if (blockOn.x == 55 && (randomNumber < 0.2f && eleFactor < 136 + Math.abs(randomNumber * 250))) {
                                    Cube.generate(blockOn, x, elevation, z, (rockNoise < 0.05f ? 56 : 10), 0, (int) (1 + (rand.nextFloat() * (Utils.gradient((int) eleFactor, 131, 181, 0, 2)))));
                                } else if (blockOn.x == BlockTypes.getId(BlockTypes.SNOW)) {
                                    if (biome == Biomes.SNOWY_TAIGA.id) {
                                        if (randomNumber < 0.0005f || randomNumber < featureNoise / 50) {
                                            int maxHeight = rand.nextInt(19) + 5;
                                            PineTree.generate(rand, blockOn, x, elevation, z, maxHeight, true, BlockTypes.getId(BlockTypes.SPRUCE_LOG), 0, BlockTypes.getId(BlockTypes.SPRUCE_LEAVES), 0);
                                        } else if (randomNumber < 0.001f) {
                                            int maxHeight = rand.nextInt(6) + 12;
                                            SpruceTree.generate(rand, blockOn, x, elevation, z, maxHeight,true, BlockTypes.getId(BlockTypes.SPRUCE_LOG), 0, BlockTypes.getId(BlockTypes.SPRUCE_LEAVES), 0);
                                        }
                                    }
                                } else if (!snowy && blockOn.x == 2) {
                                    if (biome == Biomes.TROPICAL_ISLAND.id) {
                                        if (randomNumber < 0.0067f) {
                                            PalmTree.generate(rand, blockOn, x, elevation, z, rand.nextInt(8, 22), 25, 0, 27, 0);
                                        } else if (randomNumber < 0.02f+Math.max(0, 0.06f*featureNoiseSmall)) {
                                            PalmTree.generate(rand, blockOn, x, elevation, z, rand.nextInt(2, 3), 20, 0, 21, 0);
                                        }
                                    } else if (biome == Biomes.REDWOOD_FOREST.id) {
                                        if (randomNumber < 0.0004f) {
                                            Blob.generate(blockOn, x, elevation, z, 48, 0, (int) (2 + (rand.nextFloat() * 7)));
                                        } else if (randomNumber < 0.003) {
                                            int maxHeight = rand.nextInt(42, 54);
                                            int radius = rand.nextInt(3, 4);
                                            int leavesHeight = 3;
                                            int branchChance = rand.nextInt(4, 7);
                                            RedwoodTree.generate(rand, blockOn, x, elevation, z, maxHeight, radius, leavesHeight, BlockTypes.getId(BlockTypes.REDWOOD_LOG), 0, BlockTypes.getId(BlockTypes.REDWOOD_LEAVES), 0, branchChance);
                                        } else if (randomNumber < 0.0031f || randomNumber < featureNoise / 50) {
                                            int maxHeight = rand.nextInt(19) + 5;
                                            PineTree.generate(rand, blockOn, x, elevation, z, maxHeight, false, BlockTypes.getId(BlockTypes.SPRUCE_LOG), 0, BlockTypes.getId(BlockTypes.SPRUCE_LEAVES), 0);
                                        } else if (randomNumber < 0.0032f) {
                                            int maxHeight = rand.nextInt(6) + 12;
                                            SpruceTree.generate(rand, blockOn, x, elevation, z, maxHeight, false, BlockTypes.getId(BlockTypes.SPRUCE_LOG), 0, BlockTypes.getId(BlockTypes.SPRUCE_LEAVES), 0);
                                        }
                                    } else if (biome == Biomes.TAIGA.id) {
                                        if (randomNumber < 0.0004f) {
                                            Blob.generate(blockOn, x, elevation, z, 48, 0, (int) (2 + (rand.nextFloat() * 7)));
                                        } else if (randomNumber < 0.00045f || randomNumber < featureNoise / 50) {
                                            int maxHeight = rand.nextInt(19) + 5;
                                            PineTree.generate(rand, blockOn, x, elevation, z, maxHeight, false, BlockTypes.getId(BlockTypes.SPRUCE_LOG), 0, BlockTypes.getId(BlockTypes.SPRUCE_LEAVES), 0);
                                        } else if (randomNumber < 0.0015f) {
                                            int maxHeight = rand.nextInt(6) + 12;
                                            SpruceTree.generate(rand, blockOn, x, elevation, z, maxHeight, false, BlockTypes.getId(BlockTypes.SPRUCE_LOG), 0, BlockTypes.getId(BlockTypes.SPRUCE_LEAVES), 0);
                                        }
                                    } else if (biome == Biomes.CHERRY_GROVE.id) {
                                        if (randomNumber < 0.0004f) {
                                            Blob.generate(blockOn, x, elevation, z, 48, 0, (int) (2 + (rand.nextFloat() * 7)));
                                        } else if (randomNumber < 0.005f) {
                                            int maxHeight = rand.nextInt(16) + 12;
                                            int radius = rand.nextInt(2) + 3;
                                            boolean overgrown = rand.nextInt(4) == 0;
                                            JungleTree.generate(rand, blockOn, x, elevation, z, maxHeight, radius, BlockTypes.getId(BlockTypes.CHERRY_LOG), 0, BlockTypes.getId(BlockTypes.CHERRY_LEAVES), 0, overgrown);
                                        }
                                    } else {
                                        if (randomNumber < 0.0004f) {
                                            Blob.generate(blockOn, x, elevation, z, 48, 0, (int) (2 + (rand.nextFloat() * 7)));
                                        } else if (randomNumber < featureNoise / 10) {
                                            int maxHeight = rand.nextInt(25, 32);
                                            int radius = rand.nextInt(20, 32);
                                            int leavesHeight = 8;
                                            int branchChance = 1;
                                            GiantOakTree.generate(rand, blockOn, x, elevation, z, maxHeight, radius, leavesHeight, BlockTypes.getId(BlockTypes.OAK_LOG), 0, BlockTypes.getId(BlockTypes.OAK_LEAVES), 0, branchChance);
                                        } else if (randomNumber > 0.002 && randomNumber < 0.002125f) {
                                            int maxHeight = rand.nextInt(16) + 12;
                                            int radius = rand.nextInt(2) + 3;
                                            boolean overgrown = rand.nextInt(4) == 0;
                                            JungleTree.generate(rand, blockOn, x, elevation, z, maxHeight, radius, BlockTypes.getId(BlockTypes.CHERRY_LOG), 0, BlockTypes.getId(BlockTypes.CHERRY_LEAVES), 0, overgrown);
                                        } else if (randomNumber > 0.002125 && randomNumber < 0.002175f) {
                                            int maxHeight = (int) (rand.nextFloat() * 6) + 12;
                                            DeadOakTree.generate(rand, blockOn, x, elevation, z, maxHeight, 16, 0);
                                            Blob.generate(blockOn, x, elevation, z, BlockTypes.getId(BlockTypes.MUD), 0, (int) (2 + ((rand.nextFloat() + 1) * 3)), new int[]{2, 23}, true);
                                        }
                                    }
                                } else if ((blockOn.x == 73 && (biome == Biomes.DESERT.id || biome == Biomes.TEMPERATE.id)) || (biome == Biomes.TROPICAL_ISLAND.id && blockOn.x == 2)) {
                                    if (blockIn.x() != 1) {
                                        if (randomNumber < 0.0067f) {
                                            PalmTree.generate(rand, blockOn, x, elevation, z, rand.nextInt(8, 22), 25, 0, 27, 0);
                                        }
                                    }
                                } else if (blockOn.x == 23 && biome == Biomes.DESERT.id) {
                                    if (randomNumber < 0.002f) {
                                        double deadBushChance = rand.nextFloat();
                                        if (deadBushChance < 0.03) {
                                            int variant = deadBushChance < 0.015 ? 0 : 1;
                                            Blob.generate(blockOn, x, elevation - 3 + variant, z + variant, 24, 0, 3 + variant);
                                            Blob.generate(blockOn, x, elevation, z + variant, 24, 0, 2 + variant);
                                            Blob.generate(blockOn, x, elevation + 2 + variant, z + variant, 24, 0, 1);
                                            Blob.generate(blockOn, x, elevation + 4 + variant, z + 1 + variant, 24, 0, 1);
                                            variant *= 2;
                                            Blob.generate(blockOn, x, elevation + 7 + variant, z + 1 + variant, 24, 0, 2 + variant);
                                            Blob.generate(blockOn, x - 1 - variant, elevation + 7 + variant, z + variant, 24, 0, 2 + variant);
                                            Blob.generate(blockOn, x + 1 + variant, elevation + 7 + variant, z + variant, 24, 0, 2 + variant);
                                        } else if (deadBushChance < 0.2) {
                                            if (deadBushChance > 0.19) {
                                                int maxHeight = (int) (rand.nextFloat() * 6) + 12;
                                                DeadOakTree.generate(rand, blockOn, x, elevation, z, maxHeight, 16, 0);
                                                Blob.generate(blockOn, x, elevation, z, 33, 0, (int) (2 + ((rand.nextFloat() + 1) * 3)), new int[]{2, 23}, true);
                                            } else {
                                                setBlock(x, elevation + 1, z, 30, deadBushChance < 0.1 ? 0 : 1);
                                            }
                                        } else {
                                            Pillar.generate(blockOn, x, elevation + 1, z, (int) (rand.nextFloat() * 6) + 2, 29, 0);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        System.out.print("Took "+(System.currentTimeMillis()-startTime)+"ms to generate features. \n");

//        for (int x = 950; x < 1050; x++) {
//            for (int z = 950; z < 1050; z++) {
//                for (int y = 150; y < 250; y++) {
//                    World.setBlock(x, y, z, 1, 15);
//                }
//            }
//        }
        startTime = System.currentTimeMillis();
        threads = Math.min(Runtime.getRuntime().availableProcessors(), sizeChunks);
        pool = Executors.newFixedThreadPool(threads);
        int cloudInterval = (sizeChunks + threads - 1) / threads;
        for (int thread = 0; thread < threads; thread++) {
            int startX = thread * cloudInterval;
            int endX = Math.min(startX + cloudInterval, sizeChunks);
            pool.submit(() -> {
                Random rand = new Random(World.seed);
                for (int cX = startX; cX < endX; cX++) {
                    for (int cZ = 0; cZ < sizeChunks; cZ++) {
                        if (cX > 0 && cX < sizeChunks-1 && cZ > 0 && cZ < sizeChunks-1) { //skip outer chunks
                            for (int x = cX * chunkSize; x < (cX * chunkSize) + chunkSize; x++) {
                                for (int z = cZ * chunkSize; z < (cZ * chunkSize) + chunkSize; z++) {
                                    double cloudNoise = Math.abs(SimplexNoise.noise(x / 400.f, z / 400.f));
                                    double cloudSecondaryNoise = Math.abs(SimplexNoise.noise(x / 600.f, z / 600.f));
                                    if (cloudNoise < 0.4f && cloudSecondaryNoise > 0.5f && rand.nextFloat() > 0.95f && heightmap[packPos(x, z)] < 166) {
                                        int cloudHeight = 216 + (int) Math.abs(SimplexNoise.noise(x/800.f, z/800.f) * 84);
                                        boolean isRainCloud = rand.nextFloat() < 0.0005f;
                                        int radius = (int) ((((isRainCloud ? 6 : 0) + rand.nextInt(2, 6)) * (1+(150*Math.pow(0.4f-Math.min(0.4f, cloudNoise), 2))))/15);
                                        if (radius > 0) {
                                            Cube.generate(new Vector2i(0), x, cloudHeight, z, isRainCloud ? 32 : 31, 0, radius, true);
                                            Cube.generate(new Vector2i(0), size-x, cloudHeight+75, z, 31, 0, radius, true);
                                            Cube.generate(new Vector2i(0), x, cloudHeight+150, size-z, 31, 0, radius, true);
                                            Cube.generate(new Vector2i(0), size-x, cloudHeight+200, size-z, 31, 0, radius+1, true);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        System.out.print("Took "+(System.currentTimeMillis()-startTime)+"ms to generate clouds. \n");
        generating = false;
        System.out.print("Took "+(System.currentTimeMillis()-start)+"ms to generate world. \n");
    }
    public short getOldElevation(int x, int z) {
        double mountainNoise = Math.max(0, SimplexNoise.noise(x / 400.f, z / 400.f));
        double elevationNoise = SimplexNoise.noise(x / 500.f, z / 500.f) + 0.5f;
        double elevationMul = (mountainNoise * elevationNoise)+0.25F;
        double detailNoise = ((SimplexNoise.noise(x / 75.f, z / 75.f)+elevationNoise) * 16);
        double elevation = Math.abs(detailNoise * Math.max(-0.5f, elevationMul*(2.f+(3*SimplexNoise.noise(x/1250.f, z/1250.f)))));
        double centDist = new Vector2i(x, z).distance(2048, 2048);
        double riverness = (0.5f-Math.abs(Math.max(-1.f, Math.min(0, centDist-1024)/150)+0.5f))*3;
        double continentNoise = (-(Math.abs(elevationNoise-0.5f)-0.7f))-riverness;
        elevation += 66*Math.min(0, continentNoise);
        double hilLElevation = Math.max(0, SimplexNoise.noise(x / 800.f, z / 800.f)-0.25f)*150;
        elevation += 66+hilLElevation;
        return (short)Math.max(16, elevation);
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
//        for (int cX = 0; cX < sizeChunks; cX++) {
//            for (int cZ = 0; cZ < sizeChunks; cZ++) {
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
//        for (int cX = 0; cX < sizeChunks; cX++) {
//            for (int cZ = 0; cZ < sizeChunks; cZ++) {
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
