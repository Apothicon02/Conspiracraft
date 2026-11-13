package org.conspiracraft.items.types;

import org.conspiracraft.audio.SFX;
import org.conspiracraft.audio.Sounds;
import org.conspiracraft.blocks.BlockTag;
import org.conspiracraft.blocks.BlockTags;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.elements.Elements;
import org.conspiracraft.graphics.buffers.Buffer;
import org.conspiracraft.graphics.textures.ImageHelper;
import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.ItemSFX;
import org.conspiracraft.items.ItemTag;
import org.conspiracraft.items.ItemTags;
import org.conspiracraft.utils.Utils;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class ItemTypes {
    public static int itemTexSize = 16;
    public static int standardStackSize = 100;
    public static Map<Integer, ItemType> itemTypeMap = new HashMap<>(Map.of());

    public static int getId(ItemType type) {
        int id = 0;
        for (ItemType mapType : itemTypeMap.values()) {
            if (mapType.equals(type)) {
                return id;
            }
            id++;
        }
        return 0;
    }

    public static final int STEEL_SPD = 15, GOLD_SPD = 30, METAL_SPD = 10, COPPER_SPD = 8, STONE_SPD = 6, WOOD_SPD = 6;
    public static final int STEEL_STR = 600, METAL_STR = 500, STONE_STR = 300, WOOD_STR = 200;
    public static final int STEEL_DRB = 160, METAL_DRB = 80, STONE_DRB = 40, WOOD_DRB = 20;

    public static final ItemType
            AIR = create(new ItemType("misc/texture/air").maxStackSize(1)),
            HEART = create(List.of(ItemTags.healthBar), new HeartItemType("magic/texture/heart", 100).hideDurability().maxStackSize(1)),
            HEART_HALF = create(List.of(ItemTags.healthBar), new HeartItemType("magic/texture/heart_half", 100).hideDurability().maxStackSize(1)),
            HEART_BROKEN = create(List.of(ItemTags.healthBar), new HeartItemType("magic/texture/heart_broken", 100).hideDurability().maxStackSize(1)),
            SKULL = create(List.of(ItemTags.healthBar), new ItemType("magic/texture/skull").immovable().maxStackSize(1)),
            APPLE = create(new ItemType("food/texture/apple").maxStackSize(1).blockToPlace(1, 15)),
            ORANGE = create(new ItemType("food/texture/orange").maxStackSize(1)),
            CHERRY = create(new ItemType("food/texture/cherry").maxStackSize(1)),
            FURNACE = create(new ItemType("stone/texture/furnace").maxStackSize(standardStackSize).blockToPlace(BlockTypes.STONE.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 0.5f, 0.75f))),
            KILN = create(new ItemType("clay/texture/kiln").maxStackSize(standardStackSize).blockToPlace(BlockTypes.CLAY.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 0.5f, 0.75f))),
            FORGE = create(new ItemType("metal/steel/texture/forge").maxStackSize(standardStackSize).blockToPlace(BlockTypes.STEEL_PLATING.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            RESEARCH_TABLE = create(new ItemType("wood/texture/research_table").maxStackSize(standardStackSize).blockToPlace(BlockTypes.RESEARCH_TABLE.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            BLUEPRINT = create(new BlueprintItemType("wood/texture/blueprint").maxStackSize(1)),
            OAK_LOG = create(List.of(ItemTags.log), new ItemType("wood/oak/texture/log").maxStackSize(standardStackSize).blockToPlace(BlockTypes.OAK_LOG.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            OAK_PLANK = create(new ItemType("wood/oak/texture/plank").blockToPlace(BlockTypes.OAK_PLANK.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            OAK_BEAM = create(new ItemType("wood/oak/texture/beam").blockToPlace(BlockTypes.OAK_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            OAK_CRATE = create(new ItemType("wood/oak/texture/crate").blockToPlace(BlockTypes.OAK_CRATE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            OAK_BARREL = create(new ItemType("wood/oak/texture/barrel").blockToPlace(BlockTypes.OAK_BARREL.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            OAK_FENCE = create(new ItemType("wood/oak/texture/fence").blockToPlace(BlockTypes.OAK_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            OAK_GATE = create(new ItemType("wood/oak/texture/gate").blockToPlace(BlockTypes.OAK_GATE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            OAK_DOOR = create(new ItemType("wood/oak/texture/door").blockToPlace(BlockTypes.OAK_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            OAK_TRAPDOOR = create(new ItemType("wood/oak/texture/trapdoor").blockToPlace(BlockTypes.OAK_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            OAK_SIGN = create(new ItemType("wood/oak/texture/sign").blockToPlace(BlockTypes.OAK_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            BIRCH_LOG = create(List.of(ItemTags.log), new ItemType("wood/birch/texture/log").maxStackSize(standardStackSize).blockToPlace(BlockTypes.BIRCH_LOG.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            BIRCH_PLANK = create(new ItemType("wood/birch/texture/plank").blockToPlace(BlockTypes.BIRCH_PLANK.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            BIRCH_BEAM = create(new ItemType("wood/birch/texture/beam").blockToPlace(BlockTypes.BIRCH_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            BIRCH_CRATE = create(new ItemType("wood/birch/texture/crate").blockToPlace(BlockTypes.BIRCH_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            BIRCH_BARREL = create(new ItemType("wood/birch/texture/barrel").blockToPlace(BlockTypes.BIRCH_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            BIRCH_FENCE = create(new ItemType("wood/birch/texture/fence").blockToPlace(BlockTypes.BIRCH_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            BIRCH_GATE = create(new ItemType("wood/birch/texture/gate").blockToPlace(BlockTypes.BIRCH_GATE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            BIRCH_DOOR = create(new ItemType("wood/birch/texture/door").blockToPlace(BlockTypes.BIRCH_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            BIRCH_TRAPDOOR = create(new ItemType("wood/birch/texture/trapdoor").blockToPlace(BlockTypes.BIRCH_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            BIRCH_SIGN = create(new ItemType("wood/birch/texture/sign").blockToPlace(BlockTypes.BIRCH_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            CHERRY_LOG = create(List.of(ItemTags.log), new ItemType("wood/cherry/texture/log").maxStackSize(standardStackSize).blockToPlace(BlockTypes.CHERRY_LOG.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            CHERRY_PLANK = create(new ItemType("wood/cherry/texture/plank").blockToPlace(BlockTypes.CHERRY_PLANK.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            CHERRY_BEAM = create(new ItemType("wood/cherry/texture/beam").blockToPlace(BlockTypes.CHERRY_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            CHERRY_CRATE = create(new ItemType("wood/cherry/texture/crate").blockToPlace(BlockTypes.CHERRY_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            CHERRY_BARREL = create(new ItemType("wood/cherry/texture/barrel").blockToPlace(BlockTypes.CHERRY_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            CHERRY_FENCE = create(new ItemType("wood/cherry/texture/fence").blockToPlace(BlockTypes.CHERRY_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            CHERRY_GATE = create(new ItemType("wood/cherry/texture/gate").blockToPlace(BlockTypes.CHERRY_GATE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            CHERRY_DOOR = create(new ItemType("wood/cherry/texture/door").blockToPlace(BlockTypes.CHERRY_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            CHERRY_TRAPDOOR = create(new ItemType("wood/cherry/texture/trapdoor").blockToPlace(BlockTypes.CHERRY_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            CHERRY_SIGN = create(new ItemType("wood/cherry/texture/sign").blockToPlace(BlockTypes.CHERRY_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            MAHOGANY_LOG = create(List.of(ItemTags.log), new ItemType("wood/mahogany/texture/log").maxStackSize(standardStackSize).blockToPlace(BlockTypes.MAHOGANY_LOG.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            MAHOGANY_PLANK = create(new ItemType("wood/mahogany/texture/plank").blockToPlace(BlockTypes.MAHOGANY_PLANK.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            MAHOGANY_BEAM = create(new ItemType("wood/mahogany/texture/beam").blockToPlace(BlockTypes.MAHOGANY_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            MAHOGANY_CRATE = create(new ItemType("wood/mahogany/texture/crate").blockToPlace(BlockTypes.MAHOGANY_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            MAHOGANY_BARREL = create(new ItemType("wood/mahogany/texture/barrel").blockToPlace(BlockTypes.MAHOGANY_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            MAHOGANY_FENCE = create(new ItemType("wood/mahogany/texture/fence").blockToPlace(BlockTypes.MAHOGANY_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            MAHOGANY_GATE = create(new ItemType("wood/mahogany/texture/gate").blockToPlace(BlockTypes.MAHOGANY_GATE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            MAHOGANY_DOOR = create(new ItemType("wood/mahogany/texture/door").blockToPlace(BlockTypes.MAHOGANY_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            MAHOGANY_TRAPDOOR = create(new ItemType("wood/mahogany/texture/trapdoor").blockToPlace(BlockTypes.MAHOGANY_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            MAHOGANY_SIGN = create(new ItemType("wood/mahogany/texture/sign").blockToPlace(BlockTypes.MAHOGANY_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            ACACIA_LOG = create(List.of(ItemTags.log), new ItemType("wood/acacia/texture/log").maxStackSize(standardStackSize).blockToPlace(BlockTypes.ACACIA_LOG.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            ACACIA_PLANK = create(new ItemType("wood/acacia/texture/plank").blockToPlace(BlockTypes.ACACIA_PLANK.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            ACACIA_BEAM = create(new ItemType("wood/acacia/texture/beam").blockToPlace(BlockTypes.ACACIA_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            ACACIA_CRATE = create(new ItemType("wood/acacia/texture/crate").blockToPlace(BlockTypes.ACACIA_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            ACACIA_BARREL = create(new ItemType("wood/acacia/texture/barrel").blockToPlace(BlockTypes.ACACIA_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            ACACIA_FENCE = create(new ItemType("wood/acacia/texture/fence").blockToPlace(BlockTypes.ACACIA_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            ACACIA_GATE = create(new ItemType("wood/acacia/texture/gate").blockToPlace(BlockTypes.ACACIA_GATE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            ACACIA_DOOR = create(new ItemType("wood/acacia/texture/door").blockToPlace(BlockTypes.ACACIA_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            ACACIA_TRAPDOOR = create(new ItemType("wood/acacia/texture/trapdoor").blockToPlace(BlockTypes.ACACIA_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            ACACIA_SIGN = create(new ItemType("wood/acacia/texture/sign").blockToPlace(BlockTypes.ACACIA_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            PALM_LOG = create(List.of(ItemTags.log), new ItemType("wood/palm/texture/log").maxStackSize(standardStackSize).blockToPlace(BlockTypes.PALM_LOG.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            PALM_PLANK = create(new ItemType("wood/palm/texture/plank").blockToPlace(BlockTypes.PALM_PLANK.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            PALM_BEAM = create(new ItemType("wood/palm/texture/beam").blockToPlace(BlockTypes.PALM_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            PALM_CRATE = create(new ItemType("wood/palm/texture/crate").blockToPlace(BlockTypes.PALM_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            PALM_BARREL = create(new ItemType("wood/palm/texture/barrel").blockToPlace(BlockTypes.PALM_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            PALM_FENCE = create(new ItemType("wood/palm/texture/fence").blockToPlace(BlockTypes.PALM_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            PALM_GATE = create(new ItemType("wood/palm/texture/gate").blockToPlace(BlockTypes.PALM_GATE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            PALM_DOOR = create(new ItemType("wood/palm/texture/door").blockToPlace(BlockTypes.PALM_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            PALM_TRAPDOOR = create(new ItemType("wood/palm/texture/trapdoor").blockToPlace(BlockTypes.PALM_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            PALM_SIGN = create(new ItemType("wood/palm/texture/sign").blockToPlace(BlockTypes.PALM_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            SPRUCE_LOG = create(List.of(ItemTags.log), new ItemType("wood/spruce/texture/log").maxStackSize(standardStackSize).blockToPlace(BlockTypes.SPRUCE_LOG.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            SPRUCE_PLANK = create(new ItemType("wood/spruce/texture/plank").blockToPlace(BlockTypes.SPRUCE_PLANK.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            SPRUCE_BEAM = create(new ItemType("wood/spruce/texture/beam").blockToPlace(BlockTypes.SPRUCE_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            SPRUCE_CRATE = create(new ItemType("wood/spruce/texture/crate").blockToPlace(BlockTypes.SPRUCE_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            SPRUCE_BARREL = create(new ItemType("wood/spruce/texture/barrel").blockToPlace(BlockTypes.SPRUCE_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            SPRUCE_FENCE = create(new ItemType("wood/spruce/texture/fence").blockToPlace(BlockTypes.SPRUCE_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            SPRUCE_GATE = create(new ItemType("wood/spruce/texture/gate").blockToPlace(BlockTypes.SPRUCE_GATE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            SPRUCE_DOOR = create(new ItemType("wood/spruce/texture/door").blockToPlace(BlockTypes.SPRUCE_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            SPRUCE_TRAPDOOR = create(new ItemType("wood/spruce/texture/trapdoor").blockToPlace(BlockTypes.SPRUCE_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            SPRUCE_SIGN = create(new ItemType("wood/spruce/texture/sign").blockToPlace(BlockTypes.SPRUCE_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            WILLOW_LOG = create(List.of(ItemTags.log), new ItemType("wood/willow/texture/log").maxStackSize(standardStackSize).blockToPlace(BlockTypes.WILLOW_LOG.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            WILLOW_PLANK = create(new ItemType("wood/willow/texture/plank").blockToPlace(BlockTypes.WILLOW_PLANK.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            WILLOW_BEAM = create(new ItemType("wood/willow/texture/beam").blockToPlace(BlockTypes.WILLOW_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            WILLOW_CRATE = create(new ItemType("wood/willow/texture/crate").blockToPlace(BlockTypes.WILLOW_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            WILLOW_BARREL = create(new ItemType("wood/willow/texture/barrel").blockToPlace(BlockTypes.WILLOW_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            WILLOW_FENCE = create(new ItemType("wood/willow/texture/fence").blockToPlace(BlockTypes.WILLOW_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            WILLOW_GATE = create(new ItemType("wood/willow/texture/gate").blockToPlace(BlockTypes.WILLOW_GATE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            WILLOW_DOOR = create(new ItemType("wood/willow/texture/door").blockToPlace(BlockTypes.WILLOW_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            WILLOW_TRAPDOOR = create(new ItemType("wood/willow/texture/trapdoor").blockToPlace(BlockTypes.WILLOW_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            WILLOW_SIGN = create(new ItemType("wood/willow/texture/sign").blockToPlace(BlockTypes.WILLOW_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            REDWOOD_LOG = create(List.of(ItemTags.log), new ItemType("wood/redwood/texture/log").maxStackSize(standardStackSize).blockToPlace(BlockTypes.REDWOOD_LOG.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1))),
            REDWOOD_PLANK = create(new ItemType("wood/redwood/texture/plank").blockToPlace(BlockTypes.REDWOOD_PLANK.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            REDWOOD_BEAM = create(new ItemType("wood/redwood/texture/beam").blockToPlace(BlockTypes.REDWOOD_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            REDWOOD_CRATE = create(new ItemType("wood/redwood/texture/crate").blockToPlace(BlockTypes.REDWOOD_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            REDWOOD_BARREL = create(new ItemType("wood/redwood/texture/barrel").blockToPlace(BlockTypes.REDWOOD_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            REDWOOD_FENCE = create(new ItemType("wood/redwood/texture/fence").blockToPlace(BlockTypes.REDWOOD_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            REDWOOD_GATE = create(new ItemType("wood/redwood/texture/gate").blockToPlace(BlockTypes.REDWOOD_GATE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            REDWOOD_DOOR = create(new ItemType("wood/redwood/texture/door").blockToPlace(BlockTypes.REDWOOD_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            REDWOOD_TRAPDOOR = create(new ItemType("wood/redwood/texture/trapdoor").blockToPlace(BlockTypes.REDWOOD_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            REDWOOD_SIGN = create(new ItemType("wood/redwood/texture/sign").blockToPlace(BlockTypes.REDWOOD_FENCE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.1f))),
            DRIFTWOOD = create(new ItemType("wood/texture/driftwood").blockToPlace(BlockTypes.DEAD_PLANK.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.33f, 1.33f))),
            STICK = create(new ItemType("wood/texture/stick").maxStackSize(standardStackSize).blockToPlace(BlockTypes.STICK.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.33f, 1.33f))),
            PEBBLE = create(new ItemType("stone/texture/pebble").maxStackSize(standardStackSize).blockToPlace(BlockTypes.PEBBLE.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 0.5f, 0.75f))),
            STONE = create(List.of(ItemTags.stone), new ItemType("stone/texture/stone").maxStackSize(standardStackSize).blockToPlace(BlockTypes.STONE.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 0.5f, 0.75f))),
            MARBLE = create(List.of(ItemTags.stone), new ItemType("stone/texture/marble").maxStackSize(standardStackSize).blockToPlace(BlockTypes.MARBLE.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 0.45f, 0.85f))),
            GLASS = create(new ItemType("sand/texture/glass").blockToPlace(BlockTypes.GLASS.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.5f, 0.8f))),
            MAGENTA_STAINED_GLASS = create(new ItemType("sand/texture/magenta_stained_glass").blockToPlace(BlockTypes.MAGENTA_STAINED_GLASS.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.5f, 0.8f))),
            LIME_STAINED_GLASS = create(new ItemType("sand/texture/lime_stained_glass").blockToPlace(BlockTypes.LIME_STAINED_GLASS.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.5f, 0.8f))),
            BLUE_STAINED_GLASS = create(new ItemType("sand/texture/blue_stained_glass").blockToPlace(BlockTypes.BLUE_STAINED_GLASS.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.5f, 0.8f))),
            RED_STAINED_GLASS = create(new ItemType("sand/texture/red_stained_glass").blockToPlace(BlockTypes.RED_STAINED_GLASS.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.5f, 0.8f))),
            TORCH = create(new WallMountableItemType("wood/texture/torch").blockToPlace(BlockTypes.TORCH.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.4f, 1.25f))),
            MAGMA = create(new ItemType("stone/texture/magma").blockToPlace(BlockTypes.MAGMA.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.SIZZLE1, Sounds.SIZZLE2}, 0.45f, 0.95f))),
            PORECAP = create(new ItemType("plant/texture/porecap").blockToPlace(BlockTypes.PORECAP.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.DIRT_STEP1, Sounds.DIRT_STEP2, Sounds.DIRT_STEP3}, 0.45f, 0.95f))),
            GRASS = create(new ItemType("plant/texture/grass").blockToPlace(BlockTypes.TALL_GRASS.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.GRASS_STEP1, Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 0.45f, 1.f))),
            ROSE = create(new ItemType("plant/texture/rose").blockToPlace(BlockTypes.ROSE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.GRASS_STEP1, Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 0.45f, 1.f))),
            HYDRANGEA = create(new ItemType("plant/texture/hydrangea").blockToPlace(BlockTypes.HYDRANGEA.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.GRASS_STEP1, Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 0.45f, 1.f))),
            DANDELION = create(new ItemType("plant/texture/dandelion").blockToPlace(BlockTypes.DANDELION.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.GRASS_STEP1, Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 0.45f, 1.f))),
            DAISY = create(new ItemType("plant/texture/daisy").blockToPlace(BlockTypes.DAISY.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.GRASS_STEP1, Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 0.45f, 1.f))),
            LILY = create(new ItemType("plant/texture/lily").blockToPlace(BlockTypes.LILY.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.GRASS_STEP1, Sounds.GRASS_STEP2, Sounds.GRASS_STEP3}, 0.45f, 1.f))),
            KYANITE = create(new ItemType("crystal/texture/kyanite").blockToPlace(BlockTypes.KYANITE.id, 0).maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.6f, 1.2f))),
            FLINT = create(new ItemType("stone/texture/flint").maxStackSize(standardStackSize).blockToPlace(BlockTypes.FLINT.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 0.5f, 1.05f))),
            GRAVEL = create(new ItemType("stone/texture/gravel").maxStackSize(standardStackSize).blockToPlace(BlockTypes.GRAVEL.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.GRAVEL_STEP1, Sounds.GRAVEL_STEP2}, 0.4f, 1.f))),
            SAND = create(new ItemType("sand/texture/sand").maxStackSize(standardStackSize).blockToPlace(BlockTypes.SAND.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.SAND_STEP1, Sounds.SAND_STEP2}, 0.4f, 1.f))),
            SANDSTONE = create(List.of(ItemTags.stone), new ItemType("sand/texture/sandstone").maxStackSize(standardStackSize).blockToPlace(BlockTypes.SANDSTONE.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 0.5f, 1.f))),
            RED_SAND = create(new ItemType("sand/texture/red_sand").maxStackSize(standardStackSize).blockToPlace(BlockTypes.RED_SAND.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.SAND_STEP1, Sounds.SAND_STEP2}, 0.4f, 1.f))),
            RED_SANDSTONE = create(List.of(ItemTags.stone), new ItemType("sand/texture/red_sandstone").maxStackSize(standardStackSize).blockToPlace(BlockTypes.RED_SANDSTONE.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.ROCK_PLACE1, Sounds.ROCK_PLACE2}, 0.5f, 1.f))),
            RED_GRAVEL = create(new ItemType("stone/texture/red_gravel").maxStackSize(standardStackSize).blockToPlace(BlockTypes.RED_GRAVEL.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.GRAVEL_STEP1, Sounds.GRAVEL_STEP2}, 0.4f, 1.f))),
            MARTIAN_REGOLITH = create(new ItemType("soil/texture/martian_regolith").maxStackSize(standardStackSize).blockToPlace(BlockTypes.ORANGE_SAND.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.SAND_STEP1, Sounds.SAND_STEP2}, 0.4f, 1.f))),
            REGOLITH = create(new ItemType("soil/texture/regolith").maxStackSize(standardStackSize).blockToPlace(BlockTypes.ORANGE_SANDSTONE.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.SAND_STEP1, Sounds.SAND_STEP2}, 0.4f, 1.f))),
            DIRT = create(new ItemType("soil/texture/dirt").maxStackSize(standardStackSize).blockToPlace(BlockTypes.DIRT.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.DIRT_STEP1, Sounds.DIRT_STEP2, Sounds.DIRT_STEP3}, 0.5f, 1.f))),
            GRASSY_DIRT = create(new ItemType("soil/texture/grassy_dirt").maxStackSize(standardStackSize).blockToPlace(BlockTypes.GRASS.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.DIRT_STEP1, Sounds.DIRT_STEP2, Sounds.DIRT_STEP3}, 0.5f, 1.f))),
            CLAY = create(new ItemType("clay/texture/clay").maxStackSize(standardStackSize).blockToPlace(BlockTypes.CLAY.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.MUD_STEP1, Sounds.MUD_STEP2}, 0.5f, 0.66f))),
            MUD = create(new ItemType("soil/texture/mud").maxStackSize(standardStackSize).blockToPlace(BlockTypes.MUD.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.MUD_STEP1, Sounds.MUD_STEP2}, 0.5f, 0.66f))),
            FIREBALL = create(new FireballItemType("magic/texture/fireball").maxStackSize(10).sfx(new ItemSFX(new SFX[]{Sounds.FIREBALL_QUICK}, 1.0f, 1.0f))),
            LIGHTNING = create(new LightningItemType("magic/texture/lightning").maxStackSize(10).sfx(new ItemSFX(new SFX[]{Sounds.THUNDER_1, Sounds.THUNDER_2}, 1.0f, 1.0f))),
            OXYGEN_ICE = create(new IceItemType("ice/texture/oxygen_ice").element(Elements.OXYGEN).maxStackSize(Integer.MAX_VALUE).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.6f, 0.66f))),
            CARBON_DIOXIDE_ICE = create(new IceItemType("ice/texture/carbon_dioxide_ice").element(Elements.CARBON_DIOXIDE).maxStackSize(Integer.MAX_VALUE).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.6f, 0.66f))),
            NITROGEN_ICE = create(new IceItemType("ice/texture/nitrogen_ice").element(Elements.NITROGEN).maxStackSize(Integer.MAX_VALUE).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.6f, 0.66f))),
            ARGON_ICE = create(new IceItemType("ice/texture/argon_ice").element(Elements.ARGON).maxStackSize(Integer.MAX_VALUE).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.6f, 0.66f))),
            HYDROGEN_ICE = create(new IceItemType("ice/texture/hydrogen_ice").element(Elements.HYDROGEN).maxStackSize(Integer.MAX_VALUE).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.6f, 0.66f))),
            WATER_ICE = create(new IceItemType("ice/texture/water_ice").element(Elements.WATER).maxStackSize(Integer.MAX_VALUE).blockToPlace(BlockTypes.ICE.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.6f, 0.66f))),
            HELIUM_ICE = create(new IceItemType("ice/texture/helium_ice").element(Elements.HELIUM).maxStackSize(Integer.MAX_VALUE).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.6f, 0.66f))),
            NEON_ICE = create(new IceItemType("ice/texture/neon_ice").element(Elements.NEON).maxStackSize(Integer.MAX_VALUE).sfx(new ItemSFX(new SFX[]{Sounds.GLASS_STEP1, Sounds.GLASS_STEP2}, 0.6f, 0.66f))),
            IRON_ORE = create(new ItemType("metal/iron/texture/iron").maxStackSize(standardStackSize).blockToPlace(BlockTypes.IRON_ORE.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 0.6f))),
            COPPER_ORE = create(new ItemType("metal/copper/texture/copper").maxStackSize(standardStackSize).blockToPlace(BlockTypes.COPPER_ORE.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 0.6f))),
            ZINC_ORE = create(new ItemType("metal/zinc/texture/zinc").maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 0.6f))),
            GOLD_ORE = create(new ItemType("metal/gold/texture/gold").maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 0.6f))),
            STEEL_FRAME = create(new ItemType("metal/steel/texture/steel_frame").maxStackSize(standardStackSize).blockToPlace(BlockTypes.STEEL_FRAME.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            SCREW = create(new ItemType("metal/steel/texture/screw").maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            STEEL_ROD = create(new ItemType("metal/steel/texture/steel_rod").maxStackSize(standardStackSize).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            POWERED_VENT = create(new ItemType("metal/steel/texture/powered_vent").maxStackSize(standardStackSize).blockToPlace(BlockTypes.POWERED_VENT.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            IRON_PLATE = create(List.of(ItemTags.healthBar, ItemTags.armor), new DurableItemType("metal/iron/texture/iron_plate", 200).maxStackSize(1).blockToPlace(BlockTypes.IRON_PLATING.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            STEEL_PLATE = create(List.of(ItemTags.healthBar, ItemTags.armor), new DurableItemType("metal/steel/texture/steel_plate", 400).maxStackSize(1).blockToPlace(BlockTypes.STEEL_PLATING.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            COPPER_PLATE = create(List.of(ItemTags.healthBar, ItemTags.armor), new DurableItemType("metal/copper/texture/copper_plate", 150).maxStackSize(1).blockToPlace(BlockTypes.COPPER_PLATING.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            ZINC_PLATE = create(List.of(ItemTags.healthBar, ItemTags.armor), new DurableItemType("metal/zinc/texture/zinc_plate", 200).maxStackSize(1).blockToPlace(BlockTypes.ZINC_PLATING.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            GOLD_PLATE = create(List.of(ItemTags.healthBar, ItemTags.armor), new DurableItemType("metal/gold/texture/gold_plate", 150).maxStackSize(1).blockToPlace(BlockTypes.GOLD_PLATING.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            IRON_PANEL = create(new ItemType("metal/iron/texture/iron_panel").maxStackSize(standardStackSize).blockToPlace(BlockTypes.IRON_PLATING.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            STEEL_PANEL = create(new ItemType("metal/steel/texture/steel_panel").maxStackSize(standardStackSize).blockToPlace(BlockTypes.STEEL_PLATING.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            COPPER_PANEL = create(new ItemType("metal/copper/texture/copper_panel").maxStackSize(standardStackSize).blockToPlace(BlockTypes.COPPER_PLATING.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            ZINC_PANEL = create(new ItemType("metal/zinc/texture/zinc_panel").maxStackSize(standardStackSize).blockToPlace(BlockTypes.ZINC_PLATING.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            GOLD_PANEL = create(new ItemType("metal/gold/texture/gold_panel").maxStackSize(standardStackSize).blockToPlace(BlockTypes.GOLD_PLATING.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            HAZARD_PANEL = create(new ItemType("metal/steel/texture/hazard_panel").maxStackSize(standardStackSize).blockToPlace(BlockTypes.HAZARD.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            STEEL_WRENCH = create(new ToolItemType("metal/steel/texture/wrench", STEEL_STR, STEEL_DRB, new BlockTag[]{BlockTags.wrenchEfficient}).useSpeed(STEEL_SPD).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            IRON_WRENCH = create(new ToolItemType("metal/iron/texture/wrench", METAL_STR, METAL_DRB, new BlockTag[]{BlockTags.wrenchEfficient}).useSpeed(METAL_SPD).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            COPPER_WRENCH = create(new ToolItemType("metal/copper/texture/wrench", METAL_STR, METAL_DRB, new BlockTag[]{BlockTags.wrenchEfficient}).useSpeed(COPPER_SPD).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            ZINC_WRENCH = create(new ToolItemType("metal/zinc/texture/wrench", METAL_STR, METAL_DRB, new BlockTag[]{BlockTags.wrenchEfficient}).useSpeed(METAL_SPD).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            GOLD_WRENCH = create(new ToolItemType("metal/gold/texture/wrench", STONE_STR, STONE_DRB, new BlockTag[]{BlockTags.wrenchEfficient}).useSpeed(GOLD_SPD).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.6f, 1.0f))),
            STEEL_SCYTHE = create(new ToolItemType("metal/steel/texture/scythe", STEEL_STR, STEEL_DRB, new BlockTag[]{BlockTags.scytheEfficient}).useSpeed(STEEL_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            IRON_SCYTHE = create(new ToolItemType("metal/iron/texture/scythe", METAL_STR, METAL_DRB, new BlockTag[]{BlockTags.scytheEfficient}).useSpeed(METAL_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            COPPER_SCYTHE = create(new ToolItemType("metal/copper/texture/scythe", METAL_STR, METAL_DRB, new BlockTag[]{BlockTags.scytheEfficient}).useSpeed(COPPER_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            ZINC_SCYTHE = create(new ToolItemType("metal/zinc/texture/scythe", METAL_STR, METAL_DRB, new BlockTag[]{BlockTags.scytheEfficient}).useSpeed(METAL_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            GOLD_SCYTHE = create(new ToolItemType("metal/gold/texture/scythe", STONE_STR, STONE_DRB, new BlockTag[]{BlockTags.scytheEfficient}).useSpeed(GOLD_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            STEEL_PICK = create(new ToolItemType("metal/steel/texture/pick", STEEL_STR, STEEL_DRB, new BlockTag[]{BlockTags.pickEfficient}).useSpeed(STEEL_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            IRON_PICK = create(new ToolItemType("metal/iron/texture/pick", METAL_STR, METAL_DRB, new BlockTag[]{BlockTags.pickEfficient}).useSpeed(METAL_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            COPPER_PICK = create(new ToolItemType("metal/copper/texture/pick", METAL_STR, METAL_DRB, new BlockTag[]{BlockTags.pickEfficient}).useSpeed(COPPER_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            ZINC_PICK = create(new ToolItemType("metal/zinc/texture/pick", METAL_STR, METAL_DRB, new BlockTag[]{BlockTags.pickEfficient}).useSpeed(METAL_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            GOLD_PICK = create(new ToolItemType("metal/gold/texture/pick", STONE_STR, STONE_DRB, new BlockTag[]{BlockTags.pickEfficient}).useSpeed(GOLD_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            STONE_PICK = create(new ToolItemType("stone/texture/pick", STONE_STR, STONE_DRB, new BlockTag[]{BlockTags.pickEfficient}).useSpeed(STONE_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            STEEL_HATCHET = create(List.of(ItemTags.axe), new HatchetItemType("metal/steel/texture/hatchet", STEEL_STR, STEEL_DRB, new BlockTag[]{BlockTags.hatchetEfficient}).useSpeed(STEEL_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            IRON_HATCHET = create(List.of(ItemTags.axe), new HatchetItemType("metal/iron/texture/hatchet", METAL_STR, METAL_DRB, new BlockTag[]{BlockTags.hatchetEfficient}).useSpeed(METAL_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            COPPER_HATCHET = create(List.of(ItemTags.axe), new HatchetItemType("metal/copper/texture/hatchet", METAL_STR, METAL_DRB, new BlockTag[]{BlockTags.hatchetEfficient}).useSpeed(COPPER_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            ZINC_HATCHET = create(List.of(ItemTags.axe), new HatchetItemType("metal/zinc/texture/hatchet", METAL_STR, METAL_DRB, new BlockTag[]{BlockTags.hatchetEfficient}).useSpeed(METAL_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            GOLD_HATCHET = create(List.of(ItemTags.axe), new HatchetItemType("metal/gold/texture/hatchet", STONE_STR, STONE_DRB, new BlockTag[]{BlockTags.hatchetEfficient}).useSpeed(GOLD_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            STONE_HATCHET = create(List.of(ItemTags.axe), new HatchetItemType("stone/texture/hatchet", STONE_STR, STONE_DRB, new BlockTag[]{BlockTags.hatchetEfficient}).useSpeed(STONE_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            STEEL_SPADE = create(new ToolItemType("metal/steel/texture/spade", STEEL_STR, STEEL_DRB, new BlockTag[]{BlockTags.spadeEfficient}).useSpeed(STEEL_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            IRON_SPADE = create(new ToolItemType("metal/iron/texture/spade", METAL_STR, METAL_DRB, new BlockTag[]{BlockTags.spadeEfficient}).useSpeed(METAL_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            COPPER_SPADE = create(new ToolItemType("metal/copper/texture/spade", METAL_STR, METAL_DRB, new BlockTag[]{BlockTags.spadeEfficient}).useSpeed(COPPER_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            ZINC_SPADE = create(new ToolItemType("metal/zinc/texture/spade", METAL_STR, METAL_DRB, new BlockTag[]{BlockTags.spadeEfficient}).useSpeed(METAL_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            GOLD_SPADE = create(new ToolItemType("metal/gold/texture/spade", STONE_STR, STONE_DRB, new BlockTag[]{BlockTags.spadeEfficient}).useSpeed(GOLD_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            WOOD_SPADE = create(new ToolItemType("wood/texture/spade", WOOD_STR, WOOD_DRB, new BlockTag[]{BlockTags.spadeEfficient}).useSpeed(WOOD_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            STONE_SPADE = create(new ToolItemType("stone/texture/spade", STONE_STR, STONE_DRB, new BlockTag[]{BlockTags.spadeEfficient}).useSpeed(STONE_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            STEEL_HOE = create(new ToolItemType("metal/steel/texture/hoe", STEEL_STR, STEEL_DRB, new BlockTag[]{BlockTags.hoeEfficient}).useSpeed(STEEL_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            IRON_HOE = create(new ToolItemType("metal/iron/texture/hoe", METAL_STR, METAL_DRB, new BlockTag[]{BlockTags.hoeEfficient}).useSpeed(METAL_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            COPPER_HOE = create(new ToolItemType("metal/copper/texture/hoe", METAL_STR, METAL_DRB, new BlockTag[]{BlockTags.hoeEfficient}).useSpeed(COPPER_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            ZINC_HOE = create(new ToolItemType("metal/zinc/texture/hoe", METAL_STR, METAL_DRB, new BlockTag[]{BlockTags.hoeEfficient}).useSpeed(METAL_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            GOLD_HOE = create(new ToolItemType("metal/gold/texture/hoe", STONE_STR, STONE_DRB, new BlockTag[]{BlockTags.hoeEfficient}).useSpeed(GOLD_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            STEEL_CHISEL = create(new ChiselItemType("metal/steel/texture/chisel", STEEL_STR, STEEL_DRB, new BlockTag[]{BlockTags.chiselEfficient}).useSpeed(STEEL_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            IRON_CHISEL = create(new ChiselItemType("metal/iron/texture/chisel", METAL_STR, METAL_DRB, new BlockTag[]{BlockTags.chiselEfficient}).useSpeed(METAL_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            COPPER_CHISEL = create(new ChiselItemType("metal/copper/texture/chisel", METAL_STR, METAL_DRB, new BlockTag[]{BlockTags.chiselEfficient}).useSpeed(COPPER_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            ZINC_CHISEL = create(new ChiselItemType("metal/zinc/texture/chisel", METAL_STR, METAL_DRB, new BlockTag[]{BlockTags.chiselEfficient}).useSpeed(METAL_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            GOLD_CHISEL = create(new ChiselItemType("metal/gold/texture/chisel", STONE_STR, STONE_DRB, new BlockTag[]{BlockTags.chiselEfficient}).useSpeed(GOLD_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            STONE_CHISEL = create(new ChiselItemType("stone/texture/chisel", STONE_STR, STONE_DRB, new BlockTag[]{BlockTags.chiselEfficient}).useSpeed(STONE_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            IRON_GRAPPLE = create(new GrappleItemType("metal/iron/texture/grapple", METAL_STR, METAL_DRB).useSpeed(METAL_SPD).maxStackSize(1).sfx(new ItemSFX(new SFX[]{Sounds.METAL_SMALL_PLACE1, Sounds.METAL_SMALL_PLACE2}, 0.3f, 0.7f))),
            BAMBOO = create(new ItemType("wood/texture/bamboo").maxStackSize(standardStackSize).blockToPlace(BlockTypes.BAMBOO.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.WOOD_STEP1, Sounds.WOOD_STEP2}, 0.5f, 1.15f))),
            PAPER = create(new ItemType("wood/texture/paper").maxStackSize(standardStackSize).blockToPlace(BlockTypes.PAPER.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.CLOUD}, 0.5f, 0.75f))),
            SHOJI = create(new ItemType("wood/texture/shoji").maxStackSize(standardStackSize).blockToPlace(BlockTypes.SHOJI.id, 0).sfx(new ItemSFX(new SFX[]{Sounds.CLOUD}, 0.5f, 0.75f)));

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

    public static Item loadItem(IntBuffer data) {
        ItemType type = itemTypeMap.get(data.get(data.position()));
        return type.createItem().load(data);
    }
}
