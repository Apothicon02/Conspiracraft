package org.conspiracraft.graphics.textures;

import org.conspiracraft.world.World;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.conspiracraft.graphics.Device.vkDevice;
import static org.lwjgl.vulkan.VK14.*;

public class Textures {
    public static List<Texture> textures = new ArrayList<>(List.of());
    public static Texture atlas = create(584, 64, 1024/64, 4);

    public static Texture create(int width, int height, int channels) {
        Texture texture = new Texture(width, height, channels);
        textures.addLast(texture);
        return texture;
    }
    public static Texture create(int width, int height, int depth, int channels) {
        Texture texture = new Texture3D(width, height, depth, channels);
        textures.addLast(texture);
        return texture;
    }

    public static void generate(MemoryStack stack) {
        textures.forEach((texture) -> {
            int texFormat = texture.channels == 4 ? VK_FORMAT_R8G8B8A8_SRGB : (texture.channels == 3 ? VK_FORMAT_R8G8B8_SRGB : (texture.channels == 2 ? VK_FORMAT_R8G8_SRGB : VK_FORMAT_R8_SRGB));
            long[] imageData = ImageHelper.createImage(stack, texture.width, texture.height, texture instanceof Texture3D tex3D ? tex3D.depth : 1, texFormat, VK_IMAGE_TILING_OPTIMAL, VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
            texture.image = imageData[0];
            texture.memory = imageData[1];
            texture.imageView = ImageHelper.createImageView(stack, texture instanceof Texture3D, texture.image, texFormat, texture.channels);
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                    .magFilter(VK_FILTER_LINEAR)
                    .minFilter(VK_FILTER_LINEAR)
                    .addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .anisotropyEnable(true)
                    .maxAnisotropy(16)
                    .borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK)
                    .unnormalizedCoordinates(false);
            LongBuffer samplerBuf = stack.mallocLong(1);
            vkCreateSampler(vkDevice, samplerInfo, null, samplerBuf);
            texture.sampler = samplerBuf.get(0);
        });
    }
}
