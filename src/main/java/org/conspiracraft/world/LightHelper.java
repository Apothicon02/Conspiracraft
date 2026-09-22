package org.conspiracraft.world;

import org.conspiracraft.blocks.types.BlockType;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.blocks.types.LightBlockType;
import org.conspiracraft.world.types.Earth;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.conspiracraft.world.World.*;

public class LightHelper {
    public static final int maxSunlightLevel = 23;
    public static final ArrayDeque<Vector3i> lightQueueWG = new ArrayDeque<>();
    public static final ArrayDeque<Vector3i> lightQueueSkipWG = new ArrayDeque<>();
    public static final ArrayDeque<Vector3i> lightQueue = new ArrayDeque<>();
    public static final ArrayDeque<Vector3i> lightQueueSkip = new ArrayDeque<>();
    public static final ConcurrentLinkedDeque<Chunk> dirtyChunks = new ConcurrentLinkedDeque<>();

    public static void queueLightUpdate(Vector3i pos) {
        long globalCP = World.packChunkPos(pos.x() >> chunkBits, pos.y() >> chunkBits, pos.z() >> chunkBits);
        Chunk chunk = getChunkGlobalPos(globalCP);
        int packedLp = Chunk.packLocalPos(pos.x() & 15, pos.y() & 15, pos.z() & 15);
        synchronized (lightQueue) {
            boolean exists = chunk.lightUpdateArr()[packedLp];
            if (!exists) {
                chunk.lightUpdateArr()[packedLp] = true;
                lightQueue.add(pos);
            }
        }
    }
    public static void queueLightUpdate(ArrayDeque<Vector3i> queue, Vector3i pos) {
        long packedCp = World.packChunkPos(pos.x()>>chunkBits, pos.y()>>chunkBits, pos.z()>>chunkBits);
        Chunk chunk = getChunkGlobalPos(packedCp);
        int packedLp = Chunk.packLocalPos(pos.x()&15, pos.y()&15, pos.z()&15);
        synchronized (queue) {
            boolean exists = chunk.lightUpdateArr()[packedLp];
            if (!exists) {
                chunk.lightUpdateArr[packedLp] = true;
                queue.add(pos);
            }
        }
    }

