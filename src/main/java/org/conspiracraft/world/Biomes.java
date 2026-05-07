package org.conspiracraft.world;

import org.conspiracraft.blocks.types.BlockTypes;

import java.util.ArrayList;

public class Biomes {
    public static ArrayList<Biome> biomesTemp = new ArrayList<Biome>();

    public static final Biome LAKE = create();
    public static final Biome TEMPERATE = create();
    public static final Biome TAIGA = create();
    public static final Biome CHERRY_GROVE = create();
    public static final Biome REDWOOD_FOREST = create();
    public static final Biome SNOWY_PEAK = create();
    public static final Biome SNOWY_TAIGA = create();
    public static final Biome DESERT = create();
    public static final Biome PALMY_PLAINS = create();
    public static final Biome TROPICAL_ISLAND = create();
    public static final Biome BEACH = create();

    public static final Biome[] biomes = biomesTemp.toArray(new Biome[0]);
    public static Biome create() {
        Biome biome = new Biome((byte) (biomesTemp.size()));
        biomesTemp.addLast(biome);
        return biome;
    }

    public static int getSurfaceBlock(final byte biomeId, final short elevation, final int y) {
        final Biome biome = biomes[biomeId];
        int type;
        int subtype;
        if (elevation <= World.seaLevel) {
            type = BlockTypes.WET_SAND.id;
            subtype = 0;
        } else if (elevation <= World.seaLevel+2+(biome == BEACH ? 1 : 0) || biome == DESERT) {
            type = BlockTypes.SAND.id;
            subtype = 0;
        } else if (biome == SNOWY_PEAK || biome == SNOWY_TAIGA) {
            type = BlockTypes.SNOW.id;
            subtype = 0;
        } else if (biome == LAKE) {
            type = BlockTypes.WET_SAND.id;
            subtype = 0;
        } else if (biome == TAIGA || biome == CHERRY_GROVE || biome == REDWOOD_FOREST) {
            type = y >= elevation ? BlockTypes.GRASS.id : BlockTypes.DIRT.id;
            subtype = 1;
        } else {
            type = y >= elevation ? BlockTypes.GRASS.id : BlockTypes.DIRT.id;
            subtype = 0;
        }
        return Chunk.packInts(type, subtype);
    }
}
