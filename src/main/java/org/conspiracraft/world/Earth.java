package org.conspiracraft.world;

import de.articdive.jnoise.generators.noise_parameters.simplex_variants.Simplex2DVariant;
import de.articdive.jnoise.generators.noise_parameters.simplex_variants.Simplex3DVariant;
import de.articdive.jnoise.generators.noise_parameters.simplex_variants.Simplex4DVariant;
import de.articdive.jnoise.modules.octavation.fractal_functions.FractalFunction;
import de.articdive.jnoise.pipeline.JNoise;
import org.conspiracraft.Main;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.entities.EntityTypes;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.world.shapes.*;
import org.conspiracraft.world.trees.*;
import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.lang.Math;
import java.lang.Runtime;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.conspiracraft.Main.timeNs;
import static org.conspiracraft.graphics.Renderer.drawCube;
import static org.conspiracraft.graphics.Renderer.pushUBO;
import static org.conspiracraft.world.World.*;

public class Earth extends WorldType {
    public Path getWorldPath() {return Path.of(Main.mainFolder+"world0/earth");}
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
        generating = true;
        long startTime = System.currentTimeMillis();
        final byte[] biomes = new byte[size*size];
        final short[] chunksMinElevations = new short[sizeChunks*sizeChunks];
        final short[] chunksMaxElevations = new short[sizeChunks*sizeChunks];
        final short[] lakesMaxElevations = new short[size*size];
        final Queue<Lake> lakes = new ConcurrentLinkedQueue<>();
        int threads = Math.min(Runtime.getRuntime().availableProcessors(), sizeChunks);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        final int heightmapInterval = sizeChunks/threads;
        for (int thread = 0; thread < threads; thread++) {
            final int threadId = thread;
            final int startX = thread * heightmapInterval;
            final int endX  = Math.min(startX + heightmapInterval, sizeChunks);
            pool.execute(() -> {
                final Random rand = new Random(World.seed+threadId);
                for (int cX = startX; cX < endX; cX++) {
                    for (int cZ = 0; cZ < sizeChunks; cZ++) {
                        short minElevation = (short) (height - 1);
                        short maxElevation = (short) 0;
                        for (int x = cX * chunkSize; x < (cX * chunkSize) + chunkSize; x++) {
                            for (int z = cZ * chunkSize; z < (cZ * chunkSize) + chunkSize; z++) {
                                double centDist = Math.clamp(Math.max(Math.abs(x - 2048), Math.abs(z - 2048)), 0, 2048) / 2048.f;
                                double seaDist = new Vector2i(size-750).distance(x, z);
                                double unseaness = (Math.clamp(seaDist, 300, 600)-300) / 300.f;
                                double seaness = 1-unseaness;
                                double uncoastalness = (Math.clamp(seaDist, 600, 900)-600) / 300.f;
                                double desertDist = new Vector2i(0).distance(x, z);
                                double undesertness = (Math.clamp(desertDist, 1000, 1200)-1000) / 200.f;
                                double palmyPlainness = 1-((Math.clamp(Math.min(new Vector2i(-750, 1250).distance(x, z), new Vector2i(1250, -750).distance(x, z)), 1000, 1200)-1000) / 200.f);
                                double desertness = 1-undesertness;
                                double unsavannaness = (Math.clamp(new Vector2i(1250, 0).distance(x, z), 1000, 1200)-1000) / 200.f;
                                double savannaness = 1-unsavannaness;
                                double ogMoutainness = SimplexNoise.noise(x / 500.f, z / 500.f);
                                //double riverness = Math.min(0, Math.abs(ogMoutainness)-0.25f)*50;
                                double mountainness = ogMoutainness;
                                double badlandness = 0;
                                if (mountainness < 0.f) {
                                    mountainness *= Utils.mix(-0.2f, -0.15f, undesertness);
                                } else {
                                    mountainness *= Math.max(undesertness, 0.4f);
                                    double badlandnessBase = savannaness*Math.max(1, 1 + (((savannaness - desertness) - 0.9f) * 100));
                                    double badlandsTerrain = Math.min((mountainness - 0.5f) * badlandnessBase, 1);
                                    badlandness = Math.clamp((mountainness - 0.45f) * badlandnessBase, 0, 1);
                                    mountainness *= Math.max(unsavannaness, Math.min(0.34f, badlandsTerrain));
                                    mountainness += badlandsTerrain*0.34f;
                                }
                                double elevationNoise = Math.max(savannaness, (noisePipeline.evaluateNoise((x - size) / 333.d, (z - size) / 333.d) +
                                        ((noisePipeline.evaluateNoise((x - size) / 125.d, (z - size) / 125.d) * 0.33f)))*undesertness*uncoastalness);
                                if (desertness > 0.f) {
                                    elevationNoise += noisePipeline.evaluateNoise((x - size) / 525.d, (z - size) / 525.d)*desertness;
                                }
                                double elevation = elevationNoise * (125 * Math.max(desertness/3, mountainness));
                                double midElevation = (0.15f - (Math.clamp(centDist, 0.2f, 0.35f) - 0.2f)) * 6.667f;
                                double baseHilliness = SimplexNoise.noise((x + size) / 500.f, (z + size) / 500.f);
                                if (baseHilliness < 0.f) {
                                    baseHilliness *= -0.5;
                                }
                                double hilliness = (Math.max(0, baseHilliness) * 35)  * midElevation * undesertness;
                                //riverness *= 1-centerElevation;
                                double detailNoise = SimplexNoise.noise(x / 100.f, z / 100.f);
                                double ogIslandsNoise = SimplexNoise.noise(x / 200.f, z / 200.f);
                                double islandsNoise = ogIslandsNoise;
                                if (islandsNoise < 0.f) {islandsNoise *= -4*(5*baseHilliness);}
                                double islands = ((60+(islandsNoise*10))+Math.max(0, (-islandsNoise)*Math.abs(elevationNoise*20)))*seaness;
                                short finalElevation = (short) Math.max(60, Math.max(islands, 10 + (56*Math.min(1, unseaness+Math.max(0, ogIslandsNoise*0.15f))) + (midElevation * 40) + Math.max(0, detailNoise*3) + hilliness + elevation));
                                double snowiness = Utils.gradient(finalElevation, 96, 120, 0, 1);
                                double centBiomeFactor = centDist + (elevationNoise * 0.05f);
                                double redwoodness = 1-Math.clamp(new Vector2f(halfSize+quarterSize, halfSize+quarterSize).distance(x, z)/quarterSize, 0, 1);
                                double volcanicness = 1-Math.clamp(new Vector2f(halfSize+quarterSize, halfSize+quarterSize).distance(x, z)/(quarterSize*1.2f), 0, 1);
                                boolean isDesert = Math.max(palmyPlainness-(desertDist/1500), desertness)-Math.abs(detailNoise * 0.25f) > 0;
                                boolean isBeach = uncoastalness < 0.3f;
                                boolean isIslands = seaness > 0.5f;
                                biomes[x * size + z] = (byte) (isBeach ? (isIslands ? Biomes.TROPICAL_ISLAND.id : Biomes.BEACH.id) : (badlandness > 0 ? Biomes.BADLANDS.id : (isDesert ? Biomes.DESERT.id : savannaness > 0 ? Biomes.SAVANNA.id : (palmyPlainness > 0 ? Biomes.PALMY_PLAINS.id : (((elevationNoise * ogMoutainness) + (detailNoise * 0.05f) > snowiness ? Biomes.SNOWY_PEAK.id : (centBiomeFactor < 0.2f ? (volcanicness > 0.f ? Biomes.VOLCANIC_SNOWY_TAIGA.id : Biomes.SNOWY_TAIGA.id) : (ogMoutainness > 0.1 ? Biomes.CHERRY_GROVE.id : (centBiomeFactor < 0.4f ? (redwoodness > 0.f ? Biomes.REDWOOD_FOREST.id : (volcanicness > 0.f ? Biomes.VOLCANIC_TAIGA.id : Biomes.TAIGA.id)) : Biomes.TEMPERATE.id)))))))));
                                heightmap[packPos(x, z)] = finalElevation;
                                minElevation = (short) Math.min(minElevation, finalElevation);
                                maxElevation = (short) Math.max(maxElevation, finalElevation);
                                if (finalElevation > 66 && rand.nextFloat() < 0.0001f*Math.max(0.1f, undesertness)) {
                                    lakes.add(new Lake(new Vector3i(x, finalElevation+1, z)));
                                }
                            }
                        }
                        chunksMinElevations[packChunkPos(cX, cZ)] = minElevation;
                        chunksMaxElevations[packChunkPos(cX, cZ)] = maxElevation;
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
            pool.execute(() -> {
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
                                    int packedCP = packChunkPos(x>>chunkBits, z>>chunkBits);
                                    chunksMaxElevations[packedCP] = (short) Math.max(lake.pos.y(), chunksMaxElevations[packedCP]);
                                    lakesMaxElevations[packedPos] = (short) Math.max(lake.pos.y(), lakesMaxElevations[packedPos]);
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
        final int surfaceInterval = (sizeChunks + threads - 1) / threads;
        for (int thread = 0; thread < threads; thread++) {
            final int threadId = thread;
            final int startX = thread * surfaceInterval;
            final int endX  = Math.min(startX + surfaceInterval, sizeChunks);
            pool.execute(() -> {
                final Random rand = new Random(World.seed+threadId);
                for (int cX = startX; cX < endX; cX++) {
                    for (int cZ = 0; cZ < sizeChunks; cZ++) {
                        final int minChunkElevation = chunksMinElevations[packChunkPos(cX, cZ)]>>chunkBits;
                        for (int cY = 0; cY < minChunkElevation; cY++) {
                            final int packedCP = World.packChunkPos(cX, cY, cZ);
                            final Chunk chunk = new Chunk(packedCP);
                            chunk.blockPalette.set(0, Chunk.packInts(BlockTypes.STONE.id, 0));
                            World.chunks[packedCP] = chunk;
                            updateRegion(cX, cY, cZ, false);
                            for (int x = cX * chunkSize; x < (cX * chunkSize) + chunkSize; x+=lodSize) {
                                for (int z = cZ * chunkSize; z < (cZ * chunkSize) + chunkSize; z+=lodSize) {
                                    for (int y = cY * chunkSize; y < (cY * chunkSize) + chunkSize; y+=lodSize) {
                                        int lodIdx = packLodPos(x >>lodBits, y >>lodBits, z >>lodBits);
                                        lods[lodIdx] = 0xFFFFFFFFFFFFFFFFL;
                                    }
                                }
                            }
                        }
                        final int maxChunkElevation = Math.max(seaLevel, chunksMaxElevations[packChunkPos(cX, cZ)])>>chunkBits;
                        for (int cY = minChunkElevation; cY <= maxChunkElevation; cY++) {
                            final int packedCP = World.packChunkPos(cX, cY, cZ);
                            final Chunk chunk = new Chunk(packedCP);
                            boolean setAnything = false;
                            for (int x = cX * chunkSize; x < (cX * chunkSize) + chunkSize; x++) {
                                for (int z = cZ * chunkSize; z < (cZ * chunkSize) + chunkSize; z++) {
                                    final int packedPos = packPos(x, z);
                                    final short elevation = heightmap[packedPos];
                                    final byte biome = biomes[packedPos];
                                    int maxSteepness = 0;
                                    if (biome != Biomes.DESERT.id) {
                                        for (int i = 0; i < xOffset.length; i++) {
                                            int packedOffPos = packPos(x + xOffset[i], z + zOffset[i]);
                                            if (packedOffPos >= 0 && packedOffPos < heightmap.length) {
                                                int nY = heightmap[packedOffPos];
                                                int steepness = Math.abs(elevation - nY);
                                                maxSteepness = Math.max(maxSteepness, steepness);
                                            }
                                        }
                                    }
                                    final boolean flat = maxSteepness < 4;
                                    final int floor = (cY * chunkSize);
                                    final int ceil = floor + chunkSize;
                                    final int seafloor = Math.min(elevation+1, ceil);
                                    final int seafloorAbove = Math.min(elevation+2, ceil);
                                    if (biome == Biomes.LAKE.id || elevation <= seaLevel) {
                                        int waterSurface = Math.min(Math.max(seaLevel, lakesMaxElevations[packedPos]-1), ceil);
                                        int waterSurfaceBelow = waterSurface-1;
                                        if (waterSurface > elevation) {
                                            if (waterSurface < ceil) {
                                                setAnything = true;
                                                updateLod(x, waterSurface, z, false);
                                                chunk.setBlock(x & 15, waterSurface & 15, z & 15, 1, 14);
                                            }
                                            for (int y = waterSurfaceBelow; y >= Math.max(floor, seafloor); y--) {
                                                setAnything = true;
                                                updateLod(x, y, z, false);
                                                chunk.setBlock(x & 15, y & 15, z & 15, 1, 15);
                                            }
                                        }
                                    }

                                    float foliageNoise = SimplexNoise.noise(x / 100.f, z / 100.f);
                                    for (int y = floor; y < seafloorAbove; y++) {
                                        final int block = flat ? Biomes.getSurfaceBlock(biome, elevation, y) : (biome == Biomes.BADLANDS.id ? Chunk.packInts(Biomes.getBadlandsBands(y+(int)(5*foliageNoise)), 0) : Chunk.packInts(BlockTypes.GRAVEL.id, 0));
                                        final int blockType = block >> 16;
                                        if (blockType > 0) {
                                            final int blockSubtype = block & 0xFFFF;
                                            if (y == seafloor) {
                                                if (blockType == BlockTypes.GRASS.id) {
                                                    if (rand.nextBoolean() && rand.nextFloat() < foliageNoise - 0.2f && biome != Biomes.SAVANNA.id) {
                                                        setAnything = true;
                                                        updateLod(x, y, z, false);
                                                        chunk.setBlock(x & 15, y & 15, z & 15, 5, rand.nextInt(3));
                                                    } else if (rand.nextFloat() < 0.003f) {
                                                        setAnything = true;
                                                        updateLod(x, y, z, false);
                                                        chunk.setBlock(x & 15, y & 15, z & 15, 18, rand.nextInt(3));
                                                    } else if (rand.nextFloat() < 0.3f && (rand.nextFloat() > foliageNoise || biome == Biomes.SAVANNA.id)) {
                                                        setAnything = true;
                                                        updateLod(x, y, z, false);
                                                        chunk.setBlock(x & 15, y & 15, z & 15, 4, (blockSubtype * 4) + rand.nextInt(3));
                                                    }
                                                }
                                            } else {
                                                setAnything = true;
                                                updateLod(x, y, z, false);
                                                chunk.setBlock(x & 15, y & 15, z & 15, blockType, blockSubtype);
                                            }
                                        }
                                    }
                                }
                            }
                            World.chunks[packedCP] = chunk;
                            if (setAnything) {updateRegion(cX, cY, cZ, false);}
                        }
                        for (int cY = maxChunkElevation+1; cY < heightChunks; cY++) {
                            int packedCP = World.packChunkPos(cX, cY, cZ);
                            World.chunks[packedCP] = new Chunk(packedCP);
                        }
                    }
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        System.out.print("Took "+(System.currentTimeMillis()-startTime)+"ms to generate surface. \n");

//        startTime = System.currentTimeMillis();
//        threads = Math.min(Runtime.getRuntime().availableProcessors(), sizeChunks);
//        pool = Executors.newFixedThreadPool(threads);
//        int featuresInterval = (sizeChunks + threads - 1) / threads;
//        for (int thread = 0; thread < threads; thread++) {
//            int threadId = thread;
//            int startX = thread * featuresInterval;
//            int endX  = Math.min(startX + featuresInterval, sizeChunks);
//            pool.execute(() -> {
//                final Random rand = new Random(World.seed+threadId);
//                for (int cX = startX; cX < endX; cX++) {
//                    for (int cZ = 0; cZ < sizeChunks; cZ++) {
//                        for (int x = cX * chunkSize; x < (cX * chunkSize) + chunkSize; x++) {
//                            for (int z = cZ * chunkSize; z < (cZ * chunkSize) + chunkSize; z++) {
//                                int elevation = heightmap[(x * size) + z];
//                                byte biome = biomes[x * size + z];
//                                Vector2i blockOn = getBlock(x, elevation, z);
//                                Vector2i blockIn = getBlock(x, elevation+1, z);
//                                float randomNumber = rand.nextFloat();
//                                float featureNoise = SimplexNoise.noise(x / 300.f, z / 300.f);
//                                float featureNoiseSmall = SimplexNoise.noise(x / 100.f, z / 100.f);
//                                double rockNoise = Math.abs(SimplexNoise.noise(x / 150.f, z / 150.f));
//                                double eleFactor = elevation + (rockNoise * 50);
//                                boolean snowy = eleFactor > 136;
//                                int springSand = rand.nextBoolean() ? BlockTypes.RED_SAND.id : BlockTypes.ORANGE_SAND.id;
//                                if (biome == Biomes.VOLCANIC_SNOWY_TAIGA.id) {
//                                    if (randomNumber < 0.0067f) {
//                                        Spring.generate(x, elevation, z, springSand, 0, (int) (10 + (rand.nextFloat() * 10)));
//                                    }
//                                } else if ((biome == Biomes.REDWOOD_FOREST.id || biome == Biomes.VOLCANIC_TAIGA.id) && (randomNumber < 0.0005f || randomNumber < 0.015f*(featureNoise-0.15f))) {
//                                    Spring.generate(x, elevation, z, springSand, 0, (int) (10 + (rand.nextFloat() * 10)));
//                                }
//                                if (blockOn.x == 55 && (randomNumber < 0.2f && eleFactor < 136 + Math.abs(randomNumber * 250))) {
//                                    Cube.generate(blockOn, x, elevation, z, (rockNoise < 0.05f ? 56 : 10), 0, (int) (1 + (rand.nextFloat() * (Utils.gradient((int) eleFactor, 131, 181, 0, 2)))));
//                                } else if (blockOn.x == BlockTypes.SNOW.id) {
//                                    if (biome == Biomes.SNOWY_TAIGA.id || biome == Biomes.VOLCANIC_SNOWY_TAIGA.id) {
//                                        if (randomNumber < 0.0005f || randomNumber < featureNoise / 50) {
//                                            int maxHeight = rand.nextInt(19) + 5;
//                                            PineTree.generate(rand, blockOn, x, elevation, z, maxHeight, true, BlockTypes.SPRUCE_LOG.id, 0, BlockTypes.SPRUCE_LEAVES.id, 0);
//                                        } else if (randomNumber < 0.001f) {
//                                            int maxHeight = rand.nextInt(6) + 12;
//                                            SpruceTree.generate(rand, blockOn, x, elevation, z, maxHeight,true, BlockTypes.SPRUCE_LOG.id, 0, BlockTypes.SPRUCE_LEAVES.id, 0);
//                                        }
//                                    }
//                                } else if (!snowy && blockOn.x == 2) {
//                                    if (biome == Biomes.PALMY_PLAINS.id) {
//                                        if (randomNumber < 0.0067f) {
//                                            PalmTree.generate(rand, blockOn, x, elevation, z, rand.nextInt(8, 22), 25, 0, 27, 0);
//                                        } else if (randomNumber < Math.max(0, 0.06f*featureNoiseSmall)-0.02f || randomNumber < 0.0071f) {
//                                            PalmTree.generate(rand, blockOn, x, elevation, z, rand.nextInt(2, 3), 20, 0, 21, 0);
//                                        }
//                                    } else if (biome == Biomes.TROPICAL_ISLAND.id) {
//                                        if (randomNumber < 0.0067f) {
//                                            PalmTree.generate(rand, blockOn, x, elevation, z, rand.nextInt(8, 22), 25, 0, 27, 0);
//                                        } else if (randomNumber < 0.02f+Math.max(0, 0.06f*featureNoiseSmall)) {
//                                            PalmTree.generate(rand, blockOn, x, elevation, z, rand.nextInt(2, 3), 20, 0, 21, 0);
//                                        }
//                                    } else if (biome == Biomes.REDWOOD_FOREST.id) {
//                                        if (randomNumber < 0.0004f) {
//                                            Blob.generate(blockOn, x, elevation, z, 48, 0, (int) (2 + (rand.nextFloat() * 7)));
//                                        } else if (randomNumber < 0.006) {
//                                            int maxHeight = rand.nextInt(42, 54);
//                                            int radius = rand.nextInt(3, 4);
//                                            int leavesHeight = 3;
//                                            int branchChance = rand.nextInt(4, 7);
//                                            RedwoodTree.generate(rand, blockOn, x, elevation, z, maxHeight, radius, leavesHeight, BlockTypes.REDWOOD_LOG.id, 0, BlockTypes.REDWOOD_LEAVES.id, 0, branchChance);
//                                        } else if (randomNumber < 0.0061f || randomNumber < featureNoise / 50) {
//                                            int maxHeight = rand.nextInt(19) + 5;
//                                            PineTree.generate(rand, blockOn, x, elevation, z, maxHeight, false, BlockTypes.SPRUCE_LOG.id, 0, BlockTypes.SPRUCE_LEAVES.id, 0);
//                                        } else if (randomNumber < 0.0062f) {
//                                            int maxHeight = rand.nextInt(6) + 12;
//                                            SpruceTree.generate(rand, blockOn, x, elevation, z, maxHeight, false, BlockTypes.SPRUCE_LOG.id, 0, BlockTypes.SPRUCE_LEAVES.id, 0);
//                                        }
//                                    } else if (biome == Biomes.TAIGA.id || biome == Biomes.VOLCANIC_TAIGA.id) {
//                                        if (randomNumber < 0.0004f) {
//                                            Blob.generate(blockOn, x, elevation, z, 48, 0, (int) (2 + (rand.nextFloat() * 7)));
//                                        } else if (randomNumber < 0.00045f || randomNumber < featureNoise / 50) {
//                                            int maxHeight = rand.nextInt(19) + 5;
//                                            PineTree.generate(rand, blockOn, x, elevation, z, maxHeight, false, BlockTypes.SPRUCE_LOG.id, 0, BlockTypes.SPRUCE_LEAVES.id, 0);
//                                        } else if (randomNumber < 0.0015f) {
//                                            int maxHeight = rand.nextInt(6) + 12;
//                                            SpruceTree.generate(rand, blockOn, x, elevation, z, maxHeight, false, BlockTypes.SPRUCE_LOG.id, 0, BlockTypes.SPRUCE_LEAVES.id, 0);
//                                        }
//                                    } else if (biome == Biomes.CHERRY_GROVE.id) {
//                                        if (randomNumber < 0.0004f) {
//                                            Blob.generate(blockOn, x, elevation, z, 48, 0, (int) (2 + (rand.nextFloat() * 7)));
//                                        } else if (randomNumber < 0.005f) {
//                                            int maxHeight = rand.nextInt(16) + 12;
//                                            int radius = rand.nextInt(2) + 3;
//                                            boolean overgrown = rand.nextInt(4) == 0;
//                                            JungleTree.generate(rand, blockOn, x, elevation, z, maxHeight, radius, BlockTypes.CHERRY_LOG.id, 0, BlockTypes.CHERRY_LEAVES.id, 0, overgrown);
//                                        }
//                                    } else if (biome == Biomes.SAVANNA.id) {
//                                        if (randomNumber < 0.0003f) {
//                                            Blob.generate(blockOn, x, elevation, z, 48, 0, (int) (2 + (rand.nextFloat() * 7)));
//                                        } else if (randomNumber < 0.0005f) {
//                                            int maxHeight = rand.nextInt(17, 20);
//                                            AcaciaTree.generate(rand, blockOn, x, elevation, z, maxHeight, BlockTypes.ACACIA_LOG.id, 0, BlockTypes.ACACIA_LEAVES.id, 0);
//                                        }
//                                    } else {
//                                        if (randomNumber < 0.0004f) {
//                                            Blob.generate(blockOn, x, elevation, z, 48, 0, (int) (2 + (rand.nextFloat() * 7)));
//                                        } else if (randomNumber < featureNoise / 10) {
//                                            int maxHeight = rand.nextInt(25, 32);
//                                            int radius = rand.nextInt(20, 32);
//                                            int leavesHeight = 8;
//                                            int branchChance = 1;
//                                            GiantOakTree.generate(rand, blockOn, x, elevation, z, maxHeight, radius, leavesHeight, BlockTypes.OAK_LOG.id, 0, BlockTypes.OAK_LEAVES.id, 0, branchChance);
//                                        } else if (randomNumber > 0.002 && randomNumber < 0.002125f) {
//                                            int maxHeight = rand.nextInt(16) + 12;
//                                            int radius = rand.nextInt(2) + 3;
//                                            boolean overgrown = rand.nextInt(4) == 0;
//                                            JungleTree.generate(rand, blockOn, x, elevation, z, maxHeight, radius, BlockTypes.CHERRY_LOG.id, 0, BlockTypes.CHERRY_LEAVES.id, 0, overgrown);
//                                        } else if (randomNumber > 0.002125 && randomNumber < 0.002175f) {
//                                            int maxHeight = (int) (rand.nextFloat() * 6) + 12;
//                                            DeadOakTree.generate(rand, blockOn, x, elevation, z, maxHeight, 16, 0);
//                                            Blob.generate(blockOn, x, elevation, z, BlockTypes.MUD.id, 0, (int) (2 + ((rand.nextFloat() + 1) * 3)), new int[]{2, 23}, true);
//                                        }
//                                    }
//                                } else if ((blockOn.x == BlockTypes.WET_SAND.id && biome == Biomes.DESERT.id) || ((blockOn.x == BlockTypes.SAND.id || blockOn.x == BlockTypes.WET_SAND.id) && (biome == Biomes.BEACH.id || biome == Biomes.TROPICAL_ISLAND.id || biome == Biomes.PALMY_PLAINS.id))) {
//                                    if (blockIn.x() != 1) {
//                                        if (randomNumber < 0.0067f*(blockOn.x == BlockTypes.SAND.id ? 0.25f : 1.f)) {
//                                            PalmTree.generate(rand, blockOn, x, elevation, z, rand.nextInt(8, 22), 25, 0, 27, 0);
//                                        }
//                                    }
//                                } else if ((blockOn.x == BlockTypes.SAND.id && biome == Biomes.DESERT.id) || (blockOn.x == BlockTypes.RED_SAND.id && biome == Biomes.BADLANDS.id)) {
//                                    if (biome == Biomes.BADLANDS.id) {
//                                        if (randomNumber < 0.0067f) {
//                                            PalmTree.generate(rand, blockOn, x, elevation, z, rand.nextInt(8, 22), 25, 0, 27, 0);
//                                        } else if (randomNumber < 0.02f+Math.max(0, 0.06f*featureNoiseSmall)) {
//                                            PalmTree.generate(rand, blockOn, x, elevation, z, rand.nextInt(2, 3), 20, 0, 21, 0);
//                                        }
//                                    }
//                                    if (randomNumber < 0.002f) {
//                                        double deadBushChance = rand.nextFloat();
//                                        if (deadBushChance < 0.03) {
//                                            int variant = deadBushChance < 0.015 ? 0 : 1;
//                                            Blob.generate(blockOn, x, elevation - 3 + variant, z + variant, 24, 0, 3 + variant);
//                                            Blob.generate(blockOn, x, elevation, z + variant, 24, 0, 2 + variant);
//                                            Blob.generate(blockOn, x, elevation + 2 + variant, z + variant, 24, 0, 1);
//                                            Blob.generate(blockOn, x, elevation + 4 + variant, z + 1 + variant, 24, 0, 1);
//                                            variant *= 2;
//                                            Blob.generate(blockOn, x, elevation + 7 + variant, z + 1 + variant, 24, 0, 2 + variant);
//                                            Blob.generate(blockOn, x - 1 - variant, elevation + 7 + variant, z + variant, 24, 0, 2 + variant);
//                                            Blob.generate(blockOn, x + 1 + variant, elevation + 7 + variant, z + variant, 24, 0, 2 + variant);
//                                        } else if (deadBushChance < 0.2) {
//                                            if (deadBushChance > 0.19) {
//                                                int maxHeight = (int) (rand.nextFloat() * 6) + 12;
//                                                DeadOakTree.generate(rand, blockOn, x, elevation, z, maxHeight, 16, 0);
//                                                Blob.generate(blockOn, x, elevation, z, 33, 0, (int) (2 + ((rand.nextFloat() + 1) * 3)), new int[]{2, 23}, true);
//                                            } else {
//                                                setBlock(x, elevation + 1, z, 30, deadBushChance < 0.1 ? 0 : 1);
//                                            }
//                                        } else {
//                                            Pillar.generate(blockOn, x, elevation + 1, z, (int) (rand.nextFloat() * 6) + 2, 29, 0);
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            });
//        }
//        pool.shutdown();
//        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
//        System.out.print("Took "+(System.currentTimeMillis()-startTime)+"ms to generate features. \n");

        startTime = System.currentTimeMillis();
        threads = Math.min(Runtime.getRuntime().availableProcessors(), sizeChunks);
        pool = Executors.newFixedThreadPool(threads);
        final int cloudInterval = (sizeChunks + threads - 1) / threads;
        for (int thread = 0; thread < threads; thread++) {
            final int threadId = thread;
            final int startX = thread * cloudInterval;
            final int endX = Math.min(startX + cloudInterval, sizeChunks);
            pool.execute(() -> {
                final Random rand = new Random(World.seed+threadId);
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
                                            Cloud.generate(x, cloudHeight, z, isRainCloud ? 32 : 31, 0, radius);
                                            Cloud.generate(size-x, cloudHeight+75, z, 31, 0, radius);
                                            Cloud.generate(x, cloudHeight+150, size-z, 31, 0, radius);
                                            Cloud.generate(size-x, cloudHeight+200, size-z, 31, 0, radius+1);
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

        startTime = System.currentTimeMillis();
        for (int cX = 0; cX < sizeChunks; cX++) {
            for (int cZ = 0; cZ < sizeChunks; cZ++) {
                boolean foundMax = false;
                for (short cY = (short) (heightChunks - 1); cY >= 0; cY--) {
                    Chunk chunk = World.chunks[packChunkPos(cX, cY, cZ)];
                    if (chunk.blockPalette != null) {
                        for (int data : chunk.blockPalette) {
                            Vector2i block = Chunk.unpackInt(data);
                            boolean obstructingHeightmap = BlockTypes.blockTypeMap.get(block.x()).obstructingHeightmap(block);
                            if (!foundMax && obstructingHeightmap) {
                                foundMax = true;
                                chunksMaxElevations[packChunkPos(cX, cZ)] = cY;
                                chunksMinElevations[packChunkPos(cX, cZ)] = cY;
                                break;
                            } else if (foundMax && !obstructingHeightmap) {
                                chunksMinElevations[packChunkPos(cX, cZ)] = cY;
                                break;
                            }
                        }
                    }
                }
            }
        }

        Arrays.fill(heightmap, (short) 0);

        threads = Math.min(Runtime.getRuntime().availableProcessors(), sizeChunks);
        pool = Executors.newFixedThreadPool(threads);
        final int heightInterval = (sizeChunks + threads - 1) / threads;
        for (int thread = 0; thread < threads; thread++) {
            final int startX = thread * heightInterval;
            final int endX = Math.min(startX + heightInterval, sizeChunks);
            pool.execute(() -> {
                for (int cX = startX; cX < endX; cX++) {
                    for (int cZ = 0; cZ < sizeChunks; cZ++) {
                        int packedHorizontalCP = packChunkPos(cX, cZ);
                        int maxCy = chunksMaxElevations[packedHorizontalCP];
                        int minCy = chunksMinElevations[packedHorizontalCP];
                        for (int cY = maxCy; cY >= minCy; cY--) {
                            Chunk chunk = World.chunks[packChunkPos(cX, cY, cZ)];
                            for (int x = 0; x < chunkSize; x++) {
                                for (int z = 0; z < chunkSize; z++) {
                                    for (int y = chunkSize - 1; y >= 0; y--) {
                                        int localPos = Chunk.condenseLocalPos(x, y, z);
                                        Vector2i block = chunk.getBlock(localPos);
                                        int pos = packPos((cX*chunkSize)+x, (cZ*chunkSize)+z);
                                        int gY = (cY*chunkSize)+y;
                                        short elevation = heightmap[pos];
                                        if (BlockTypes.blockTypeMap.get(block.x()).obstructingHeightmap(block)) {
                                            heightmap[pos] = (short) Math.max(elevation, gY);
                                            chunk.setLight(x, y, z, 0);
                                        } else if (gY <= elevation) {
                                            chunk.setLight(x, y, z, 0);
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
        System.out.print("Took "+(System.currentTimeMillis()-startTime)+"ms to update heightmap. \n");

        startTime = System.currentTimeMillis();
        threads = Math.min(Runtime.getRuntime().availableProcessors(), sizeChunks);
        pool = Executors.newFixedThreadPool(threads);
        final int lightInterval = (sizeChunks + threads - 1) / threads;
        for (int thread = 0; thread < threads; thread++) {
            final int startX = thread * lightInterval;
            final int endX = Math.min(startX + lightInterval, sizeChunks);
            pool.execute(() -> {
                for (int cX = startX; cX < endX; cX++) {
                    for (int cZ = 0; cZ < sizeChunks; cZ++) {
                        int packedHorizontalCP = packChunkPos(cX, cZ);
                        synchronized (chunks) {
                            int minY = chunksMinElevations[packedHorizontalCP] * chunkSize;
                            for (int x = cX * chunkSize; x < (cX * chunkSize) + chunkSize; x++) {
                                for (int z = cZ * chunkSize; z < (cZ * chunkSize) + chunkSize; z++) {
                                    int packedHorizontalPos = packPos(x, z);
                                    int maxY = heightmap[packedHorizontalPos];
                                    for (int y = maxY; y >= minY; y--) {
                                        LightHelper.updateLight(new Vector3i(x, y, z), getBlock(x, y, z), getLight(x, y, z));
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
        System.out.print("Took "+(System.currentTimeMillis()-startTime)+"ms to fill lighting. \n");
    }
}
