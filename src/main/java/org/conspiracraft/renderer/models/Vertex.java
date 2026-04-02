package org.conspiracraft.renderer.models;

import org.joml.Vector3f;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import static org.lwjgl.vulkan.VK10.*;

public record Vertex(Vector3f pos, Vector3f normal) {
    public static final int size = 4*(3+3);
    public static VkVertexInputBindingDescription.Buffer getBindingDescription() {
        VkVertexInputBindingDescription.Buffer bindingDescription = VkVertexInputBindingDescription.calloc(1)
                .binding(0)
                .stride(size)
                .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
        return bindingDescription;
    }
    public static VkVertexInputAttributeDescription.Buffer getAttributeDescriptions() {
        VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.calloc(2);
        attributeDescriptions.get(0)
                .location(0)
                .binding(0)
                .format(VK_FORMAT_R32G32B32_SFLOAT)
                .offset(0);
        attributeDescriptions.get(1)
                .location(1)
                .binding(0)
                .format(VK_FORMAT_R32G32B32_SFLOAT)
                .offset(12);
        return attributeDescriptions;
    }
}
