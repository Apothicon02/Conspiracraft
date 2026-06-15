package org.conspiracraft.world;

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
import org.conspiracraft.utils.Utils;
import org.conspiracraft.world.shapes.*;
import org.conspiracraft.world.trees.*;
import org.joml.*;

import java.lang.Math;
import java.lang.Runtime;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.conspiracraft.world.LightHelper.iterateLightQueueMultithreaded;
import static org.conspiracraft.world.LightHelper.maxSunlightLevel;
import static org.conspiracraft.world.World.*;

public class Marb extends WorldType {
    private final float longitude = (float) Math.toRadians(40.f);
    @Override
    public float getLongitude() {return longitude;}
    public Planet parent = StarSystem.planets[0];
    @Override
    public Planet getPlanet(){return parent.moons[0];}
    public Path getWorldPath() {return Path.of(Main.mainFolder+"world0/marb");}
    public static Vector3f nearestLightning = new Vector3f();
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
            skylightMul.set(1.f, 0.95f, 0.f);
            return new Vector4f(nearestLightning.x(), nearestLightning.y(), nearestLightning.z(), 4);
        }
        skylightMul.set(1);
        return (StarSystem.relativePos.y() < 0 && StarSystem.relativePos.y() < parent.pos.y() ? new Vector4f(parent.pos, 1.5f) : new Vector4f(StarSystem.relativePos, 1)).max(new Vector4f(0, height, 0, 0));
    }
    @Override
    public Vector4f getAtmosphereColor() {return new Vector4f(1.f, 1.f, 1.f, 0.f);}
    @Override
    public Vector4f getNightAtmosphereColor() {return new Vector4f(oliviusColor.x(), oliviusColor.y(), oliviusColor.z(), 0.f);}
    @Override
    public Vector4f getSunsetAtmosphereColor() {return new Vector4f(oliviusColor.x(), oliviusColor.y(), oliviusColor.z(), 0.f);}
    @Override
    public Vector4f getDeepSunsetAtmosphereColor() {return new Vector4f(oliviusColor.x(), oliviusColor.y(), oliviusColor.z(), 0.f);}
    @Override
    public void tick() {}
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
        Vector3i[] craters = new Vector3i[200];
        for (int i = 0; i < craters.length; i++) {
            int radius = (int) ((seededRand.nextInt(90)*Math.min(1, seededRand.nextFloat()+0.5f)) + 10);
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
                        short maxElevation = (short) 0;
                        for (int x = cX * chunkSize; x < (cX * chunkSize) + chunkSize; x++) {
                            for (int z = cZ * chunkSize; z < (cZ * chunkSize) + chunkSize; z++) {
                                float basePerlinNoise = (SimplexNoise.noise(x / 1250.f, z / 1250.f)+1)*2;
                                float detailNoise = (SimplexNoise.noise(x / 150.f, z / 150.f) + 0.5f) / 2;
                                double baseCellularNoise = worleyNoisePipeline.evaluateNoise(x / 1000.f, z / 1000.f) / 2;
                                int surface = (int) (((100 * (Math.max(0.1f, baseCellularNoise) * (detailNoise+basePerlinNoise))) + 70));
                                double craterSurfMul = 1.f;
                                double craterSurfMaxMul = 1.f;
                                boolean inCrater = false;
                                for (Vector3i crater : craters) {
                                    double craterDist = Utils.distance(crater.x(), crater.z(), x, z);
                                    int radius = crater.y();
                                    if (craterDist < radius) {
                                        inCrater = true;
                                        craterDist /= radius;
                                        craterDist = Math.pow(craterDist, 2);
                                        craterDist *= 0.5f; //depth
                                        double antiRidge = Utils.gradient(Math.clamp(surface, 70, 96), 70, 96, 0.2f, 0.f);
                                        craterDist += 0.7f-antiRidge;
                                        double ridgePeak = 1.1f-(antiRidge/2);
                                        if (craterDist > ridgePeak) { //ridges
                                            craterDist -= ((craterDist-ridgePeak)*2.f);
                                        }
                                        craterSurfMul = Math.min(craterDist, craterSurfMul);
                                        craterSurfMaxMul = Math.max(craterDist, craterSurfMaxMul);
                                    }
                                }
                                surface = (int) Math.max(16, surface*(craterSurfMul >= 1.f ? Math.pow(craterSurfMaxMul, 2) : craterSurfMul));

                                biomes[x * size + z] = (byte)(inCrater ? Biomes.MARB_CRATER.id : Biomes.MARB_HIGHLANDS.id);
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
                            chunk.blockPalette.set(0, Chunk.packInts(BlockTypes.MARBLE.id, 0));
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
                                    double noise = noisePipeline.evaluateNoise(x / 35.f, z / 60.f);
                                    double simplexNoise = SimplexNoise.noise(x / 100.f, z / 100.f);
                                    //double bigNoise = SimplexNoise.noise(x / 2000.f, z / 2000.f);

                                    for (int y = floor; y < seafloorAbove; y++) {
                                        final int blockType = biome == Biomes.MARB_CRATER.id ?
                                                ((((simplexNoise > 0.3 && simplexNoise < 0.5) || (simplexNoise > -0.5 && simplexNoise < -0.3)) && noise > 0.34f) ? ((y == seafloorAbove-1 && noise > 0.5f) ? BlockTypes.IRON_ORE.id : BlockTypes.STONE.id) : BlockTypes.GRAVEL.id) :
                                                (flat ? (Utils.gradient(y, 92, 100, 1, 0)-Math.abs(simplexNoise) > 0.25f ? BlockTypes.CINNABAR.id : BlockTypes.GRANITE.id) : BlockTypes.FLINT.id);
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
                                //byte biome = biomes[x * size + z];
                                Vector2i blockOn = getBlock(x, elevation, z);
                                float randomNumber = rand.nextFloat();
                                boolean onGravel = blockOn.x() == BlockTypes.GRAVEL.id;
                                if (randomNumber < (onGravel ? 0.005f : 0.0005f)) {
                                    boolean crater = !onGravel ? (rand.nextBoolean() || rand.nextBoolean()) : rand.nextBoolean();
                                    Blob.generate(blockOn, x, crater ? elevation+2 : elevation, z, crater ? 0 : BlockTypes.FLINT.id, 0, (int) (2 + (rand.nextFloat() * 14)));
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
