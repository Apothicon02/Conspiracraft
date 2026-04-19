package org.conspiracraft.graphics.textures;

import org.conspiracraft.Settings;
import org.conspiracraft.graphics.Swapchain;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK14.*;

public class Textures {
    public static List<Texture> textures = new ArrayList<>(List.of());
    public static Texture atlas;
    public static Texture noises;
    public static Texture colors1;
    public static Texture depth1;
    public static Texture norms1;
    public static Texture colors2;
    public static Texture depth2;
    public static Texture norms2;
    public static Texture gui;
    public static Texture items;
    public static Texture entities;

    public static Texture create(int width, int height, int channels, int format, int usage, boolean windowResizable) {
        Texture texture = new Texture(width, height, channels, format, usage, windowResizable);
        textures.addLast(texture);
        return texture;
    }
    public static Texture create(int width, int height, int depth, int channels, int format, int usage) {
        Texture texture = new Texture3D(width, height, depth, channels, format, usage, false);
        textures.addLast(texture);
        return texture;
    }

    public static void resize(MemoryStack stack) {
        for (Texture tex : textures) {
            if (tex.windowResizable) {
                tex.destroy();
                tex.width = Settings.width;
                tex.height = Settings.height;
                if (tex.format != VK_FORMAT_D32_SFLOAT) {tex.format = Swapchain.vkSurfFormat.format();}
                tex.create(stack);
            }
        }
    }
    public static void generate(MemoryStack stack) {
        atlas = create(584, 64, 1024/64, 4, VK_FORMAT_R8G8B8A8_SRGB, VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT);
        noises = create(2048, 2048, 4, VK_FORMAT_R8G8B8A8_SRGB, VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT, false);
        colors1 = create(Settings.width, Settings.height, 4, Swapchain.vkSurfFormat.format(), VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT, true);
        depth1 = create(Settings.width, Settings.height, 1, VK_FORMAT_D32_SFLOAT, VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT, true);
        norms1 = create(Settings.width, Settings.height, 4, Swapchain.vkSurfFormat.format(), VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT, true);
        colors2 = create(Settings.width, Settings.height, 4, Swapchain.vkSurfFormat.format(), VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT, true);
        depth2 = create(Settings.width, Settings.height, 1, VK_FORMAT_D32_SFLOAT, VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT, true);
        norms2 = create(Settings.width, Settings.height, 4, Swapchain.vkSurfFormat.format(), VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT, true);
        gui = create(3840, 2160, 6, 4, VK_FORMAT_R8G8B8A8_SRGB, VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT);
        items = create(4096, 16, 4, VK_FORMAT_R8G8B8A8_SRGB, VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT, false);
        entities = create(16, 48, 4, VK_FORMAT_R8G8B8A8_SRGB, VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT, false);
        textures.forEach((tex) -> {tex.create(stack);});
    }
}
