package org.conspiracraft.world;

import de.articdive.jnoise.generators.noise_parameters.simplex_variants.Simplex2DVariant;
import de.articdive.jnoise.generators.noise_parameters.simplex_variants.Simplex3DVariant;
import de.articdive.jnoise.generators.noise_parameters.simplex_variants.Simplex4DVariant;
import de.articdive.jnoise.generators.noisegen.worley.WorleyNoiseGenerator;
import de.articdive.jnoise.modules.octavation.fractal_functions.FractalFunction;
import de.articdive.jnoise.pipeline.JNoise;
import org.conspiracraft.Main;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.blocks.types.LightBlockType;
import org.conspiracraft.effects.Effect;
import org.conspiracraft.effects.Lightning;
import org.conspiracraft.entities.EntityTypes;
import org.conspiracraft.space.Planet;
import org.conspiracraft.space.StarSystem;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.world.shapes.Blob;
import org.conspiracraft.world.shapes.Cloud;
import org.conspiracraft.world.shapes.Cube;
import org.conspiracraft.world.trees.WillowTree;
import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.lang.Math;
import java.lang.Runtime;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.conspiracraft.Main.timeNs;
import static org.conspiracraft.graphics.Renderer.drawCube;
import static org.conspiracraft.graphics.Renderer.pushUBO;
import static org.conspiracraft.world.LightHelper.iterateLightQueueMultithreaded;
import static org.conspiracraft.world.LightHelper.maxSunlightLevel;
import static org.conspiracraft.world.World.*;

public class Aksala extends WorldType {
    private final float longitude = (float) Math.toRadians(15.f);
    @Override
    public float getLongitude() {return longitude;}
    @Override
    public Planet getPlanet(){return StarSystem.planets[1];}
    @Override
    public float gravity() {return 1.f;}
    public Path getWorldPath() {return Path.of(Main.mainFolder+"world0/aksala");}
    public static Vector3f prevSunPos = new Vector3f(0, World.height*2, 0), sunPos = new Vector3f(0, World.height*2, 0),
            prevOliviusPos = new Vector3f(0, World.height*-2, 0), oliviusPos = new Vector3f(0, World.height*-2, 0), nearestLightning = new Vector3f();
    public static Vector4f oliviusColor = new Vector4f(0.34f, 0.949f, 0.475f, 1);

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
            skylightMul.set(1.f, 0.1f, 0.0f);
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
    public float getFogginess() {return 1.3f;}
    @Override
    public Vector4f getAtmosphereColor() {return new Vector4f(0.36f, 0.54f, 1.2f, 1.f);}
    @Override
    public Vector4f getNightAtmosphereColor() {return new Vector4f(0.3f, 0.06f, 1.2f, 1.f);}
    @Override
    public Vector4f getSunsetAtmosphereColor() {return new Vector4f(1, 0.65f, 0.25f, 1.f);}
    @Override
    public Vector4f getDeepSunsetAtmosphereColor() {return new Vector4f(1, 0.3f, 0.25f, 1.f);}
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
    public JNoise noisePipeline = JNoise.newBuilder().fastSimplex(1301, Simplex2DVariant.IMPROVE_X, Simplex3DVariant.IMPROVE_XY, Simplex4DVariant.IMPROVE_XYZ_IMPROVE_XZ)
            .octavate(4,1,1.25f, FractalFunction.RIDGED_MULTI,false).build();
    public WorleyNoiseGenerator worleyNoisePipeline = WorleyNoiseGenerator.newBuilder().setSeed(5315).build();

