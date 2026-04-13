package org.conspiracraft.graphics.textures;

import org.conspiracraft.Settings;
import org.conspiracraft.graphics.Swapchain;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.conspiracraft.graphics.Device.vkDevice;
import static org.lwjgl.vulkan.VK14.*;

public class Textures {
    public static List<Texture> textures = new ArrayList<>(List.of());
    public static Texture atlas;
    public static Texture noises;
    public static Texture dda;

    public static Texture create(int width, int height, int channels, int format, boolean attachmentTexture) {
        Texture texture = new Texture(width, height, channels, format, attachmentTexture);
        textures.addLast(texture);
        return texture;
    }
    public static Texture create(int width, int height, int depth, int channels, int format) {
        Texture texture = new Texture3D(width, height, depth, channels, format, false);
        textures.addLast(texture);
        return texture;
    }

    public static void resize(MemoryStack stack) {
        for (Texture tex : textures) {
            if (tex.attachment) {
                tex.destroy();
                tex.width = Settings.width;
                tex.height = Settings.height;
                tex.format = Swapchain.vkSurfFormat.format();
                tex.create(stack);
            }
        }
    }
    public static void generate(MemoryStack stack) {
        atlas = create(584, 64, 1024/64, 4, VK_FORMAT_R8G8B8A8_SRGB);
        noises = create(2048, 2048, 4, VK_FORMAT_R8G8B8A8_SRGB, false);
        dda = create(Settings.width, Settings.height, 4, Swapchain.vkSurfFormat.format(), true);
        textures.forEach((tex) -> {tex.create(stack);});
    }
}
