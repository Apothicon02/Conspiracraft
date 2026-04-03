package org.conspiracraft.renderer.models;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import static org.lwjgl.vulkan.VK10.*;

public record Vertex(Vector3f pos, Vector3f normal, Vector2f uvs) {
    public static final int SIZE = 4*(3+3+2);
    public static VkVertexInputBindingDescription.Buffer getBindingDescription() {
        VkVertexInputBindingDescription.Buffer bindingDescription = VkVertexInputBindingDescription.calloc(1)
                .binding(0)
                .stride(SIZE)
                .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
        return bindingDescription;
    }
    public static VkVertexInputAttributeDescription.Buffer getAttributeDescriptions() {
        VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.calloc(3);
        attributeDescriptions.get(0) //pos
                .location(0)
                .binding(0)
                .format(VK_FORMAT_R32G32B32_SFLOAT)
                .offset(0);
        attributeDescriptions.get(1) //normal
                .location(1)
                .binding(0)
                .format(VK_FORMAT_R32G32B32_SFLOAT)
                .offset(12);
        attributeDescriptions.get(2) //uv
                .location(2)
                .binding(0)
                .format(VK_FORMAT_R32G32_SFLOAT)
                .offset(24);
        return attributeDescriptions;
    }
}
