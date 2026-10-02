package org.conspiracraft.world;

import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.graphics.Renderer;
import org.conspiracraft.utils.Utils;
import org.joml.Vector3i;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkBufferCopy;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Arrays;

import static org.conspiracraft.graphics.Graphics.chunkSSBO;
import static org.conspiracraft.graphics.Graphics.lightChunkSSBO;
import static org.conspiracraft.graphics.Renderer.currentCmdBuffer;
import static org.conspiracraft.world.World.*;
import static org.lwjgl.util.vma.Vma.vmaVirtualFree;
import static org.lwjgl.vulkan.VK10.vkCmdCopyBuffer;

public class Region {
    public final long condensedRegionPos;
    public final long rX, rY, rZ;
    public final int rXI, rYI, rZI;
    public static final int totalChunks = regionSizeChunks*regionSizeChunks*regionSizeChunks;
    public final Chunk[] chunks;
    public boolean generated = false;
    public boolean neighborsGenerated = false;
    public final ArrayDeque<Vector3i> lightQueueSkipWG = new ArrayDeque<>();
    public final ArrayDeque<Vector3i> lightQueueSkip = new ArrayDeque<>();

    public void save(String basePath) throws IOException {
        if (!generated || !neighborsGenerated) {return;}
        String path = basePath+"regions/";
        new File(path).mkdirs();
        FileChannel out = FileChannel.open(Path.of(path+condensedRegionPos+".data"), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        int dataSize = 0;
        for (Chunk chunk : chunks) {
            int[] blockData = chunk.getBlockData();
            int[] lightData = chunk.getLightData();
            long[] lodData = chunk.lods;
            dataSize += 5+chunk.blockPalette.size()+(blockData == null ? 0 : blockData.length)+chunk.lightPalette.size()+(lightData == null ? 0 : lightData.length)+(lodData == null ? 0 : lodData.length*2);
        }
        MappedByteBuffer data = out.map(FileChannel.MapMode.READ_WRITE, 0, dataSize * 4L);
        data.order(ByteOrder.BIG_ENDIAN);
        int[] emptyData = new int[0];
        long[] emptyDataL = new long[0];
        for (Chunk chunk : chunks) {
            int[] subdata  = chunk.getBlockData();
            subdata = subdata == null ? emptyData : subdata;
            int[] palette = chunk.getBlockPalette();
            putInts(data, palette);
            putInts(data, subdata);
            subdata = chunk.getLightData();
            subdata = subdata == null ? emptyData : subdata;
            palette = chunk.getLightPalette();
            putInts(data, palette);
            putInts(data, subdata);
            long[] subdataL = chunk.lods;
            subdataL = subdataL == null ? emptyDataL : subdataL;
            putLongs(data, subdataL);
        }
        Utils.unmap(data);
        out.close();
    }

    public static void putInts(ByteBuffer buf, int[] ints) {
        buf.putInt(ints.length);
        buf.asIntBuffer().put(ints);
        buf.position(buf.position()+ints.length*4);
    }
    public static void putLongs(ByteBuffer buf, long[] longs) {
        buf.putInt(longs.length);
        buf.asLongBuffer().put(longs);
        buf.position(buf.position()+longs.length*8);
    }

    public boolean load(String basePath) throws IOException {
        Path path = Path.of(basePath + "regions/" + condensedRegionPos + ".data");
        if (!Files.exists(path)) {return false;}
        FileChannel in = FileChannel.open(path, StandardOpenOption.READ);
        MappedByteBuffer data = in.map(FileChannel.MapMode.READ_ONLY, 0, in.size());
        data.order(ByteOrder.BIG_ENDIAN);
        for (Chunk chunk : chunks) {
            chunk.setBlockPalette(getInts(data));
            chunk.setBlockData(getInts(data));
            chunk.setLightPalette(getInts(data));
            chunk.setLightData(getInts(data));
            chunk.setLodData(getLongs(data));
        }
        Utils.unmap(data);
        in.close();
        return true;
    }

    public static int[] getInts(ByteBuffer buf) {
        int[] ints = new int[buf.getInt()];
        buf.asIntBuffer().get(ints);
        buf.position(buf.position()+ints.length*4);
        return ints;
    }
    public static long[] getLongs(ByteBuffer buf) {
        long[] longs = new long[buf.getInt()];
        buf.asLongBuffer().get(longs);
        buf.position(buf.position()+longs.length*8);
        return longs;
    }

    public void setGenerated() {
        generated = true;
        updateNeighborsGeneratedAndTheirNeighbors();
    }
    public void updateNeighborsGeneratedAndTheirNeighbors() {
        for (int x = rXI-1; x <= rXI+1; x++) {
            for (int y = rYI - 1; y <= rYI + 1; y++) {
                for (int z = rZI - 1; z <= rZI + 1; z++) {
                    long cRP = World.packRegionPos(x, y, z);
                    Region region = getRegion(cRP);
                    if (region != null) {
                        region.updateNeighborsGenerated();
                    }
                }
            }
        }
    }
    public void updateNeighborsGenerated() {
        if (!neighborsGenerated) {
            boolean safe = true;
            loop:
            for (int x = rXI - 1; x <= rXI + 1; x++) {
                for (int y = rYI - 1; y <= rYI + 1; y++) {
                    for (int z = rZI - 1; z <= rZI + 1; z++) {
                        long cRP = World.packRegionPos(x, y, z);
                        Region region = getRegion(cRP);
                        if (region == null || !region.generated) {
                            safe = false;
                            break loop;
                        }
                    }
                }
            }
            neighborsGenerated = safe;
            if (neighborsGenerated) {
                while (!lightQueueSkip.isEmpty()) {
                    LightHelper.queueLightUpdate(lightQueueSkip.pollFirst());
                }
                while (!lightQueueSkipWG.isEmpty()) {
                    LightHelper.queueLightUpdate(LightHelper.lightQueueWG, lightQueueSkipWG.pollFirst());
                }
                for (Chunk chunk : chunks) {
                    updateQueue.addLast(chunk.cCP);
                }
            }
        }
    }

    public Region(long condensedRegionPos) {
        this.condensedRegionPos = condensedRegionPos;
        this.rX = (condensedRegionPos >> 42) & 0x3FFFFF;
        this.rZ = (condensedRegionPos >> 20) & 0x3FFFFF;
        this.rY = condensedRegionPos & 0xFFFFF;
        this.rXI = (int)this.rX;
        this.rYI = (int)this.rY;
        this.rZI = (int)this.rZ;
        chunks = new Chunk[totalChunks];
        for (int cX = rXI*regionSizeChunks; cX < (rXI*regionSizeChunks)+regionSizeChunks; cX++) {
            for (int cY = rYI*regionSizeChunks; cY < (rYI*regionSizeChunks)+regionSizeChunks; cY++) {
                for (int cZ = rZI*regionSizeChunks; cZ < (rZI*regionSizeChunks)+regionSizeChunks; cZ++) {
                    chunks[packLocalPos(cX%regionSizeChunks, cY%regionSizeChunks, cZ%regionSizeChunks)] = new Chunk(World.packChunkPos(cX, cY, cZ));
                }
            }
        }
    }

    public static int packLocalPos(int x, int y, int z) {
        return (((x*regionSizeChunks)+z)*regionSizeChunks)+y;
    }
    public static int packLocalPos(Vector3i pos) {
        return (((pos.x*regionSizeChunks)+pos.z)*regionSizeChunks)+pos.y;
    }

    public Chunk getChunk(int cP) {
        return chunks[cP];
    }

    public void unload() throws IOException {
        save(World.worldType.getWorldPath() + "/");
        for (Chunk chunk : chunks) {
            vmaVirtualFree(Renderer.blocks.get(0), Renderer.chunkBlockAllocs.get(chunk.cCP));
            vmaVirtualFree(Renderer.lights.get(0), Renderer.chunkLightBlockAllocs.get(chunk.cCP));
            long wPackedChunkPos = ((((chunk.cX % sizeChunks) * sizeChunks) + (chunk.cZ % sizeChunks)) * heightChunks) + (chunk.cY % heightChunks);
            long chunkPtr = chunkSSBO.stagingBuffer.pointer.get(0);
            long chunkBufOffset = wPackedChunkPos * Renderer.chunkByteSize;
            MemoryUtil.memIntBuffer(chunkPtr + chunkBufOffset, 7);
            VkBufferCopy.Buffer chunkBufferCopy = VkBufferCopy.calloc(1).srcOffset(chunkBufOffset).dstOffset(chunkBufOffset).size(Renderer.chunkByteSize);
            vkCmdCopyBuffer(currentCmdBuffer, chunkSSBO.stagingBuffer.buffer[0], chunkSSBO.buffer.buffer[0], chunkBufferCopy);

            chunkPtr = lightChunkSSBO.stagingBuffer.pointer.get(0);
            MemoryUtil.memIntBuffer(chunkPtr + chunkBufOffset, 7);
            vkCmdCopyBuffer(currentCmdBuffer, lightChunkSSBO.stagingBuffer.buffer[0], lightChunkSSBO.buffer.buffer[0], chunkBufferCopy);
        }
        World.removeRegion(condensedRegionPos);
    }
}
