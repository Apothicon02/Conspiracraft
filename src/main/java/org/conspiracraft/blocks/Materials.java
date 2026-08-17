package org.conspiracraft.blocks;

import org.conspiracraft.blocks.types.BlockType;
import org.conspiracraft.graphics.buffers.Buffer;
import org.conspiracraft.graphics.textures.ImageHelper;
import org.conspiracraft.graphics.textures.Texture3D;
import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.utils.Utils;
import org.lwjgl.system.MemoryStack;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.vulkan.VK10.*;

public class Materials {
    public static final int materialWidth = 16;
    public static final long materialWidthL = 16;
    public static final int materialHeight = materialWidth*6;
    private static List<Material> materialsList = new ArrayList<>();

    public static final Material
            AIR = create("misc/material/air"),
            WATER = create("natural/material/water"),
            CLOUD = create("natural/material/cloud"),
            RAIN_CLOUD = create("natural/material/rain_cloud"),
            GRASS = create("plant/material/grass"),
            DARK_GRASS = create("plant/material/dark_grass"),
            DRY_GRASS = create("plant/material/dry_grass"),
            LIME_GRASS = create("plant/material/lime_grass"),
            GRASS_TOP = create("plant/material/grass_top"),
            DARK_GRASS_TOP = create("plant/material/dark_grass_top"),
            DRY_GRASS_TOP = create("plant/material/dry_grass_top"),
            LIME_GRASS_TOP = create("plant/material/lime_grass_top"),
            DIRT = create("natural/material/dirt"),
            SNOW = create("natural/material/snow"),
            ICE = create("natural/material/ice"),
            MUD = create("natural/material/mud"),
            DRY_MUD = create("natural/material/dry_mud"),
            SAND = create("natural/material/sand"),
            WET_SAND = create("natural/material/wet_sand"),
            SANDSTONE = create("geological/material/sandstone"),
            ORANGE_SAND = create("natural/material/orange_sand"),
            ORANGE_SANDSTONE = create("geological/material/orange_sandstone"),
            RED_SAND = create("natural/material/red_sand"),
            RED_SANDSTONE = create("geological/material/red_sandstone"),
            GRAVEL = create("natural/material/gravel"),
            FLINT = create("geological/material/flint"),
            ROSE = create("plant/material/rose"),
            BAMBOO = create("plant/material/bamboo"),
            BAMBOO_RING = create("plant/material/bamboo_ring"),
            CACTUS = create("plant/material/cactus"),
            CACTUS_BARB = create("plant/material/cactus_barb"),
            HYDRANGEA = create("plant/material/hydrangea"),
            PORECAP = create("plant/material/porecap"),
            PORECAP_STEM = create("plant/material/porecap_stem"),
            TORCH_FLAME = create("crafted/material/torch_flame"),
            MARBLE = create("geological/material/marble"),
            STONE = create("geological/material/stone"),
            IGNEOUS = create("geological/material/igneous"),
            MAGMA = create("geological/material/magma"),
            KYANITE = create("geological/material/kyanite"),
            GLASS = create("crafted/material/glass"),
            RED_GLASS = create("crafted/material/red_stained_glass"),
            BLUE_GLASS = create("crafted/material/blue_stained_glass"),
            LIME_GLASS = create("crafted/material/lime_stained_glass"),
            MAGENTA_GLASS = create("crafted/material/magenta_stained_glass"),
            OAK_LOG = create("tree/material/oak_log"),
            BIRCH_LOG = create("tree/material/birch_log"),
            CHERRY_LOG = create("tree/material/cherry_log"),
            SPRUCE_LOG = create("tree/material/spruce_log"),
            PALM_LOG = create("tree/material/palm_log"),
            MAHOGANY_LOG = create("tree/material/mahogany_log"),
            WILLOW_LOG = create("tree/material/willow_log"),
            ACACIA_LOG = create("tree/material/acacia_log"),
            REDWOOD_LOG = create("tree/material/redwood_log"),
            DEAD_LOG = create("tree/material/dead_log"),
            OAK_PLANK = create("tree/material/oak_planks"),
            BIRCH_PLANK = create("tree/material/birch_planks"),
            CHERRY_PLANK = create("tree/material/cherry_planks"),
            SPRUCE_PLANK = create("tree/material/spruce_planks"),
            PALM_PLANK = create("tree/material/palm_planks"),
            MAHOGANY_PLANK = create("tree/material/mahogany_planks"),
            WILLOW_PLANK = create("tree/material/willow_planks"),
            ACACIA_PLANK = create("tree/material/acacia_planks"),
            REDWOOD_PLANK = create("tree/material/redwood_planks"),
            DEAD_PLANK = create("tree/material/dead_planks"),
            OAK_LEAVES = create("tree/material/oak_leaves"),
            BIRCH_LEAVES = create("tree/material/birch_leaves"),
            CHERRY_LEAVES = create("tree/material/cherry_leaves"),
            SPRUCE_LEAVES = create("tree/material/spruce_leaves"),
            PALM_LEAVES = create("tree/material/palm_leaves"),
            MAHOGANY_LEAVES = create("tree/material/mahogany_leaves"),
            WILLOW_LEAVES = create("tree/material/willow_leaves"),
            ACACIA_LEAVES = create("tree/material/acacia_leaves"),
            REDWOOD_LEAVES = create("tree/material/redwood_leaves"),
            DEAD_LEAVES = create("tree/material/dead_leaves"),
            BARREL = create("crafted/barrel/material/oak"),
            BLUEPRINT = create("machine/material/blueprint"),
            STEEL = create("crafted/material/steel");

