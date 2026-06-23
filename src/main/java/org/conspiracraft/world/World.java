package org.conspiracraft.world;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.conspiracraft.Main;
import org.conspiracraft.blocks.entities.BlockEntity;
import org.conspiracraft.blocks.types.BlockType;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.effects.Effect;
import org.conspiracraft.entities.Entity;
import org.conspiracraft.items.Item;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.world.types.WorldType;
import org.conspiracraft.world.types.WorldTypes;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static org.conspiracraft.world.LightHelper.maxSunlightLevel;

public class World {
    public static final int seed = 67;
    public static final int seaLevel = 63;
    public static final int size = 2048;
    public static final long sizeL = size;
    public static final int halfSize = size/2;
    public static final int quarterSize = size/4;
    public static final int height = 640;
    public static final long heightL = height;
    public static final byte chunkSize = 16;
    public static final int chunkBits = Integer.numberOfTrailingZeros(chunkSize);
    public static final int sizeChunks = size>>chunkBits;
    public static final int heightChunks = height>>chunkBits;
    public static final byte regionSizeChunks = 4;
    public static final int regionBits = Integer.numberOfTrailingZeros(regionSizeChunks);
    public static final int sizeRegions = sizeChunks>>regionBits;
    public static final int heightRegions = heightChunks>>regionBits;
    public static final byte lodSize = 4;
    public static final int lodBits = Integer.numberOfTrailingZeros(lodSize);
    public static final int sizeLods = size >>lodBits;
    public static final int heightLods = height >>lodBits;
    public static boolean generating = false;
    public static WorldType worldType = WorldTypes.VERA;
    public static final ObjectOpenHashSet<Item> items = new ObjectOpenHashSet<>();
    public static final Int2ObjectOpenHashMap<BlockEntity> blockEntities = new Int2ObjectOpenHashMap<>();

    public static void tickItems() {
        for (Item item : World.items) {
            if (item.timeExisted >= 600000) { //600000ms = 10m
                World.items.remove(item);
            } else {
                item.tick();
            }
        }
    }
    public static void dropItem(Item item) {
        World.items.add(item.clone().timeExisted(-2000).moveTo(new Vector3f(Main.player.pos).add(0, Main.player.eyeHeight, 0)));
    }

