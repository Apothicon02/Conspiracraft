package org.conspiracraft.items;

import org.conspiracraft.audio.SFX;
import org.conspiracraft.audio.Sounds;
import org.conspiracraft.blocks.types.BlockTypes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ItemTypes {
    public static int itemTexSize = 16;
    public static int itemTexByteSize = (itemTexSize*itemTexSize)*4;
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
            OAK_LOG = create(List.of(ItemTags.log), new ItemType("resource/texture/oak_log").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.OAK_LOG), 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            BIRCH_LOG = create(List.of(ItemTags.log), new ItemType("resource/texture/birch_log").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.BIRCH_LOG), 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            CHERRY_LOG = create(List.of(ItemTags.log), new ItemType("resource/texture/cherry_log").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.CHERRY_LOG), 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            MAHOGANY_LOG = create(List.of(ItemTags.log), new ItemType("resource/texture/mahogany_log").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.MAHOGANY_LOG), 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            ACACIA_LOG = create(List.of(ItemTags.log), new ItemType("resource/texture/acacia_log").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.ACACIA_LOG), 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            PALM_LOG = create(List.of(ItemTags.log), new ItemType("resource/texture/palm_log").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.PALM_LOG), 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            SPRUCE_LOG = create(List.of(ItemTags.log), new ItemType("resource/texture/spruce_log").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.SPRUCE_LOG), 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            WILLOW_LOG = create(List.of(ItemTags.log), new ItemType("resource/texture/willow_log").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.WILLOW_LOG), 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            REDWOOD_LOG = create(List.of(ItemTags.log), new ItemType("resource/texture/redwood_log").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.REDWOOD_LOG), 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            OAK_PLANK = create(new ItemType("component/texture/oak_plank").blockToPlace(BlockTypes.getId(BlockTypes.OAK_PLANK), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            BIRCH_PLANK = create(new ItemType("component/texture/birch_plank").blockToPlace(BlockTypes.getId(BlockTypes.BIRCH_PLANK), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            CHERRY_PLANK = create(new ItemType("component/texture/cherry_plank").blockToPlace(BlockTypes.getId(BlockTypes.CHERRY_PLANK), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            MAHOGANY_PLANK = create(new ItemType("component/texture/mahogany_plank").blockToPlace(BlockTypes.getId(BlockTypes.MAHOGANY_PLANK), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            ACACIA_PLANK = create(new ItemType("component/texture/acacia_plank").blockToPlace(BlockTypes.getId(BlockTypes.ACACIA_PLANK), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            PALM_PLANK = create(new ItemType("component/texture/palm_plank").blockToPlace(BlockTypes.getId(BlockTypes.PALM_PLANK), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            SPRUCE_PLANK = create(new ItemType("component/texture/spruce_plank").blockToPlace(BlockTypes.getId(BlockTypes.SPRUCE_PLANK), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            WILLOW_PLANK = create(new ItemType("component/texture/willow_plank").blockToPlace(BlockTypes.getId(BlockTypes.WILLOW_PLANK), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            REDWOOD_PLANK = create(new ItemType("component/texture/redwood_plank").blockToPlace(BlockTypes.getId(BlockTypes.REDWOOD_PLANK), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            STICK = create(new ItemType("resource/texture/stick").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.STICK), 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.33f, 1.33f))),
            STONE = create(new ItemType("resource/texture/stone").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.STONE), 0).sfx(new ItemSFX(new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 0.5f, 0.75f))),
            MARBLE = create(new ItemType("resource/texture/marble").blockToPlace(BlockTypes.getId(BlockTypes.MARBLE), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 0.45f, 0.85f))),
            GLASS = create(new ItemType("component/texture/glass").blockToPlace(BlockTypes.getId(BlockTypes.GLASS), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.5f, 0.8f))),
            MAGENTA_STAINED_GLASS = create(new ItemType("component/texture/magenta_stained_glass").blockToPlace(BlockTypes.getId(BlockTypes.MAGENTA_STAINED_GLASS), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.5f, 0.8f))),
            LIME_STAINED_GLASS = create(new ItemType("component/texture/lime_stained_glass").blockToPlace(BlockTypes.getId(BlockTypes.LIME_STAINED_GLASS), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.5f, 0.8f))),
            BLUE_STAINED_GLASS = create(new ItemType("component/texture/blue_stained_glass").blockToPlace(BlockTypes.getId(BlockTypes.BLUE_STAINED_GLASS), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.5f, 0.8f))),
            RED_STAINED_GLASS = create(new ItemType("component/texture/red_stained_glass").blockToPlace(BlockTypes.getId(BlockTypes.RED_STAINED_GLASS), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.5f, 0.8f))),
            TORCH = create(new ItemType("block/texture/torch").blockToPlace(BlockTypes.getId(BlockTypes.TORCH), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.4f, 1.25f))),
            MAGMA = create(new ItemType("resource/texture/magma").blockToPlace(BlockTypes.getId(BlockTypes.MAGMA), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.SIZZLE1, Sounds.SIZZLE2}, 0.45f, 0.95f))),
            PORECAP = create(new ItemType("plant/texture/porecap").blockToPlace(BlockTypes.getId(BlockTypes.PORECAP), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.DIRT_STEP1, Sounds.DIRT_STEP2, Sounds.DIRT_STEP3}, 0.45f, 0.95f))),
            GRASS = create(new ItemType("plant/texture/grass").blockToPlace(BlockTypes.getId(BlockTypes.TALL_GRASS), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.GRASS_STEP1, Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 0.45f, 1.f))),
            ROSE = create(new ItemType("plant/texture/rose").blockToPlace(BlockTypes.getId(BlockTypes.ROSE), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.GRASS_STEP1, Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 0.45f, 1.f))),
            HYDRANGEA = create(new ItemType("plant/texture/hydrangea").blockToPlace(BlockTypes.getId(BlockTypes.HYDRANGEA), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.GRASS_STEP1, Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 0.45f, 1.f))),
            KYANITE = create(new ItemType("resource/texture/kyanite").blockToPlace(BlockTypes.getId(BlockTypes.KYANITE), 0).maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.6f, 1.2f))),
            FLINT = create(new ItemType("resource/texture/flint").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.FLINT), 0).sfx(new ItemSFX(new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 0.5f, 1.05f))),
            IRON_ORE = create(new ItemType("resource/texture/iron").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.IRON_ORE), 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 0.6f))),
            COPPER_ORE = create(new ItemType("resource/texture/copper").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.COPPER_ORE), 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 0.6f))),
            GRAVEL = create(new ItemType("resource/texture/gravel").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.GRAVEL), 0).sfx(new ItemSFX(new SFX[]{Sounds.GRAVEL_STEP1, Sounds.GRAVEL_STEP2}, 0.4f, 1.f))),
            SAND = create(new ItemType("resource/texture/sand").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.SAND), 0).sfx(new ItemSFX(new SFX[]{Sounds.SAND_STEP1, Sounds.SAND_STEP2}, 0.4f, 1.f))),
            SANDSTONE = create(new ItemType("resource/texture/sandstone").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.SANDSTONE), 0).sfx(new ItemSFX(new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 0.5f, 1.f))),
            RED_SAND = create(new ItemType("resource/texture/red_sand").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.RED_SAND), 0).sfx(new ItemSFX(new SFX[]{Sounds.SAND_STEP1, Sounds.SAND_STEP2}, 0.4f, 1.f))),
            RED_SANDSTONE = create(new ItemType("resource/texture/red_sandstone").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.RED_SANDSTONE), 0).sfx(new ItemSFX(new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 0.5f, 1.f))),
            RED_GRAVEL = create(new ItemType("resource/texture/red_gravel").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.RED_GRAVEL), 0).sfx(new ItemSFX(new SFX[]{Sounds.GRAVEL_STEP1, Sounds.GRAVEL_STEP2}, 0.4f, 1.f))),
            MARTIAN_REGOLITH = create(new ItemType("resource/texture/martian_regolith").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.MARTIAN_REGOLITH), 0).sfx(new ItemSFX(new SFX[]{Sounds.SAND_STEP1, Sounds.SAND_STEP2}, 0.4f, 1.f))),
            REGOLITH = create(new ItemType("resource/texture/regolith").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.REGOLITH), 0).sfx(new ItemSFX(new SFX[]{Sounds.SAND_STEP1, Sounds.SAND_STEP2}, 0.4f, 1.f))),
            DIRT = create(new ItemType("resource/texture/dirt").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.DIRT), 0).sfx(new ItemSFX(new SFX[]{Sounds.DIRT_STEP1, Sounds.DIRT_STEP2, Sounds.DIRT_STEP3}, 0.5f, 1.f))),
            GRASSY_DIRT = create(new ItemType("resource/texture/grassy_dirt").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.GRASS), 0).sfx(new ItemSFX(new SFX[]{Sounds.DIRT_STEP1, Sounds.DIRT_STEP2, Sounds.DIRT_STEP3}, 0.5f, 1.f))),
            CLAY = create(new ItemType("resource/texture/clay").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.CLAY), 0).sfx(new ItemSFX(new SFX[]{Sounds.MUD_STEP1, Sounds.MUD_STEP2}, 0.5f, 0.66f))),
            MUD = create(new ItemType("resource/texture/mud").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.MUD), 0).sfx(new ItemSFX(new SFX[]{Sounds.MUD_STEP1, Sounds.MUD_STEP2}, 0.5f, 0.66f))),
            STEEL_FRAME = create(new ItemType("component/texture/steel_frame").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.STEEL_FRAME), 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            SCREW = create(new ItemType("component/texture/screw").maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            STEEL_ROD = create(new ItemType("component/texture/steel_rod").maxStackSize(64).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            POWERED_VENT = create(new ItemType("machine/texture/powered_vent").maxStackSize(64).blockToPlace(BlockTypes.getId(BlockTypes.POWERED_VENT), 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f)));

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

    public static ByteBuffer[] itemTextures;
    public static ExecutorService pool;
//    public static void fillTexture() {
//        glBindTexture(GL_TEXTURE_3D, Textures.items.id);
//        int texSize = Textures.items.width*Textures.items.height*((Texture3D)(Textures.items)).depth;
//        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA32F, Textures.items.width, Textures.items.height, ((Texture3D)(Textures.items)).depth, 0, GL_RGBA, GL_FLOAT, 0);
//        itemTextures = new ByteBuffer[texSize/itemTexSize];
//
//        pool = Executors.newFixedThreadPool(1);
//        pool.submit(() -> { try {
//                int xOffset = 0;
//                int yOffset = 0;
//                int i = 0;
//                for (ItemType itemType : itemTypeMap.values()) {
//                    itemTextures[i++] = Utils.imageToBuffer(Utils.loadImage("item/"+itemType.name));
//                    itemType.atlasOffset(xOffset, yOffset);
//                    xOffset += itemTexSize;
//                    if (xOffset >= 4096-itemTexSize) {
//                        xOffset = 0;
//                        yOffset += itemTexSize;
//                    }
//                }
//            } catch (IOException e) {throw new RuntimeException(e);}});
//        pool.shutdown();
//    }
}