    private static Material create(String path) {
        Material material = new Material(path, materialsList.size());
        materialsList.add(material);
        return material;
    }

    public static final Material[] materials = createArr();
    public static Material[] createArr() {
        Material[] newArr = materialsList.toArray(new Material[0]);
        materialsList = null;
        return newArr;
    }
    public static boolean reloading = false;
    public static Buffer materialBuffer;
    public static void fillTexture(MemoryStack stack) throws IOException {
        int texSize = Textures.materials.width*Textures.materials.height;
        if (materialBuffer == null) {
            materialBuffer = new Buffer(stack, texSize * 4, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, true);
        }
        for (int i = 0; i < materials.length; i++) {
            Material material = materials[i];
            BufferedImage image = Utils.loadImage("block/"+material.path());
            int height = image.getHeight();
            ByteBuffer matBuf = Utils.imageToBuffer(image);
            if (height <= materialWidth) {
                copyTexture(matBuf, i, materialWidth, 0, 0);
                copyTexture(matBuf, i, materialWidth, 0, materialWidthL*Textures.materials.width);
                copyTexture(matBuf, i, materialWidth, 0, materialWidthL*Textures.materials.width*2);
                copyTexture(matBuf, i, materialWidth, 0, materialWidthL*Textures.materials.width*3);
                copyTexture(matBuf, i, materialWidth, 0, materialWidthL*Textures.materials.width*4);
                copyTexture(matBuf, i, materialWidth, 0, materialWidthL*Textures.materials.width*5);
            } else if (height < materialHeight) {
                copyTexture(matBuf, i, materialWidth, 0, 0);
                copyTexture(matBuf, i, materialWidth, materialWidth*materialWidth, materialWidthL*Textures.materials.width);
                copyTexture(matBuf, i, materialWidth, materialWidth*materialWidth, materialWidthL*Textures.materials.width*2);
                copyTexture(matBuf, i, materialWidth, materialWidth*materialWidth, materialWidthL*Textures.materials.width*3);
                copyTexture(matBuf, i, materialWidth, materialWidth*materialWidth, materialWidthL*Textures.materials.width*4);
                copyTexture(matBuf, i, materialWidth, materialWidth*materialWidth*2, materialWidthL*Textures.materials.width*5);
            } else {
                copyTexture(matBuf, i, materialHeight, 0, 0);
            }
        }
        ImageHelper.fillImage(stack, Textures.materials, materialBuffer, reloading);
        reloading = true;
    }
    public static void copyTexture(ByteBuffer buf, int i, int height, long srcOffset, long dstOffset) {
        long loopedI = i * materialWidthL;
        long matRow = loopedI/Textures.materials.width;
        loopedI = (loopedI-(matRow*Textures.materials.width))+(matRow*Textures.materials.width*materialHeight);
        for (long row = 0; row < height; row++) {
            memCopy(memAddress(buf) + ((srcOffset+(row * materialWidth)) * 4L),
                    materialBuffer.pointer.get(0) + ((dstOffset + (row * Textures.materials.width + loopedI)) * 4L),
                    materialWidth * 4L);
        }
    }
}
