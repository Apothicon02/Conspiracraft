package org.conspiracraft.graphics.models;

import org.joml.Vector3f;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import static org.lwjgl.vulkan.VK10.*;

public record Vertex(Vector3f pos) {
    public static final int SIZE = 4*3;
    public static VkVertexInputBindingDescription.Buffer getBindingDescription() {
        VkVertexInputBindingDescription.Buffer bindingDescription = VkVertexInputBindingDescription.calloc(1)
                .binding(0)
                .stride(SIZE)
                .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
        return bindingDescription;
    }
    public static VkVertexInputAttributeDescription.Buffer getAttributeDescriptions() {
        VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.calloc(1);
        attributeDescriptions.get(0)
                .location(0)
                .binding(0)
                .format(VK_FORMAT_R32G32B32_SFLOAT)
                .offset(0);
        return attributeDescriptions;
    }
}
