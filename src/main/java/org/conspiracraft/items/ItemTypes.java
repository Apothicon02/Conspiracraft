package org.conspiracraft.items;

import org.conspiracraft.audio.SFX;
import org.conspiracraft.audio.Sounds;
import org.conspiracraft.blocks.types.BlockTypes;
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

public class ItemTypes {
    public static int itemTexSize = 16;
    public static Map<Integer, ItemType> itemTypeMap = new HashMap<>(Map.of());

    public static int getId(ItemType type) {
        int id = 0;
        for (ItemType mapBlocKType : itemTypeMap.values()) {
            if (mapBlocKType.equals(type)) {
                return id;
            }
            id++;
        }
        return 0;
    }

    public static ItemType
            AIR = create(new ItemType("misc/texture/air").maxStackSize(1)),
            STEEL_SCYTHE = create(new ItemType("tool/steel/texture/scythe").maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            STEEL_PICK = create(new ItemType("tool/steel/texture/pick").maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            STEEL_HATCHET = create(List.of(ItemTags.axe), new ItemType("tool/steel/texture/hatchet").maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            STEEL_SPADE = create(new ItemType("tool/steel/texture/spade").maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            STEEL_HOE = create(new ItemType("tool/steel/texture/hoe").maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            APPLE = create(new ItemType("food/texture/apple").maxStackSize(2)),
            ORANGE = create(new ItemType("food/texture/orange").maxStackSize(2)),
            CHERRY = create(new ItemType("food/texture/cherry").maxStackSize(2)),
            OAK_LOG = create(List.of(ItemTags.log), new ItemType("resource/texture/oak_log").maxStackSize(64).blockToPlace(BlockTypes.OAK_LOG.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            BIRCH_LOG = create(List.of(ItemTags.log), new ItemType("resource/texture/birch_log").maxStackSize(64).blockToPlace(BlockTypes.BIRCH_LOG.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            CHERRY_LOG = create(List.of(ItemTags.log), new ItemType("resource/texture/cherry_log").maxStackSize(64).blockToPlace(BlockTypes.CHERRY_LOG.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            MAHOGANY_LOG = create(List.of(ItemTags.log), new ItemType("resource/texture/mahogany_log").maxStackSize(64).blockToPlace(BlockTypes.MAHOGANY_LOG.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            ACACIA_LOG = create(List.of(ItemTags.log), new ItemType("resource/texture/acacia_log").maxStackSize(64).blockToPlace(BlockTypes.ACACIA_LOG.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            PALM_LOG = create(List.of(ItemTags.log), new ItemType("resource/texture/palm_log").maxStackSize(64).blockToPlace(BlockTypes.PALM_LOG.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            SPRUCE_LOG = create(List.of(ItemTags.log), new ItemType("resource/texture/spruce_log").maxStackSize(64).blockToPlace(BlockTypes.SPRUCE_LOG.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            WILLOW_LOG = create(List.of(ItemTags.log), new ItemType("resource/texture/willow_log").maxStackSize(64).blockToPlace(BlockTypes.WILLOW_LOG.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            REDWOOD_LOG = create(List.of(ItemTags.log), new ItemType("resource/texture/redwood_log").maxStackSize(64).blockToPlace(BlockTypes.REDWOOD_LOG.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            OAK_PLANK = create(new ItemType("component/texture/oak_plank").blockToPlace(BlockTypes.OAK_PLANK.id, 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            BIRCH_PLANK = create(new ItemType("component/texture/birch_plank").blockToPlace(BlockTypes.BIRCH_PLANK.id, 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            CHERRY_PLANK = create(new ItemType("component/texture/cherry_plank").blockToPlace(BlockTypes.CHERRY_PLANK.id, 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            MAHOGANY_PLANK = create(new ItemType("component/texture/mahogany_plank").blockToPlace(BlockTypes.MAHOGANY_PLANK.id, 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            ACACIA_PLANK = create(new ItemType("component/texture/acacia_plank").blockToPlace(BlockTypes.ACACIA_PLANK.id, 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            PALM_PLANK = create(new ItemType("component/texture/palm_plank").blockToPlace(BlockTypes.PALM_PLANK.id, 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            SPRUCE_PLANK = create(new ItemType("component/texture/spruce_plank").blockToPlace(BlockTypes.SPRUCE_PLANK.id, 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            WILLOW_PLANK = create(new ItemType("component/texture/willow_plank").blockToPlace(BlockTypes.WILLOW_PLANK.id, 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            REDWOOD_PLANK = create(new ItemType("component/texture/redwood_plank").blockToPlace(BlockTypes.REDWOOD_PLANK.id, 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            STICK = create(new ItemType("resource/texture/stick").maxStackSize(64).blockToPlace(BlockTypes.STICK.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.33f, 1.33f))),
            STONE = create(new ItemType("resource/texture/stone").maxStackSize(64).blockToPlace(BlockTypes.STONE.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 0.5f, 0.75f))),
            MARBLE = create(new ItemType("resource/texture/marble").blockToPlace(BlockTypes.MARBLE.id, 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 0.45f, 0.85f))),
            GLASS = create(new ItemType("component/texture/glass").blockToPlace(BlockTypes.GLASS.id, 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.5f, 0.8f))),
            MAGENTA_STAINED_GLASS = create(new ItemType("component/texture/magenta_stained_glass").blockToPlace(BlockTypes.MAGENTA_STAINED_GLASS.id, 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.5f, 0.8f))),
            LIME_STAINED_GLASS = create(new ItemType("component/texture/lime_stained_glass").blockToPlace(BlockTypes.LIME_STAINED_GLASS.id, 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.5f, 0.8f))),
            BLUE_STAINED_GLASS = create(new ItemType("component/texture/blue_stained_glass").blockToPlace(BlockTypes.BLUE_STAINED_GLASS.id, 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.5f, 0.8f))),
            RED_STAINED_GLASS = create(new ItemType("component/texture/red_stained_glass").blockToPlace(BlockTypes.RED_STAINED_GLASS.id, 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.5f, 0.8f))),
            TORCH = create(new ItemType("block/texture/torch").blockToPlace(BlockTypes.TORCH.id, 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.4f, 1.25f))),
            MAGMA = create(new ItemType("resource/texture/magma").blockToPlace(BlockTypes.MAGMA.id, 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.SIZZLE1, Sounds.SIZZLE2}, 0.45f, 0.95f))),
            PORECAP = create(new ItemType("plant/texture/porecap").blockToPlace(BlockTypes.PORECAP.id, 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.DIRT_STEP1, Sounds.DIRT_STEP2, Sounds.DIRT_STEP3}, 0.45f, 0.95f))),
            GRASS = create(new ItemType("plant/texture/grass").blockToPlace(BlockTypes.TALL_GRASS.id, 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.GRASS_STEP1, Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 0.45f, 1.f))),
            ROSE = create(new ItemType("plant/texture/rose").blockToPlace(BlockTypes.ROSE.id, 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.GRASS_STEP1, Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 0.45f, 1.f))),
            HYDRANGEA = create(new ItemType("plant/texture/hydrangea").blockToPlace(BlockTypes.HYDRANGEA.id, 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.GRASS_STEP1, Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 0.45f, 1.f))),
            KYANITE = create(new ItemType("resource/texture/kyanite").blockToPlace(BlockTypes.KYANITE.id, 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.6f, 1.2f))),
            FLINT = create(new ItemType("resource/texture/flint").maxStackSize(64).blockToPlace(BlockTypes.FLINT.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 0.5f, 1.05f))),
            IRON_ORE = create(new ItemType("resource/texture/iron").maxStackSize(64).blockToPlace(BlockTypes.IRON_ORE.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 0.6f))),
            COPPER_ORE = create(new ItemType("resource/texture/copper").maxStackSize(64).blockToPlace(BlockTypes.COPPER_ORE.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 0.6f))),
            GRAVEL = create(new ItemType("resource/texture/gravel").maxStackSize(64).blockToPlace(BlockTypes.GRAVEL.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.GRAVEL_STEP1, Sounds.GRAVEL_STEP2}, 0.4f, 1.f))),
            SAND = create(new ItemType("resource/texture/sand").maxStackSize(64).blockToPlace(BlockTypes.SAND.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.SAND_STEP1, Sounds.SAND_STEP2}, 0.4f, 1.f))),
            SANDSTONE = create(new ItemType("resource/texture/sandstone").maxStackSize(64).blockToPlace(BlockTypes.SANDSTONE.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 0.5f, 1.f))),
            RED_SAND = create(new ItemType("resource/texture/red_sand").maxStackSize(64).blockToPlace(BlockTypes.RED_SAND.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.SAND_STEP1, Sounds.SAND_STEP2}, 0.4f, 1.f))),
            RED_SANDSTONE = create(new ItemType("resource/texture/red_sandstone").maxStackSize(64).blockToPlace(BlockTypes.RED_SANDSTONE.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 0.5f, 1.f))),
            RED_GRAVEL = create(new ItemType("resource/texture/red_gravel").maxStackSize(64).blockToPlace(BlockTypes.RED_GRAVEL.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.GRAVEL_STEP1, Sounds.GRAVEL_STEP2}, 0.4f, 1.f))),
            MARTIAN_REGOLITH = create(new ItemType("resource/texture/martian_regolith").maxStackSize(64).blockToPlace(BlockTypes.ORANGE_SAND.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.SAND_STEP1, Sounds.SAND_STEP2}, 0.4f, 1.f))),
            REGOLITH = create(new ItemType("resource/texture/regolith").maxStackSize(64).blockToPlace(BlockTypes.REGOLITH.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.SAND_STEP1, Sounds.SAND_STEP2}, 0.4f, 1.f))),
            DIRT = create(new ItemType("resource/texture/dirt").maxStackSize(64).blockToPlace(BlockTypes.DIRT.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.DIRT_STEP1, Sounds.DIRT_STEP2, Sounds.DIRT_STEP3}, 0.5f, 1.f))),
            GRASSY_DIRT = create(new ItemType("resource/texture/grassy_dirt").maxStackSize(64).blockToPlace(BlockTypes.GRASS.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.DIRT_STEP1, Sounds.DIRT_STEP2, Sounds.DIRT_STEP3}, 0.5f, 1.f))),
            CLAY = create(new ItemType("resource/texture/clay").maxStackSize(64).blockToPlace(BlockTypes.CLAY.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.MUD_STEP1, Sounds.MUD_STEP2}, 0.5f, 0.66f))),
            MUD = create(new ItemType("resource/texture/mud").maxStackSize(64).blockToPlace(BlockTypes.MUD.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.MUD_STEP1, Sounds.MUD_STEP2}, 0.5f, 0.66f))),
            STEEL_FRAME = create(new ItemType("component/texture/steel_frame").maxStackSize(64).blockToPlace(BlockTypes.STEEL_FRAME.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            SCREW = create(new ItemType("component/texture/screw").maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            STEEL_ROD = create(new ItemType("component/texture/steel_rod").maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            POWERED_VENT = create(new ItemType("machine/texture/powered_vent").maxStackSize(64).blockToPlace(BlockTypes.POWERED_VENT.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f)));

    private static ItemType create(ItemType type) {
        itemTypeMap.put(itemTypeMap.size(), type);
        return type;
    }

    private static ItemType create(List<ItemTag> tags, ItemType type) {
        for (ItemTag tag : tags) {
            tag.tagged.add(type);
        }
        type.tags = tags;
        itemTypeMap.put(itemTypeMap.size(), type);
        return type;
    }
    public static void fillTexture(MemoryStack stack) throws IOException {
        int texSize = Textures.items.width*Textures.items.height;
        Buffer stagingBuffer = new Buffer(stack, texSize*4, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, true);

        int xOffset = 0;
        int yOffset = 0;
        for (ItemType itemType : itemTypeMap.values()) {
            ByteBuffer itemBuf = Utils.imageToBuffer(Utils.loadImage("item/"+itemType.name));
            for (long row = 0; row < itemTexSize; row++) {
                memCopy(memAddress(itemBuf) + row * itemTexSize * 4L,
                        stagingBuffer.pointer.get(0) + (((yOffset + row) * Textures.items.width + xOffset) * 4L),
                        itemTexSize * 4L);
            }
            memFree(itemBuf);
            itemType.atlasOffset(xOffset, yOffset);
            xOffset += itemTexSize;
            if (xOffset + itemTexSize > Textures.items.width) {
                xOffset = 0;
                yOffset += itemTexSize;
            }
        }
        ImageHelper.fillImage(stack, Textures.items, stagingBuffer);
    }
}
