package org.conspiracraft.blocks.entities;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.conspiracraft.blocks.types.BlockType;
import org.conspiracraft.blocks.types.BlockTypes;

public class BlockEntityTypes {
    public static Object2ObjectOpenHashMap<BlockType, BlockEntity> blockTypeToEntity = new Object2ObjectOpenHashMap<BlockType, BlockEntity>(
            new BlockType[]{
                    BlockTypes.POWERED_VENT
            },
            new BlockEntity[]{
                    new PoweredVentBlockEntity()
            });
}
