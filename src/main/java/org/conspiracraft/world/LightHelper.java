package org.conspiracraft.world;

import org.conspiracraft.blocks.types.BlockType;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.blocks.types.LightBlockType;
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
    public static final int maxSunlightLevel = 31;
    public static final ArrayDeque<Vector3i> lightQueue = new ArrayDeque<>();
    public static final ConcurrentLinkedDeque<Chunk> dirtyChunks = new ConcurrentLinkedDeque<>();

    public static void queueLightUpdate(Vector3i pos) {
        int packedCp = World.packChunkPos(pos.x()>>chunkBits, pos.y()>>chunkBits, pos.z()>>chunkBits);
        Chunk chunk = chunks[packedCp];
        int packedLp = Chunk.condenseLocalPos(pos.x()&15, pos.y()&15, pos.z()&15);
        boolean exists = chunk.lightUpdateArr()[packedLp];
        if (!exists) {
            chunk.lightUpdateArr[packedLp] = true;
            synchronized (lightQueue) {
                lightQueue.add(pos);
            }
        }
    }
    public static void queueLightUpdate(ArrayDeque<Vector3i> queue, Vector3i pos) {
        int packedCp = World.packChunkPos(pos.x()>>chunkBits, pos.y()>>chunkBits, pos.z()>>chunkBits);
        Chunk chunk = chunks[packedCp];
        int packedLp = Chunk.condenseLocalPos(pos.x()&15, pos.y()&15, pos.z()&15);
        boolean exists = chunk.lightUpdateArr()[packedLp];
        if (!exists) {
            chunk.lightUpdateArr[packedLp] = true;
            queue.add(pos);
        }
    }

    public static void iterateLightQueue() throws InterruptedException {
        int threads = Runtime.getRuntime().availableProcessors();
        ArrayDeque<Vector3i>[] queues = new ArrayDeque[threads];
        for (int t = 0; t < threads; t++) {
            queues[t] = new ArrayDeque<>();
        }
        int i = 0;
        for (Vector3i pos : lightQueue) {
            queues[i++ % threads].add(pos);
        }
        lightQueue.clear();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (ArrayDeque<Vector3i> queue : queues) {
            pool.submit(() -> {
                while (!queue.isEmpty()) {
                    Vector3i pos = queue.pollFirst();
                    if (inBounds(1, pos.x(), pos.y(), pos.z())) {
                        updateLight(queue, pos, getBlock(pos), getLight(pos));
                        int packedCp = World.packChunkPos(pos.x()>>chunkBits, pos.y()>>chunkBits, pos.z()>>chunkBits);
                        Chunk chunk = chunks[packedCp];
                        chunk.lightUpdateArr[Chunk.condenseLocalPos(pos.x()&15, pos.y()&15, pos.z()&15)] = false;
                    }
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
    public static void updateLight(ArrayDeque<Vector3i> queue, Vector3i pos, Vector2i block, Light light) {
        BlockType blockType = BlockTypes.blockTypes[block.x()];
        boolean isLight = blockType instanceof LightBlockType;
        if (!blocksLight(block) || isLight) {
            int r = Math.max(light.r(), isLight ? ((LightBlockType) blockType).lightBlockProperties().r : 0);
            int g = Math.max(light.g(), isLight ? ((LightBlockType) blockType).lightBlockProperties().g : 0);
            int b = Math.max(light.b(), isLight ? ((LightBlockType) blockType).lightBlockProperties().b : 0);
            int s = (pos.y > heightmap[packPos(pos.x, pos.z)] ? maxSunlightLevel : light.s());
            for (Vector3i neighborPos : new Vector3i[]{
                    new Vector3i(pos.x, pos.y, pos.z + 1), new Vector3i(pos.x + 1, pos.y, pos.z), new Vector3i(pos.x, pos.y, pos.z - 1),
                    new Vector3i(pos.x - 1, pos.y, pos.z), new Vector3i(pos.x, pos.y + 1, pos.z), new Vector3i(pos.x, pos.y - 1, pos.z)
            }) {
                Vector2i neighbor = getBlock(neighborPos);
                Light neighborLight = getLight(neighborPos);
                BlockType neighborBlockType = BlockTypes.blockTypes[neighbor.x];
                boolean isNLight = neighborBlockType instanceof LightBlockType;
                if (!blocksLight(neighbor) || isNLight) {
                    r = Math.max(r, Math.max(neighborLight.r(), isNLight ? ((LightBlockType) neighborBlockType).lightBlockProperties().r : 0) - 1);
                    g = Math.max(g, Math.max(neighborLight.g(), isNLight ? ((LightBlockType) neighborBlockType).lightBlockProperties().g : 0) - 1);
                    b = Math.max(b, Math.max(neighborLight.b(), isNLight ? ((LightBlockType) neighborBlockType).lightBlockProperties().b : 0) - 1);
                    s = Math.max(s, neighborLight.s() - 1);
                }
            }
            setLight(pos.x, pos.y, pos.z, new Light(r, g, b, s));
            for (Vector3i neighborPos : new Vector3i[]{
                    new Vector3i(pos.x, pos.y, pos.z + 1), new Vector3i(pos.x + 1, pos.y, pos.z), new Vector3i(pos.x, pos.y, pos.z - 1),
                    new Vector3i(pos.x - 1, pos.y, pos.z), new Vector3i(pos.x, pos.y + 1, pos.z), new Vector3i(pos.x, pos.y - 1, pos.z)
            }) {
                Light nLight = getLight(neighborPos);
                if (isDarker(r, g, b, s, nLight)) {
                    queueLightUpdate(queue, neighborPos);
                }
            }
        }
    }
    public static boolean isDarker(int r, int g, int b, int s, Light darker) {
        return r-2 > darker.r() || g-2 > darker.g() || b-2 > darker.b() || s-2 > darker.s();
    }
    public static boolean blocksLight(Vector2i block) {
        return BlockTypes.blockTypes[block.x()].blocksLight(block);
    }

    public static ArrayDeque<lightNode> removalQueue = new ArrayDeque<>();
    public static HashSet<Vector3i> removalSet = new HashSet<>();
    public static void recalculateLight(Vector3i ogPos, Light light) {
        recalculateLight(ogPos, light.r(), light.g(), light.b(), light.s());
    }
    public static void recalculateLight(Vector3i ogPos, int r, int g, int b, int s) {
        removalQueue.add(new lightNode(ogPos.x(), ogPos.y(), ogPos.z(), r, g, b, s));
        removalSet.add(ogPos);

        while (!removalQueue.isEmpty()) {
            lightNode node = removalQueue.pollFirst();
            Vector3i pos = new Vector3i(node.x, node.y, node.z);
            Light light = new Light(node.r(), node.g(), node.b(), node.s());
            if (light.r() > 0 || light.g() > 0 || light.b() > 0 || light.s() > 0) {
                setLight(pos.x(), pos.y(), pos.z(), new Light(0, 0, 0, 0));
                for (Vector3i neighborPos : new Vector3i[]{
                        new Vector3i(pos.x, pos.y, pos.z + 1), new Vector3i(pos.x + 1, pos.y, pos.z), new Vector3i(pos.x, pos.y, pos.z - 1),
                        new Vector3i(pos.x - 1, pos.y, pos.z), new Vector3i(pos.x, pos.y + 1, pos.z), new Vector3i(pos.x, pos.y - 1, pos.z)
                }) {
                    if (!removalSet.contains(neighborPos)) {
                        lightQueue.add(neighborPos);
                        int packedCp = World.packChunkPos(neighborPos.x()>>chunkBits, neighborPos.y()>>chunkBits, neighborPos.z()>>chunkBits);
                        Chunk chunk = chunks[packedCp];
                        chunk.lightUpdateArr()[Chunk.condenseLocalPos(neighborPos.x()&15, neighborPos.y()&15, neighborPos.z()&15)] = true;
                        Light nLight = getLight(neighborPos);
                        if ((nLight.r() > 0 && nLight.r() == light.r() - 1) || (nLight.g() > 0 && nLight.g() == light.g() - 1) ||
                                (nLight.b() > 0 && nLight.b() == light.b() - 1) || (nLight.s() > 0 && nLight.s() == light.s() - 1)) {
                            removalQueue.add(new lightNode(neighborPos.x(), neighborPos.y(), neighborPos.z(), nLight.r(), nLight.g(), nLight.b(), nLight.s()));
                            removalSet.add(neighborPos);
                        }
                    }
                }
            }
        }
        removalSet.clear();
    }

    public record lightNode(int x, int y, int z, int r, int g, int b, int s) {}
}