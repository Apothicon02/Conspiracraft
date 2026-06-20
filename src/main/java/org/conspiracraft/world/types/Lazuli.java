package org.conspiracraft.world.types;

import de.articdive.jnoise.generators.noise_parameters.simplex_variants.Simplex2DVariant;
import de.articdive.jnoise.generators.noise_parameters.simplex_variants.Simplex3DVariant;
import de.articdive.jnoise.generators.noise_parameters.simplex_variants.Simplex4DVariant;
import de.articdive.jnoise.generators.noisegen.worley.WorleyNoiseGenerator;
import de.articdive.jnoise.modules.octavation.fractal_functions.FractalFunction;
import de.articdive.jnoise.pipeline.JNoise;
import org.conspiracraft.Main;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.effects.Effect;
import org.conspiracraft.effects.Lightning;
import org.conspiracraft.space.Planet;
import org.conspiracraft.space.StarSystem;
import org.conspiracraft.world.*;
import org.conspiracraft.world.shapes.BevelledCube;
import org.conspiracraft.world.shapes.Blob;
import org.joml.*;

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
import static org.conspiracraft.world.LightHelper.iterateLightQueueMultithreaded;
import static org.conspiracraft.world.LightHelper.maxSunlightLevel;
import static org.conspiracraft.world.World.*;

public class Lazuli extends WorldType {
    public static class VeraSpace extends WorldType {
        @Override
        public Path getWorldPath() {return Path.of(Main.mainFolder+"world0/lazuli_space");}
        public Planet parent = StarSystem.planets[2];
        @Override
        public Planet getPlanet(){return parent;}
    }
    public VeraSpace space = new VeraSpace();
    @Override
    public WorldType space() {
        return space;
    }
    private final float longitude = (float) Math.toRadians(80.f);
    @Override
    public float getLongitude() {return longitude;}
    @Override
    public Planet getPlanet(){return StarSystem.planets[2];}
    @Override
    public float gravity() {return 0.04f;}
@Override
    public Path getWorldPath() {return Path.of(Main.mainFolder+"world0/lazuli");}
    public static Vector3f prevSunPos = new Vector3f(0, World.height*2, 0), sunPos = new Vector3f(0, World.height*2, 0),
            prevOliviusPos = new Vector3f(0, World.height*-2, 0), oliviusPos = new Vector3f(0, World.height*-2, 0), nearestLightning = new Vector3f();
    public static Vector4f sunsetColor = new Vector4f(1.f, 0.125f, 0.01f, 1);

    @Override
    public Vector4f getSkylight() {
        nearestLightning.set(-100000);
        for (Effect effect : effects) {
            if (effect instanceof Lightning lightning) {
                Vector3f lightningPos = lightning.pos;
                if (Main.player.pos.distance(lightningPos) <= Main.player.pos.distance(nearestLightning)) {
                    nearestLightning.set(lightningPos);
                }
            }
        }
        if (nearestLightning.x() >= 0) {
            skylightMul.set(0.35f, 0.0f, 1.0f);
            return new Vector4f(nearestLightning.x(), nearestLightning.y(), nearestLightning.z(), 4);
        }
        skylightMul.set(1);
        Vector4f skylight = new Vector4f(StarSystem.relativePos, 1);
        if (skylight.y() <= 0) {
            return new Vector4f(0);
        } else {
            return new Vector4f(skylight.x(), Math.max(height, skylight.y()), skylight.z(), skylight.w());
        }
    }
    
    @Override
    public float getFogginess() {return 1.9f;}
    @Override
    public Vector4f getAtmosphereColor() {return new Vector4f(0.f, 0.8f, 1.2f, 0.85f);}
    @Override
    public Vector4f getNightAtmosphereColor() {return new Vector4f(1.f, 0.f, 0.8f, 0.85f);}
    @Override
    public Vector4f getSunsetAtmosphereColor() {return new Vector4f(sunsetColor.x(), sunsetColor.y(), sunsetColor.z(), 0.85f);}
    @Override
    public Vector4f getDeepSunsetAtmosphereColor() {return new Vector4f(sunsetColor.x(), sunsetColor.y(), sunsetColor.z(), 0.85f);}
    @Override
    public void tick() {
        prevSunPos.set(sunPos);
        sunPos.set(0, size*2, 0);
        sunPos.rotateZ(timeNs/1000000000000.f);
        sunPos.rotateX(0.5f);
        sunPos.rotateY(2.f);
        sunPos.set(sunPos.x+(size/2f), sunPos.y, sunPos.z+(size/2f)+128);
        prevOliviusPos.set(oliviusPos);
        oliviusPos.set(0, size*-2, 0);
        oliviusPos.rotateZ(timeNs/1000000000000.f);
        oliviusPos.rotateX(-0.2f);
        oliviusPos.rotateY(-1.5f);
        oliviusPos.set(oliviusPos.x+(size/2f), oliviusPos.y, oliviusPos.z+(size/2f)+128);
    }

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