    public static void iterateLightQueue() {
        while (!lightQueue.isEmpty()) {
            Vector3i pos = lightQueue.pollFirst();
            int rX = pos.x()>>regionBits, rY = pos.y()>>regionBits, rZ = pos.z()>>regionBits;
            Region region = getRegion(packRegionPos(rX, rY, rZ));
            if (!region.neighborsGenerated) {
                lightQueueSkip.add(pos);
            } else {
                updateLight(lightQueue, pos, getBlock(pos), getLight(pos));
                long cCP = World.packChunkPos(pos.x()>>chunkBits, pos.y()>>chunkBits, pos.z()>>chunkBits);
                Chunk chunk = getChunkGlobalPos(cCP);
                chunk.lightUpdateArr()[Chunk.packLocalPos(pos.x()&15, pos.y()&15, pos.z()&15)] = false;
            }
        }
        int i = 0;
        while (!lightQueueWG.isEmpty()) {
            if (i++ >= 5000) {break;}
            Vector3i pos = lightQueueWG.pollFirst();
            int rX = pos.x()>>regionBits, rY = pos.y()>>regionBits, rZ = pos.z()>>regionBits;
            Region region = getRegion(packRegionPos(rX, rY, rZ));
            if (!region.neighborsGenerated) {
                lightQueueSkipWG.add(pos);
            } else {
                updateLight(lightQueueWG, pos, getBlock(pos), getLight(pos));
                long cCP = World.packChunkPos(pos.x()>>chunkBits, pos.y()>>chunkBits, pos.z()>>chunkBits);
                Chunk chunk = getChunkGlobalPos(cCP);
                chunk.lightUpdateArr()[Chunk.packLocalPos(pos.x()&15, pos.y()&15, pos.z()&15)] = false;
            }
        }
        for (Chunk chunk : dirtyChunks) {
            chunk.lightUpdateArr = null;
            updateQueue.add(chunk.cCP);
        }
        dirtyChunks.clear();
        while (!lightQueueSkip.isEmpty()) {
            queueLightUpdate(lightQueueSkip.pollFirst());
        }
        while (!lightQueueSkipWG.isEmpty()) {
            queueLightUpdate(lightQueueWG, lightQueueSkipWG.pollFirst());
        }
    }
    public static void iterateLightQueueMultithreaded() throws InterruptedException {
        final int threads = Runtime.getRuntime().availableProcessors();
        final ArrayDeque<Vector3i>[] queues = new ArrayDeque[threads];
        for (int t = 0; t < threads; t++) {
            queues[t] = new ArrayDeque<>();
        }
        int i = 0;
        for (Vector3i pos : lightQueue) {
            queues[i++ % threads].add(pos);
        }
        lightQueue.clear();
        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (final ArrayDeque<Vector3i> queue : queues) {
            pool.submit(() -> {
                while (!queue.isEmpty()) {
                Vector3i pos = queue.pollFirst();
                    updateLight(queue, pos, getBlock(pos), getLight(pos));
                    long globalCP = World.packChunkPos(pos.x()>>chunkBits, pos.y()>>chunkBits, pos.z()>>chunkBits);
                    Chunk chunk = getChunkGlobalPos(globalCP);
                    chunk.lightUpdateArr()[Chunk.packLocalPos(pos.x()&15, pos.y()&15, pos.z()&15)] = false;
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        for (Chunk chunk : dirtyChunks) {chunk.lightUpdateArr = null;}
        dirtyChunks.clear();
    }

    public static void updateLight(Vector3i pos, Vector2i block, Light light) {
        updateLight(lightQueue, pos, block, light);
    }
    public static final Vector3i[] neighborPositions = new Vector3i[]{
            new Vector3i(0, 0, 1), new Vector3i(1, 0, 0), new Vector3i(0, 0, -1),
            new Vector3i(-1, 0, 0), new Vector3i(0, 1, 0), new Vector3i(0, -1, 0)};
    public static final Vector2i[] neighborBlocks = new Vector2i[6];
    public static final BlockType[] neighborBlockTypes = new BlockType[6];
    public static final Light[] neighborLights = new Light[6];
    public static void updateLight(ArrayDeque<Vector3i> queue, Vector3i pos, Vector2i block, Light light) {
        BlockType blockType = BlockTypes.blockTypes[block.x()];
        boolean isLight = blockType instanceof LightBlockType;
        boolean isSlab = blockType.blockProperties.hasSlab, isTopSlab = false, isBottomSlab = false;
        if (isSlab) {
            if (block.y() == 1) {
                isTopSlab = true;
            } else if (block.y() == 2) {
                isBottomSlab = true;
            } else {
                isSlab = false;
            }
        }
        if (!BlockTypes.blockTypes[block.x()].blocksLight(block) || isLight || isSlab) {
            int r = Math.max(light.r(), isLight ? ((LightBlockType) blockType).lightBlockProperties().r : 0);
            int g = Math.max(light.g(), isLight ? ((LightBlockType) blockType).lightBlockProperties().g : 0);
            int b = Math.max(light.b(), isLight ? ((LightBlockType) blockType).lightBlockProperties().b : 0);
            boolean aboveHeightmap = pos.y > Earth.GROUND_LEVEL+getRegion2D(packRegionPos(pos.x()>>regionBits, 0, pos.z()>>regionBits)).heights[Region2D.packLocalPos(pos.x()%regionSize, pos.z()%regionSize)];
            int s = (aboveHeightmap ? maxSunlightLevel : light.s());
            for (int i = 0; i < 6; i++) {
                Vector3i neighborPos = neighborPositions[i];
                Vector2i neighbor = getBlock(pos.x()+neighborPos.x(), pos.y()+neighborPos.y(), pos.z()+neighborPos.z());
                neighborBlocks[i] = neighbor;
                BlockType neighborBlockType = BlockTypes.blockTypes[neighbor.x()];
                neighborBlockTypes[i] = neighborBlockType;
                Light neighborLight = getLight(pos.x()+neighborPos.x(), pos.y()+neighborPos.y(), pos.z()+neighborPos.z());
                neighborLights[i] = neighborLight;
                if (!((isTopSlab && neighborPos.y() > 0) || (isBottomSlab && neighborPos.y() < 0))) { //don't spread light to neighbors the slab blocks
                    boolean isNLight = neighborBlockType instanceof LightBlockType;
                    boolean isNSlab = neighborBlockType.blockProperties.hasSlab;
                    if (isNSlab) {
                        if (neighbor.y() == 1) { //top slab
                            if (neighborPos.y()+pos.y() < pos.y()) {
                                isNSlab = false;
                            }
                        } else if (neighbor.y() == 2) { //bottom slab
                            if (neighborPos.y()+pos.y() > pos.y()) {
                                isNSlab = false;
                            }
                        } else {
                            isNSlab = false;
                        }
                    }
                    if (!neighborBlockType.blocksLight(neighbor) || isNLight || isNSlab) {
                        r = Math.max(r, Math.max(neighborLight.r(), isNLight ? ((LightBlockType) neighborBlockType).lightBlockProperties().r : 0) - 1);
                        g = Math.max(g, Math.max(neighborLight.g(), isNLight ? ((LightBlockType) neighborBlockType).lightBlockProperties().g : 0) - 1);
                        b = Math.max(b, Math.max(neighborLight.b(), isNLight ? ((LightBlockType) neighborBlockType).lightBlockProperties().b : 0) - 1);
                        s = Math.max(s, neighborLight.s() - 1);
                    }
                }
            }
            setLight(pos.x, pos.y, pos.z, new Light(r, g, b, s));
            for (int i = 0; i < 6; i++) {
                Vector3i neighborPos = neighborPositions[i];
                Vector2i nBlock = neighborBlocks[i];
                if (!neighborBlockTypes[i].blocksLight(nBlock)) {
                    Light nLight = neighborLights[i];
                    if (isDarker(r, g, b, s, nLight)) {
                        queueLightUpdate(queue, new Vector3i(neighborPos.x()+pos.x(), neighborPos.y()+pos.y(), neighborPos.z()+pos.z()));
                    }
                }
            }
        }
    }
    public static boolean isDarker(int r, int g, int b, int s, Light darker) {
        return r-2 > darker.r() || g-2 > darker.g() || b-2 > darker.b() || s-2 > darker.s();
    }

    public static final ArrayDeque<lightNode> removalQueue = new ArrayDeque<>();
    public static final HashSet<Vector3i> removalSet = new HashSet<>();
    public static void recalculateLight(Vector3i ogPos, Light light) {
        recalculateLight(ogPos, light.r(), light.g(), light.b(), light.s());
    }
    public static void recalculateLight(Vector3i ogPos, int r, int g, int b, int s) {
        removalQueue.add(new lightNode(ogPos.x(), ogPos.y(), ogPos.z(), r, g, b, s));
        removalSet.add(ogPos);

        while (!removalQueue.isEmpty()) {
            lightNode node = removalQueue.pollFirst();
            int rX = node.x()>>regionBits, rY = node.y()>>regionBits, rZ = node.z()>>regionBits;
            Region region = getRegion(packRegionPos(rX, rY, rZ));
            if (region.neighborsGenerated) {
                Vector3i pos = new Vector3i(node.x, node.y, node.z);
                Light light = new Light(node.r(), node.g(), node.b(), node.s());
                if (light.r() > 0 || light.g() > 0 || light.b() > 0 || light.s() > 0) {
                    setLight(pos.x(), pos.y(), pos.z(), new Light(0, 0, 0, 0));
                    for (Vector3i neighborPos : new Vector3i[]{
                            new Vector3i(pos.x, pos.y, pos.z + 1), new Vector3i(pos.x + 1, pos.y, pos.z), new Vector3i(pos.x, pos.y, pos.z - 1),
                            new Vector3i(pos.x - 1, pos.y, pos.z), new Vector3i(pos.x, pos.y + 1, pos.z), new Vector3i(pos.x, pos.y - 1, pos.z)
                    }) {
                        if (removalSet.add(neighborPos)) {
                            Vector2i nBlock = getBlock(neighborPos);
                            if (!BlockTypes.blockTypes[nBlock.x()].blocksLight(nBlock)) {
                                lightQueue.add(neighborPos);
                                long packedCp = World.packChunkPos(neighborPos.x() >> chunkBits, neighborPos.y() >> chunkBits, neighborPos.z() >> chunkBits);
                                Chunk chunk = getChunkGlobalPos(packedCp);
                                chunk.lightUpdateArr()[Chunk.packLocalPos(neighborPos.x() & 15, neighborPos.y() & 15, neighborPos.z() & 15)] = true;
                                Light nLight = getLight(neighborPos);
                                if ((nLight.r() > 0 && nLight.r() == light.r() - 1) || (nLight.g() > 0 && nLight.g() == light.g() - 1) ||
                                        (nLight.b() > 0 && nLight.b() == light.b() - 1) || (nLight.s() > 0 && nLight.s() == light.s() - 1)) {
                                    removalQueue.add(new lightNode(neighborPos.x(), neighborPos.y(), neighborPos.z(), nLight.r(), nLight.g(), nLight.b(), nLight.s()));
                                }
                            }
                        }
                    }
                }
            }
        }
        removalSet.clear();
    }
//    public static void recalculateLight(Vector3i ogPos, int r, int g, int b, int s) {
//        removalQueue.add(new lightNode(ogPos.x(), ogPos.y(), ogPos.z(), r, g, b, s));
//        removalSet.add(ogPos);
//
//        while (!removalQueue.isEmpty()) {
//            lightNode node = removalQueue.pollFirst();
//            Vector3i pos = new Vector3i(node.x, node.y, node.z);
//            Light light = new Light(node.r(), node.g(), node.b(), node.s());
//            if (light.r() > 0 || light.g() > 0 || light.b() > 0 || light.s() > 0) {
//                setLight(pos.x(), pos.y(), pos.z(), new Light(0, 0, 0, 0));
//                Vector2i block = World.getBlock(pos);
//                BlockType blockType = BlockTypes.blockTypes[block.x()];
//                boolean isTopSlab = false, isBottomSlab = false;
//                if (blockType.blockProperties.hasSlab) {
//                    if (block.y() == 1) {
//                        isTopSlab = true;
//                    } else if (block.y() == 2) {
//                        isBottomSlab = true;
//                    }
//                }
//                for (Vector3i neighborPos : new Vector3i[]{
//                        new Vector3i(pos.x, pos.y, pos.z + 1), new Vector3i(pos.x + 1, pos.y, pos.z), new Vector3i(pos.x, pos.y, pos.z - 1),
//                        new Vector3i(pos.x - 1, pos.y, pos.z), new Vector3i(pos.x, pos.y + 1, pos.z), new Vector3i(pos.x, pos.y - 1, pos.z)
//                }) {
//                    if (!((isTopSlab && neighborPos.y() > pos.y()) || (isBottomSlab && neighborPos.y() < pos.y()))) { //don't interact across the blocked by slab direction
//                        if (removalSet.add(neighborPos)) {
//                            lightQueue.add(neighborPos);
//                            long cCP = World.packChunkPos(neighborPos.x() >> chunkBits, neighborPos.y() >> chunkBits, neighborPos.z() >> chunkBits);
//                            Chunk chunk = getChunkGlobalPos(cCP);
//                            chunk.lightUpdateArr()[Chunk.packLocalPos(neighborPos.x() & 15, neighborPos.y() & 15, neighborPos.z() & 15)] = true;
//                            Light nLight = getLight(neighborPos);
//                            if ((nLight.r() > 0 && nLight.r() == light.r() - 1) || (nLight.g() > 0 && nLight.g() == light.g() - 1) ||
//                                    (nLight.b() > 0 && nLight.b() == light.b() - 1) || (nLight.s() > 0 && nLight.s() == light.s() - 1)) {
//                                removalQueue.add(new lightNode(neighborPos.x(), neighborPos.y(), neighborPos.z(), nLight.r(), nLight.g(), nLight.b(), nLight.s()));
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        removalSet.clear();
//    }

    public record lightNode(int x, int y, int z, int r, int g, int b, int s) {}
}