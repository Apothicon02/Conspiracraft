package org.conspiracraft.world;

import org.conspiracraft.graphics.Renderer;
import org.joml.Vector3i;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkBufferCopy;

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

    public void unload() {
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