    public JNoise noisePipeline = JNoise.newBuilder().fastSimplex(3301, Simplex2DVariant.IMPROVE_X, Simplex3DVariant.IMPROVE_XY, Simplex4DVariant.IMPROVE_XYZ_IMPROVE_XZ)
            .octavate(4,1,1.25f, FractalFunction.RIDGED_MULTI,false).build();
    public WorleyNoiseGenerator worleyNoisePipeline = WorleyNoiseGenerator.newBuilder().setSeed(5315).build();

    static final int[] xOffset = { 3, -3, 0, 0, 3, -3, -3, 3 };
    static final int[] zOffset = { 0, 0, 3, -3, 3, -3, 3, -3 };
    @Override
    public void generate() throws InterruptedException {
        generating = true;
        long startTime = System.currentTimeMillis();
        Random seededRand = new Random(35311350L);
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
                                double elevationNoise = noisePipeline.evaluateNoise((x - size) / 250.d, (z - size) / 250.d);
                                double hilliness = SimplexNoise.noise(x / 750.f, z / 750.f);
                                double surface = 64+(elevationNoise * (32+(hilliness*20)));
                                boolean badlands = false;
                                if (hilliness > 0.5f) {
                                    surface *= 1+(2.3f*(Math.min(0.15f, hilliness-0.5)-Math.clamp((hilliness-0.75)/3, 0, 0.05f)));
                                    if (hilliness > 0.6f) {
                                        double bonus = hilliness > 0.925f ? 1.f+(50*Math.min(0.005f, hilliness-0.925f)) : 1.f;
                                        double badlandsSurface = 64 + (1000 * bonus * Math.min(0.05f, hilliness - 0.6)) + (9 * elevationNoise);
                                        if (badlandsSurface > surface) {
                                            surface = badlandsSurface;
                                            badlands = true;
                                        }
                                    }
                                }

                                byte biome = badlands ? Biomes.LAZULI_BADLANDS.id : (elevationNoise > 0.5f && hilliness > 0.5f ? Biomes.LAZULI_RIDGES.id : Biomes.LAZULI_DUNES.id);
                                biomes[x * size + z] = biome;
                                heightmap[packPos(x, z)] = (short)surface;
                                minElevation = (short) Math.min(minElevation, surface);
                                maxElevation = (short) Math.max(maxElevation, surface);
                                if (biome == Biomes.LAZULI_DUNES.id && rand.nextFloat() < 0.0001f) {
                                    lakes.add(new Lake(new Vector3i(x, (int) (surface+1), z)));
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
                            chunk.blockPalette.set(0, Chunk.packInts(BlockTypes.OBSIDIAN.id, 0));
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
                                    final int waterSurface = lakesMaxElevations[packedPos]-1;
                                    final short elevation = heightmap[packedPos];
                                    final byte biome = biomes[packedPos];
                                    int maxSteepness = 0;
                                    for (int i = 0; i < xOffset.length; i++) {
                                        int packedOffPos = packPos(x + xOffset[i], z + zOffset[i]);
                                        if (packedOffPos >= 0 && packedOffPos < heightmap.length) {
                                            int nY = heightmap[packedOffPos];
                                            int steepness = Math.abs(elevation - nY);
                                            maxSteepness = Math.max(maxSteepness, steepness);
                                        }
                                    }
                                    final boolean flat = maxSteepness < 3;
                                    final int floor = (cY * chunkSize);
                                    final int ceil = floor + chunkSize;
                                    final int seafloor = Math.min(elevation+1, ceil);
                                    final int seafloorAbove = Math.min(elevation+2, ceil);
                                    double rockNoise = Math.abs(SimplexNoise.noise(x / 150.f, z / 150.f));
                                    double eleFactor = elevation + (rockNoise * 50);
                                    boolean snowy = eleFactor > 136;
                                    int lapisAzurine = rockNoise > 0.67f || rockNoise < 0.15f ? BlockTypes.AZURINE.id : (rockNoise > 0.4f && rockNoise < 0.5f ? BlockTypes.ORANGE_SAND.id : BlockTypes.LAPIS.id);
                                    for (int y = floor; y < Math.max(waterSurface, seafloorAbove); y++) {
                                        final int blockType = y >= seafloorAbove ? BlockTypes.OBSIDIAN.id : biome == Biomes.LAZULI_BADLANDS.id ? (flat ? (rockNoise < 0.3f ? (rockNoise < 0.15f ? (y >= (seafloor-1)-(100*(0.15f-rockNoise)) ? (y >= 112 ? 0 : BlockTypes.OBSIDIAN.id) : BlockTypes.ORANGE_SAND.id) : BlockTypes.TURQUOISE.id) : lapisAzurine) : lapisAzurine) : (biome == Biomes.LAZULI_RIDGES.id ? BlockTypes.ORANGE_SAND.id : BlockTypes.OBSIDIAN_DUST.id);
                                        if (blockType > 0) {
                                            final int lX = x & 15, lY = y & 15, lZ = z & 15;
                                            setAnything = true;
                                            updateLod(x, y, z, false);
                                            chunk.setBlock(lX, lY, lZ, blockType, 0);
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

        startTime = System.currentTimeMillis();
        threads = Math.min(Runtime.getRuntime().availableProcessors(), sizeChunks);
        pool = Executors.newFixedThreadPool(threads);
        int featuresInterval = (sizeChunks + threads - 1) / threads;
        for (int thread = 0; thread < threads; thread++) {
            int threadId = thread;
            int startX = thread * featuresInterval;
            int endX  = Math.min(startX + featuresInterval, sizeChunks);
            pool.execute(() -> {
                final Random rand = new Random(World.seed+threadId);
                for (int cX = startX; cX < endX; cX++) {
                    for (int cZ = 0; cZ < sizeChunks; cZ++) {
                        for (int x = cX * chunkSize; x < (cX * chunkSize) + chunkSize; x++) {
                            for (int z = cZ * chunkSize; z < (cZ * chunkSize) + chunkSize; z++) {
                                int elevation = heightmap[(x * size) + z];
                                byte biome = biomes[x * size + z];
                                Vector2i blockOn = getBlock(x, elevation, z);
                                float randomNumber = rand.nextFloat();
                                boolean onOrangeSand = blockOn.x() == BlockTypes.ORANGE_SAND.id;
                                if (biome != Biomes.LAZULI_BADLANDS.id && randomNumber < (onOrangeSand ? 0.0002f : 0.0001f)) {
                                    BevelledCube.generate(x, elevation, z, BlockTypes.SANDSTONE.id, 0, (int) (2 + (rand.nextFloat() * 14)));
                                }
                                boolean onObsidianOrLapis = biome == Biomes.LAZULI_BADLANDS.id && (blockOn.x() == BlockTypes.OBSIDIAN.id || blockOn.x() == BlockTypes.LAPIS.id);
                                if (randomNumber < (onObsidianOrLapis ? 0.0034f : 0.f)) {
                                    Blob.generate(blockOn, x, elevation+1, z, rand.nextFloat() < 0.935f ? 0 : BlockTypes.LAPIS.id, 0, (int) (1 + (rand.nextFloat() * 2)));
                                }
                                boolean onTurquoise = blockOn.x() == BlockTypes.TURQUOISE.id;
                                boolean onRedSand = blockOn.x() == BlockTypes.RED_SAND.id;
                                if ((randomNumber < 0.005f && onTurquoise || (randomNumber < 0.0002f && onRedSand))) {
                                    Blob.generate(blockOn, x, elevation, z, BlockTypes.MARBLE.id, 0, (int) (1 + (rand.nextFloat() * 2)));
                                }
                                if (blockOn.x() == BlockTypes.AZURINE.id && randomNumber < 0.1f) {
                                    Blob.generate(blockOn, x, elevation, z, BlockTypes.AZURINE.id, 0, (int) (1 + (rand.nextFloat() * 3)));
                                } else if (!onRedSand && (randomNumber > 0.9f && randomNumber < 0.90015f)) {
                                    Blob.generate(blockOn, x, elevation, z, rand.nextBoolean() ? BlockTypes.AZURINE.id : BlockTypes.TURQUOISE.id, 0, (int) (1 + (rand.nextFloat() * 3)));
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

//        startTime = System.currentTimeMillis();
//        threads = Math.min(Runtime.getRuntime().availableProcessors(), sizeChunks);
//        pool = Executors.newFixedThreadPool(threads);
//        final int cloudInterval = (sizeChunks + threads - 1) / threads;
//        for (int thread = 0; thread < threads; thread++) {
//            final int threadId = thread;
//            final int startX = thread * cloudInterval;
//            final int endX = Math.min(startX + cloudInterval, sizeChunks);
//            pool.execute(() -> {
//                final Random rand = new Random(World.seed+threadId);
//                for (int cX = startX; cX < endX; cX++) {
//                    for (int cZ = 0; cZ < sizeChunks; cZ++) {
//                        if (cX > 0 && cX < sizeChunks-1 && cZ > 0 && cZ < sizeChunks-1) { //skip outer chunks
//                            for (int x = cX * chunkSize; x < (cX * chunkSize) + chunkSize; x++) {
//                                for (int z = cZ * chunkSize; z < (cZ * chunkSize) + chunkSize; z++) {
//                                    double cloudNoise = Math.abs(SimplexNoise.noise(x / 400.f, z / 400.f));
//                                    double cloudSecondaryNoise = Math.abs(SimplexNoise.noise(x / 600.f, z / 600.f));
//                                    if (cloudNoise < 0.4f && cloudSecondaryNoise > 0.5f && rand.nextFloat() > 0.95f && heightmap[packPos(x, z)] < 166) {
//                                        int cloudHeight = 216 + (int) Math.abs(SimplexNoise.noise(x/800.f, z/800.f) * 84);
//                                        boolean isRainCloud = rand.nextFloat() < 0.0005f;
//                                        int radius = (int) ((((isRainCloud ? 6 : 0) + rand.nextInt(2, 6)) * (1+(150*Math.pow(0.4f-Math.min(0.4f, cloudNoise), 2))))/15);
//                                        if (radius > 0) {
//                                            Cloud.generate(x, cloudHeight, z, isRainCloud ? 32 : 31, 0, radius);
//                                            Cloud.generate(size-x, cloudHeight+75, z, 31, 0, radius);
//                                            Cloud.generate(x, cloudHeight+150, size-z, 31, 0, radius);
//                                            Cloud.generate(size-x, cloudHeight+200, size-z, 31, 0, radius+1);
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
//        System.out.print("Took "+(System.currentTimeMillis()-startTime)+"ms to generate clouds. \n");

        startTime = System.currentTimeMillis();
        for (int cX = 0; cX < sizeChunks; cX++) {
            for (int cZ = 0; cZ < sizeChunks; cZ++) {
                boolean foundMax = false;
                for (short cY = (short) (heightChunks - 1); cY >= 0; cY--) {
                    Chunk chunk = World.chunks[packChunkPos(cX, cY, cZ)];
                    if (chunk.blockPalette != null) {
                        for (int data : chunk.blockPalette) {
                            Vector2i block = Chunk.unpackInt(data);
                            boolean obstructingHeightmap = BlockTypes.blockTypes[block.x()].obstructingHeightmap(block);
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
                                        if (BlockTypes.blockTypes[block.x()].obstructingHeightmap(block)) {
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
                        int minY = chunksMinElevations[packedHorizontalCP] * chunkSize;
                        for (int x = cX * chunkSize; x < (cX * chunkSize) + chunkSize; x++) {
                            for (int z = cZ * chunkSize; z < (cZ * chunkSize) + chunkSize; z++) {
                                int packedHorizontalPos = packPos(x, z);
                                int maxY = heightmap[packedHorizontalPos];
                                boolean prevBlocking = false;
                                for (int y = maxY; y >= minY; y--) {
                                    Vector2i block = World.getBlock(x, y, z);
                                    boolean blocking = BlockTypes.blockTypes[block.x()].obstructingHeightmap(block);
                                    if (prevBlocking && !blocking) {
                                        if (getLight(x, y, z).s() == 0 && (getLight(x + 1, y, z).s() >= maxSunlightLevel || getLight(x, y, z + 1).s() >= maxSunlightLevel || getLight(x - 1, y, z).s() >= maxSunlightLevel || getLight(x, y, z - 1).s() >= maxSunlightLevel)) {
                                            LightHelper.queueLightUpdate(new Vector3i(x, y, z));
                                        }
                                    }
                                    prevBlocking = blocking;
                                }
                            }
                        }
                    }
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        System.out.print("Took "+(System.currentTimeMillis()-startTime)+"ms to init light queue. \n");
        startTime = System.currentTimeMillis();
        iterateLightQueueMultithreaded();
        System.out.print("Took "+(System.currentTimeMillis()-startTime)+"ms to fill lighting. \n");
    }
}
