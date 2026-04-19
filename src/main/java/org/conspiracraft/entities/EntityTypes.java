package org.conspiracraft.entities;

import org.conspiracraft.graphics.buffers.Buffer;
import org.conspiracraft.graphics.textures.ImageHelper;
import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.utils.Utils;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class EntityTypes {
    public static int entityTexWidth = 8;
    public static int entityTexHeight = 48;
    public static Map<Integer, EntityType> entityTypeMap = new HashMap<>(Map.of());

    public static EntityType
            SUN = create(new EntityType("celestial/texture/sun")),
            COW = create(new EntityType("animal/texture/cow"));

    private static EntityType create(EntityType type) {
        entityTypeMap.put(entityTypeMap.size(), type);
        return type;
    }
    public static void fillTexture(MemoryStack stack) throws IOException {
        int texSize = Textures.entities.width*Textures.entities.height;
        Buffer stagingBuffer = new Buffer(stack, texSize*4, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, true);

        int xOffset = 0;
        int yOffset = 0;
        for (EntityType entityType : entityTypeMap.values()) {
            ByteBuffer entityBuf = Utils.imageToBuffer(Utils.loadImage("entity/"+entityType.name));
            for (long row = 0; row < entityTexHeight; row++) {
                memCopy(memAddress(entityBuf) + row * entityTexWidth * 4L,
                        stagingBuffer.pointer.get(0) + (((yOffset + row) * Textures.entities.width + xOffset) * 4L),
                        entityTexWidth * 4L);
            }
            memFree(entityBuf);
            entityType.atlasOffset(xOffset, yOffset);
            xOffset += entityTexWidth;
            if (xOffset + entityTexWidth > Textures.entities.width) {
                xOffset = 0;
                yOffset += entityTexHeight;
            }
        }
        ImageHelper.fillImage(stack, Textures.entities, stagingBuffer);
    }
}