    static final int[] xOffset = { 3, -3, 0, 0, 3, -3, -3, 3 };
    static final int[] zOffset = { 0, 0, 3, -3, 3, -3, 3, -3 };
    @Override
    public void generate() throws InterruptedException {
        generating = true;
        long startTime = System.currentTimeMillis();
        Random seededRand = new Random(35311350L);
        Vector3i[] craters = new Vector3i[200];
        for (int i = 0; i < craters.length; i++) {
            int radius = seededRand.nextInt(90) + 10;
            int borderOffset = radius*2;
            int x = seededRand.nextInt(size-borderOffset) + (borderOffset/2);
            int z = seededRand.nextInt(size-borderOffset) + (borderOffset/2);
            craters[i] = new Vector3i(x, radius, z);
        }
        final byte[] biomes = new byte[size*size];
        final short[] chunksMinElevations = new short[sizeChunks*sizeChunks];
        final short[] chunksMaxElevations = new short[sizeChunks*sizeChunks];
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
                        short maxElevation = (short) 63;
                        for (int x = cX * chunkSize; x < (cX * chunkSize) + chunkSize; x++) {
                            for (int z = cZ * chunkSize; z < (cZ * chunkSize) + chunkSize; z++) {
                                double simplex = SimplexNoise.noise(x / 400.f, z / 400.f);
                                double hilliness = Math.max(0, SimplexNoise.noise(x / 1500.f, z / 1500.f)+(simplex/4));
                                double elevationNoise = hilliness < 0.2f ? 0.d : ((hilliness-0.2)*(noisePipeline.evaluateNoise((x - size) / 450.d, (z - size) / 450.d)));
                                double detail = SimplexNoise.noise(x / 150.f, z / 200.f)+(SimplexNoise.noise(x / 25.f, z / 25.f)*Math.min(2, hilliness*40));
                                int surface = (int) (63+(detail*4)+(hilliness*200)+(elevationNoise*250));

                                biomes[x * size + z] = elevationNoise > 0 ? Biomes.AKSALA_PEAKS.id : Biomes.AKSALA.id;
                                heightmap[packPos(x, z)] = (short)surface;
                                minElevation = (short) Math.min(minElevation, surface);
                                maxElevation = (short) Math.max(maxElevation, surface);
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
                            chunk.blockPalette.set(0, Chunk.packInts(BlockTypes.BASALT.id, 0));
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
                                    if (biome == Biomes.AKSALA_PEAKS.id) {
                                        for (int i = 0; i < xOffset.length; i++) {
                                            int packedOffPos = packPos(x + xOffset[i], z + zOffset[i]);
                                            if (packedOffPos >= 0 && packedOffPos < heightmap.length) {
                                                int nY = heightmap[packedOffPos];
                                                int steepness = Math.abs(elevation - nY);
                                                maxSteepness = Math.max(maxSteepness, steepness);
                                            }
                                        }
                                    }
                                    final boolean flat = maxSteepness < 5;
                                    final int floor = (cY * chunkSize);
                                    final int ceil = floor + chunkSize;
                                    final int seafloor = Math.min(elevation+1, ceil);
                                    final int seafloorAbove = Math.min(elevation+2, ceil);
                                    for (int y = floor; y < seafloorAbove; y++) {
                                        final int lX = x & 15, lY = y & 15, lZ = z & 15;
                                        setAnything = true;
                                        updateLod(x, y, z, false);
                                        chunk.setBlock(lX, lY, lZ, y > 60 ? flat ? BlockTypes.SNOW.id : BlockTypes.BASALT.id : BlockTypes.BASALT.id, 0);
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

        final Random islandRand = new Random(World.seed);
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                double erosion = (noisePipeline.evaluateNoise(x / 1500.d, z / 1500.d)-0.2f)*2;
                if (erosion > 0.f) {
                    double stalacites = noisePipeline.evaluateNoise(z / 100.d, x / 100.d);
                    double simplex = SimplexNoise.noise(x / 400.f, z / 400.f);
                    double hilliness = Math.max(0, (SimplexNoise.noise(x / 1500.f, z / 1500.f)+(simplex/4))*75);
                    int offset = (int) ((simplex*20)+hilliness-240);
                    int ogMinElevation = (int) (400-(erosion * 200))+offset;
                    int stalaciteLength = (int) Math.max(0, stalacites*20);
                    int minElevation = ogMinElevation-stalaciteLength;
                    int maxElevation = (int) ((Math.sqrt(erosion) * 75) + 300)+offset;
                    if (maxElevation > minElevation) {
                        World.setBlock(x, maxElevation, z, BlockTypes.SNOW.id, 0);
                        heightmap[World.packPos(x, z)] = (short) maxElevation;
                        for (int y = maxElevation-1; y >= minElevation; y--) {
                            World.setBlock(x, y, z, BlockTypes.ICE.id, 0);
                        }
                        float cloudChance = islandRand.nextFloat();
                        double cloudNoise = SimplexNoise.noise((x / 200.f)+(z/100.f), ogMinElevation / 75.f);
                        if (Math.abs(cloudNoise) < 0.05f && cloudChance < 0.34f) {
                            int blockType = maxElevation-minElevation > 42 ? BlockTypes.KYANITE.id : BlockTypes.ICE.id;
                            World.setBlock(x, minElevation, z, blockType, 0);
                            World.setBlock(x, minElevation-1, z, blockType, 0);
                            World.setBlock(x, minElevation-2, z, blockType, 0);
                            World.setBlock(x, minElevation-3, z, blockType, 0);
                            World.setBlock(x, minElevation-4, z, blockType, 0);
                            if (blockType == BlockTypes.KYANITE.id) {
                                LightHelper.queueLightUpdate(new Vector3i(x, minElevation-4, z));
                            }
                        }
                    }
                }
            }
        }
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
                                //byte biome = biomes[x * size + z];
                                Vector2i blockOn = getBlock(x, elevation, z);
                                float randomNumber = rand.nextFloat();
                                boolean onSnow = blockOn.x() == BlockTypes.SNOW.id;
                                if (randomNumber < (onSnow ? 0.0005f : 0.f)) {
                                    Blob.generate(blockOn, x, elevation, z, BlockTypes.ICE.id, 0, (int) (2 + (rand.nextFloat() * 14)));
                                } else if (blockOn.x() == BlockTypes.BASALT.id && randomNumber < 0.2f) {
                                    World.setBlock(x, elevation+1, z, BlockTypes.BASALT.id, 0);
                                    if (randomNumber < 0.09f) {
                                        World.setBlock(x, elevation+2, z, BlockTypes.BASALT.id, 0);
                                        if (randomNumber < 0.03f) {
                                            World.setBlock(x, elevation+3, z, BlockTypes.BASALT.id, 0);
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
                                        int radius = (int) ((((rand.nextFloat() < 0.0005f ? 6 : 0) + rand.nextInt(2, 6)) * (1+(150*Math.pow(0.4f-Math.min(0.4f, cloudNoise), 2))))/15);
                                        if (radius > 0) {
                                            Cloud.generate(x, cloudHeight, z, BlockTypes.KYANITE.id, 0, radius);
                                            Cloud.generate(size-x, cloudHeight+75, z, BlockTypes.CLOUD.id, 0, radius);
                                            Cloud.generate(x, cloudHeight+150, size-z, BlockTypes.KYANITE.id, 0, radius);
                                            Cloud.generate(size-x, cloudHeight+200, size-z, BlockTypes.CLOUD.id, 0, radius+1);
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
                                    if ((prevBlocking && !blocking) || BlockTypes.blockTypes[block.x()] instanceof LightBlockType) {
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
