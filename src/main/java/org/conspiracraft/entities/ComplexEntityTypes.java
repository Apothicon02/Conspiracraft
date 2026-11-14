package org.conspiracraft.entities;

import org.conspiracraft.graphics.buffers.Buffer;
import org.conspiracraft.graphics.textures.ImageHelper;
import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.utils.Utils;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class ComplexEntityTypes {
    public static int entityTexWidth = 24;
    public static int entityTexHeight = 72;
    public static Map<Integer, ComplexEntityType> entityTypeMap = new HashMap<>(Map.of());

    public static int getId(EntityType type) {
        int id = 0;
        for (EntityType mapBlocKType : entityTypeMap.values()) {
            if (mapBlocKType.equals(type)) {
                return id;
            }
            id++;
        }
        return 0;
    }

    public static ComplexEntityType
            ZOMBIE = create(List.of(EntityTags.hostile), (ComplexEntityType) new ComplexEntityType("mob/texture/zombie").setScale(0.8f, 1.95f, 0.8f));

    private static ComplexEntityType create(ComplexEntityType type) {
        entityTypeMap.put(entityTypeMap.size(), type);
        return type;
    }
    private static ComplexEntityType create(List<EntityTag> tags, ComplexEntityType type) {
        for (EntityTag tag : tags) {
            tag.tagged.add(entityTypeMap.size());
        }
        entityTypeMap.put(entityTypeMap.size(), type);
        return type;
    }
    public static void fillTexture(MemoryStack stack) throws IOException {
        int texSize = Textures.complexEntities.width*Textures.complexEntities.height;
        Buffer stagingBuffer = new Buffer(stack, texSize*4, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, true);

        int xOffset = 0;
        int yOffset = 0;
        for (EntityType entityType : entityTypeMap.values()) {
            ByteBuffer entityBuf = Utils.imageToBuffer(Utils.loadImage("entity/"+entityType.name));
            for (long row = 0; row < entityTexHeight; row++) {
                memCopy(memAddress(entityBuf) + row * entityTexWidth * 4L,
                        stagingBuffer.pointer.get(0) + (((yOffset + row) * Textures.complexEntities.width + xOffset) * 4L),
                        entityTexWidth * 4L);
            }
            memFree(entityBuf);
            entityType.atlasOffset(xOffset, yOffset);
            xOffset += entityTexWidth;
            if (xOffset + entityTexWidth > Textures.complexEntities.width) {
                xOffset = 0;
                yOffset += entityTexHeight;
            }
        }
        ImageHelper.fillImage(stack, Textures.complexEntities, stagingBuffer);
    }
}