    public static void save(String path) throws IOException {
        long start = System.currentTimeMillis();
        new File(path).mkdirs();

        long[] globalData = new long[]{Main.timeNs};
        FileChannel out = FileChannel.open(Path.of(path + "global.data"), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        MappedByteBuffer data = out.map(FileChannel.MapMode.READ_WRITE, 0, globalData.length * 8L);
        data.order(ByteOrder.BIG_ENDIAN);
        data.asLongBuffer().put(globalData);
        Utils.unmap(data);
        out.close();

        out = FileChannel.open(Path.of(path + "heightmap.data"), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        data = out.map(FileChannel.MapMode.READ_WRITE, 0, lods.length * 2L);
        data.order(ByteOrder.BIG_ENDIAN);
        data.asShortBuffer().put(heightmap);
        Utils.unmap(data);
        out.close();

        out = FileChannel.open(Path.of(path + "lods.data"), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        data = out.map(FileChannel.MapMode.READ_WRITE, 0, lods.length * 8L);
        data.order(ByteOrder.BIG_ENDIAN);
        data.asLongBuffer().put(lods);
        Utils.unmap(data);
        out.close();

        out = FileChannel.open(Path.of(path + "regions.data"), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        data = out.map(FileChannel.MapMode.READ_WRITE, 0, regions.length * 8L);
        data.order(ByteOrder.BIG_ENDIAN);
        data.asLongBuffer().put(regions);
        Utils.unmap(data);
        out.close();

        int size = 0;
        for (Chunk chunk : World.chunks) {
            int[] blockData = chunk.getBlockData();
            int[] lightData = chunk.getLightData();
            size += 4+chunk.blockPalette.size()+(blockData == null ? 0 : blockData.length)+chunk.lightPalette.size()+(lightData == null ? 0 : lightData.length);
        }
        out = FileChannel.open(Path.of(path + "chunks.data"), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        data = out.map(FileChannel.MapMode.READ_WRITE, 0, size*4L);
        data.order(ByteOrder.BIG_ENDIAN);
        IntBuffer chunkData = data.asIntBuffer();

        int[] emptyData = new int[0];
        for (Chunk chunk : World.chunks) {
            int[] subdata  = chunk.getBlockData();
            subdata = subdata == null ? emptyData : subdata;
            int[] palette = chunk.getBlockPalette();
            chunkData.put(palette.length);
            chunkData.put(palette);
            chunkData.put(subdata.length);
            chunkData.put(subdata);
            subdata = chunk.getLightData();
            subdata = subdata == null ? emptyData : subdata;
            palette = chunk.getLightPalette();
            chunkData.put(palette.length);
            chunkData.put(palette);
            chunkData.put(subdata.length);
            chunkData.put(subdata);
        }
        Utils.unmap(data);
        out.close();

        IntArrayList itemsData = new IntArrayList();
        int i = 0;
        for (Item item : items) {
            int[] itemData = item.getData();
            itemsData.addElements(i, itemData);
            i += itemData[0]+1;
        }
        out = FileChannel.open(Path.of(path + "items.data"), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        data = out.map(FileChannel.MapMode.READ_WRITE, 0, itemsData.size() * 4L);
        data.order(ByteOrder.BIG_ENDIAN);
        data.asIntBuffer().put(itemsData.toIntArray());
        Utils.unmap(data);
        out.close();

        IntArrayList entitiesData = new IntArrayList();
        i = 0;
        for (Entity entity : entities) {
            int[] entityData = entity.getData();
            entitiesData.addElements(i, entityData);
            i += entityData[0]+1;
        }
        out = FileChannel.open(Path.of(path + "entities.data"), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        data = out.map(FileChannel.MapMode.READ_WRITE, 0, entitiesData.size() * 4L);
        data.order(ByteOrder.BIG_ENDIAN);
        data.asIntBuffer().put(entitiesData.toIntArray());
        Utils.unmap(data);
        out.close();
        System.out.println("Took " + (System.currentTimeMillis()-start) + "ms to save world. ");
    }
    public static boolean previouslyGenerated = false;
    public static void load(String path) throws IOException, InterruptedException {
        long start = System.currentTimeMillis();
        if (previouslyGenerated) {
            Arrays.fill(lods, 0L);
            Arrays.fill(regions, 0L);
            Arrays.fill(chunks, null);
        }
        previouslyGenerated = true;
        boolean didExist = Files.exists(Path.of(path));
        if (didExist) {
            long[] globalData = Utils.flipLongArray(Utils.byteArrayToLongArray(new FileInputStream(path + "global.data").readAllBytes()));
            Main.timeNs = globalData[0];

            FileChannel in = FileChannel.open(Path.of(path + "heightmap.data"), StandardOpenOption.READ);
            MappedByteBuffer data = in.map(FileChannel.MapMode.READ_ONLY, 0, in.size());
            data.order(ByteOrder.BIG_ENDIAN);
            data.asShortBuffer().get(heightmap);
            Utils.unmap(data);
            in.close();

            in = FileChannel.open(Path.of(path + "lods.data"), StandardOpenOption.READ);
            data = in.map(FileChannel.MapMode.READ_ONLY, 0, in.size());
            data.order(ByteOrder.BIG_ENDIAN);
            data.asLongBuffer().get(lods);
            Utils.unmap(data);
            in.close();

            in = FileChannel.open(Path.of(path + "regions.data"), StandardOpenOption.READ);
            data = in.map(FileChannel.MapMode.READ_ONLY, 0, in.size());
            data.order(ByteOrder.BIG_ENDIAN);
            data.asLongBuffer().get(regions);
            Utils.unmap(data);
            in.close();

            in = FileChannel.open(Path.of(path + "chunks.data"), StandardOpenOption.READ);
            data = in.map(FileChannel.MapMode.READ_ONLY, 0, in.size());
            data.order(ByteOrder.BIG_ENDIAN);
            IntBuffer chunkData = data.asIntBuffer();
            for (int chunkPos = 0; chunkPos < chunks.length; chunkPos++) {
                Chunk chunk = new Chunk(chunkPos);

                int dataSize = chunkData.get();
                int[] subdata = new int[dataSize];
                chunkData.get(subdata);
                chunk.setBlockPalette(subdata);

                dataSize = chunkData.get();
                subdata = new int[dataSize];
                chunkData.get(subdata);
                chunk.setBlockData(subdata);

                dataSize = chunkData.get();
                subdata = new int[dataSize];
                chunkData.get(subdata);
                chunk.setLightPalette(subdata);

                dataSize = chunkData.get();
                subdata = new int[dataSize];
                chunkData.get(subdata);
                chunk.setLightData(subdata);

                chunks[chunkPos] = chunk;
            }
            Utils.unmap(data);
            in.close();

            if (Files.exists(Path.of(path + "items.data"))) {
                in = FileChannel.open(Path.of(path + "items.data"), StandardOpenOption.READ);
                data = in.map(FileChannel.MapMode.READ_ONLY, 0, in.size());
                data.order(ByteOrder.BIG_ENDIAN);
                IntBuffer itemsData = data.asIntBuffer();
                while (itemsData.position() < itemsData.capacity()) {
                    int itemDataLength = itemsData.get();
                    if (itemDataLength > 0) {
                        items.add(Item.load(itemsData));
                    }
                }
                Utils.unmap(data);
                in.close();
            }

            if (Files.exists(Path.of(path + "entities.data"))) {
                in = FileChannel.open(Path.of(path + "entities.data"), StandardOpenOption.READ);
                data = in.map(FileChannel.MapMode.READ_ONLY, 0, in.size());
                data.order(ByteOrder.BIG_ENDIAN);
                IntBuffer entitiesData = data.asIntBuffer();
                while (entitiesData.position() < entitiesData.capacity()) {
                    int itemDataLength = entitiesData.get();
                    if (itemDataLength > 0) {
                        entitiesAddQueue.add(Entity.load(entitiesData));
                    }
                }
                Utils.unmap(data);
                in.close();
            }
        } else {
            worldType.generate();
        }
        System.out.println("Took "+(System.currentTimeMillis()-start)+"ms to load world.");
    }
    public static boolean inBounds(float x, float y, float z) {
        return !(x < 0 || x >= size || y < 0 || y >= height || z < 0 || z >= size);
    }
    public static boolean inBounds(Vector3f pos) {
        return inBounds(pos.x(), pos.y(), pos.z());
    }
    public static boolean inBounds(int x, int y, int z) {
        return !(x < 0 || x >= size || y < 0 || y >= height || z < 0 || z >= size);
    }
    public static boolean inBounds(Vector3i pos) {
        return inBounds(pos.x(), pos.y(), pos.z());
    }
    public static boolean inBounds(int padding, int x, int y, int z) {
        return !(x < padding || x >= size-padding || y < padding || y >= height-padding || z < padding || z >= size-padding);
    }
    public static final short[] heightmap = new short[size*size];
    public static int packPos(int x, int z) {return (x*size)+z;}
    public static int packPosClamped(int x, int z) {return packPos(Math.clamp(x, 0, size-1), Math.clamp(z, 0, size-1));}
    public static long packPos(int x, int y, int z) {return x+y*sizeL+z*sizeL*heightL;}
    public static final Chunk[] chunks = new Chunk[sizeChunks*sizeChunks*heightChunks];
    public static final long[] regions = new long[sizeRegions*sizeRegions*heightRegions];
    public static int packRegionPos(int x, int y, int z) {return x+y*sizeRegions+z*sizeRegions*heightRegions;}
    public static int packRegionPos(Vector3i pos) {return pos.x()+pos.y()*sizeRegions+pos.z()*sizeRegions*heightRegions;}
    public static final long[] lods = new long[sizeLods*sizeLods*heightLods];
    public static int packLodPos(int x, int y, int z) {return x+y*sizeLods+z*sizeLods*heightLods;}
    public static int packLodPos(Vector3i pos) {return pos.x()+pos.y()*sizeLods+pos.z()*sizeLods*heightLods;}
    public static int packChunkPos(Vector3i pos) {
        return (((pos.x*World.sizeChunks)+pos.z)*World.heightChunks)+pos.y;
    }
    public static int packChunkPos(int x, int y, int z) {
        return (((x*World.sizeChunks)+z)*World.heightChunks)+y;
    }
    public static int packChunkPos(int x, int z) {
        return (x*World.sizeChunks)+z;
    }
    public static Light getLight(Vector3i pos) {
        return getLight(pos.x(), pos.y(), pos.z());
    }
    public static Light getLight(int x, int y, int z) {
        if (x < 0 || x >= size || y < 0 || y >= height || z < 0 || z >= size) {
            //System.out.print("Tried getting block that's out of bounds: x"+x+", y"+y+", z"+z);
            return new Light(0, 0, 0, maxSunlightLevel);
        }
        int cX = x>>chunkBits, cY = y>>chunkBits, cZ = z>>chunkBits;
        Chunk chunk = chunks[packChunkPos(cX, cY, cZ)];
        int pos = Chunk.condenseLocalPos(x&15, y&15, z&15);
        synchronized (chunk) {
            return chunk.getLight(pos);
        }
    }
    public static void setLight(int x, int y, int z, Light light) {
        if (x < 0 || x >= size || y < 0 || y >= height || z < 0 || z >= size) {
            //System.out.print("Tried setting block that's out of bounds: x"+x+", y"+y+", z"+z);
            return;
        }
        Vector3i chunkPos = new Vector3i(x>>chunkBits, y>>chunkBits, z>>chunkBits);
        Chunk chunk = chunks[packChunkPos(chunkPos.x(), chunkPos.y(), chunkPos.z())];
        int lX = x&15;
        int lY = y&15;
        int lZ = z&15;
        if (!generating && !updateSet.contains(chunkPos)) {
            updateSet.add(chunkPos);
            updateQueue.addLast(chunkPos);
        }
        synchronized (chunk) {
            chunk.setLight(lX, lY, lZ, light);
        }
    }
    public static int getBlockTypeUnchecked(int x, int y, int z) {
        int cX = x>>chunkBits, cY = y>>chunkBits, cZ = z>>chunkBits;
        Chunk chunk = chunks[packChunkPos(cX, cY, cZ)];
        int pos = Chunk.condenseLocalPos(x&15, y&15, z&15);
        return chunk.getBlockType(pos);
    }
    public static Vector2i getBlock(Vector3i pos) {return getBlock(pos.x(), pos.y(), pos.z());}
    public static Vector2i getBlock(float x, float y, float z) {
        return getBlock((int)x, (int)y, (int)z);
    }
    public static Vector2i getBlock(int x, int y, int z) {
        if (x < 0 || x >= size || y < 0 || y >= height || z < 0 || z >= size) {
            //System.out.print("Tried getting block that's out of bounds: x"+x+", y"+y+", z"+z);
            return new Vector2i(0);
        }
        int cX = x>>chunkBits, cY = y>>chunkBits, cZ = z>>chunkBits;
        Chunk chunk = chunks[packChunkPos(cX, cY, cZ)];
        int pos = Chunk.condenseLocalPos(x&15, y&15, z&15);
        synchronized (chunk) {
            return chunk.getBlock(pos);
        }
    }
    public static final ArrayDeque<Vector3i> updateQueue = new ArrayDeque<>();
    public static final HashSet<Vector3i> updateSet = new HashSet<>();
    public static void setBlock(int x, int y, int z, int type, int subType, boolean idk, boolean idk2, int idk3, boolean idk4) {
        setBlock(x, y, z, type, subType, true);
    }
    public static void setBlock(int x, int y, int z, int type, int subType) {
        setBlock(x, y, z, type, subType, true);
    }
    public static void setBlock(int x, int y, int z, int type, int subType, boolean updateLighting) {
        if (x < 0 || x >= size || y < 0 || y >= height || z < 0 || z >= size) {
            //System.out.print("Tried setting block that's out of bounds: x"+x+", y"+y+", z"+z);
            return;
        }
        Vector3i chunkPos = new Vector3i(x>>chunkBits, y>>chunkBits, z>>chunkBits);
        Chunk chunk = chunks[packChunkPos(chunkPos.x(), chunkPos.y(), chunkPos.z())];
        int lX = x&15;
        int lY = y&15;
        int lZ = z&15;
        synchronized (chunk) {
            chunk.setBlock(lX, lY, lZ, type, subType);
            updateLod(x, y, z, type == 0);
            updateRegion(chunkPos.x(), chunkPos.y(), chunkPos.z(), !(chunk.blockPalette.size() > 1 || chunk.blockPalette.getFirst() != 0));
        }
        if (!generating) {
            if (updateLighting) {
                int pos = Chunk.condenseLocalPos(lX, lY, lZ);
                Light oldLight = chunk.getLight(pos);
                chunk.setLight(lX, lY, lZ, new Light(0, 0, 0, 0));
                updateHeightmap(x, y, z);
                LightHelper.recalculateLight(new Vector3i(x, y, z), oldLight);
            }
            if (!updateSet.contains(chunkPos)) {
                updateSet.add(chunkPos);
                updateQueue.addLast(chunkPos);
            }
        }
    }
    public static void replaceBlock(int x, int y, int z, int type, int subType) {
        replaceBlock(x, y, z, type, subType, true);
    }
    public static void replaceBlock(int x, int y, int z, int type, int subType, boolean updateLighting) {
        if (x < 0 || x >= size || y < 0 || y >= height || z < 0 || z >= size) {
            //System.out.print("Tried replacing block that's out of bounds: x"+x+", y"+y+", z"+z);
            return;
        }
        Vector3i chunkPos = new Vector3i(x>>chunkBits, y>>chunkBits, z>>chunkBits);
        Chunk chunk = chunks[packChunkPos(chunkPos.x(), chunkPos.y(), chunkPos.z())];
        int lX = x&15;
        int lY = y&15;
        int lZ = z&15;
        int pos = Chunk.condenseLocalPos(lX, lY, lZ);
        synchronized (chunk) {
            if (BlockTypes.blockTypes[chunk.getBlock(pos).x()].blockProperties.isFluidReplaceable) {
                chunk.setBlock(lX, lY, lZ, type, subType);
                updateLod(x, y, z, type == 0);
                updateRegion(chunkPos.x(), chunkPos.y(), chunkPos.z(), !(chunk.blockPalette.size() > 1 || chunk.blockPalette.getFirst() != 0));
            }
        }
        if (!generating) {
            if (updateLighting) {
                Light oldLight = chunk.getLight(pos);
                chunk.setLight(lX, lY, lZ, new Light(0, 0, 0, 0));
                updateHeightmap(x, y, z);
                LightHelper.recalculateLight(new Vector3i(x, y, z), oldLight);
            }
            if (!updateSet.contains(chunkPos)) { //may not need to do this since the light recalculation will prob do it
                updateSet.add(chunkPos);
                updateQueue.addLast(chunkPos);
            }
        }
    }
    public static void updateHeightmap(int x, int newY, int z) {
        int packedPos = packPos(x, z);
        int elevation = heightmap[packedPos];
        if (newY >= elevation) {
            int newElevation = elevation;
            boolean setHeightmap = false;
            for (int y = newY; y >= 0; y--) {
                if (!setHeightmap) {
                    Vector2i block = getBlock(x, y, z);
                    BlockType type = BlockTypes.blockTypes[block.x()];
                    if (!type.obstructingHeightmap(block)) {
                        Light light = getLight(x, y, z);
                        if (light.s() < maxSunlightLevel) {
                            Light newLight = new Light(light.s(), light.s(), light.s(), maxSunlightLevel);
                            setLight(x, y, z, newLight);
                        }
                    } else {
                        setHeightmap = true;
                        newElevation = y;
                        heightmap[packedPos] = (short) y;
                    }
                } else {
                    Light light = getLight(x, y, z);
                    if (light.s() > 0) {
                        Light newLight = new Light(light.s(), light.s(), light.s(), 0);
                        setLight(x, y, z, newLight);
                        LightHelper.recalculateLight(new Vector3i(x, y, z), light);
                    }
                }
            }
            for (int y = elevation - 1; y > newElevation; y--) {
                LightHelper.updateLight(new Vector3i(x, y, z), getBlock(x, y, z), getLight(x, y, z));
            }
        }
    }
    public static void updateLod(int x, int y, int z, boolean empty) {
        int lodIdx = packLodPos(x >>lodBits, y >>lodBits, z >>lodBits);
        int bitIdx = (x % lodSize) + (y % lodSize) * lodSize + (z % lodSize) * lodSize * lodSize;
        long mask = 1L << bitIdx;
        if (empty) {lods[lodIdx] &= ~mask;} else {lods[lodIdx] |= mask;}
    }
    public static void updateRegion(int cX, int cY, int cZ, boolean empty) {
        int regionIdx = packRegionPos(cX >>regionBits, cY >>regionBits, cZ >>regionBits);
        int bitIdx = (cX % regionSizeChunks) + (cY % regionSizeChunks) * regionSizeChunks + (cZ % regionSizeChunks) * regionSizeChunks * regionSizeChunks;
        long mask = 1L << bitIdx;
        if (empty) {regions[regionIdx] &= ~mask;} else {regions[regionIdx] |= mask;}
    }

    public static final ArrayDeque<Entity> entitiesAddQueue = new ArrayDeque<>();
    public static final List<Effect> effects = new ArrayList<>();
    public static final List<Entity> entities = new ArrayList<>(); //never add to this, add to the queue instead.
    public static void tick() {
        for (int i = 0; i < entities.size(); i++) {
            if(entities.get(i).tick()) {entities.remove(i--);}
        }
        while (!entitiesAddQueue.isEmpty()) {entities.add(entitiesAddQueue.poll());}
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i).tick()) {effects.remove(i--);}
        }
    }
}
